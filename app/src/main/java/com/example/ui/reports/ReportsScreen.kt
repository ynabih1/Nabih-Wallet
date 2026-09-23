package com.example.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeDark
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
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
        allTransactions.filter {
            it.type == "EXPENSE" && it.dateMillis in filterStartDate..filterEndDate
        }
    }

    val totalExpenses = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    // 2. Categories Breakdown & Color Mapping
    data class CategoryStat(
        val name: String,
        val amount: Double,
        val percentage: Float,
        val color: Color
    )

    val categoryStats = remember(expenseTransactions, totalExpenses) {
        val grouped = expenseTransactions
            .groupBy { FinancialConstants.normalizeCategoryName(it.category) }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }

        val volume = if (totalExpenses > 0.0) totalExpenses else 1.0
        grouped.mapIndexed { index, pair ->
            val color = WarmArcPalette[index % WarmArcPalette.size]
            val pct = (pair.second / volume).toFloat()
            CategoryStat(pair.first, pair.second, pct, color)
        }
    }

    // 3. Month Breakdown Card Data (Last months with spending)
    val monthlyHistory = remember(allTransactions, isArabic) {
        val expenses = allTransactions.filter { it.type == "EXPENSE" }
        val sdf = SimpleDateFormat("MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)
        expenses.groupBy {
            val cal = Calendar.getInstance().apply {
                timeInMillis = it.dateMillis
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }.map { (monthMillis, list) ->
            Triple(monthMillis, sdf.format(Date(monthMillis)), list.sumOf { it.amount })
        }.sortedByDescending { it.first }
        .take(4)
    }

    // Date range labels
    val formattedStart = remember(filterStartDate) { DateFilterUtils.formatDateArabic(filterStartDate) }
    val formattedEnd = remember(filterEndDate) { DateFilterUtils.formatDateArabic(filterEndDate) }
    val periodDisplay = remember(filterPreset, formattedStart, formattedEnd, isArabic) {
        when (filterPreset) {
            DateFilterPreset.THIS_MONTH -> if (isArabic) "هذا الشهر ($formattedStart - $formattedEnd)" else "This Month ($formattedStart - $formattedEnd)"
            DateFilterPreset.LAST_3_MONTHS -> if (isArabic) "آخر 3 شهور ($formattedStart - $formattedEnd)" else "Last 3 Months ($formattedStart - $formattedEnd)"
            DateFilterPreset.THIS_YEAR -> if (isArabic) "هذه السنة ($formattedStart - $formattedEnd)" else "This Year ($formattedStart - $formattedEnd)"
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
            // 1. TOP CONTROLS CARD (قسم التحكم العلوي مع تصفح التاريخ السريع المحسن)
            // «هذا الشهر» | «آخر 3 شهور» | «هذه السنة» | مخصص
            // =========================================================================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Title + Active Period Range
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) "المصروفات" else "Spending",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            // Clickable Date Picker trigger
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCustomDateDialog = true }
                                    .testTag("open_date_picker_btn"),
                                color = WarmCardSurface,
                                border = BorderStroke(0.8.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isArabic) "تخصيص" else "Custom",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Filter Pills Row (هذا الشهر | آخر 3 شهور | هذه السنة | الكل)
                        val quickPresets = listOf(
                            DateFilterPreset.THIS_MONTH,
                            DateFilterPreset.LAST_3_MONTHS,
                            DateFilterPreset.THIS_YEAR,
                            DateFilterPreset.ALL
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            quickPresets.forEach { preset ->
                                val isSelected = filterPreset == preset
                                val label = when (preset) {
                                    DateFilterPreset.THIS_MONTH -> if (isArabic) "هذا الشهر" else "This Month"
                                    DateFilterPreset.LAST_3_MONTHS -> if (isArabic) "آخر 3 شهور" else "Last 3 Months"
                                    DateFilterPreset.THIS_YEAR -> if (isArabic) "هذه السنة" else "This Year"
                                    DateFilterPreset.ALL -> if (isArabic) "الكل" else "All Time"
                                    else -> if (isArabic) preset.titleAr else preset.titleEn
                                }

                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            val range = DateFilterUtils.calculatePresetRange(preset, allTransactions)
                                            viewModel.setDateFilter(range.first, range.second, preset)
                                        }
                                        .testTag("filter_pill_${preset.name.lowercase()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) BurntOrangePrimary else PillUnselectedBg,
                                    border = BorderStroke(1.dp, if (isSelected) BurntOrangePrimary else BorderSubtle)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimaryDark,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. MAIN CHART CARD (كارت الرسم البياني والمبلغ الإجمالي)
            // Displays:
            // - Top: Total Amount in bold, active date range below in TextSecondaryBrown
            // - Center: Semi-donut Arc Chart in harmonious warm colors
            // - Bottom: Categories breakdown with color dot, percentage, and amount
            // =========================================================================
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderSubtle),
                    shadowElevation = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        // Header Row: Amount + Date Subtitle on one side, Filter button on the other
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "${Formatters.formatMoney(totalExpenses)} $currencySymbol",
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimaryDark,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = periodSubText,
                                    fontSize = 12.sp,
                                    color = TextSecondaryBrown
                                )
                            }

                            // Filter ⊶ Oval Button (Opens custom date dialog)
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showCustomDateDialog = true },
                                color = WarmCardSurface,
                                border = BorderStroke(0.8.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isArabic) "تحديد تاريخ" else "Filter",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Semi-donut / Arc Chart (Sweep Arc with StrokeCap.Round)
                        SemiCircleSpendingArcChart(
                            percentages = categoryStats.map { it.percentage },
                            colors = categoryStats.map { it.color },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Categories Breakdown Title
                        Text(
                            text = if (isArabic) "الفئات" else "Categories",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (categoryStats.isEmpty()) {
                            Text(
                                text = if (isArabic) "لا توجد مصروفات مسجلة في هذه الفترة" else "No expenses recorded for this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedBrown,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            categoryStats.forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(cat.color)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${String.format(Locale.US, "%.1f", cat.percentage * 100)}%)",
                                            fontSize = 11.5.sp,
                                            color = TextSecondaryBrown
                                        )
                                    }

                                    Text(
                                        text = "${Formatters.formatMoney(cat.amount)} $currencySymbol",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 3. MONTH BREAKDOWN CARD (كارت التاريخ/الشهور السفلي)
            // Displays month name, total spending and directional chevron >
            // =========================================================================
            if (monthlyHistory.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, BorderSubtle),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = if (isArabic) "الشهور" else "Month",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            monthlyHistory.forEachIndexed { index, (_, monthTitle, amount) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = monthTitle,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = TextPrimaryDark
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${Formatters.formatMoney(amount)} $currencySymbol",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isArabic) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = TextSecondaryBrown,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (index < monthlyHistory.lastIndex) {
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.6.dp)
                                            .background(BorderSubtle.copy(alpha = 0.6f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom spacing to avoid content overlap by the floating export bar
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        // =========================================================================
        // 4. FLOATING EXPORT & SHARE BAR (أزرار التصدير العائمة في الأسفل)
        // Light floating pill container with download and share buttons
        // =========================================================================
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = CardWhite,
            border = BorderStroke(1.dp, BorderSubtle),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PDF Export Button
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
                    enabled = !isGenerating && expenseTransactions.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurntOrangePrimary,
                        disabledContainerColor = BurntOrangePrimary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("export_pdf_btn")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "جاري الإنشاء..." else "Generating...",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تصدير تقرير PDF" else "Export PDF Report",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Quick Share Button
                OutlinedButton(
                    onClick = {
                        viewModel.shareExpensesPdf(
                            context = context,
                            periodTitle = periodDisplay,
                            customTransactions = expenseTransactions,
                            startDateStr = formattedStart,
                            endDateStr = formattedEnd
                        )
                    },
                    enabled = !isGenerating && expenseTransactions.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("share_pdf_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = if (isArabic) "مشاركة" else "Share",
                        tint = BurntOrangePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
