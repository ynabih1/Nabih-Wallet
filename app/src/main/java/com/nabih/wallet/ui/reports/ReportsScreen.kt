package com.nabih.wallet.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.model.CategoryExpenseSummary
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.data.model.YearMonthKey
import com.nabih.wallet.pdf.PdfExpenseReportData
import com.nabih.wallet.ui.WalletUiState
import com.nabih.wallet.ui.components.EmptyStateCard
import com.nabih.wallet.ui.components.ExpressiveLinearProgressIndicator
import com.nabih.wallet.ui.components.ExpressiveSegmentedCardItem
import com.nabih.wallet.ui.components.SectionHeaderRow
import com.nabih.wallet.ui.components.TonalCategoryIcon
import com.nabih.wallet.ui.components.TonalSquircleIcon
import com.nabih.wallet.ui.home.TransactionRowItem
import com.nabih.wallet.ui.theme.CategoryColors
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    uiState: WalletUiState,
    onSelectMonth: (YearMonthKey?) -> Unit,
    onCategoryClick: (CategoryExpenseSummary) -> Unit,
    onOpenPdfPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        LargeTopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            title = {
                Text(
                    text = stringResource(R.string.nav_expenses),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            actions = {
                FilledTonalButton(
                    onClick = onOpenPdfPreview,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("export_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.export_pdf),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 6.dp,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.selectedReportMonth != null) {
                item(key = "active_month_filter") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { onSelectMonth(null) },
                            label = {
                                Text("الفترة: ${Formatters.monthYear(uiState.selectedReportMonth)}")
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "إلغاء الفلتر",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            item(key = "total_expenses_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reports_total_card"),
                    shape = RoundedCornerShape(28.dp),
                    color = semantic.expenseContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildString {
                                    append(stringResource(R.string.total_expenses))
                                    uiState.selectedReportMonth?.let {
                                        append(" • ")
                                        append(Formatters.monthYear(it))
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = semantic.onExpenseContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatAmount(uiState.reportTotalExpense),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = semantic.onExpenseContainer
                            )
                        }

                        TonalSquircleIcon(
                            icon = Icons.Rounded.PieChart,
                            contentDescription = null,
                            containerColor = semantic.onExpenseContainer.copy(alpha = 0.12f),
                            iconTint = semantic.onExpenseContainer,
                            boxSize = 48.dp,
                            iconSize = 24.dp
                        )
                    }
                }
            }

            item(key = "categories_header") {
                SectionHeaderRow(title = stringResource(R.string.expenses_by_category))
            }

            if (uiState.expensesByCategory.isEmpty()) {
                item(key = "empty_categories") {
                    EmptyStateCard(
                        icon = Icons.Rounded.PieChart,
                        title = "لا توجد مصروفات بعد",
                        subtitle = "عند تسجيل مصروفاتك سيظهر توزيعها التفصيلي حسب الفئة هنا"
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.expensesByCategory,
                    key = { _, item -> "cat_sum_${item.categoryId}" }
                ) { index, summary ->
                    val accent = CategoryColors.accentColor(summary.colorKey, isDark)
                    ExpressiveSegmentedCardItem(
                        index = index,
                        count = uiState.expensesByCategory.size,
                        onClick = { onCategoryClick(summary) },
                        modifier = Modifier.testTag("report_category_${summary.categoryId}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TonalCategoryIcon(
                                    iconKey = summary.iconKey,
                                    colorKey = summary.colorKey,
                                    contentDescription = summary.categoryName
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = summary.categoryName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${Formatters.formatPercent(summary.percentage)} من الإجمالي",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = Formatters.formatAmount(summary.totalAmount),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            ExpressiveLinearProgressIndicator(
                                progress = (summary.percentage / 100.0).toFloat(),
                                activeColor = accent,
                                height = 8.dp
                            )
                        }
                    }
                    if (index < uiState.expensesByCategory.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }

            if (uiState.monthlyHistory.isNotEmpty()) {
                item(key = "months_header") {
                    SectionHeaderRow(title = stringResource(R.string.months_history))
                }

                itemsIndexed(
                    items = uiState.monthlyHistory,
                    key = { _, item -> "month_${item.key.year}_${item.key.month}" }
                ) { index, monthItem ->
                    val isSelected = uiState.selectedReportMonth == monthItem.key
                    ExpressiveSegmentedCardItem(
                        index = index,
                        count = uiState.monthlyHistory.size,
                        onClick = { onSelectMonth(monthItem.key) },
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            null
                        },
                        modifier = Modifier.testTag("month_history_${monthItem.key.year}_${monthItem.key.month}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TonalSquircleIcon(
                                icon = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = Formatters.monthYear(monthItem.key),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Formatters.formatAmount(monthItem.totalExpense),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (index < uiState.monthlyHistory.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryName: String,
    iconKey: String,
    colorKey: String,
    transactions: List<TransactionItem>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val total = transactions.sumOf { it.entity.amount }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val summarySurface = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("category_detail_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            title = {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "cat_summary_card") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = summarySurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TonalCategoryIcon(
                            iconKey = iconKey,
                            colorKey = colorKey,
                            contentDescription = categoryName,
                            boxSize = 52.dp,
                            iconSize = 26.dp
                        )
                        Column {
                            Text(
                                text = "إجمالي $categoryName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatAmount(total),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item(key = "cat_tx_header") {
                SectionHeaderRow(title = "العمليات المسجلة (${transactions.size})")
            }

            if (transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.PieChart,
                        title = "لا توجد عمليات في هذه الفئة",
                        subtitle = "تم حذف جميع العمليات أو نقلها"
                    )
                }
            } else {
                itemsIndexed(
                    items = transactions,
                    key = { _, item -> "cat_tx_${item.entity.id}" }
                ) { index, item ->
                    TransactionRowItem(
                        item = item,
                        index = index,
                        count = transactions.size,
                        onClick = { onTransactionClick(item) }
                    )
                    if (index < transactions.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewBottomSheet(
    report: PdfExpenseReportData,
    onDismiss: () -> Unit,
    onSharePdf: () -> Unit,
    onSavePdfDirect: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.pdf_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "إغلاق"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // A4-styled report preview card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .border(
                        width = 1.dp,
                        color = semantic.pdfBorder,
                        shape = RoundedCornerShape(22.dp)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nabih_wallet_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nabih Wallet",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تقرير المصروفات — ${report.periodLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تاريخ التصدير: ${Formatters.formatFullDate(report.exportedAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = semantic.pdfCardSurface
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.total_expenses),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = Formatters.formatAmount(report.totalExpense),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = semantic.expense
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = semantic.pdfBorder)

                    // Table header
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = semantic.pdfCardSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الفئة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = "النسبة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = "المبلغ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (report.categories.isEmpty()) {
                        Text(
                            text = "لا توجد مصروفات مسجلة في هذه الفترة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else {
                        report.categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.4f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CategoryColors.accentColor(cat.colorKey, isDark))
                                    )
                                    Text(
                                        text = cat.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = Formatters.formatPercent(cat.percentage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.6f)
                                )
                                Text(
                                    text = Formatters.formatAmount(cat.totalAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = semantic.pdfBorder.copy(alpha = 0.6f))
                        }
                    }

                    // Total row
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = semantic.pdfCardSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "الإجمالي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.6f)
                            )
                            Text(
                                text = Formatters.formatAmount(report.totalExpense),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pdf_footer_brand),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "صفحة 1 من 1",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSharePdf,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("pdf_share_button"),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.save_and_share_pdf),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSavePdfDirect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pdf_save_device_button"),
                shape = CircleShape
            ) {
                Text(
                    text = "حفظ الملف على الجهاز",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
