package com.example.ui.reports

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeDark
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmAmberWarning
import com.example.ui.theme.WarmBeigeBackground
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmOffWhiteSurface
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// -------------------------------------------------------------------------
// Warm & Harmonious Palette (متناسقة تماماً مع هوية تطبيق محفظة نبيه)
// -------------------------------------------------------------------------
private val CardWhite = WarmOffWhiteSurface
private val PillSelectedBg = BurntOrangePrimary
private val PillUnselectedBg = WarmCardSurface
private val ArcTrackWarm = Color(0xFFECEAE2)

// الألوان المتناسقة للرسم البياني المقوس (Terracotta, Olive, Forest Green, Mustard, Teal, Deep Earth)
private val WarmArcPalette = listOf(
    BurntOrangePrimary,                // برتقالي طوبي - هوية التطبيق الأساسية
    Color(0xFF4A5568),                 // نيلي رمادي عميق
    MutedIncomeGreen,                  // أخضر مالي هادئ
    Color(0xFFE29578),                 // مرجاني دافئ
    Color(0xFF006D77),                 // بترولي عميق
    Color(0xFFD4A373)                  // خردلي رملي
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

    // 1. Transactions in Date Range (Expenses)
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

    // 2. Categories Breakdown & Color Mapping
    data class CategoryStat(
        val name: String,
        val amount: Double,
        val percentage: Float,
        val color: Color
    )

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
        grouped.mapIndexed { index, pair ->
            val color = WarmArcPalette[index % WarmArcPalette.size]
            val pct = (pair.second / volume).toFloat()
            CategoryStat(pair.first, pair.second, pct, color)
        }
    }

    // 3. Month Breakdown Card Data (Last months with spending)
    data class MonthHistoryItem(
        val startMillis: Long,
        val endMillis: Long,
        val title: String,
        val amount: Double
    )

    val monthlyHistory = remember(allTransactions, isArabic) {
        val activeMonths = MonthlyCalculationUtils.getAllActiveMonths(allTransactions)
        val sdf = SimpleDateFormat("MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
        activeMonths.mapNotNull { ym ->
            val monthSpent = MonthlyCalculationUtils.calculateMonthlyExpenses(allTransactions, ym.year, ym.monthValue)
            if (monthSpent > 0.0) {
                val startMillis = MonthlyCalculationUtils.getMonthStartMillis(ym.year, ym.monthValue)
                val endMillis = MonthlyCalculationUtils.getMonthEndMillis(ym.year, ym.monthValue)
                MonthHistoryItem(startMillis, endMillis, sdf.format(Date(startMillis)), monthSpent)
            } else null
        }.sortedByDescending { it.startMillis }
        .take(6)
    }

    // Date range labels
    val formattedStart = remember(filterStartDate) { DateFilterUtils.formatDateArabic(filterStartDate) }
    val formattedEnd = remember(filterEndDate) { DateFilterUtils.formatDateArabic(filterEndDate) }
    val periodDisplay = remember(filterPreset, formattedStart, formattedEnd, isArabic) {
        when (filterPreset) {
            DateFilterPreset.THIS_MONTH -> if (isArabic) "هذا الشهر • $formattedStart - $formattedEnd" else "This Month • $formattedStart - $formattedEnd"
            DateFilterPreset.LAST_3_MONTHS -> if (isArabic) "آخر 3 شهور • $formattedStart - $formattedEnd" else "Last 3 Months • $formattedStart - $formattedEnd"
            DateFilterPreset.THIS_YEAR -> if (isArabic) "هذه السنة • $formattedStart - $formattedEnd" else "This Year • $formattedStart - $formattedEnd"
            DateFilterPreset.ALL -> if (isArabic) "كافة الفترات" else "All Time"
            DateFilterPreset.CUSTOM -> "$formattedStart - $formattedEnd"
        }
    }

    val periodSubText = remember(filterPreset, formattedStart, formattedEnd, isArabic) {
        if (filterPreset == DateFilterPreset.ALL) {
            if (isArabic) "كافة الفترات المسجلة" else "All recorded transactions"
        } else {
            "$formattedStart - $formattedEnd"
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBeigeBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(10.dp)) }

            // =========================================================================
            // 1. TOP CONTROLS CARD (قسم التحكم العلوي مع تصفح التاريخ السريع المتوازن)
            // =========================================================================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.5.dp, BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Title + Custom Date Picker trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isArabic) "المصروفات" else "Spending",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•  $periodSubText",
                                    fontSize = 11.5.sp,
                                    color = TextSecondaryBrown
                                )
                            }

                            // Clickable Date Picker trigger
                            val isCustomActive = filterPreset == DateFilterPreset.CUSTOM
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showCustomDateDialog = true }
                                    .testTag("open_date_picker_btn"),
                                color = if (isCustomActive) BurntOrangePrimary.copy(alpha = 0.12f) else WarmCardSurface,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isCustomActive) BurntOrangePrimary else BorderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "تحديد فترة" else "Custom",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCustomActive) BurntOrangePrimary else TextPrimaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Equal-width Filter Pills Row (هذا الشهر | آخر 3 شهور | هذه السنة | الكل)
                        val quickPresets = listOf(
                            DateFilterPreset.THIS_MONTH,
                            DateFilterPreset.LAST_3_MONTHS,
                            DateFilterPreset.THIS_YEAR,
                            DateFilterPreset.ALL
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val range = DateFilterUtils.calculatePresetRange(preset, allTransactions)
                                            viewModel.setDateFilter(range.first, range.second, preset)
                                        }
                                        .testTag("filter_pill_${preset.name.lowercase()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) BurntOrangePrimary else PillUnselectedBg,
                                    border = BorderStroke(0.5.dp, if (isSelected) BurntOrangePrimary else BorderSubtle),
                                    shadowElevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.8.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextPrimaryDark,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. MAIN CHART CARD (كارت الرسم البياني والمبلغ الإجمالي والفئات)
            // =========================================================================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.5.dp, BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Header Row: Total Expense Amount on right, Top Category & Count badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isArabic) "إجمالي المصروفات" else "Total Spending",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryBrown
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${Formatters.formatMoney(totalExpenses)} $currencySymbol",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryDark,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            // Compact Operations Badge on opposite side
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WarmCardSurface,
                                border = BorderStroke(0.5.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "${expenseTransactions.size} عملية" else "${expenseTransactions.size} items",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }

                        // Balanced 2-Column Stat Summary Cards (No empty space)
                        Spacer(modifier = Modifier.height(12.dp))
                        val topCatStat = categoryStats.firstOrNull()
                        val avgPerTx = if (expenseTransactions.isNotEmpty()) totalExpenses / expenseTransactions.size else 0.0

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stat 1: Highest Category
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = WarmCardSurface,
                                border = BorderStroke(0.5.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = WarmAmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isArabic) "الأعلى:" else "Top:",
                                            fontSize = 11.sp,
                                            color = TextSecondaryBrown
                                        )
                                    }
                                    Text(
                                        text = topCatStat?.name ?: (if (isArabic) "—" else "—"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Stat 2: Average Transaction
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = WarmCardSurface,
                                border = BorderStroke(0.5.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "المتوسط:" else "Avg:",
                                        fontSize = 11.sp,
                                        color = TextSecondaryBrown
                                    )
                                    Text(
                                        text = "${Formatters.formatMoney(avgPerTx)} $currencySymbol",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Semi-donut / Arc Chart with Center Summary Caption
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(138.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            SemiCircleSpendingArcChart(
                                percentages = categoryStats.map { it.percentage },
                                colors = categoryStats.map { it.color },
                                modifier = Modifier.fillMaxSize()
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "${categoryStats.size} فئات" else "${categoryStats.size} Categories",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                if (topCatStat != null) {
                                    Text(
                                        text = "${topCatStat.name} • ${String.format(Locale.US, "%.0f", topCatStat.percentage * 100)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondaryBrown
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Categories Breakdown Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "توزيع الفئات" else "Categories Breakdown",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (isArabic) "النسبة والمبلغ" else "Share & Amount",
                                fontSize = 11.sp,
                                color = TextSecondaryBrown
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (categoryStats.isEmpty()) {
                            Text(
                                text = if (isArabic) "لا توجد مصروفات مسجلة في هذه الفترة" else "No expenses recorded for this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedBrown,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            categoryStats.forEachIndexed { index, cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Category Rounded Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cat.color.copy(alpha = 0.13f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = FinancialConstants.getCategoryIcon(cat.name),
                                            contentDescription = null,
                                            tint = cat.color,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Right-to-Left Column: Top (Name + % Pill + Amount), Bottom (Progress Bar)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = cat.name,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = TextPrimaryDark
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = cat.color.copy(alpha = 0.12f)
                                                ) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", cat.percentage * 100)}%",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = cat.color,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "${Formatters.formatMoney(cat.amount)} $currencySymbol",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Smooth progress bar aligned with text column
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(CircleShape)
                                                .background(ArcTrackWarm)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(cat.percentage.coerceIn(0.03f, 1f))
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .background(cat.color)
                                            )
                                        }
                                    }
                                }

                                if (index < categoryStats.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 2.dp)
                                            .height(0.5.dp)
                                            .background(BorderSubtle.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. MONTH BREAKDOWN CARD (كارت الشهور السفلي)
            // =========================================================================
            if (monthlyHistory.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.5.dp, BorderSubtle),
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = if (isArabic) "سجل الشهور" else "Monthly History",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            monthlyHistory.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.setDateFilter(
                                                item.startMillis,
                                                item.endMillis,
                                                DateFilterPreset.CUSTOM
                                            )
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(BurntOrangePrimary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = BurntOrangePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${Formatters.formatMoney(item.amount)} $currencySymbol",
                                            fontSize = 13.8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = TextSecondaryBrown,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }

                                if (index < monthlyHistory.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(BorderSubtle.copy(alpha = 0.55f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. INLINE FLAT PRIMARY EXPORT BUTTON (زر تصدير PDF مدمج ومسطح ضمن عناصر الشاشة)
            // =========================================================================
            item {
                val exportInteractionSource = remember { MutableInteractionSource() }
                val isExportPressed by exportInteractionSource.collectIsPressedAsState()
                val isExportHovered by exportInteractionSource.collectIsHoveredAsState()
                val isExportEnabled = !isGenerating && expenseTransactions.isNotEmpty()

                val animatedExportBgColor by animateColorAsState(
                    targetValue = when {
                        !isExportEnabled -> WarmCardSurface
                        isExportPressed -> BurntOrangeDark
                        isExportHovered -> BurntOrangeDark.copy(alpha = 0.92f)
                        else -> BurntOrangePrimary
                    },
                    animationSpec = tween(durationMillis = 150),
                    label = "export_btn_bg"
                )

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
                    interactionSource = exportInteractionSource,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 0.6.dp,
                        color = if (isExportEnabled) BurntOrangeDark.copy(alpha = 0.25f) else BorderSubtle
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedExportBgColor,
                        contentColor = Color.White,
                        disabledContainerColor = WarmCardSurface,
                        disabledContentColor = TextMutedBrown
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("export_pdf_btn")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "جاري تصدير ملف PDF..." else "Exporting PDF...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isExportEnabled) Color.White.copy(alpha = 0.18f)
                                    else BorderSubtle.copy(alpha = 0.6f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = if (isExportEnabled) Color.White else TextMutedBrown,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "تصدير PDF" else "Export PDF",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExportEnabled) Color.White else TextMutedBrown
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Custom Date Range Dialog
        if (showCustomDateDialog) {
            DateRangeSelectionDialog(
                currentStartMillis = filterStartDate,
                currentEndMillis = filterEndDate,
                isArabic = isArabic,
                onDismiss = { showCustomDateDialog = false },
                onApply = { start, end ->
                    showCustomDateDialog = false
                    viewModel.setDateFilter(start, end, DateFilterPreset.CUSTOM)
                }
            )
        }
    }
}

/**
 * Semi-donut / Arc Chart (SemiCircleSpendingArcChart)
 * Draws a wide, smooth sweep arc via Compose Canvas with StrokeCap.Round
 */
@Composable
fun SemiCircleSpendingArcChart(
    percentages: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(percentages) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 850))
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 32.dp.toPx()
        val diameter = size.width.coerceAtMost(size.height * 2.1f) - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset((size.width - diameter) / 2f, size.height - diameter / 2f)

        // 1. Subtle background track arc in warm cream
        drawArc(
            color = ArcTrackWarm,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Colored Category Arcs
        var startAngle = 180f
        percentages.forEachIndexed { index, pct ->
            val sweep = pct * 180f * progress.value
            if (sweep > 0f) {
                drawArc(
                    color = colors.getOrElse(index) { Color.LightGray },
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
    }
}
