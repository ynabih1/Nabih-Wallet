package com.nabih.wallet

import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.TransactionEntity
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.FinancialCalculator
import com.nabih.wallet.data.model.MonthComparisonTrend
import com.nabih.wallet.data.model.YearMonthKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MonthlyCalculationTest {

    private fun timestampFor(year: Int, month1To12: Int, day: Int, hour: Int = 12): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month1To12 - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `net balance equals total income minus total expenses`() {
        val transactions = listOf(
            TransactionEntity(1, TransactionType.INCOME, 25000.0, 101, "راتب", timestamp = timestampFor(2026, 11, 1)),
            TransactionEntity(2, TransactionType.EXPENSE, 3200.0, 7, "تعليم", timestamp = timestampFor(2026, 11, 5)),
            TransactionEntity(3, TransactionType.EXPENSE, 1450.5, 1, "طعام", timestamp = timestampFor(2026, 11, 10)),
            TransactionEntity(4, TransactionType.INCOME, 500.5, 102, "عمل حر", timestamp = timestampFor(2026, 11, 12))
        )

        assertEquals(25500.5, FinancialCalculator.calculateTotalIncome(transactions), 0.001)
        assertEquals(4650.5, FinancialCalculator.calculateTotalExpense(transactions), 0.001)
        assertEquals(20850.0, FinancialCalculator.calculateNetBalance(transactions), 0.001)
    }

    @Test
    fun `monthly comparison computes lower and higher percentages accurately`() {
        val refNow = timestampFor(2026, 11, 25)
        val txsLess = listOf(
            TransactionEntity(1, TransactionType.EXPENSE, 5000.0, 1, "طعام", timestamp = timestampFor(2026, 10, 15)),
            TransactionEntity(2, TransactionType.EXPENSE, 4100.0, 1, "طعام", timestamp = timestampFor(2026, 11, 10))
        )
        val compLess = FinancialCalculator.calculateMonthlyComparison(txsLess, refNow)
        assertEquals(4100.0, compLess.currentMonthExpense, 0.001)
        assertEquals(5000.0, compLess.previousMonthExpense, 0.001)
        assertEquals(18, compLess.percentChange)
        assertEquals(MonthComparisonTrend.LESS, compLess.trend)

        val txsMore = listOf(
            TransactionEntity(1, TransactionType.EXPENSE, 2000.0, 1, "طعام", timestamp = timestampFor(2026, 10, 5)),
            TransactionEntity(2, TransactionType.EXPENSE, 2500.0, 1, "طعام", timestamp = timestampFor(2026, 11, 8))
        )
        val compMore = FinancialCalculator.calculateMonthlyComparison(txsMore, refNow)
        assertEquals(25, compMore.percentChange)
        assertEquals(MonthComparisonTrend.MORE, compMore.trend)

        val compNoPrev = FinancialCalculator.calculateMonthlyComparison(
            listOf(TransactionEntity(1, TransactionType.EXPENSE, 900.0, 1, "طعام", timestamp = timestampFor(2026, 11, 2))),
            refNow
        )
        assertEquals(MonthComparisonTrend.NO_PREVIOUS_DATA, compNoPrev.trend)
    }

    @Test
    fun `last 7 days activity aggregates expenses per day and ignores income`() {
        val refNow = timestampFor(2026, 11, 25, 18)
        val transactions = listOf(
            TransactionEntity(1, TransactionType.EXPENSE, 1450.0, 1, "طعام", timestamp = timestampFor(2026, 11, 25, 9)),
            TransactionEntity(2, TransactionType.EXPENSE, 375.0, 6, "شحن رصيد", timestamp = timestampFor(2026, 11, 25, 15)),
            TransactionEntity(3, TransactionType.INCOME, 9000.0, 101, "راتب", timestamp = timestampFor(2026, 11, 25, 10)),
            TransactionEntity(4, TransactionType.EXPENSE, 980.0, 9, "تسوق", timestamp = timestampFor(2026, 11, 24, 14)),
            TransactionEntity(5, TransactionType.EXPENSE, 500.0, 2, "بقالة", timestamp = timestampFor(2026, 11, 10, 12)) // outside 7 days
        )

        val points = FinancialCalculator.calculateLast7DaysActivity(transactions, refNow)
        assertEquals(7, points.size)
        // Last point is today (25 Nov)
        assertEquals(25, points.last().dayOfMonth)
        assertEquals(1825.0, points.last().totalExpense, 0.001)
        // Previous day (24 Nov)
        assertEquals(24, points[5].dayOfMonth)
        assertEquals(980.0, points[5].totalExpense, 0.001)
        // Earlier days in window have 0.0
        assertEquals(0.0, points[0].totalExpense, 0.001)
    }

    @Test
    fun `category expense breakdown calculates percentages and filters by month`() {
        val cats = mapOf(
            1L to CategoryEntity(1L, "طعام", TransactionType.EXPENSE, "restaurant", "amber", true, 1),
            7L to CategoryEntity(7L, "تعليم", TransactionType.EXPENSE, "school", "purple", true, 7)
        )
        val raw = listOf(
            TransactionEntity(1, TransactionType.EXPENSE, 3000.0, 7L, "تعليم", timestamp = timestampFor(2026, 11, 3)),
            TransactionEntity(2, TransactionType.EXPENSE, 1000.0, 1L, "طعام", timestamp = timestampFor(2026, 11, 4)),
            TransactionEntity(3, TransactionType.EXPENSE, 2000.0, 1L, "طعام", timestamp = timestampFor(2026, 10, 12))
        )
        val mapped = FinancialCalculator.mapTransactions(raw, cats)

        val novBreakdown = FinancialCalculator.calculateExpensesByCategory(mapped, YearMonthKey(2026, 11))
        assertEquals(2, novBreakdown.size)
        assertEquals("تعليم", novBreakdown[0].categoryName)
        assertEquals(75.0, novBreakdown[0].percentage, 0.01)
        assertEquals("طعام", novBreakdown[1].categoryName)
        assertEquals(25.0, novBreakdown[1].percentage, 0.01)
        assertEquals(100.0, novBreakdown.sumOf { it.percentage }, 0.01)
    }
}
