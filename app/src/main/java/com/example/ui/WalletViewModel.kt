package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.BackupPackage
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.WalletRepository
import com.example.model.CategoryItem
import com.example.model.DebtWithPayments
import com.example.model.FinancialConstants
import com.example.model.IconLibrary
import com.example.model.toDebtRecord
import com.example.pdf.PdfExporter
import com.example.ui.components.DateFilterPreset
import com.example.ui.components.DateFilterUtils
import com.example.ui.components.MonthlyCalculationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface UserFeedback {
    data class Success(
        val message: String,
        val actionLabel: String? = null,
        val action: (() -> Unit)? = null
    ) : UserFeedback
    data class Error(val message: String) : UserFeedback
}

enum class DebtFilter {
    ALL, OWED_TO_ME, I_OWE
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository = AppDatabase.getDatabase(application).let { db ->
        WalletRepository(
            transactionDao = db.transactionDao(),
            debtDao = db.debtDao(),
            debtPaymentDao = db.debtPaymentDao(),
            database = db
        )
    }
    private val prefs = application.getSharedPreferences("nabih_wallet_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(prefs.getString("language", "ar") ?: "ar")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currency = MutableStateFlow(
        prefs.getString("currency_code", null)?.takeIf { it.isNotBlank() }
            ?: if ((prefs.getString("language", "ar") ?: "ar") == "ar") "ج.م" else "EGP"
    )
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("language", lang).apply()
        val saved = prefs.getString("currency_code", null)
        if (saved == null || saved == "ج.م" || saved == "EGP") {
            val newDefault = if (lang == "ar") "ج.م" else "EGP"
            _currency.value = newDefault
            prefs.edit().putString("currency_code", newDefault).apply()
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private val _categoryIcons = MutableStateFlow<Map<String, String>>(loadCategoryIcons())
    val categoryIcons: StateFlow<Map<String, String>> = _categoryIcons.asStateFlow()

    private val _customCategories = MutableStateFlow<List<CategoryItem>>(loadCustomCategories())
    val customCategories: StateFlow<List<CategoryItem>> = _customCategories.asStateFlow()

    private val _feedback = MutableSharedFlow<UserFeedback>()
    val feedback: SharedFlow<UserFeedback> = _feedback.asSharedFlow()

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf: StateFlow<Boolean> = _isGeneratingPdf.asStateFlow()

    private val _debtFilter = MutableStateFlow(DebtFilter.ALL)
    val debtFilter: StateFlow<DebtFilter> = _debtFilter.asStateFlow()

    // Date Range Filter State
    private val _filterStartDate = MutableStateFlow<Long>(DateFilterUtils.getDefaultStartDate(emptyList()))
    val filterStartDate: StateFlow<Long> = _filterStartDate.asStateFlow()

    private val _filterEndDate = MutableStateFlow<Long>(DateFilterUtils.getTodayEndOfDay())
    val filterEndDate: StateFlow<Long> = _filterEndDate.asStateFlow()

    private val _filterPreset = MutableStateFlow(DateFilterPreset.ALL)
    val filterPreset: StateFlow<DateFilterPreset> = _filterPreset.asStateFlow()

    // Backup & Restore State
    private val _lastBackupDate = MutableStateFlow<String?>(prefs.getString("last_backup_date", null))
    val lastBackupDate: StateFlow<String?> = _lastBackupDate.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    fun setDateFilter(start: Long, end: Long, preset: DateFilterPreset = DateFilterPreset.CUSTOM) {
        _filterStartDate.value = start
        _filterEndDate.value = end
        _filterPreset.value = preset
    }

    fun applyDatePreset(preset: DateFilterPreset) {
        val range = DateFilterUtils.calculatePresetRange(preset, allTransactions.value)
        _filterStartDate.value = range.first
        _filterEndDate.value = range.second
        _filterPreset.value = preset
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dateFilteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        filterStartDate,
        filterEndDate
    ) { list, start, end ->
        list.filter { tx ->
            tx.dateMillis in start..end
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebtsWithPayments: StateFlow<List<DebtWithPayments>> = repository.allDebtsWithPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDebts: StateFlow<List<DebtWithPayments>> = combine(
        allDebtsWithPayments,
        debtFilter
    ) { debts, filter ->
        when (filter) {
            DebtFilter.ALL -> debts
            DebtFilter.OWED_TO_ME -> debts.filter { it.debt.type == "OWED_TO_ME" }
            DebtFilter.I_OWE -> debts.filter { it.debt.type == "I_OWE" }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = allTransactions.combine(allTransactions) { list, _ ->
        list.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = allTransactions.combine(allTransactions) { list, _ ->
        list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpense) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalOwedToMe: StateFlow<Double> = allDebtsWithPayments.combine(allDebtsWithPayments) { list, _ ->
        list.filter { it.debt.type == "OWED_TO_ME" }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalIOwe: StateFlow<Double> = allDebtsWithPayments.combine(allDebtsWithPayments) { list, _ ->
        list.filter { it.debt.type == "I_OWE" }.sumOf { it.remainingAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val netDebtBalance: StateFlow<Double> = combine(totalOwedToMe, totalIOwe) { owed, owe ->
        owed - owe
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        FinancialConstants.setCustomIconOverrides(_categoryIcons.value)

        viewModelScope.launch(Dispatchers.IO) {
            consolidateSameMonthCategories()
        }

        // Sync default start/end date when transactions change if preset is ALL
        viewModelScope.launch {
            repository.allTransactions.collect { txs ->
                if (_filterPreset.value == DateFilterPreset.ALL && txs.isNotEmpty()) {
                    _filterStartDate.value = DateFilterUtils.getDefaultStartDate(txs)
                    val maxTxDate = txs.maxOfOrNull { it.dateMillis } ?: System.currentTimeMillis()
                    val todayEnd = DateFilterUtils.getTodayEndOfDay()
                    _filterEndDate.value = maxOf(todayEnd, DateFilterUtils.getEndOfDay(maxTxDate))
                }
            }
        }
    }

    /**
     * Automatically merges duplicate transactions of the same type and category within the same month,
     * and clears any legacy endDateMillis ranges.
     */
    private suspend fun consolidateSameMonthCategories() {
        val all = repository.getAllTransactionsSync()
        if (all.isEmpty()) return

        val grouped = all.groupBy { tx ->
            val normCat = FinancialConstants.normalizeCategoryName(tx.category)
            val ym = MonthlyCalculationUtils.getYearMonth(tx.dateMillis)
            Triple(tx.type, normCat, ym)
        }

        for ((key, group) in grouped) {
            val normCat = key.second
            if (group.size > 1) {
                val primary = group.maxByOrNull { it.dateMillis } ?: group.first()
                val totalAmount = group.sumOf { it.amount }
                val latestDate = group.maxOf { it.dateMillis }
                val mergedNotes = group.map { it.notes.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" | ")
                val anyPinned = group.any { it.isPinned }

                val updatedPrimary = primary.copy(
                    category = normCat,
                    amount = totalAmount,
                    dateMillis = latestDate,
                    endDateMillis = null,
                    notes = mergedNotes,
                    isPinned = anyPinned
                )
                repository.updateTransaction(updatedPrimary)

                for (dup in group) {
                    if (dup.id != primary.id) {
                        repository.deleteTransactionById(dup.id)
                    }
                }
            } else {
                val single = group.first()
                if (single.category != normCat || single.endDateMillis != null) {
                    repository.updateTransaction(
                        single.copy(
                            category = normCat,
                            endDateMillis = null
                        )
                    )
                }
            }
        }
    }

    fun setDebtFilter(filter: DebtFilter) {
        _debtFilter.value = filter
    }

    fun setCurrency(newCurrency: String) {
        val trimmed = newCurrency.trim()
        val finalCurrency = if (trimmed.isBlank()) {
            if (_language.value == "ar") "ج.م" else "EGP"
        } else {
            trimmed
        }
        _currency.value = finalCurrency
        prefs.edit().putString("currency_code", finalCurrency).apply()
        viewModelScope.launch {
            val msg = if (_language.value == "ar") "تم حفظ العملة: $finalCurrency" else "Currency saved: $finalCurrency"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun getCurrencySymbol(): String {
        val current = _currency.value.trim()
        if (current.isNotBlank()) return current
        val saved = prefs.getString("currency_code", null)?.trim()
        if (!saved.isNullOrBlank()) {
            _currency.value = saved
            return saved
        }
        val defaultCurrency = if (_language.value == "ar") "ج.م" else "EGP"
        _currency.value = defaultCurrency
        return defaultCurrency
    }

    fun saveTransaction(
        id: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        dateMillis: Long,
        endDateMillis: Long? = null,
        notes: String,
        isPinned: Boolean,
        receiptUri: String?,
        receiptMimeType: String?
    ) {
        viewModelScope.launch {
            val normalizedCat = FinancialConstants.normalizeCategoryName(category)
            val allExisting = withContext(Dispatchers.IO) { repository.getAllTransactionsSync() }

            if (id > 0) {
                // Editing an existing transaction: check if another transaction with the same type & category exists in the target month
                val sameMonthOthers = allExisting.filter { tx ->
                    tx.id != id &&
                        tx.type == type &&
                        FinancialConstants.normalizeCategoryName(tx.category) == normalizedCat &&
                        MonthlyCalculationUtils.isSameMonth(tx.dateMillis, dateMillis)
                }

                if (sameMonthOthers.isNotEmpty()) {
                    val primary = sameMonthOthers.first()
                    val mergedAmount = sameMonthOthers.sumOf { it.amount } + amount
                    val mergedNotes = (sameMonthOthers.map { it.notes.trim() } + notes.trim())
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" | ")
                    val anyPinned = isPinned || sameMonthOthers.any { it.isPinned }

                    withContext(Dispatchers.IO) {
                        repository.updateTransaction(
                            primary.copy(
                                category = normalizedCat,
                                amount = mergedAmount,
                                dateMillis = dateMillis,
                                endDateMillis = null,
                                notes = mergedNotes,
                                isPinned = anyPinned
                            )
                        )
                        repository.deleteTransactionById(id)
                        for (extra in sameMonthOthers.drop(1)) {
                            repository.deleteTransactionById(extra.id)
                        }
                    }
                    val msg = if (_language.value == "ar") {
                        "تم تحديث العملية ودمجها مع فئة \"$normalizedCat\" لنفس الشهر"
                    } else {
                        "Transaction updated and merged with \"$normalizedCat\" for the same month"
                    }
                    _feedback.emit(UserFeedback.Success(msg))
                } else {
                    val entity = TransactionEntity(
                        id = id,
                        type = type,
                        amount = amount,
                        category = normalizedCat,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        endDateMillis = null,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = null,
                        receiptMimeType = null
                    )
                    withContext(Dispatchers.IO) {
                        repository.updateTransaction(entity)
                    }
                    val msg = if (_language.value == "ar") "تم تعديل العملية بنجاح" else "Transaction updated successfully"
                    _feedback.emit(UserFeedback.Success(msg))
                }
            } else {
                // Adding a NEW transaction: check if the same category & type already exists in the same month
                val sameMonthMatches = allExisting.filter { tx ->
                    tx.type == type &&
                        FinancialConstants.normalizeCategoryName(tx.category) == normalizedCat &&
                        MonthlyCalculationUtils.isSameMonth(tx.dateMillis, dateMillis)
                }

                if (sameMonthMatches.isNotEmpty()) {
                    val primary = sameMonthMatches.first()
                    val mergedAmount = sameMonthMatches.sumOf { it.amount } + amount
                    val latestDate = maxOf(primary.dateMillis, dateMillis)
                    val mergedNotes = (sameMonthMatches.map { it.notes.trim() } + notes.trim())
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" | ")
                    val anyPinned = isPinned || sameMonthMatches.any { it.isPinned }

                    withContext(Dispatchers.IO) {
                        repository.updateTransaction(
                            primary.copy(
                                category = normalizedCat,
                                amount = mergedAmount,
                                dateMillis = latestDate,
                                endDateMillis = null,
                                notes = mergedNotes,
                                isPinned = anyPinned
                            )
                        )
                        for (extra in sameMonthMatches.drop(1)) {
                            repository.deleteTransactionById(extra.id)
                        }
                    }
                    val msg = if (_language.value == "ar") {
                        "تمت إضافة المبلغ ودمجه تلقائياً مع فئة \"$normalizedCat\" لهذا الشهر"
                    } else {
                        "Amount added and merged with \"$normalizedCat\" for this month"
                    }
                    _feedback.emit(UserFeedback.Success(msg))
                } else {
                    val entity = TransactionEntity(
                        id = 0,
                        type = type,
                        amount = amount,
                        category = normalizedCat,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        endDateMillis = null,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = null,
                        receiptMimeType = null
                    )
                    withContext(Dispatchers.IO) {
                        repository.insertTransaction(entity)
                    }
                    val msg = if (_language.value == "ar") "تم حفظ العملية بنجاح" else "Transaction saved successfully"
                    _feedback.emit(UserFeedback.Success(msg))
                }
            }
        }
    }

    fun updateTransactionDate(transaction: TransactionEntity, newDateMillis: Long) {
        viewModelScope.launch {
            val normalizedCat = FinancialConstants.normalizeCategoryName(transaction.category)
            val allExisting = withContext(Dispatchers.IO) { repository.getAllTransactionsSync() }

            val sameMonthOthers = allExisting.filter { tx ->
                tx.id != transaction.id &&
                    tx.type == transaction.type &&
                    FinancialConstants.normalizeCategoryName(tx.category) == normalizedCat &&
                    MonthlyCalculationUtils.isSameMonth(tx.dateMillis, newDateMillis)
            }

            if (sameMonthOthers.isNotEmpty()) {
                val primary = sameMonthOthers.first()
                val mergedAmount = sameMonthOthers.sumOf { it.amount } + transaction.amount
                val mergedNotes = (sameMonthOthers.map { it.notes.trim() } + transaction.notes.trim())
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" | ")
                val anyPinned = transaction.isPinned || sameMonthOthers.any { it.isPinned }

                withContext(Dispatchers.IO) {
                    repository.updateTransaction(
                        primary.copy(
                            category = normalizedCat,
                            amount = mergedAmount,
                            dateMillis = newDateMillis,
                            endDateMillis = null,
                            notes = mergedNotes,
                            isPinned = anyPinned
                        )
                    )
                    repository.deleteTransactionById(transaction.id)
                    for (extra in sameMonthOthers.drop(1)) {
                        repository.deleteTransactionById(extra.id)
                    }
                }
                val msg = if (_language.value == "ar") {
                    "تم تغيير التاريخ ودمج العملية مع فئة \"$normalizedCat\" لنفس الشهر"
                } else {
                    "Date updated and merged with \"$normalizedCat\" in the target month"
                }
                _feedback.emit(UserFeedback.Success(msg))
            } else {
                val updated = transaction.copy(
                    category = normalizedCat,
                    dateMillis = newDateMillis,
                    endDateMillis = null
                )
                withContext(Dispatchers.IO) {
                    repository.updateTransaction(updated)
                }
                val msg = if (_language.value == "ar") "تم تحديث تاريخ العملية بنجاح" else "Transaction date updated successfully"
                _feedback.emit(UserFeedback.Success(msg))
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTransaction(transaction)
            }
            _feedback.emit(UserFeedback.Success("تم حذف العملية بنجاح"))
        }
    }

    fun togglePin(transaction: TransactionEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.togglePinTransaction(transaction)
            }
        }
    }

    fun saveDebt(
        id: Long,
        type: String,
        personName: String,
        totalAmount: Double,
        lentDateMillis: Long,
        dueDateMillis: Long?,
        notes: String,
        enableReminder: Boolean
    ) {
        viewModelScope.launch {
            val entity = DebtEntity(
                id = if (id > 0) id else 0,
                type = type,
                personName = personName,
                totalAmount = totalAmount,
                lentDateMillis = lentDateMillis,
                dueDateMillis = dueDateMillis,
                notes = notes,
                enableReminder = enableReminder
            )
            withContext(Dispatchers.IO) {
                if (id > 0) {
                    repository.updateDebt(entity)
                } else {
                    repository.insertDebt(entity)
                }
            }
            if (id > 0) {
                _feedback.emit(UserFeedback.Success("تم تعديل بيانات الدين بنجاح"))
            } else {
                _feedback.emit(UserFeedback.Success("تم تسجيل الدين بنجاح"))
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteDebt(debt)
            }
            _feedback.emit(UserFeedback.Success("تم حذف الدين بنجاح"))
        }
    }

    fun logDebtPayment(debtId: Long, amount: Double, note: String, paymentDate: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val payment = DebtPaymentEntity(
                debtId = debtId,
                amount = amount,
                paymentDateMillis = paymentDate,
                note = note
            )
            withContext(Dispatchers.IO) {
                repository.addDebtPayment(payment)
            }
            _feedback.emit(UserFeedback.Success("تم تسجيل دفعة السداد بنجاح"))
        }
    }

    fun deleteDebtPayment(payment: DebtPaymentEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteDebtPayment(payment)
            }
            _feedback.emit(UserFeedback.Success("تم حذف الدفعة بنجاح"))
        }
    }

    fun exportExpensesPdfToDownloads(
        context: Context,
        periodTitle: String,
        customTransactions: List<TransactionEntity>? = null,
        startDateStr: String = "",
        endDateStr: String = ""
    ) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                if (_filterStartDate.value > _filterEndDate.value) {
                    _feedback.emit(UserFeedback.Error(if (_language.value == "ar") "تنبيه: تاريخ البداية لا يمكن أن يكون بعد تاريخ النهاية" else "Start date cannot be after end date"))
                    return@launch
                }
                val rawTransactions = customTransactions ?: dateFilteredTransactions.value
                val transactions = rawTransactions.map { tx ->
                    val allocated = MonthlyCalculationUtils.calculateTransactionAmountForRange(
                        tx, _filterStartDate.value, _filterEndDate.value
                    )
                    tx.copy(amount = allocated)
                }.filter { it.amount > 0.0 }
                if (transactions.isEmpty()) {
                    _feedback.emit(UserFeedback.Error(if (_language.value == "ar") "لا توجد معاملات في نطاق التاريخ المحدد للتصدير" else "No transactions in the selected date range"))
                    return@launch
                }
                val currencySymbol = getCurrencySymbol()
                val result = withContext(Dispatchers.IO) {
                    PdfExporter.exportPdfReport(
                        context = context,
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        periodName = periodTitle,
                        startDateStr = startDateStr,
                        endDateStr = endDateStr,
                        share = false
                    )
                }
                when (result) {
                    is PdfExporter.SaveResult.Success -> {
                        val msg = if (_language.value == "ar") {
                            "تم حفظ التقرير في مجلد التنزيلات بنجاح: ${result.fileName}"
                        } else {
                            "Report saved to Downloads: ${result.fileName}"
                        }
                        val actionLabel = if (_language.value == "ar") "فتح" else "Open"
                        _feedback.emit(
                            UserFeedback.Success(
                                message = msg,
                                actionLabel = actionLabel,
                                action = { PdfExporter.openPdf(context, result.uri, result.file) }
                            )
                        )
                    }
                    is PdfExporter.SaveResult.Failure -> {
                        _feedback.emit(UserFeedback.Error(result.errorMessage))
                    }
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "حدث خطأ أثناء تصدير التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun shareExpensesPdf(
        context: Context,
        periodTitle: String,
        customTransactions: List<TransactionEntity>? = null,
        startDateStr: String = "",
        endDateStr: String = ""
    ) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                if (_filterStartDate.value > _filterEndDate.value) {
                    _feedback.emit(UserFeedback.Error(if (_language.value == "ar") "تنبيه: تاريخ البداية لا يمكن أن يكون بعد تاريخ النهاية" else "Start date cannot be after end date"))
                    return@launch
                }
                val rawTransactions = customTransactions ?: dateFilteredTransactions.value
                val transactions = rawTransactions.map { tx ->
                    val allocated = MonthlyCalculationUtils.calculateTransactionAmountForRange(
                        tx, _filterStartDate.value, _filterEndDate.value
                    )
                    tx.copy(amount = allocated)
                }.filter { it.amount > 0.0 }
                if (transactions.isEmpty()) {
                    _feedback.emit(UserFeedback.Error(if (_language.value == "ar") "لا توجد معاملات في نطاق التاريخ المحدد للتصدير" else "No transactions in the selected date range"))
                    return@launch
                }
                val currencySymbol = getCurrencySymbol()
                val result = withContext(Dispatchers.IO) {
                    PdfExporter.exportPdfReport(
                        context = context,
                        transactions = transactions,
                        currencySymbol = currencySymbol,
                        periodName = periodTitle,
                        startDateStr = startDateStr,
                        endDateStr = endDateStr,
                        share = true
                    )
                }
                if (result is PdfExporter.SaveResult.Failure) {
                    _feedback.emit(UserFeedback.Error(result.errorMessage))
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل في مشاركة التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun exportDebtsPdf(context: Context, share: Boolean = false) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val debts = allDebtsWithPayments.value.map { it.toDebtRecord() }
                if (debts.isEmpty()) {
                    _feedback.emit(UserFeedback.Error(if (_language.value == "ar") "لا توجد ديون مسجلة لتصدير التقرير" else "No debts recorded to export"))
                    return@launch
                }
                val currencySymbol = getCurrencySymbol()
                val result = withContext(Dispatchers.IO) {
                    PdfExporter.exportDebtsPdfReport(
                        context = context,
                        debts = debts,
                        currencySymbol = currencySymbol,
                        share = share
                    )
                }
                if (!share) {
                    when (result) {
                        is PdfExporter.SaveResult.Success -> {
                            val msg = if (_language.value == "ar") {
                                "تم حفظ تقرير الديون في مجلد التنزيلات: ${result.fileName}"
                            } else {
                                "Debts report saved to Downloads: ${result.fileName}"
                            }
                            val actionLabel = if (_language.value == "ar") "فتح" else "Open"
                            _feedback.emit(
                                UserFeedback.Success(
                                    message = msg,
                                    actionLabel = actionLabel,
                                    action = { PdfExporter.openPdf(context, result.uri, result.file) }
                                )
                            )
                        }
                        is PdfExporter.SaveResult.Failure -> {
                            _feedback.emit(UserFeedback.Error(result.errorMessage))
                        }
                    }
                } else if (result is PdfExporter.SaveResult.Failure) {
                    _feedback.emit(UserFeedback.Error(result.errorMessage))
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "حدث خطأ أثناء تصدير تقرير الديون"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val appContext = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    val transactions = repository.getAllTransactionsSync()
                    val debts = repository.getAllDebtsSync()
                    val payments = repository.getAllDebtPaymentsSync()
                    val customCats = _customCategories.value
                    val catIcons = _categoryIcons.value

                    BackupManager.exportBackup(
                        context = appContext,
                        transactions = transactions,
                        debts = debts,
                        debtPayments = payments,
                        customCategories = customCats,
                        categoryIcons = catIcons
                    )
                }

                result.fold(
                    onSuccess = { exportResult ->
                        val dateFormatted = DateFilterUtils.formatDateArabic(System.currentTimeMillis())
                        _lastBackupDate.value = dateFormatted
                        prefs.edit().putString("last_backup_date", dateFormatted).apply()
                        val msg = if (_language.value == "ar") {
                            "تم تصدير النسخة الاحتياطية بنجاح: ${exportResult.fileName}"
                        } else {
                            "Backup exported successfully: ${exportResult.fileName}"
                        }
                        val shareLabel = if (_language.value == "ar") "مشاركة" else "Share"
                        _feedback.emit(
                            UserFeedback.Success(
                                message = msg,
                                actionLabel = shareLabel,
                                action = { BackupManager.shareBackupFile(context, exportResult.localFile) }
                            )
                        )
                    },
                    onFailure = { error ->
                        val msg = error.localizedMessage ?: "حدث خطأ أثناء تصدير النسخة الاحتياطية"
                        _feedback.emit(UserFeedback.Error(msg))
                    }
                )
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل تصدير النسخة الاحتياطية"))
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val appContext = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    val transactions = repository.getAllTransactionsSync()
                    val debts = repository.getAllDebtsSync()
                    val payments = repository.getAllDebtPaymentsSync()
                    val customCats = _customCategories.value
                    val catIcons = _categoryIcons.value

                    BackupManager.exportBackupToUri(
                        context = appContext,
                        targetUri = uri,
                        transactions = transactions,
                        debts = debts,
                        debtPayments = payments,
                        customCategories = customCats,
                        categoryIcons = catIcons
                    )
                }
                result.fold(
                    onSuccess = {
                        val dateFormatted = DateFilterUtils.formatDateArabic(System.currentTimeMillis())
                        _lastBackupDate.value = dateFormatted
                        prefs.edit().putString("last_backup_date", dateFormatted).apply()
                        val msg = if (_language.value == "ar") {
                            "تم حفظ ملف النسخة الاحتياطية بنجاح"
                        } else {
                            "Backup file saved successfully"
                        }
                        _feedback.emit(UserFeedback.Success(msg))
                    },
                    onFailure = { error ->
                        val msg = error.localizedMessage ?: "حدث خطأ أثناء حفظ النسخة الاحتياطية"
                        _feedback.emit(UserFeedback.Error(msg))
                    }
                )
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل حفظ النسخة الاحتياطية"))
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun validateBackupFileAsync(
        uri: Uri,
        onSuccess: (BackupPackage) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val appContext = getApplication<Application>()
                val result = withContext(Dispatchers.IO) {
                    BackupManager.parseAndValidateBackup(appContext, uri)
                }
                result.fold(
                    onSuccess = { pkg ->
                        onSuccess(pkg)
                    },
                    onFailure = { err ->
                        val msg = err.localizedMessage
                            ?: if (_language.value == "ar") "ملف النسخة الاحتياطية غير صالح أو تالف" else "Invalid backup file"
                        onFailure(msg)
                    }
                )
            } catch (e: Exception) {
                val msg = e.localizedMessage
                    ?: if (_language.value == "ar") "ملف النسخة الاحتياطية غير صالح" else "Invalid backup file"
                onFailure(msg)
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun restoreBackup(context: Context, backupPackage: BackupPackage) {
        viewModelScope.launch {
            _isRestoring.value = true
            try {
                // Restore custom categories & icons first so UI maps them immediately
                _customCategories.value = backupPackage.customCategories
                saveCustomCategories(backupPackage.customCategories)

                _categoryIcons.value = backupPackage.categoryIcons
                saveCategoryIcons(backupPackage.categoryIcons)
                FinancialConstants.setCustomIconOverrides(backupPackage.categoryIcons)

                withContext(Dispatchers.IO) {
                    repository.restoreAllData(
                        transactions = backupPackage.transactions,
                        debts = backupPackage.debts,
                        payments = backupPackage.debtPayments
                    )
                    consolidateSameMonthCategories()
                }

                // Update date filter range so restored transactions are immediately visible
                if (backupPackage.transactions.isNotEmpty()) {
                    val range = DateFilterUtils.calculatePresetRange(_filterPreset.value, backupPackage.transactions)
                    _filterStartDate.value = range.first
                    val maxTxDate = backupPackage.transactions.maxOfOrNull { it.dateMillis } ?: range.second
                    _filterEndDate.value = maxOf(range.second, DateFilterUtils.getEndOfDay(maxTxDate))
                }

                val msg = if (_language.value == "ar") {
                    "تمت استعادة البيانات بنجاح: ${backupPackage.transactions.size} معاملة، ${backupPackage.debts.size} ديون"
                } else {
                    "Data restored successfully: ${backupPackage.transactions.size} transactions, ${backupPackage.debts.size} debts"
                }
                _feedback.emit(UserFeedback.Success(msg))
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error("فشلت استعادة البيانات: ${e.localizedMessage}"))
            } finally {
                _isRestoring.value = false
            }
        }
    }

    fun exportDebtsPdfToDownloads(context: Context) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val debts = allDebtsWithPayments.value.map { it.toDebtRecord() }
                val result = withContext(Dispatchers.IO) {
                    PdfExporter.exportDebtsPdfReport(
                        context = context,
                        debts = debts,
                        currencySymbol = getCurrencySymbol(),
                        share = false
                    )
                }
                when (result) {
                    is PdfExporter.SaveResult.Success -> {
                        val msg = if (_language.value == "ar") {
                            "تم حفظ تقرير الديون في مجلد التنزيلات بنجاح: ${result.fileName}"
                        } else {
                            "Debts report saved to Downloads: ${result.fileName}"
                        }
                        val actionLabel = if (_language.value == "ar") "فتح" else "Open"
                        _feedback.emit(
                            UserFeedback.Success(
                                message = msg,
                                actionLabel = actionLabel,
                                action = { PdfExporter.openPdf(context, result.uri, result.file) }
                            )
                        )
                    }
                    is PdfExporter.SaveResult.Failure -> {
                        _feedback.emit(UserFeedback.Error(result.errorMessage))
                    }
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "حدث خطأ أثناء تصدير تقرير الديون"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun shareDebtsPdf(context: Context) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val debts = allDebtsWithPayments.value.map { it.toDebtRecord() }
                val result = withContext(Dispatchers.IO) {
                    PdfExporter.exportDebtsPdfReport(
                        context = context,
                        debts = debts,
                        currencySymbol = getCurrencySymbol(),
                        share = true
                    )
                }
                if (result is PdfExporter.SaveResult.Failure) {
                    _feedback.emit(UserFeedback.Error(result.errorMessage))
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل في مشاركة التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
            _customCategories.value = emptyList()
            _categoryIcons.value = emptyMap()
            prefs.edit()
                .remove("custom_categories_list")
                .remove("custom_category_icons")
                .apply()
            FinancialConstants.setCustomIconOverrides(emptyMap())
            val msg = if (_language.value == "ar") "تم مسح وتصفير كافة البيانات بنجاح" else "All data reset successfully"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun setCategoryIcon(categoryNameOrId: String, iconKey: String) {
        val trimmed = categoryNameOrId.trim()
        val normalized = FinancialConstants.normalizeCategoryName(trimmed)
        val updated = _categoryIcons.value.toMutableMap()
        updated[trimmed] = iconKey
        updated[normalized] = iconKey
        _categoryIcons.value = updated
        saveCategoryIcons(updated)
        FinancialConstants.setCustomIconOverrides(updated)
        viewModelScope.launch {
            val msg = if (_language.value == "ar") "تم تحديث أيقونة الفئة بنجاح" else "Category icon updated successfully"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun resetCategoryIcons() {
        _categoryIcons.value = emptyMap()
        prefs.edit().remove("custom_category_icons").apply()
        FinancialConstants.setCustomIconOverrides(emptyMap())
        viewModelScope.launch {
            val msg = if (_language.value == "ar") "تمت استعادة الأيقونات الافتراضية" else "Reset to default icons"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun addCustomCategory(nameAr: String, nameEn: String, type: String, iconKey: String, colorHex: Long = 0xFFD97757) {
        val prefix = if (type == "EXPENSE") "custom_exp_" else "custom_inc_"
        val id = "$prefix${System.currentTimeMillis()}"
        val icon = IconLibrary.getIconByKey(iconKey)
        val item = CategoryItem(
            id = id,
            nameAr = nameAr.trim(),
            nameEn = nameEn.trim().ifBlank { nameAr.trim() },
            icon = icon,
            iconKey = iconKey,
            color = colorHex,
            type = type,
            descriptionAr = if (type == "EXPENSE") "فئة مخصصة للمصروفات" else "فئة مخصصة للإيرادات",
            descriptionEn = if (type == "EXPENSE") "Custom Expense Category" else "Custom Income Category"
        )
        val list = _customCategories.value + item
        _customCategories.value = list
        saveCustomCategories(list)

        setCategoryIcon(nameAr.trim(), iconKey)
        setCategoryIcon(id, iconKey)
        viewModelScope.launch {
            val msg = if (_language.value == "ar") "تمت إضافة الفئة الجديدة بنجاح" else "New category added successfully"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        val list = _customCategories.value.filterNot { it.id == categoryId }
        _customCategories.value = list
        saveCustomCategories(list)
        viewModelScope.launch {
            val msg = if (_language.value == "ar") "تم حذف الفئة المخصصة" else "Custom category deleted"
            _feedback.emit(UserFeedback.Success(msg))
        }
    }

    fun getAllExpenseCategories(): List<CategoryItem> {
        val custom = _customCategories.value.filter { it.type == "EXPENSE" || it.id.startsWith("custom_exp_") }
        val base = FinancialConstants.expenseCategories.map { cat ->
            val customKey = _categoryIcons.value[cat.nameAr] ?: _categoryIcons.value[cat.id]
            if (customKey != null) {
                cat.copy(icon = IconLibrary.getIconByKey(customKey), iconKey = customKey)
            } else {
                cat
            }
        }
        return base + custom
    }

    fun getAllIncomeCategories(): List<CategoryItem> {
        val custom = _customCategories.value.filter { it.type == "INCOME" || it.id.startsWith("custom_inc_") }
        val base = FinancialConstants.incomeCategories.map { cat ->
            val customKey = _categoryIcons.value[cat.nameAr] ?: _categoryIcons.value[cat.id]
            if (customKey != null) {
                cat.copy(icon = IconLibrary.getIconByKey(customKey), iconKey = customKey)
            } else {
                cat
            }
        }
        return base + custom
    }

    private fun loadCategoryIcons(): Map<String, String> {
        val jsonStr = prefs.getString("custom_category_icons", null) ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.getString(k)
            }
        } catch (e: Exception) {
            // ignore
        }
        return map
    }

    private fun saveCategoryIcons(map: Map<String, String>) {
        try {
            val obj = JSONObject()
            for ((k, v) in map) {
                obj.put(k, v)
            }
            prefs.edit().putString("custom_category_icons", obj.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun loadCustomCategories(): List<CategoryItem> {
        val jsonStr = prefs.getString("custom_categories_list", null) ?: return emptyList()
        val list = mutableListOf<CategoryItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val itemObj = arr.getJSONObject(i)
                val id = itemObj.getString("id")
                val nameAr = itemObj.getString("nameAr")
                val nameEn = itemObj.getString("nameEn")
                val iconKey = itemObj.optString("iconKey", "tag")
                val color = itemObj.optLong("color", 0xFFD97757)
                val type = itemObj.optString("type", if (id.startsWith("custom_inc_")) "INCOME" else "EXPENSE")
                val descAr = itemObj.optString("descriptionAr", "")
                val descEn = itemObj.optString("descriptionEn", "")
                list.add(
                    CategoryItem(
                        id = id,
                        nameAr = nameAr,
                        nameEn = nameEn,
                        icon = IconLibrary.getIconByKey(iconKey),
                        iconKey = iconKey,
                        color = color,
                        type = type,
                        descriptionAr = descAr,
                        descriptionEn = descEn
                    )
                )
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    private fun saveCustomCategories(list: List<CategoryItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("nameAr", item.nameAr)
                    put("nameEn", item.nameEn)
                    put("iconKey", item.iconKey)
                    put("color", item.color)
                    put("type", item.type)
                    put("descriptionAr", item.descriptionAr)
                    put("descriptionEn", item.descriptionEn)
                }
                arr.put(obj)
            }
            prefs.edit().putString("custom_categories_list", arr.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }
}
