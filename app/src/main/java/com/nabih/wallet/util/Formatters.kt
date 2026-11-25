package com.nabih.wallet.util

import com.nabih.wallet.data.model.YearMonthKey
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Formatters {

    private val arabicMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val arabicWeekDays = mapOf(
        Calendar.SATURDAY to "السبت",
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الاثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة"
    )

    private fun decimalFormat(): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        return DecimalFormat("#,##0.00", symbols)
    }

    fun formatNumber(amount: Double): String {
        val safe = if (abs(amount) < 0.005) 0.0 else amount
        return decimalFormat().format(safe)
    }

    fun formatAmount(amount: Double): String = "${formatNumber(amount)} ج.م"

    fun formatSignedExpense(amount: Double): String = "-${formatNumber(abs(amount))} ج.م"

    fun formatSignedIncome(amount: Double): String = "+${formatNumber(abs(amount))} ج.م"

    fun formatPercent(value: Double): String = "${value.roundToInt()}%"

    fun formatPercentInt(value: Int): String = "$value%"

    fun monthName(month1To12: Int): String {
        val index = (month1To12 - 1).coerceIn(0, 11)
        return arabicMonths[index]
    }

    fun monthYear(key: YearMonthKey): String = "${monthName(key.month)} ${key.year}"

    fun weekDayName(dayOfWeek: Int): String = arabicWeekDays[dayOfWeek] ?: ""

    fun formatDayMonth(timestampMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthName(cal.get(Calendar.MONTH) + 1)
        return "$day $month"
    }

    fun formatFullDate(timestampMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthName(cal.get(Calendar.MONTH) + 1)
        val year = cal.get(Calendar.YEAR)
        return "$day $month $year"
    }

    fun formatDateWithRelativePrefix(timestampMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val isSameDay = target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
        val full = formatFullDate(timestampMillis)
        return if (isSameDay) "اليوم، $full" else full
    }

    fun formatDayOfWeekAndDate(dayOfWeek: Int, dayOfMonth: Int, month1To12: Int): String {
        return "${weekDayName(dayOfWeek)} $dayOfMonth ${monthName(month1To12)}"
    }

    fun formatTime12Hour(hour24: Int, minute: Int): String {
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val minPadded = String.format(Locale.US, "%02d", minute)
        val period = if (isPm) "م" else "ص"
        return "$hour12:$minPadded $period"
    }

    fun initialsFor(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return "?"
        val firstChar = parts[0].first()
        val isLatin = firstChar in 'A'..'Z' || firstChar in 'a'..'z'
        return when {
            parts.size >= 2 && isLatin ->
                "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size >= 2 ->
                "${parts[0].first()} ${parts[1].first()}"
            isLatin ->
                parts[0].take(2).uppercase(Locale.US)
            else ->
                parts[0].take(1)
        }
    }

    fun parseAmountInput(raw: String): Double? {
        val normalized = raw
            .replace(",", "")
            .replace("٬", "")
            .replace("٫", ".")
            .map { ch ->
                when (ch) {
                    in '٠'..'٩' -> ('0'.code + (ch.code - '٠'.code)).toChar()
                    in '۰'..'۹' -> ('0'.code + (ch.code - '۰'.code)).toChar()
                    else -> ch
                }
            }
            .joinToString("")
            .trim()
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }
}
