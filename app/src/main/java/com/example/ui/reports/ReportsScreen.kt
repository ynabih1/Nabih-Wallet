package com.example.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FinancialConstants
import com.example.ui.WalletViewModel
import com.example.ui.components.DateFilterPreset
import com.example.ui.components.DateFilterUtils
import com.example.ui.components.DateRangeSelectionDialog
import com.example.ui.components.Formatters
import com.example.ui.components.MonthlyCalculationUtils
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

private data class SimpleCategoryStat(
    val name: String,
    val amount: Double,
    val percentage: Float
)

private data class MonthlyExpenseStat(
    val yearMonth: YearMonth,
    val label: String,
    val totalAmount: Double,
    val categories: List<SimpleCategoryStat>
)

@Composable
fun ReportsScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filterStartDate by viewModel.filterStartDate.collectAsStateWithLifecycle()
    val filterEndDate by viewModel.filterEndDate.collectAsStateWithLifecycle()
    val filterPreset by viewModel.filterPreset.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isArabic = language == "ar"
    val currencySymbol = viewModel.getCurrencySymbol().ifBlank { if (isArabic) "ج.م" else "EGP" }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var expandedYearMonth by remember { mutableStateOf<YearMonth?>(null) }

    val expenseTransactions = remember(allTransactions, filterStartDate, filterEndDate) {
        allTransactions.filter { tx ->
            tx.type == "EXPENSE" && tx.dateMillis in filterStartDate..filterEndDate
        }
    }

    val totalExpenses = remember(expenseTransactions, filterStartDate, filterEndDate) {
        expenseTransactions.sumOf { tx ->
            MonthlyCalculationUtils.calculateTransactionAmountForRange(tx, filterStartDate, filterEndDate)
        }
    }

    val categoryStats = remember(expenseTransactions, totalExpenses, filterStartDate, filterEndDate) {
        val grouped = expenseTransactions
            .groupBy { FinancialConstants.normalizeCategoryName(it.category) }
            .map { (cat, list) ->
                val sum = list.sumOf { tx ->
                    MonthlyCalculationUtils.calculateTransactionAmountForRange(tx, filterStartDate, filterEndDate)
                }
                cat to sum
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

        val volume = if (totalExpenses > 0.0) totalExpenses else 1.0
        grouped.map { (name, sum) ->
            SimpleCategoryStat(
                name = name,
                amount = sum,
                percentage = (sum / volume).toFloat()
            )
        }
    }

    // Monthly Expenses Breakdown (سجل الشهور)
    val monthlyExpenseStats = remember(allTransactions, isArabic) {
        val locale = if (isArabic) Locale("ar") else Locale.US
        val monthFormatter = SimpleDateFormat("MMMM yyyy", locale)

        allTransactions
            .filter { it.type == "EXPENSE" && it.amount > 0.0 }
            .groupBy { MonthlyCalculationUtils.getYearMonth(it.dateMillis) }
            .entries
            .sortedByDescending { it.key }
            .map { (ym, txList) ->
                val monthTotal = txList.sumOf { it.amount }
                val monthStartMillis = MonthlyCalculationUtils.getMonthStartMillis(ym.year, ym.monthValue)
                val label = monthFormatter.format(Date(monthStartMillis))
                val volume = if (monthTotal > 0.0) monthTotal else 1.0

                val cats = txList
                    .groupBy { FinancialConstants.normalizeCategoryName(it.category) }
                    .map { (catName, list) ->
                        val catSum = list.sumOf { it.amount }
                        SimpleCategoryStat(
                            name = catName,
                            amount = catSum,
                            percentage = (catSum / volume).toFloat()
                        )
                    }
                    .sortedByDescending { it.amount }

                MonthlyExpenseStat(
                    yearMonth = ym,
                    label = label,
                    totalAmount = monthTotal,
                    categories = cats
                )
            }
    }

    val formattedStart = remember(filterStartDate) { DateFilterUtils.formatDateArabic(filterStartDate) }
    val formattedEnd = remember(filterEndDate) { DateFilterUtils.formatDateArabic(filterEndDate) }
    val periodDisplay = remember(filterPreset, formattedStart, formattedEnd, isArabic) {
        when (filterPreset) {
            DateFilterPreset.THIS_MONTH -> if (isArabic) "هذا الشهر" else "This Month"
            DateFilterPreset.LAST_3_MONTHS -> if (isArabic) "آخر 3 شهور" else "Last 3 Months"
            DateFilterPreset.THIS_YEAR -> if (isArabic) "هذه السنة" else "This Year"
            DateFilterPreset.ALL -> if (isArabic) "الكل" else "All Time"
            DateFilterPreset.CUSTOM -> "$formattedStart - $formattedEnd"
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isCustomActive = filterPreset == DateFilterPreset.CUSTOM
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { showCustomDateDialog = true }
                        .testTag("open_date_picker_btn"),
                    shape = CircleShape,
                    color = if (isCustomActive) SpotifyGreen else InputChipSurface,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = if (isCustomActive) OnSpotifyGreen else TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCustomActive) "$formattedStart - $formattedEnd" else if (isArabic) "تحديد فترة" else "Custom",
                            fontSize = 12.sp,
                            fontWeight = if (isCustomActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCustomActive) OnSpotifyGreen else TextPrimary
                        )
                    }
                }
            }
        }

        // 1. Period Filter Chips: Spotify-style fully rounded pills
        item {
            val quickPresets = listOf(
                DateFilterPreset.THIS_MONTH,
                DateFilterPreset.LAST_3_MONTHS,
                DateFilterPreset.THIS_YEAR,
                DateFilterPreset.ALL
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickPresets.forEach { preset ->
                    val isSelected = filterPreset == preset
                    val label = when (preset) {
                        DateFilterPreset.THIS_MONTH -> if (isArabic) "هذا الشهر" else "Month"
                        DateFilterPreset.LAST_3_MONTHS -> if (isArabic) "آخر 3 شهور" else "3 Months"
                        DateFilterPreset.THIS_YEAR -> if (isArabic) "هذه السنة" else "Year"
                        DateFilterPreset.ALL -> if (isArabic) "الكل" else "All"
                        else -> if (isArabic) preset.titleAr else preset.titleEn
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable {
                                val range = DateFilterUtils.calculatePresetRange(preset, allTransactions)
                                viewModel.setDateFilter(range.first, range.second, preset)
                            }
                            .testTag("filter_pill_${preset.name.lowercase()}"),
                        shape = CircleShape,
                        color = if (isSelected) SpotifyGreen else InputChipSurface,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnSpotifyGreen else TextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Total Expenses Summary Card: #121212 surface, 12dp radius, zero shadow
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "إجمالي المصروفات" else "Total Expenses",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Formatters.formatMoney(totalExpenses)} $currencySymbol",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }

                    val isExportEnabled = !isGenerating && expenseTransactions.isNotEmpty()
                    Button(
                        onClick = {
                            viewModel.exportExpensesPdfToDownloads(
                                context = context,
                                periodTitle = periodDisplay,
                                customTransactions = expenseTransactions,
                                startDateStr = formattedStart,
                                endDateStr = formattedEnd
                            )
                        },
                        enabled = isExportEnabled,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = OnSpotifyGreen,
                            disabledContainerColor = InputChipSurface,
                            disabledContentColor = TextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("export_pdf_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = XBlue,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PictureAsPdf,
                                contentDescription = null,
                                tint = if (isExportEnabled) OnSpotifyGreen else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تصدير PDF" else "Export PDF",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExportEnabled) OnSpotifyGreen else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 3. Categories Distribution Card (المصروفات حسب الفئة): #121212 surface, 12dp radius, zero shadow
        // Distribution colors in exact order: #1DB954, #1D9BF0, #7856FF, #FF7A00, #F91880, #FFD400
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = if (isArabic) "المصروفات حسب الفئة" else "Expenses by Category",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryStats.isEmpty()) {
                        Text(
                            text = if (isArabic) "لا توجد مصروفات في هذه الفترة" else "No expenses in this period",
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        categoryStats.forEachIndexed { index, cat ->
                            val chartColor = ChartPalette[index % ChartPalette.size]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // 24dp outlined icon with NO colored background circle
                                        Icon(
                                            imageVector = FinancialConstants.getCategoryIcon(cat.name),
                                            contentDescription = null,
                                            tint = chartColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.0f", cat.percentage * 100)}%",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    Text(
                                        text = "${Formatters.formatMoney(cat.amount)} $currencySymbol",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(InputChipSurface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(cat.percentage.coerceIn(0.02f, 1f))
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(chartColor)
                                    )
                                }
                            }

                            if (index < categoryStats.lastIndex) {
                                HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Monthly Expenses History (سجل الشهور): Flat rows on #000000 black background with #2F3336 divider
        if (monthlyExpenseStats.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppBackground)
                        .testTag("monthly_history_card")
                ) {
                    Text(
                        text = if (isArabic) "سجل الشهور" else "Monthly History",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    monthlyExpenseStats.forEach { monthStat ->
                        val isExpanded = expandedYearMonth == monthStat.yearMonth

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppBackground)
                                .clickable {
                                    expandedYearMonth = if (isExpanded) null else monthStat.yearMonth
                                }
                                .padding(horizontal = 4.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 24dp outlined icon with NO colored background square
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Text(
                                        text = monthStat.label,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${Formatters.formatMoney(monthStat.totalAmount)} $currencySymbol",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) {
                                            Icons.Outlined.KeyboardArrowDown
                                        } else {
                                            Icons.AutoMirrored.Outlined.KeyboardArrowRight
                                        },
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    monthStat.categories.forEach { cat ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = cat.name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${String.format(Locale.US, "%.0f", cat.percentage * 100)}%",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                            Text(
                                                text = "${Formatters.formatMoney(cat.amount)} $currencySymbol",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = DividerBorderColor,
                            thickness = 0.6.dp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    if (showCustomDateDialog) {
        DateRangeSelectionDialog(
            currentStartMillis = filterStartDate,
            currentEndMillis = filterEndDate,
            isArabic = isArabic,
            onDismiss = { showCustomDateDialog = false },
            onApply = { start: Long, end: Long ->
                viewModel.setDateFilter(
                    DateFilterUtils.getStartOfDay(start),
                    DateFilterUtils.getEndOfDay(end),
                    DateFilterPreset.CUSTOM
                )
                showCustomDateDialog = false
            }
        )
    }
}
