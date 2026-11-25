package com.nabih.wallet.data.model

import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.DebtEntity
import com.nabih.wallet.data.local.entity.DebtPaymentEntity
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.local.entity.TransactionEntity
import com.nabih.wallet.data.local.entity.TransactionType
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class TransactionItem(
    val entity: TransactionEntity,
    val categoryName: String,
    val iconKey: String,
    val colorKey: String
)

data class DebtWithPayments(
    val debt: DebtEntity,
    val payments: List<DebtPaymentEntity>
) {
    val totalPaid: Double
        get() = payments.sumOf { it.amount }

    val remainingAmount: Double
        get() = max(0.0, debt.originalAmount - totalPaid)

    val progressRatio: Double
        get() = if (debt.originalAmount <= 0.0) 0.0 else (totalPaid / debt.originalAmount).coerceIn(0.0, 1.0)

    val progressPercent: Int
        get() = (progressRatio * 100.0).roundToInt()

    val isFullyPaid: Boolean
        get() = remainingAmount <= 0.004
}

data class DebtSummary(
    val totalOwedToMeRemaining: Double,
    val totalOwedByMeRemaining: Double,
    val netDebt: Double, // owedToMe - owedByMe (positive = لك, negative = عليك)
    val overallProgressRatio: Double,
    val hasAnyDebts: Boolean
)

data class CategoryExpenseSummary(
    val categoryId: Long,
    val categoryName: String,
    val iconKey: String,
    val colorKey: String,
    val totalAmount: Double,
    val percentage: Double, // 0..100
    val transactionCount: Int
)

data class YearMonthKey(val year: Int, val month: Int) : Comparable<YearMonthKey> {
    // month is 1..12
    override fun compareTo(other: YearMonthKey): Int {
        return if (year != other.year) year.compareTo(other.year) else month.compareTo(other.month)
    }
}

data class MonthExpenseSummary(
    val key: YearMonthKey,
    val totalExpense: Double,
    val transactionCount: Int
)

data class DayActivityPoint(
    val dayStartMillis: Long,
    val dayOfWeek: Int, // Calendar.SATURDAY..Calendar.FRIDAY
    val dayOfMonth: Int,
    val month: Int, // 1..12
    val totalExpense: Double
)

enum class MonthComparisonTrend {
    LESS,
    MORE,
    SAME,
    NO_PREVIOUS_DATA
}

data class MonthlyComparison(
    val currentMonthExpense: Double,
    val previousMonthExpense: Double,
    val percentChange: Int,
    val trend: MonthComparisonTrend
)

object FinancialCalculator {

