package com.example.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import com.example.ui.WalletViewModel
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ClassicReportTab {
    TRANSACTIONS, DEBTS
}

enum class ReportPeriod(val titleAr: String, val titleEn: String) {
    ALL("كافة الفترات", "All Time"),
    THIS_MONTH("هذا الشهر", "This Month"),
    LAST_MONTH("الشهر الماضي", "Last Month")
}

@Composable
fun ReportsScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allDebts by viewModel.allDebtsWithPayments.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingPdf.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var selectedTab by remember { mutableStateOf(ClassicReportTab.TRANSACTIONS) }
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.ALL) }

    val periodString = if (isArabic) selectedPeriod.titleAr else selectedPeriod.titleEn

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header with Original PieChart Icon
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BurntOrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "التقارير وتصدير PDF" else "Reports & Export PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isArabic) "تصدير بيان مالي منظم بصيغة PDF" else "Export organized financial statement",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryBrown
                        )
                    }
                }
            }
        }

        // 2. 2-Tab Selector (Transactions vs Debts)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = WarmCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val isTransactions = selectedTab == ClassicReportTab.TRANSACTIONS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTransactions) BurntOrangePrimary else Color.Transparent)
                            .clickable { selectedTab = ClassicReportTab.TRANSACTIONS }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "تقرير المعاملات" else "Transactions",
                            color = if (isTransactions) Color.White else TextPrimaryDark,
                            fontWeight = if (isTransactions) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    val isDebts = selectedTab == ClassicReportTab.DEBTS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDebts) BurntOrangePrimary else Color.Transparent)
                            .clickable { selectedTab = ClassicReportTab.DEBTS }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "تقرير الديون" else "Debts",
                            color = if (isDebts) Color.White else TextPrimaryDark,
                            fontWeight = if (isDebts) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 3. Period Selector (For Transactions)
        if (selectedTab == ClassicReportTab.TRANSACTIONS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportPeriod.entries.forEach { period ->
                        val isSelected = selectedPeriod == period
                        val title = if (isArabic) period.titleAr else period.titleEn
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedPeriod = period },
                            color = if (isSelected) BurntOrangePrimary else WarmCardSurface,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BurntOrangePrimary else BorderSubtle)
                        ) {
                            Text(
                                text = title,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimaryDark
                            )
                        }
                    }
                }
            }
        }

        // 4. Exact Visual PDF Preview Card (Matching ExportUtils)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pdf_preview_card"),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7DFD2)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Document Header
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val statementName = when (selectedTab) {
                            ClassicReportTab.TRANSACTIONS -> "تقرير: بيان المصروفات الشخصية"
                            ClassicReportTab.DEBTS -> "تقرير: المديونيات"
                        }
                        Text(
                            text = statementName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B2620)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val expenseList = allTransactions.filter { it.type == "EXPENSE" }
                        val targetList = if (expenseList.isNotEmpty()) expenseList else allTransactions
                        val computedPeriod = if (targetList.isNotEmpty()) {
                            val minD = targetList.minOf { it.dateMillis }
                            val maxD = targetList.maxOf { it.dateMillis }
                            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
                            "من ${sdf.format(Date(minD))} إلى ${sdf.format(Date(maxD))}"
                        } else {
                            periodString
                        }

                        if (selectedTab == ClassicReportTab.TRANSACTIONS) {
                            Text(
                                text = "الفترة: $computedPeriod",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8A8171)
                            )
                        }

                        val issueDateStr = SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())
                        Text(
                            text = "تاريخ إصدار التقرير: $issueDateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A8171)
                        )

                        val count = if (selectedTab == ClassicReportTab.TRANSACTIONS) {
                            targetList.groupBy { it.category }.size
                        } else allDebts.map { it.debt.personName }.distinct().size

                        Text(
                            text = if (selectedTab == ClassicReportTab.TRANSACTIONS) "عدد البنود: $count" else "عدد الأشخاص: $count",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8A8171)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview Table
                    when (selectedTab) {
                        ClassicReportTab.TRANSACTIONS -> {
                            TransactionsPdfTablePreview(
                                transactions = allTransactions,
                                currencySymbol = currencySymbol,
                                isArabic = isArabic
                            )
                        }
                        ClassicReportTab.DEBTS -> {
                            DebtsPdfTablePreview(
                                debts = allDebts,
                                currencySymbol = currencySymbol,
                                isArabic = isArabic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "تم إنشاء هذا التقرير بواسطة Nabih Wallet",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8A8171)
                    )
                }
            }
        }

        // 5. Action Buttons (Export to Downloads & Share)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action: Export directly to Downloads
                Button(
                    onClick = {
                        when (selectedTab) {
                            ClassicReportTab.TRANSACTIONS -> viewModel.exportExpensesPdfToDownloads(context, periodString)
                            ClassicReportTab.DEBTS -> viewModel.exportDebtsPdfToDownloads(context)
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isArabic) "جاري إنشاء وتصدير ملف PDF..." else "Generating PDF Report...",
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "حفظ وتصدير PDF في التنزيلات" else "Export PDF to Downloads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Secondary Action: Share PDF
                OutlinedButton(
                    onClick = {
                        when (selectedTab) {
                            ClassicReportTab.TRANSACTIONS -> viewModel.shareExpensesPdf(context, periodString)
                            ClassicReportTab.DEBTS -> viewModel.shareDebtsPdf(context)
                        }
                    },
                    enabled = !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("share_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BurntOrangePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = BurntOrangePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "مشاركة ملف الـ PDF" else "Share PDF Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BurntOrangePrimary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun TransactionsPdfTablePreview(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    isArabic: Boolean
) {
    val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
    val targetList = if (expenseTransactions.isNotEmpty()) expenseTransactions else transactions

    data class CatRow(val name: String, val amount: Double, val note: String)
    val expenseByCategory = targetList
        .groupBy { it.category }
        .map { (name, items) ->
            val distinctNotes = items.map { it.notes.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val noteText = if (distinctNotes.isEmpty()) "" else distinctNotes.joinToString("، ")
            CatRow(name, items.sumOf { it.amount }, noteText)
        }
        .sortedByDescending { it.amount }

    val totalExpenses = targetList.sumOf { it.amount }
    val totalVolume = if (totalExpenses > 0) totalExpenses else 1.0

    val primaryColor = Color(0xFFB4622F)
    val rowAltBg = Color(0xFFFBF7F0)
    val totalRowBg = Color(0xFFF3E4D3)
    val borderColor = Color(0xFFE7DFD2)
    val textDark = Color(0xFF2B2620)
    val textMuted = Color(0xFF8A8171)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Table Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(0.7.dp, borderColor, RoundedCornerShape(6.dp))
        ) {
            val amountHeader = if (currencySymbol.isNotBlank()) "المبلغ ($currencySymbol)" else "المبلغ"

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("م", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("البيان", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1.3f))
                Text(amountHeader, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("النسبة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                Text("ملاحظات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1f))
            }

            if (expenseByCategory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مصروفات مسجلة لهذه الفترة",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                }
            } else {
                expenseByCategory.take(8).forEachIndexed { index, row ->
                    val pct = (row.amount / totalVolume) * 100.0
                    val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"
                    val rowBg = if (index % 2 == 1) rowAltBg else Color.White

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .border(0.5.dp, borderColor)
                            .padding(vertical = 7.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}", color = textDark, fontSize = 10.5.sp, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                        Text(row.name, color = textDark, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), maxLines = 1)
                        Text(Formatters.formatMoney(row.amount), color = textDark, fontSize = 10.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                        Text(pctStr, color = textDark, fontSize = 10.5.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                        Text(row.note.ifBlank { "-" }, color = textMuted, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    }
                }

                // Table Summary Row
                val totalAmountFormatted = if (currencySymbol.isNotBlank()) "${Formatters.formatMoney(totalExpenses)} $currencySymbol" else Formatters.formatMoney(totalExpenses)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(totalRowBg)
                        .border(0.5.dp, borderColor)
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي المصروفات",
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = totalAmountFormatted,
                        color = textDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Summary Preview Section matching ExportUtils
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = borderColor, thickness = 0.7.dp)
        Spacer(modifier = Modifier.height(12.dp))

        val currSuffix = if (currencySymbol.isNotBlank()) " $currencySymbol" else ""

        Text(
            text = "الملخص",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "• إجمالي المصروفات خلال الفترة: ${Formatters.formatMoney(totalExpenses)}$currSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = textDark
        )

        val monthsCount = 12.0
        val monthlyAvg = totalExpenses / monthsCount
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "• متوسط الإنفاق الشهري: ${Formatters.formatMoney(monthlyAvg)}$currSuffix / شهر",
            style = MaterialTheme.typography.bodySmall,
            color = textDark
        )

        if (expenseByCategory.isNotEmpty() && totalExpenses > 0) {
            val topCategory = expenseByCategory.first()
            val topPct = (topCategory.amount / totalExpenses) * 100.0
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• أعلى بند إنفاق: ${topCategory.name} (${String.format(Locale.US, "%.1f", topPct)}% من الإجمالي)",
                style = MaterialTheme.typography.bodySmall,
                color = textDark
            )
            val bottomCategory = expenseByCategory.last()
            if (bottomCategory != topCategory) {
                val botPct = (bottomCategory.amount / totalExpenses) * 100.0
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• أقل بند إنفاق: ${bottomCategory.name} (${String.format(Locale.US, "%.1f", botPct)}% من الإجمالي)",
                    style = MaterialTheme.typography.bodySmall,
                    color = textDark
                )
            }
        }
    }
}

@Composable
fun DebtsPdfTablePreview(
    debts: List<DebtWithPayments>,
    currencySymbol: String,
    isArabic: Boolean
) {
    val totalOwedToMe = debts.filter { it.debt.type == "OWED_TO_ME" }.sumOf { it.remainingAmount }
    val totalIOwe = debts.filter { it.debt.type == "I_OWE" }.sumOf { it.remainingAmount }
    val netDebts = totalOwedToMe - totalIOwe

    val primaryColor = Color(0xFFB4622F)
    val rowAltBg = Color(0xFFFBF7F0)
    val borderColor = Color(0xFFE7DFD2)
    val textDark = Color(0xFF2B2620)
    val textMuted = Color(0xFF8A8171)
    val greenPositive = Color(0xFF4E7A4E)
    val redNegative = Color(0xFFB4482F)
    val sdf = SimpleDateFormat("d MMMM", Locale("ar"))

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top 2 Summary Cards matching ExportUtils
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = rowAltBg,
                border = androidx.compose.foundation.BorderStroke(0.7.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("إجمالي المستحق لي", style = MaterialTheme.typography.labelSmall, color = textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${Formatters.formatMoney(totalOwedToMe)} $currencySymbol",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = greenPositive
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = rowAltBg,
                border = androidx.compose.foundation.BorderStroke(0.7.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("إجمالي عليّ", style = MaterialTheme.typography.labelSmall, color = textMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${Formatters.formatMoney(totalIOwe)} $currencySymbol",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = redNegative
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .border(0.7.dp, borderColor, RoundedCornerShape(6.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryColor)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الاسم", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1.3f))
                Text("النوع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(0.9f))
                Text("المبلغ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                Text("آخر عملية", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
            }

            if (debts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد مديونيات للتصدير",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                }
            } else {
                debts.take(8).forEachIndexed { index, item ->
                    val isOwedToMe = item.debt.type == "OWED_TO_ME"
                    val typeLabel = if (isOwedToMe) "مستحق لي" else "عليّ"
                    val typeColor = if (isOwedToMe) greenPositive else redNegative
                    val lastDate = item.payments.maxOfOrNull { it.paymentDateMillis } ?: item.debt.lentDateMillis
                    val rowBg = if (index % 2 == 1) rowAltBg else Color.White

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .border(0.5.dp, borderColor)
                            .padding(vertical = 7.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.debt.personName, color = textDark, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f), maxLines = 1)
                        Text(typeLabel, color = typeColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        Text(Formatters.formatMoney(item.remainingAmount), color = textDark, fontSize = 10.5.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                        Text(sdf.format(Date(lastDate)), color = textDark, fontSize = 10.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = borderColor, thickness = 0.7.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "الملخص",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textDark
        )
        Spacer(modifier = Modifier.height(6.dp))

        val netLabel = if (netDebts >= 0) "• الصافي: ${Formatters.formatMoney(netDebts)} $currencySymbol لصالحك"
                       else "• الصافي: ${Formatters.formatMoney(-netDebts)} $currencySymbol عليك"
        Text(
            text = netLabel,
            style = MaterialTheme.typography.bodySmall,
            color = textDark
        )

        val overdueCount = debts.count { it.debt.dueDateMillis?.let { d -> d < System.currentTimeMillis() && !it.isFullyPaid } ?: false }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "• عدد الديون المتأخرة: $overdueCount",
            style = MaterialTheme.typography.bodySmall,
            color = textDark
        )
    }
}
