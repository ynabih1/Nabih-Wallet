package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.AppDatabase
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.WalletRepository
import com.example.model.CategoryItem
import com.example.model.DebtWithPayments
import com.example.model.FinancialConstants
import com.example.model.IconLibrary
import com.example.model.MonthlyBudgetStatus
import com.example.model.NotificationSeverity
import com.example.model.SmartNotificationItem
import com.example.notification.NotificationHelper
import com.example.data.export.ExportUtils
import com.example.data.export.toDebtRecord
import com.example.pdf.PdfExporter
import com.example.pdf.PdfReportGenerator
import com.example.ui.components.Formatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

sealed interface UserFeedback {
    data class Success(val message: String) : UserFeedback
    data class Error(val message: String) : UserFeedback
}

enum class DebtFilter {
    ALL, OWED_TO_ME, I_OWE
}

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository
    private val prefs = application.getSharedPreferences("nabih_wallet_prefs", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow("")
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString("language", "ar") ?: "ar")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget_amount", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _isBudgetAlertEnabled = MutableStateFlow(prefs.getBoolean("budget_alert_enabled", true))
    val isBudgetAlertEnabled: StateFlow<Boolean> = _isBudgetAlertEnabled.asStateFlow()

    private val _isNotificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val isNotificationsEnabled: StateFlow<Boolean> = _isNotificationsEnabled.asStateFlow()

    private val _isDebtAlertEnabled = MutableStateFlow(prefs.getBoolean("debt_alert_enabled", true))
    val isDebtAlertEnabled: StateFlow<Boolean> = _isDebtAlertEnabled.asStateFlow()

    private val _isDailyReminderEnabled = MutableStateFlow(prefs.getBoolean("daily_reminder_enabled", true))
    val isDailyReminderEnabled: StateFlow<Boolean> = _isDailyReminderEnabled.asStateFlow()

    fun setLanguage(lang: String) {
        _language.value = lang
        prefs.edit().putString("language", lang).apply()
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _isNotificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun setDebtAlertEnabled(enabled: Boolean) {
        _isDebtAlertEnabled.value = enabled
        prefs.edit().putBoolean("debt_alert_enabled", enabled).apply()
    }

    fun setDailyReminderEnabled(enabled: Boolean) {
        _isDailyReminderEnabled.value = enabled
        prefs.edit().putBoolean("daily_reminder_enabled", enabled).apply()
    }

    fun sendTestNotification(context: Context) {
        val success = NotificationHelper.sendTestNotification(context, _language.value == "ar")
        viewModelScope.launch {
            if (success) {
                val msg = if (_language.value == "ar") "تم إرسال إشعار تجريبي بنجاح" else "Test notification sent successfully"
                _feedback.emit(UserFeedback.Success(msg))
            } else {
                val msg = if (_language.value == "ar") "يرجى منح إذن الإشعارات من إعدادات النظام" else "Please enable notification permissions in system settings"
                _feedback.emit(UserFeedback.Error(msg))
            }
        }
    }

    fun setMonthlyBudget(amount: Double) {
        val cleanAmount = if (amount < 0.0) 0.0 else amount
        _monthlyBudget.value = cleanAmount
        prefs.edit().putFloat("monthly_budget_amount", cleanAmount.toFloat()).apply()
        viewModelScope.launch {
            if (cleanAmount > 0.0) {
                val msg = if (_language.value == "ar") "تم حفظ الميزانية الشهرية بنجاح" else "Monthly budget saved successfully"
                _feedback.emit(UserFeedback.Success(msg))
            } else {
                val msg = if (_language.value == "ar") "تم إلغاء الميزانية الشهرية" else "Monthly budget removed"
                _feedback.emit(UserFeedback.Success(msg))
            }
        }
    }

    fun setBudgetAlertEnabled(enabled: Boolean) {
        _isBudgetAlertEnabled.value = enabled
        prefs.edit().putBoolean("budget_alert_enabled", enabled).apply()
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

    init {
        val db = AppDatabase.getDatabase(application)
        repository = WalletRepository(
            transactionDao = db.transactionDao(),
            debtDao = db.debtDao(),
            debtPaymentDao = db.debtPaymentDao()
        )
        FinancialConstants.setCustomIconOverrides(_categoryIcons.value)
        cleanDummyDataIfPresent()
        consolidateDuplicateCategories()
    }

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    val currentMonthExpenses: StateFlow<Double> = allTransactions.map { list ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        list.filter { tx ->
            if (tx.type != "EXPENSE") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyBudgetStatus: StateFlow<MonthlyBudgetStatus> = combine(
        monthlyBudget,
        currentMonthExpenses,
        isBudgetAlertEnabled
    ) { budget, spent, alertsEnabled ->
        if (budget <= 0.0) {
            MonthlyBudgetStatus(
                budgetAmount = 0.0,
                spentAmount = spent,
                remainingAmount = 0.0,
                percentage = 0f,
                isWarning = false,
                isExceeded = false,
                isSet = false,
                isAlertEnabled = alertsEnabled
            )
        } else {
            val remaining = (budget - spent).coerceAtLeast(0.0)
            val pct = (spent / budget).toFloat()
            val isExceeded = spent >= budget
            val isWarning = !isExceeded && pct >= 0.80f
            MonthlyBudgetStatus(
                budgetAmount = budget,
                spentAmount = spent,
                remainingAmount = remaining,
                percentage = pct,
                isWarning = isWarning,
                isExceeded = isExceeded,
                isSet = true,
                isAlertEnabled = alertsEnabled
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlyBudgetStatus())

    val smartNotifications: StateFlow<List<SmartNotificationItem>> = combine(
        monthlyBudgetStatus,
        allDebtsWithPayments,
        allTransactions,
        isNotificationsEnabled,
        isBudgetAlertEnabled,
        isDebtAlertEnabled,
        isDailyReminderEnabled
    ) { args: Array<Any> ->
        val budgetStatus = args[0] as MonthlyBudgetStatus
        @Suppress("UNCHECKED_CAST")
        val debts = args[1] as List<DebtWithPayments>
        @Suppress("UNCHECKED_CAST")
        val transactions = args[2] as List<TransactionEntity>
        val notificationsOn = args[3] as Boolean
        val budgetAlertsOn = args[4] as Boolean
        val debtAlertsOn = args[5] as Boolean
        val dailyReminderOn = args[6] as Boolean

        if (!notificationsOn) {
            emptyList()
        } else {
            val list = mutableListOf<SmartNotificationItem>()

            // 1. Budget Notifications
            if (budgetAlertsOn && budgetStatus.isSet) {
                val spentPct = (budgetStatus.percentage * 100).toInt()
                if (budgetStatus.isExceeded) {
                    val exceeded = budgetStatus.spentAmount - budgetStatus.budgetAmount
                    list.add(
                        SmartNotificationItem(
                            id = "budget_exceeded",
                            titleAr = "🚨 تجاوزت الميزانية الشهرية",
                            titleEn = "🚨 Monthly Budget Exceeded",
                            messageAr = "لقد تجاوزت ميزانيتك المحددة بمقدار ${Formatters.formatMoney(exceeded)}. يرجى مراجعة المصروفات.",
                            messageEn = "You exceeded your set budget by ${Formatters.formatMoney(exceeded)}. Please review spending.",
                            severity = NotificationSeverity.ALERT,
                            type = "BUDGET"
                        )
                    )
                } else if (budgetStatus.isWarning) {
                    list.add(
                        SmartNotificationItem(
                            id = "budget_warning",
                            titleAr = "⚠️ اقتراب من سقف الميزانية",
                            titleEn = "⚠️ Approaching Budget Limit",
                            messageAr = "تم استهلاك $spentPct% من ميزانيتك الشهرية. المتبقي: ${Formatters.formatMoney(budgetStatus.remainingAmount)}.",
                            messageEn = "You have spent $spentPct% of your monthly budget. Remaining: ${Formatters.formatMoney(budgetStatus.remainingAmount)}.",
                            severity = NotificationSeverity.WARNING,
                            type = "BUDGET"
                        )
                    )
                }
            }

            // 2. Debts Notifications
            if (debtAlertsOn) {
                val pendingOwedToMe = debts.filter { it.debt.type == "OWED_TO_ME" && it.debt.status != "FULLY_PAID" }
                val pendingIOwe = debts.filter { it.debt.type == "I_OWE" && it.debt.status != "FULLY_PAID" }

                val totalOwedToMeRemaining = pendingOwedToMe.sumOf { (it.debt.totalAmount - it.payments.sumOf { p -> p.amount }).coerceAtLeast(0.0) }
                val totalIOweRemaining = pendingIOwe.sumOf { (it.debt.totalAmount - it.payments.sumOf { p -> p.amount }).coerceAtLeast(0.0) }

                if (pendingOwedToMe.isNotEmpty() && totalOwedToMeRemaining > 0) {
                    list.add(
                        SmartNotificationItem(
                            id = "debts_owed_to_me",
                            titleAr = "💰 ديون مستحقة لك",
                            titleEn = "💰 Debts Owed to You",
                            messageAr = "لديك ${pendingOwedToMe.size} ديون مستحقة للتحصيل بإجمالي ${Formatters.formatMoney(totalOwedToMeRemaining)}.",
                            messageEn = "You have ${pendingOwedToMe.size} pending receivables totaling ${Formatters.formatMoney(totalOwedToMeRemaining)}.",
                            severity = NotificationSeverity.INFO,
                            type = "DEBT"
                        )
                    )
                }

                if (pendingIOwe.isNotEmpty() && totalIOweRemaining > 0) {
                    list.add(
                        SmartNotificationItem(
                            id = "debts_i_owe",
                            titleAr = "🤝 ديون مستحقة عليك",
                            titleEn = "🤝 Debts You Owe",
                            messageAr = "لديك ${pendingIOwe.size} ديون يتوجب سدادها بإجمالي ${Formatters.formatMoney(totalIOweRemaining)}.",
                            messageEn = "You have ${pendingIOwe.size} debts to settle totaling ${Formatters.formatMoney(totalIOweRemaining)}.",
                            severity = NotificationSeverity.WARNING,
                            type = "DEBT"
                        )
                    )
                }
            }

            // 3. Daily Reminder Notification
            if (dailyReminderOn) {
                val cal = Calendar.getInstance()
                val currentDay = cal.get(Calendar.DAY_OF_YEAR)
                val currentYear = cal.get(Calendar.YEAR)
                val hasLoggedToday = transactions.any { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    txCal.get(Calendar.DAY_OF_YEAR) == currentDay && txCal.get(Calendar.YEAR) == currentYear
                }

                if (!hasLoggedToday) {
                    list.add(
                        SmartNotificationItem(
                            id = "daily_logging_reminder",
                            titleAr = "📝 تذكير بتسجيل المصروفات اليومية",
                            titleEn = "📝 Daily Expense Log Reminder",
                            messageAr = "لم تقم بتسجيل أي عملية مالية اليوم بعد. سجّل مشترياتك أولاً بأول للحفاظ على دقة محفظتك!",
                            messageEn = "You haven't logged any transactions today. Keep your wallet up to date!",
                            severity = NotificationSeverity.INFO,
                            type = "DAILY"
                        )
                    )
                }
            }

            // 4. Highest Category Expense Insight
            val cal = Calendar.getInstance()
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            val thisMonthExpenses = transactions.filter { tx ->
                if (tx.type != "EXPENSE") return@filter false
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
            }

            if (thisMonthExpenses.isNotEmpty()) {
                val categoryGroups = thisMonthExpenses.groupBy { it.category }
                val topCategoryEntry = categoryGroups.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
                if (topCategoryEntry != null) {
                    val topAmount = topCategoryEntry.value.sumOf { it.amount }
                    val totalMonth = thisMonthExpenses.sumOf { it.amount }
                    if (totalMonth > 0 && (topAmount / totalMonth) >= 0.35 && totalMonth >= 100) {
                        val pct = ((topAmount / totalMonth) * 100).toInt()
                        list.add(
                            SmartNotificationItem(
                                id = "top_category_insight",
                                titleAr = "📊 رؤية ذكية: أعلى فئة إنفاق",
                                titleEn = "📊 Smart Insight: Top Category",
                                messageAr = "فئة (${topCategoryEntry.key}) تشكل $pct% من إجمالي مصروفاتك لهذا الشهر (${Formatters.formatMoney(topAmount)}).",
                                messageEn = "Category (${topCategoryEntry.key}) represents $pct% of your expenses this month (${Formatters.formatMoney(topAmount)}).",
                                severity = NotificationSeverity.INFO,
                                type = "CATEGORY"
                            )
                        )
                    }
                }
            }

            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = smartNotifications.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun checkAndSendBudgetAlertIfNeeded() {
        if (!_isNotificationsEnabled.value || !_isBudgetAlertEnabled.value) return
        val budgetStatus = monthlyBudgetStatus.value
        if (!budgetStatus.isSet) return

        val app = getApplication<Application>()
        if (budgetStatus.isExceeded) {
            val title = if (_language.value == "ar") "🚨 تجاوز الميزانية الشهرية!" else "🚨 Budget Exceeded!"
            val msg = if (_language.value == "ar") {
                "لقد تجاوزت ميزانيتك المحددة لهذا الشهر بمقدار ${Formatters.formatMoney(budgetStatus.spentAmount - budgetStatus.budgetAmount)}"
            } else {
                "You have exceeded your monthly budget by ${Formatters.formatMoney(budgetStatus.spentAmount - budgetStatus.budgetAmount)}"
            }
            NotificationHelper.sendBudgetAlert(app, title, msg)
        } else if (budgetStatus.isWarning) {
            val title = if (_language.value == "ar") "⚠️ تنبيه الميزانية الشهرية" else "⚠️ Monthly Budget Alert"
            val pct = (budgetStatus.percentage * 100).toInt()
            val msg = if (_language.value == "ar") {
                "تم استهلاك $pct% من ميزانيتك الشهرية. المتبقي: ${Formatters.formatMoney(budgetStatus.remainingAmount)}"
            } else {
                "You have spent $pct% of your budget. Remaining: ${Formatters.formatMoney(budgetStatus.remainingAmount)}"
            }
            NotificationHelper.sendBudgetAlert(app, title, msg)
        }
    }

    fun setDebtFilter(filter: DebtFilter) {
        _debtFilter.value = filter
    }

    fun setCurrency(newCurrency: String) {
        _currency.value = ""
    }

    fun getCurrencySymbol(): String {
        return ""
    }

    fun saveTransaction(
        id: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        dateMillis: Long,
        notes: String,
        isPinned: Boolean,
        receiptUri: String?,
        receiptMimeType: String?
    ) {
        viewModelScope.launch {
            val normalizedCat = FinancialConstants.normalizeCategoryName(category)
            val currentCurrency = getCurrencySymbol()

            if (id > 0) {
                // Editing an existing transaction
                // Check if user changed the category to another existing category item
                val otherExisting = repository.getTransactionsByTypeSync(type)
                    .filter { it.id != id && FinancialConstants.normalizeCategoryName(it.category) == normalizedCat }

                if (otherExisting.isNotEmpty()) {
                    // Merge with the existing item so no duplicate icon is created
                    val target = otherExisting.first()
                    val newTotal = target.amount + amount
                    val mergedNotes = listOf(target.notes, notes)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" • ")

                    val updatedTarget = target.copy(
                        amount = newTotal,
                        dateMillis = dateMillis,
                        paymentMethod = paymentMethod.ifBlank { target.paymentMethod },
                        notes = mergedNotes,
                        isPinned = isPinned || target.isPinned,
                        receiptUri = receiptUri ?: target.receiptUri,
                        receiptMimeType = receiptMimeType ?: target.receiptMimeType
                    )
                    repository.updateTransaction(updatedTarget)
                    repository.deleteTransactionById(id)
                    for (other in otherExisting.drop(1)) {
                        repository.deleteTransaction(other)
                    }
                    _feedback.emit(UserFeedback.Success("تم دمج المبلغ مع بند \"$normalizedCat\" ليصبح الإجمالي ${Formatters.formatMoney(newTotal)} $currentCurrency"))
                } else {
                    val entity = TransactionEntity(
                        id = id,
                        type = type,
                        amount = amount,
                        category = normalizedCat,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = receiptUri,
                        receiptMimeType = receiptMimeType
                    )
                    repository.updateTransaction(entity)
                    _feedback.emit(UserFeedback.Success("تم تعديل العملية بنجاح"))
                }
            } else {
                // Adding a NEW transaction:
                // If this category already exists, DO NOT create a new icon. Add the number/amount to the current icon!
                val existingList = repository.getTransactionsByTypeSync(type)
                    .filter { FinancialConstants.normalizeCategoryName(it.category) == normalizedCat }

                if (existingList.isNotEmpty()) {
                    val primary = existingList.first()
                    val totalPreviousAmount = existingList.sumOf { it.amount }
                    val newTotalAmount = totalPreviousAmount + amount

                    val mergedNotes = (existingList.map { it.notes } + notes)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" • ")

                    val updatedEntity = primary.copy(
                        amount = newTotalAmount,
                        dateMillis = dateMillis,
                        paymentMethod = paymentMethod.ifBlank { primary.paymentMethod },
                        notes = mergedNotes,
                        isPinned = isPinned || existingList.any { it.isPinned },
                        receiptUri = receiptUri ?: primary.receiptUri,
                        receiptMimeType = receiptMimeType ?: primary.receiptMimeType
                    )
                    repository.updateTransaction(updatedEntity)
                    for (dup in existingList.drop(1)) {
                        repository.deleteTransaction(dup)
                    }
                    _feedback.emit(UserFeedback.Success("تمت إضافة المبلغ إلى بند \"$normalizedCat\" ليصبح الإجمالي ${Formatters.formatMoney(newTotalAmount)} $currentCurrency"))
                } else {
                    val entity = TransactionEntity(
                        id = 0,
                        type = type,
                        amount = amount,
                        category = normalizedCat,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = receiptUri,
                        receiptMimeType = receiptMimeType
                    )
                    repository.insertTransaction(entity)
                    _feedback.emit(UserFeedback.Success("تم حفظ العملية بنجاح"))
                }
            }
            if (type == "EXPENSE") {
                checkAndSendBudgetAlertIfNeeded()
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _feedback.emit(UserFeedback.Success("تم حذف العملية بنجاح"))
        }
    }

    fun togglePin(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.togglePinTransaction(transaction)
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
            if (id > 0) {
                repository.updateDebt(entity)
                _feedback.emit(UserFeedback.Success("تم تعديل بيانات الدين بنجاح"))
            } else {
                repository.insertDebt(entity)
                _feedback.emit(UserFeedback.Success("تم تسجيل الدين بنجاح"))
            }
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
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
            repository.addDebtPayment(payment)
            _feedback.emit(UserFeedback.Success("تم تسجيل دفعة السداد بنجاح"))
        }
    }

    fun deleteDebtPayment(payment: DebtPaymentEntity) {
        viewModelScope.launch {
            repository.deleteDebtPayment(payment)
            _feedback.emit(UserFeedback.Success("تم حذف الدفعة بنجاح"))
        }
    }

    fun exportExpensesPdfToDownloads(context: Context, periodTitle: String) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val transactions = allTransactions.value
                val currencySymbol = getCurrencySymbol()
                val result = ExportUtils.exportPdfReport(
                    context = context,
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    periodName = periodTitle,
                    share = false
                )
                if (result != null) {
                    _feedback.emit(UserFeedback.Success("تم حفظ التقرير في مجلد التنزيلات بنجاح"))
                }
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "حدث خطأ أثناء تصدير التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun shareExpensesPdf(context: Context, periodTitle: String) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val transactions = allTransactions.value
                val currencySymbol = getCurrencySymbol()
                ExportUtils.exportPdfReport(
                    context = context,
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    periodName = periodTitle,
                    share = true
                )
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل في مشاركة التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun exportDebtsPdfToDownloads(context: Context) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val debts = allDebtsWithPayments.value.map { it.toDebtRecord() }
                val result = ExportUtils.exportDebtsPdfReport(
                    context = context,
                    debts = debts,
                    share = false
                )
                if (result != null) {
                    _feedback.emit(UserFeedback.Success("تم حفظ تقرير الديون في مجلد التنزيلات بنجاح"))
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
                ExportUtils.exportDebtsPdfReport(
                    context = context,
                    debts = debts,
                    share = true
                )
            } catch (e: Exception) {
                _feedback.emit(UserFeedback.Error(e.localizedMessage ?: "فشل في مشاركة التقرير"))
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _feedback.emit(UserFeedback.Success("تم مسح كافة البيانات بنجاح"))
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

    private fun cleanDummyDataIfPresent() {
        val hasPurgedDummy = prefs.getBoolean("has_purged_dummy_data_v2", false)
        if (!hasPurgedDummy) {
            viewModelScope.launch {
                repository.clearAllData()
                prefs.edit().putBoolean("has_purged_dummy_data_v2", true).apply()
            }
        }
    }

    fun consolidateDuplicateCategories() {
        viewModelScope.launch {
            try {
                val all = repository.getAllTransactionsSync()
                val grouped = all.groupBy { Pair(it.type, FinancialConstants.normalizeCategoryName(it.category)) }
                for ((key, txs) in grouped) {
                    if (txs.size > 1) {
                        val primary = txs.first()
                        val totalAmount = txs.sumOf { it.amount }
                        val allNotes = txs.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString(" • ")
                        val latestDate = txs.maxOf { it.dateMillis }
                        val hasPinned = txs.any { it.isPinned }
                        val receipt = txs.firstOrNull { it.receiptUri != null }

                        repository.updateTransaction(
                            primary.copy(
                                amount = totalAmount,
                                dateMillis = latestDate,
                                notes = allNotes,
                                isPinned = hasPinned,
                                receiptUri = receipt?.receiptUri,
                                receiptMimeType = receipt?.receiptMimeType,
                                category = key.second
                            )
                        )
                        for (dup in txs.drop(1)) {
                            repository.deleteTransaction(dup)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore any consolidation errors on startup
            }
        }
    }
}
