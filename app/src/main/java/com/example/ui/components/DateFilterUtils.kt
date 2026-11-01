package com.example.ui.components

import com.example.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

enum class DateFilterPreset(val titleAr: String, val titleEn: String) {
    THIS_MONTH("هذا الشهر", "This Month"),
    LAST_3_MONTHS("آخر 3 شهور", "Last 3 Months"),
    THIS_YEAR("هذه السنة", "This Year"),
    ALL("الكل", "All"),
    CUSTOM("مخصص", "Custom")
}

object MonthlyCalculationUtils {
    val zone: ZoneId get() = ZoneId.systemDefault()

    fun getYearMonth(dateMillis: Long): YearMonth {
        return YearMonth.from(Instant.ofEpochMilli(dateMillis).atZone(zone))
    }

    fun isSameMonth(dateMillis1: Long, dateMillis2: Long): Boolean {
        return getYearMonth(dateMillis1) == getYearMonth(dateMillis2)
    }

    fun getMonthStartMillis(year: Int, month: Int): Long {
        val ym = YearMonth.of(year, month)
        return ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun getMonthEndMillis(year: Int, month: Int): Long {
        val ym = YearMonth.of(year, month)
        return ym.atEndOfMonth().atTime(23, 59, 59, 999_000_000).atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Calculates the transaction's amount belonging to a specific month based on its actual day.
     */
    fun calculateTransactionAmountForMonth(tx: TransactionEntity, year: Int, month: Int): Double {
        val txYm = getYearMonth(tx.dateMillis)
        val ym = YearMonth.of(year, month)
        return if (txYm == ym) tx.amount else 0.0
    }

    /**
     * Calculates total expenses for a specific month (1..12).
     */
    fun calculateMonthlyExpenses(transactions: List<TransactionEntity>, year: Int, month: Int): Double {
        return transactions
            .filter { it.type == "EXPENSE" }
            .sumOf { calculateTransactionAmountForMonth(it, year, month) }
    }

    /**
     * Calculates current month expenses using system default timezone.
     */
    fun calculateCurrentMonthExpenses(transactions: List<TransactionEntity>): Double {
        val now = LocalDate.now(zone)
        return calculateMonthlyExpenses(transactions, now.year, now.monthValue)
    }

    /**
     * Calculates the amount of this transaction if its actual date falls within [startMillis..endMillis].
     */
    fun calculateTransactionAmountForRange(tx: TransactionEntity, startMillis: Long, endMillis: Long): Double {
        val txDate = Instant.ofEpochMilli(tx.dateMillis).atZone(zone).toLocalDate()
        val rangeStartDate = Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
        val rangeEndDate = Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()

        return if (!txDate.isBefore(rangeStartDate) && !txDate.isAfter(rangeEndDate)) {
            tx.amount
        } else {
            0.0
        }
    }

    /**
     * Returns all distinct active YearMonths with transactions or the current month.
     */
    fun getAllActiveMonths(transactions: List<TransactionEntity>): List<YearMonth> {
        val set = mutableSetOf<YearMonth>()
        set.add(YearMonth.now(zone))
        for (tx in transactions) {
            set.add(getYearMonth(tx.dateMillis))
        }
        return set.sortedDescending()
    }
}

object DateFilterUtils {

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    /**
     * Converts UTC midnight milliseconds (from Material 3 DateRangePicker/DatePicker) to local start of day (00:00:00.000).
     */
    fun utcMillisToLocalStartOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        val localCal = Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    /**
     * Converts UTC midnight milliseconds (from Material 3 DateRangePicker/DatePicker) to local end of day (23:59:59.999).
     */
    fun utcMillisToLocalEndOfDay(utcMillis: Long): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        val localCal = Calendar.getInstance().apply {
            set(year, month, day, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return localCal.timeInMillis
    }

    /**
     * Converts local timestamp to UTC midnight milliseconds for initializing DatePickerState/DateRangePickerState.
     */
    fun localMillisToUtcMidnight(localMillis: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = localMillis
        }
        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH)
        val day = localCal.get(Calendar.DAY_OF_MONTH)

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }
        return utcCal.timeInMillis
    }

    /**
     * Default starting date: earliest transaction date, or beginning of current month if empty.
     */
    fun getDefaultStartDate(transactions: List<TransactionEntity>): Long {
        val earliest = transactions.minOfOrNull { it.dateMillis }
        return if (earliest != null) {
            getStartOfDay(earliest)
        } else {
            val now = LocalDate.now(MonthlyCalculationUtils.zone)
            MonthlyCalculationUtils.getMonthStartMillis(now.year, now.monthValue)
        }
    }

    fun getTodayEndOfDay(): Long = getEndOfDay(System.currentTimeMillis())

    fun calculatePresetRange(
        preset: DateFilterPreset,
        transactions: List<TransactionEntity>
    ): Pair<Long, Long> {
        val now = LocalDate.now(MonthlyCalculationUtils.zone)

        return when (preset) {
            DateFilterPreset.THIS_MONTH -> {
                val start = MonthlyCalculationUtils.getMonthStartMillis(now.year, now.monthValue)
                val end = MonthlyCalculationUtils.getMonthEndMillis(now.year, now.monthValue)
                Pair(start, end)
            }
            DateFilterPreset.LAST_3_MONTHS -> {
                val threeMonthsAgo = now.minusMonths(2)
                val start = MonthlyCalculationUtils.getMonthStartMillis(threeMonthsAgo.year, threeMonthsAgo.monthValue)
                val end = MonthlyCalculationUtils.getMonthEndMillis(now.year, now.monthValue)
                Pair(start, end)
            }
            DateFilterPreset.THIS_YEAR -> {
                val start = MonthlyCalculationUtils.getMonthStartMillis(now.year, 1)
                val end = MonthlyCalculationUtils.getMonthEndMillis(now.year, 12)
                Pair(start, end)
            }
            DateFilterPreset.ALL -> {
                val start = getDefaultStartDate(transactions)
                val end = getEndOfDay(System.currentTimeMillis())
                Pair(start, end)
            }
            DateFilterPreset.CUSTOM -> {
                Pair(getDefaultStartDate(transactions), getEndOfDay(System.currentTimeMillis()))
            }
        }
    }

    fun getDaysDifference(startMillis: Long, endMillis: Long): Long {
        val diff = (endMillis - startMillis) / (1000 * 60 * 60 * 24) + 1
        return max(1L, diff)
    }

    fun formatDateArabic(millis: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        return sdf.format(Date(millis))
    }

    fun formatDateEnglish(millis: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.US)
        return sdf.format(Date(millis))
    }

    fun formatDateRangeLabel(startMillis: Long, endMillis: Long, isArabic: Boolean): String {
        return if (isArabic) {
            "من ${formatDateArabic(startMillis)} إلى ${formatDateArabic(endMillis)}"
        } else {
            "From ${formatDateEnglish(startMillis)} to ${formatDateEnglish(endMillis)}"
        }
    }
}
