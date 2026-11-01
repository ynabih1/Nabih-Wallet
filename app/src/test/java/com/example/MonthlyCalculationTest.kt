package com.example

import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.MonthlyCalculationUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MonthlyCalculationTest {

    private val zone = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        return LocalDate.of(year, month, day)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun `test user acceptance criteria for October and November monthly calculations`() {
        // Step 1: Add 681 "شحن رصيد" from 1 to 31 October
        val tx681 = TransactionEntity(
            id = 1L,
            type = "EXPENSE",
            amount = 681.0,
            category = "شحن رصيد",
            paymentMethod = "",
            dateMillis = dateToMillis(2025, 10, 1, 0, 0),
            endDateMillis = dateToMillis(2025, 10, 31, 23, 59)
        )

        val listStep1 = listOf(tx681)
        val octStep1 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep1, 2025, 10)
        val novStep1 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep1, 2025, 11)

        assertEquals(681.0, octStep1, 0.001)
        assertEquals(0.0, novStep1, 0.001)

        // Step 2: Add 25 "شحن رصيد" on 1 November
        val tx25 = TransactionEntity(
            id = 2L,
            type = "EXPENSE",
            amount = 25.0,
            category = "شحن رصيد",
            paymentMethod = "",
            dateMillis = dateToMillis(2025, 11, 1, 12, 0),
            endDateMillis = null
        )

        val listStep2 = listOf(tx681, tx25)
        val octStep2 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep2, 2025, 10)
        val novStep2 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep2, 2025, 11)

        assertEquals(681.0, octStep2, 0.001)
        assertEquals(25.0, novStep2, 0.001)

        // Step 3: Change date of 25 EGP to 15 October
        val tx25MovedToOct = tx25.copy(dateMillis = dateToMillis(2025, 10, 15, 10, 0))
        val listStep3 = listOf(tx681, tx25MovedToOct)
        val octStep3 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep3, 2025, 10)
        val novStep3 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep3, 2025, 11)

        assertEquals(706.0, octStep3, 0.001)
        assertEquals(0.0, novStep3, 0.001)

        // Step 4: Move back to 1 November -> numbers return as in Step 2
        val tx25MovedBackToNov = tx25MovedToOct.copy(dateMillis = dateToMillis(2025, 11, 1, 12, 0))
        val listStep4 = listOf(tx681, tx25MovedBackToNov)
        val octStep4 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep4, 2025, 10)
        val novStep4 = MonthlyCalculationUtils.calculateMonthlyExpenses(listStep4, 2025, 11)

        assertEquals(681.0, octStep4, 0.001)
        assertEquals(25.0, novStep4, 0.001)
    }

    @Test
    fun `test same-month category consolidation and multi-month separation`() {
        val txOct1 = TransactionEntity(
            id = 1L,
            type = "EXPENSE",
            amount = 200.0,
            category = "طعام",
            paymentMethod = "",
            dateMillis = dateToMillis(2025, 10, 5),
            endDateMillis = null
        )
        val txOct2 = TransactionEntity(
            id = 2L,
            type = "EXPENSE",
            amount = 150.0,
            category = "طعام",
            paymentMethod = "",
            dateMillis = dateToMillis(2025, 10, 18),
            endDateMillis = null
        )
        val txNov1 = TransactionEntity(
            id = 3L,
            type = "EXPENSE",
            amount = 90.0,
            category = "طعام",
            paymentMethod = "",
            dateMillis = dateToMillis(2025, 11, 2),
            endDateMillis = null
        )

        val consolidated = com.example.data.backup.BackupManager.consolidateSameMonthTransactions(
            listOf(txOct1, txOct2, txNov1)
        )

        // Two items in October with same category "طعام" must merge into 1 transaction of 350.0
        // And November item remains separate at 90.0
        assertEquals(2, consolidated.size)
        val octAmount = MonthlyCalculationUtils.calculateMonthlyExpenses(consolidated, 2025, 10)
        val novAmount = MonthlyCalculationUtils.calculateMonthlyExpenses(consolidated, 2025, 11)
        assertEquals(350.0, octAmount, 0.001)
        assertEquals(90.0, novAmount, 0.001)
    }

    @Test
    fun `test month bounds in local timezone`() {
        val startMillis = MonthlyCalculationUtils.getMonthStartMillis(2025, 10)
        val endMillis = MonthlyCalculationUtils.getMonthEndMillis(2025, 10)

        val localStart = java.time.Instant.ofEpochMilli(startMillis).atZone(zone)
        val localEnd = java.time.Instant.ofEpochMilli(endMillis).atZone(zone)

        assertEquals(1, localStart.dayOfMonth)
        assertEquals(0, localStart.hour)
        assertEquals(0, localStart.minute)
        assertEquals(0, localStart.second)

        assertEquals(31, localEnd.dayOfMonth)
        assertEquals(23, localEnd.hour)
        assertEquals(59, localEnd.minute)
        assertEquals(59, localEnd.second)
    }
}
