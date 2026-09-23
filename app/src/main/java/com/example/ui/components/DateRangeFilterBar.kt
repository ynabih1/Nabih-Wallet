package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface

/**
 * Reusable Date Range Filter Bar with Quick Presets and Material 3 DateRangePicker Dialog.
 * Allows filtering records and reports by day-level precision (من يوم كذا .. إلى يوم كذا).
 */
@Composable
fun DateRangeFilterBar(
    startDateMillis: Long,
    endDateMillis: Long,
    selectedPreset: DateFilterPreset,
    transactions: List<TransactionEntity>,
    isArabic: Boolean,
    onDateRangeSelected: (start: Long, end: Long, preset: DateFilterPreset) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        // Quick Preset Shortcut Chips (هذا الشهر / آخر 3 شهور / هذه السنة / الكل / مخصص)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val presets = listOf(
                DateFilterPreset.THIS_MONTH,
                DateFilterPreset.LAST_3_MONTHS,
                DateFilterPreset.THIS_YEAR,
                DateFilterPreset.ALL,
                DateFilterPreset.CUSTOM
            )

            presets.forEach { preset ->
                val isSelected = selectedPreset == preset
                val label = if (isArabic) preset.titleAr else preset.titleEn

                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (preset == DateFilterPreset.CUSTOM) {
                                showDatePickerDialog = true
                            } else {
                                val range = DateFilterUtils.calculatePresetRange(preset, transactions)
                                onDateRangeSelected(range.first, range.second, preset)
                            }
                        }
                        .testTag("date_preset_${preset.name.lowercase()}"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF2B2620) else WarmCardSurface,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF2B2620) else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (preset == DateFilterPreset.CUSTOM) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else BurntOrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextPrimaryDark
                        )
                    }
                }
            }
        }

        // Date Range Display Pill Box (e.g. 📅 من 1 سبتمبر 2026 إلى 23 سبتمبر 2026 ▼)
        val formattedStart = if (isArabic) {
            DateFilterUtils.formatDateArabic(startDateMillis)
        } else {
            DateFilterUtils.formatDateEnglish(startDateMillis)
        }

        val formattedEnd = if (isArabic) {
            DateFilterUtils.formatDateArabic(endDateMillis)
        } else {
            DateFilterUtils.formatDateEnglish(endDateMillis)
        }

        val displayRangeText = if (isArabic) {
            "من $formattedStart إلى $formattedEnd"
        } else {
            "$formattedStart - $formattedEnd"
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showDatePickerDialog = true }
                .testTag("date_range_picker_btn"),
            shape = RoundedCornerShape(12.dp),
            color = WarmCardSurface,
            border = BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = BurntOrangePrimary,
                        modifier = Modifier.size(19.dp)
                    )

                    Text(
                        text = displayRangeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondaryBrown,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDatePickerDialog) {
        DateRangeSelectionDialog(
            currentStartMillis = startDateMillis,
            currentEndMillis = endDateMillis,
            isArabic = isArabic,
            onDismiss = { showDatePickerDialog = false },
            onApply = { start, end ->
                showDatePickerDialog = false
                onDateRangeSelected(start, end, DateFilterPreset.CUSTOM)
            }
        )
    }
}

/**
 * Modern Material 3 DateRangePicker Dialog for choosing start and end days with precision.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelectionDialog(
    currentStartMillis: Long,
    currentEndMillis: Long,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onApply: (start: Long, end: Long) -> Unit
) {
    val initialUtcStart = remember(currentStartMillis) {
        DateFilterUtils.localMillisToUtcMidnight(currentStartMillis)
    }
    val initialUtcEnd = remember(currentEndMillis) {
        DateFilterUtils.localMillisToUtcMidnight(currentEndMillis)
    }

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialUtcStart,
        initialSelectedEndDateMillis = initialUtcEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val startUtc = state.selectedStartDateMillis
                    val endUtc = state.selectedEndDateMillis ?: startUtc
                    if (startUtc != null) {
                        val localStart = DateFilterUtils.utcMillisToLocalStartOfDay(startUtc)
                        val localEnd = DateFilterUtils.utcMillisToLocalEndOfDay(endUtc ?: startUtc)
                        onApply(localStart, localEnd)
                    }
                },
                enabled = state.selectedStartDateMillis != null,
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("apply_date_range_btn")
            ) {
                Text(
                    text = if (isArabic) "تطبيق الفترة" else "Apply Range",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_date_range_btn")
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    color = TextSecondaryBrown,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = {
                    Text(
                        text = if (isArabic) "تحديد النطاق الزمني باليوم" else "Select Date Range (Day-by-Day)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp)
                    )
                },
                headline = {
                    val startUtc = state.selectedStartDateMillis
                    val endUtc = state.selectedEndDateMillis
                    val headlineText = if (startUtc != null && endUtc != null) {
                        val localStart = DateFilterUtils.utcMillisToLocalStartOfDay(startUtc)
                        val localEnd = DateFilterUtils.utcMillisToLocalEndOfDay(endUtc)
                        DateFilterUtils.formatDateRangeLabel(localStart, localEnd, isArabic)
                    } else if (startUtc != null) {
                        val localStart = DateFilterUtils.utcMillisToLocalStartOfDay(startUtc)
                        if (isArabic) "من ${DateFilterUtils.formatDateArabic(localStart)} (اختر تاريخ النهاية)"
                        else "From ${DateFilterUtils.formatDateEnglish(localStart)} (Select end date)"
                    } else {
                        if (isArabic) "اختر تاريخ البداية وتاريخ النهاية" else "Choose start and end dates"
                    }

                    Text(
                        text = headlineText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BurntOrangePrimary,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = TextPrimaryDark,
                    headlineContentColor = BurntOrangePrimary,
                    weekdayContentColor = TextSecondaryBrown,
                    subheadContentColor = TextPrimaryDark,
                    yearContentColor = TextPrimaryDark,
                    currentYearContentColor = BurntOrangePrimary,
                    selectedYearContainerColor = BurntOrangePrimary,
                    selectedYearContentColor = Color.White,
                    dayContentColor = TextPrimaryDark,
                    selectedDayContainerColor = BurntOrangePrimary,
                    selectedDayContentColor = Color.White,
                    dayInSelectionRangeContainerColor = BurntOrangePrimary.copy(alpha = 0.15f),
                    dayInSelectionRangeContentColor = TextPrimaryDark,
                    todayDateBorderColor = BurntOrangePrimary,
                    todayContentColor = BurntOrangePrimary
                )
            )
        }
    }
}
