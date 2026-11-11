package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Reusable Date Range Filter Bar with Spotify-style Quick Presets and Material 3 DateRangePicker Dialog.
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
                color = TextPrimary
            )
        }

        // Quick Preset Shortcut Chips in Spotify pill style (هذا الشهر / آخر 3 شهور / هذه السنة / الكل / مخصص)
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
                        .height(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (preset == DateFilterPreset.CUSTOM) {
                                showDatePickerDialog = true
                            } else {
                                val range = DateFilterUtils.calculatePresetRange(preset, transactions)
                                onDateRangeSelected(range.first, range.second, preset)
                            }
                        }
                        .testTag("date_preset_${preset.name.lowercase()}"),
                    shape = CircleShape,
                    color = if (isSelected) SpotifyGreen else InputChipSurface,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (preset == DateFilterPreset.CUSTOM) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                tint = if (isSelected) OnSpotifyGreen else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OnSpotifyGreen else TextPrimary
                        )
                    }
                }
            }
        }

        // Date Range Display Box
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
            color = InputChipSurface,
            border = BorderStroke(1.dp, DividerBorderColor),
            shadowElevation = 0.dp
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = displayRangeText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
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
 * Modern Material 3 DateRangePicker Dialog in #1A1A1A dark elevated surface.
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = OnSpotifyGreen
                ),
                shape = CircleShape,
                modifier = Modifier.testTag("apply_date_range_btn")
            ) {
                Text(
                    text = if (isArabic) "تطبيق الفترة" else "Apply Range",
                    color = OnSpotifyGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                border = BorderStroke(1.dp, DividerBorderColor),
                modifier = Modifier.testTag("cancel_date_range_btn")
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = ElevatedSurface
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            DateRangePicker(
                state = state,
                modifier = Modifier.weight(1f),
                title = {
                    Text(
                        text = if (isArabic) "تحديد النطاق الزمني باليوم" else "Select Date Range",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
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
                        if (isArabic) "من ${DateFilterUtils.formatDateArabic(localStart)} - اختر تاريخ النهاية"
                        else "From ${DateFilterUtils.formatDateEnglish(localStart)} - Select end date"
                    } else {
                        if (isArabic) "اختر تاريخ البداية وتاريخ النهاية" else "Choose start and end dates"
                    }

                    Text(
                        text = headlineText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SpotifyGreen,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                },
                colors = DatePickerDefaults.colors(
                    containerColor = ElevatedSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = SpotifyGreen,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = SpotifyGreen,
                    selectedYearContainerColor = SpotifyGreen,
                    selectedYearContentColor = OnSpotifyGreen,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = SpotifyGreen,
                    selectedDayContentColor = OnSpotifyGreen,
                    dayInSelectionRangeContainerColor = SpotifyGreen.copy(alpha = 0.2f),
                    dayInSelectionRangeContentColor = TextPrimary,
                    todayDateBorderColor = SpotifyGreen,
                    todayContentColor = SpotifyGreen
                )
            )
        }
    }
}
