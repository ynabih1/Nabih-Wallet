package com.nabih.wallet.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nabih.wallet.data.backup.BackupManager
import com.nabih.wallet.data.backup.BackupRestoreResult
import com.nabih.wallet.data.export.FileSaver
import com.nabih.wallet.data.local.database.AppDatabase
import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.CategoryExpenseSummary
import com.nabih.wallet.data.model.DayActivityPoint
import com.nabih.wallet.data.model.DebtSummary
import com.nabih.wallet.data.model.DebtWithPayments
import com.nabih.wallet.data.model.FinancialCalculator
import com.nabih.wallet.data.model.MonthExpenseSummary
import com.nabih.wallet.data.model.MonthlyComparison
import com.nabih.wallet.data.model.MonthComparisonTrend
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.data.model.YearMonthKey
import com.nabih.wallet.data.preferences.AppPreferences
import com.nabih.wallet.data.preferences.ColorPalette
import com.nabih.wallet.data.preferences.ThemeMode
import com.nabih.wallet.data.preferences.UserPreferences
import com.nabih.wallet.data.repository.PaymentResult
import com.nabih.wallet.data.repository.WalletRepository
import com.nabih.wallet.notification.NotificationHelper
import com.nabih.wallet.pdf.PdfExpenseReportData
import com.nabih.wallet.pdf.PdfExporter
import com.nabih.wallet.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class WalletUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val expenseCategories: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionItem> = emptyList(),
    val recentTransactions: List<TransactionItem> = emptyList(),
    val netBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val monthlyComparison: MonthlyComparison = MonthlyComparison(0.0, 0.0, 0, MonthComparisonTrend.NO_PREVIOUS_DATA),
    val last7DaysActivity: List<DayActivityPoint> = emptyList(),
    val selectedReportMonth: YearMonthKey? = null,
    val reportTotalExpense: Double = 0.0,
    val expensesByCategory: List<CategoryExpenseSummary> = emptyList(),
    val monthlyHistory: List<MonthExpenseSummary> = emptyList(),
    val debts: List<DebtWithPayments> = emptyList(),
    val debtSummary: DebtSummary = DebtSummary(0.0, 0.0, 0.0, 0.0, false),
    val preferences: UserPreferences = UserPreferences()
)

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = WalletRepository(database)
    private val backupManager = BackupManager(repository)
    private val appPreferences = AppPreferences(application)

    private val _selectedReportMonth = MutableStateFlow<YearMonthKey?>(null)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val uiState: StateFlow<WalletUiState> = combine(
        repository.categories,
        repository.transactions,
        repository.debtsWithPayments,
        appPreferences.preferences,
        _selectedReportMonth
    ) { categories, rawTransactions, debts, prefs, selectedMonth ->
        val categoriesById = categories.associateBy { it.id }
        val mappedTx = FinancialCalculator.mapTransactions(rawTransactions, categoriesById)
        val now = System.currentTimeMillis()
        val expensesByCat = FinancialCalculator.calculateExpensesByCategory(mappedTx, selectedMonth)
        val reportTotal = expensesByCat.sumOf { it.totalAmount }

        WalletUiState(
            categories = categories,
            expenseCategories = categories.filter { it.type == TransactionType.EXPENSE },
            incomeCategories = categories.filter { it.type == TransactionType.INCOME },
            transactions = mappedTx,
            recentTransactions = mappedTx.take(5),
            netBalance = FinancialCalculator.calculateNetBalance(rawTransactions),
            totalIncome = FinancialCalculator.calculateTotalIncome(rawTransactions),
            totalExpense = FinancialCalculator.calculateTotalExpense(rawTransactions),
            monthlyComparison = FinancialCalculator.calculateMonthlyComparison(rawTransactions, now),
            last7DaysActivity = FinancialCalculator.calculateLast7DaysActivity(rawTransactions, now),
            selectedReportMonth = selectedMonth,
            reportTotalExpense = reportTotal,
            expensesByCategory = expensesByCat,
            monthlyHistory = FinancialCalculator.calculateMonthlyHistory(rawTransactions),
            debts = debts,
            debtSummary = FinancialCalculator.calculateDebtSummary(debts),
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WalletUiState(preferences = appPreferences.preferences.value)
    )

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun postMessage(message: String) {
        _snackbarMessage.value = message
    }

    // --- Reports filter ---

    fun selectReportMonth(month: YearMonthKey?) {
        _selectedReportMonth.value = if (_selectedReportMonth.value == month) null else month
    }

    fun buildCurrentPdfReport(): PdfExpenseReportData {
        val state = uiState.value
        val periodLabel = state.selectedReportMonth?.let { Formatters.monthYear(it) } ?: "كل الفترات"
        return PdfExpenseReportData(
            periodLabel = periodLabel,
            exportedAtMillis = System.currentTimeMillis(),
            totalExpense = state.reportTotalExpense,
            categories = state.expensesByCategory
        )
    }

    fun exportAndSharePdf(onReadyToShare: (File) -> Unit) {
        val report = buildCurrentPdfReport()
        viewModelScope.launch {
            val fileName = "nabih_wallet_report_${System.currentTimeMillis()}.pdf"
            val result = PdfExporter.exportToCacheFile(getApplication(), report, fileName)
            result.onSuccess { file ->
                onReadyToShare(file)
            }.onFailure {
                _snackbarMessage.value = "تعذر إنشاء تقرير PDF"
            }
        }
    }

    fun savePdfToUri(uri: Uri) {
        val report = buildCurrentPdfReport()
        viewModelScope.launch {
            val result = PdfExporter.exportToUri(getApplication(), report, uri)
            _snackbarMessage.value = if (result.isSuccess) {
                "تم حفظ تقرير PDF بنجاح"
            } else {
                "تعذر حفظ تقرير PDF"
            }
        }
    }

    // --- Transactions ---

    fun saveTransaction(
        editingId: Long?,
        type: String,
        amount: Double,
        categoryId: Long,
        timestamp: Long,
        note: String
    ) {
        if (amount <= 0.0) {
            _snackbarMessage.value = "يرجى إدخال مبلغ صحيح أكبر من صفر"
            return
        }
        viewModelScope.launch {
            if (editingId == null) {
                repository.addTransaction(type, amount, categoryId, timestamp, note)
                _snackbarMessage.value = if (type == TransactionType.EXPENSE) {
                    "تمت إضافة المصروف"
                } else {
                    "تمت إضافة الدخل"
                }
            } else {
                repository.updateTransaction(editingId, type, amount, categoryId, timestamp, note)
                _snackbarMessage.value = "تم تحديث العملية"
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            _snackbarMessage.value = "تم حذف العملية"
        }
    }

    // --- Categories ---

    fun addCategory(
        name: String,
        type: String,
        iconKey: String,
        colorKey: String,
        onCreated: ((Long) -> Unit)? = null
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _snackbarMessage.value = "يرجى إدخال اسم الفئة"
            return
        }
        viewModelScope.launch {
            val id = repository.addCategory(trimmed, type, iconKey, colorKey)
            onCreated?.invoke(id)
            _snackbarMessage.value = "تمت إضافة فئة \"$trimmed\""
        }
    }

    fun updateCategory(category: CategoryEntity) {
        if (category.name.trim().isEmpty()) {
            _snackbarMessage.value = "يرجى إدخال اسم الفئة"
            return
        }
        viewModelScope.launch {
            repository.updateCategory(category)
            _snackbarMessage.value = "تم تحديث الفئة"
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _snackbarMessage.value = "تم حذف الفئة"
        }
    }

    // --- Debts ---

    fun saveDebt(
        editingId: Long?,
        personName: String,
        type: String,
        originalAmount: Double,
        onResult: (Boolean) -> Unit = {}
    ) {
        val trimmed = personName.trim()
        if (trimmed.isEmpty()) {
            _snackbarMessage.value = "يرجى إدخال اسم الشخص"
            onResult(false)
            return
        }
        if (originalAmount <= 0.0) {
            _snackbarMessage.value = "يرجى إدخال مبلغ أكبر من صفر"
            onResult(false)
            return
        }
        viewModelScope.launch {
            if (editingId == null) {
                repository.addDebt(trimmed, type, originalAmount)
                _snackbarMessage.value = "تمت إضافة الدين"
                onResult(true)
            } else {
                when (val res = repository.updateDebt(editingId, trimmed, type, originalAmount)) {
                    PaymentResult.Success -> {
                        _snackbarMessage.value = "تم تحديث الدين"
                        onResult(true)
                    }
                    is PaymentResult.Error -> {
                        _snackbarMessage.value = res.message
                        onResult(false)
                    }
                }
            }
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            repository.deleteDebt(debtId)
            _snackbarMessage.value = "تم حذف الدين"
        }
    }

    fun addDebtPayment(debtId: Long, amount: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repository.addDebtPayment(debtId, amount)) {
                PaymentResult.Success -> {
                    _snackbarMessage.value = "تم تسجيل الدفعة بنجاح"
                    onSuccess()
                }
                is PaymentResult.Error -> {
                    _snackbarMessage.value = result.message
                }
            }
        }
    }

    fun deleteDebtPayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deleteDebtPayment(paymentId)
            _snackbarMessage.value = "تم حذف الدفعة"
        }
    }

    // --- Preferences & Reminders ---

    fun setThemeMode(mode: ThemeMode) {
        appPreferences.setThemeMode(mode)
    }

    fun setDynamicColorsEnabled(enabled: Boolean) {
        appPreferences.setDynamicColorsEnabled(enabled)
    }

    fun setColorPalette(palette: ColorPalette) {
        appPreferences.setColorPalette(palette)
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        appPreferences.setDailyReminderEnabled(enabled)
        val prefs = appPreferences.preferences.value
        val ctx = getApplication<Application>()
        if (enabled) {
            NotificationHelper.scheduleDailyReminder(ctx, prefs.reminderHour, prefs.reminderMinute)
            _snackbarMessage.value = "تم تفعيل التذكير اليومي الساعة ${Formatters.formatTime12Hour(prefs.reminderHour, prefs.reminderMinute)}"
        } else {
            NotificationHelper.cancelDailyReminder(ctx)
            _snackbarMessage.value = "تم إيقاف التذكير اليومي"
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        appPreferences.setReminderTime(hour, minute)
        val ctx = getApplication<Application>()
        if (appPreferences.preferences.value.dailyReminderEnabled) {
            NotificationHelper.scheduleDailyReminder(ctx, hour, minute)
        }
        _snackbarMessage.value = "تم ضبط وقت التذكير على ${Formatters.formatTime12Hour(hour, minute)}"
    }

    // --- Backup & Restore ---

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = backupManager.createBackupJson()
                val writeResult = FileSaver.writeTextToUri(getApplication(), uri, json)
                _snackbarMessage.value = if (writeResult.isSuccess) {
                    "تم تصدير النسخة الاحتياطية بنجاح"
                } else {
                    "تعذر حفظ النسخة الاحتياطية"
                }
            } catch (_: Exception) {
                _snackbarMessage.value = "حدث خطأ أثناء إنشاء النسخة الاحتياطية"
            }
        }
    }

    fun restoreBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val readResult = FileSaver.readTextFromUri(getApplication(), uri)
            readResult.onSuccess { rawJson ->
                when (val result = backupManager.restoreFromJson(rawJson)) {
                    is BackupRestoreResult.Success -> {
                        _selectedReportMonth.value = null
                        _snackbarMessage.value =
                            "تمت الاستعادة بنجاح (${result.transactionsCount} عملية، ${result.debtsCount} دين)"
                    }
                    is BackupRestoreResult.Error -> {
                        _snackbarMessage.value = result.message
                    }
                }
            }.onFailure {
                _snackbarMessage.value = "تعذر قراءة ملف النسخة الاحتياطية"
            }
        }
    }
}
