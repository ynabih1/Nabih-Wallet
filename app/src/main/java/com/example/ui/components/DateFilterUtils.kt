package com.example.ui.components

import com.example.data.local.entity.TransactionEntity
import java.text.SimpleDateFormat
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
     * Converts UTC midnight milliseconds (from Material 3 DateRangePicker) to local start of day (00:00:00.000).
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
     * Converts UTC midnight milliseconds (from Material 3 DateRangePicker) to local end of day (23:59:59.999).
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
     * Converts local timestamp to UTC midnight milliseconds for initializing DateRangePickerState.
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
     * Default starting date: earliest transaction date, or 2025/10/01 if no transactions exist.
     */
    fun getDefaultStartDate(transactions: List<TransactionEntity>): Long {
        val earliest = transactions.minOfOrNull { it.dateMillis }
        return if (earliest != null) {
            getStartOfDay(earliest)
        } else {
            val cal = Calendar.getInstance().apply {
                set(2025, Calendar.OCTOBER, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
    }

    fun getTodayEndOfDay(): Long = getEndOfDay(System.currentTimeMillis())

    fun calculatePresetRange(
        preset: DateFilterPreset,
        transactions: List<TransactionEntity>
    ): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val end = getEndOfDay(now)
        val cal = Calendar.getInstance()

        return when (preset) {
            DateFilterPreset.THIS_MONTH -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = getStartOfDay(cal.timeInMillis)
                Pair(start, end)
            }
            DateFilterPreset.LAST_3_MONTHS -> {
                cal.timeInMillis = now
                cal.add(Calendar.MONTH, -3)
                val start = getStartOfDay(cal.timeInMillis)
                Pair(start, end)
            }
            DateFilterPreset.THIS_YEAR -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_YEAR, 1)
                val start = getStartOfDay(cal.timeInMillis)
                Pair(start, end)
            }
            DateFilterPreset.ALL -> {
                val start = getDefaultStartDate(transactions)
                Pair(start, end)
            }
            DateFilterPreset.CUSTOM -> {
                Pair(getDefaultStartDate(transactions), end)
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