    fun calculateNetBalance(transactions: List<TransactionEntity>): Double {
        var totalIncome = 0.0
        var totalExpense = 0.0
        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> totalIncome += tx.amount
                TransactionType.EXPENSE -> totalExpense += tx.amount
            }
        }
        return totalIncome - totalExpense
    }

    fun calculateTotalIncome(transactions: List<TransactionEntity>): Double =
        transactions.asSequence().filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    fun calculateTotalExpense(transactions: List<TransactionEntity>): Double =
        transactions.asSequence().filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    fun mapTransactions(
        transactions: List<TransactionEntity>,
        categoriesById: Map<Long, CategoryEntity>
    ): List<TransactionItem> = transactions.map { tx ->
        val cat = categoriesById[tx.categoryId]
        TransactionItem(
            entity = tx,
            categoryName = cat?.name ?: tx.categoryNameSnapshot,
            iconKey = cat?.iconKey ?: tx.categoryIconKeySnapshot,
            colorKey = cat?.colorKey ?: tx.categoryColorKeySnapshot
        )
    }

    fun yearMonthOf(timestampMillis: Long): YearMonthKey {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        return YearMonthKey(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1
        )
    }

    fun calculateMonthlyComparison(
        transactions: List<TransactionEntity>,
        referenceMillis: Long = System.currentTimeMillis()
    ): MonthlyComparison {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val currentKey = YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        cal.add(Calendar.MONTH, -1)
        val previousKey = YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        var currentTotal = 0.0
        var previousTotal = 0.0
        for (tx in transactions) {
            if (tx.type != TransactionType.EXPENSE) continue
            when (yearMonthOf(tx.timestamp)) {
                currentKey -> currentTotal += tx.amount
                previousKey -> previousTotal += tx.amount
            }
        }

        if (previousTotal <= 0.004) {
            return MonthlyComparison(
                currentMonthExpense = currentTotal,
                previousMonthExpense = previousTotal,
                percentChange = 0,
                trend = MonthComparisonTrend.NO_PREVIOUS_DATA
            )
        }

        val diff = currentTotal - previousTotal
        val pct = ((abs(diff) / previousTotal) * 100.0).roundToInt()
        val trend = when {
            abs(diff) <= 0.004 || pct == 0 -> MonthComparisonTrend.SAME
            diff < 0 -> MonthComparisonTrend.LESS
            else -> MonthComparisonTrend.MORE
        }
        return MonthlyComparison(
            currentMonthExpense = currentTotal,
            previousMonthExpense = previousTotal,
            percentChange = pct,
            trend = trend
        )
    }

    fun calculateLast7DaysActivity(
        transactions: List<TransactionEntity>,
        referenceMillis: Long = System.currentTimeMillis()
    ): List<DayActivityPoint> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = referenceMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Build chronological list from 6 days ago up to today (7 days total)
        val days = ArrayList<Calendar>(7)
        val startCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }
        for (i in 0 until 7) {
            val dayCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            days.add(dayCal)
        }

        val totalsByStart = HashMap<Long, Double>(7)
        val firstDayStart = days.first().timeInMillis
        val afterLastDayStart = (days.last().clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        for (tx in transactions) {
            if (tx.type != TransactionType.EXPENSE) continue
            if (tx.timestamp < firstDayStart || tx.timestamp >= afterLastDayStart) continue
            val txCal = Calendar.getInstance().apply {
                timeInMillis = tx.timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val key = txCal.timeInMillis
            totalsByStart[key] = (totalsByStart[key] ?: 0.0) + tx.amount
        }

        return days.map { dayCal ->
            val startMillis = dayCal.timeInMillis
            DayActivityPoint(
                dayStartMillis = startMillis,
                dayOfWeek = dayCal.get(Calendar.DAY_OF_WEEK),
                dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                month = dayCal.get(Calendar.MONTH) + 1,
                totalExpense = totalsByStart[startMillis] ?: 0.0
            )
        }
    }

    fun calculateExpensesByCategory(
        transactions: List<TransactionItem>,
        monthFilter: YearMonthKey? = null
    ): List<CategoryExpenseSummary> {
        val filtered = transactions.filter { item ->
            item.entity.type == TransactionType.EXPENSE &&
                (monthFilter == null || yearMonthOf(item.entity.timestamp) == monthFilter)
        }
        val grandTotal = filtered.sumOf { it.entity.amount }
        if (grandTotal <= 0.0) return emptyList()

        return filtered.groupBy { it.entity.categoryId }
            .map { (categoryId, items) ->
                val sample = items.first()
                val sum = items.sumOf { it.entity.amount }
                CategoryExpenseSummary(
                    categoryId = categoryId,
                    categoryName = sample.categoryName,
                    iconKey = sample.iconKey,
                    colorKey = sample.colorKey,
                    totalAmount = sum,
                    percentage = (sum / grandTotal) * 100.0,
                    transactionCount = items.size
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun calculateMonthlyHistory(transactions: List<TransactionEntity>): List<MonthExpenseSummary> {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { yearMonthOf(it.timestamp) }
            .map { (key, list) ->
                MonthExpenseSummary(
                    key = key,
                    totalExpense = list.sumOf { it.amount },
                    transactionCount = list.size
                )
            }
            .sortedByDescending { it.key }
    }

    fun calculateDebtSummary(debts: List<DebtWithPayments>): DebtSummary {
        var owedToMe = 0.0
        var owedByMe = 0.0
        var totalOriginal = 0.0
        var totalPaid = 0.0

        for (item in debts) {
            totalOriginal += item.debt.originalAmount
            totalPaid += item.totalPaid.coerceAtMost(item.debt.originalAmount)
            when (item.debt.type) {
                DebtType.OWED_TO_ME -> owedToMe += item.remainingAmount
                DebtType.OWED_BY_ME -> owedByMe += item.remainingAmount
            }
        }

        val ratio = if (totalOriginal <= 0.0) 0.0 else (totalPaid / totalOriginal).coerceIn(0.0, 1.0)
        return DebtSummary(
            totalOwedToMeRemaining = owedToMe,
            totalOwedByMeRemaining = owedByMe,
            netDebt = owedToMe - owedByMe,
            overallProgressRatio = ratio,
            hasAnyDebts = debts.isNotEmpty()
        )
    }
}
