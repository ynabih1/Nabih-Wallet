package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.export.DebtRecord
import com.example.data.export.toDebtRecord
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern, Clean & High-Standard PDF Report Generator for Nabih Wallet (محفظة نبيه).
 * Fully aligned with modern financial report standards:
 * - Pure crisp white background (#FFFFFF) with warm accents (#C25E3E, #2E7D32).
 * - Elegant, well-structured Arabic RTL header with branding and report metadata.
 * - Prominent KPI summary metric cards with soft fills, rounded corners and neat borders.
 * - Beautifully styled table with subtle header shading, generous padding, clean borders and divider lines.
 * - Arabic BiDi compliant text rendering, avoiding parenthesized percentage clipping.
 * - Comprehensive financial summary breakdown section.
 * - Clean footer with correct Arabic text ("تم إنشاء هذا التقرير بواسطة محفظة نبيه") and localized page numbering ("صفحة X من Y").
 */
object PdfReportGenerator {

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private fun formatAmount(amount: Double): String = numberFormat.format(amount)

    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
    }

    private fun formatArabicFullDate(date: Date = Date()): String {
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        return formatter.format(date)
    }

    /**
     * Renders RTL Arabic text cleanly and accurately using StaticLayout with exact vertical positioning.
     */
    private fun drawBidiText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        textPaint: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val safeWidth = width.coerceAtLeast(10)
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, safeWidth)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setAlignment(align)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                textPaint,
                safeWidth,
                align,
                1.0f,
                0.0f,
                false
            )
        }

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
        return staticLayout.height
    }

    /**
     * Renders LTR text (e.g. Left-aligned numbers, currency, dates).
     */
    private fun drawLtrText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        textPaint: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val safeWidth = width.coerceAtLeast(10)
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, textPaint, safeWidth)
                .setTextDirection(TextDirectionHeuristics.LTR)
                .setAlignment(align)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                textPaint,
                safeWidth,
                align,
                1.0f,
                0.0f,
                false
            )
        }

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
        return staticLayout.height
    }

    /**
     * Multi-page PDF layout manager for standard A4 pages.
     */
    private class ModernPdfPageManager(
        private val document: PdfDocument,
        val reportTitle: String,
        val periodText: String,
        val issueDateText: String,
        val currencyText: String
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 40f
        val marginRight = 40f
        val contentWidth = (pageWidth - (marginLeft + marginRight)).toInt()
        val bottomMargin = 45f
        val maxContentY = pageHeight - bottomMargin

        var currentPageIndex = 0
        val pages = mutableListOf<PdfDocument.Page>()
        var currentCanvas: Canvas? = null
        var curY = 40f

        // Professional Color Palette
        val colorBrandDark = Color.parseColor("#1C1917")        // Deep Warm Charcoal
        val colorBrandPrimary = Color.parseColor("#C25E3E")     // Burnt Orange / Terracotta
        val colorSecondaryText = Color.parseColor("#57534E")    // Warm Gray
        val colorMutedText = Color.parseColor("#8C857B")        // Soft Sand Gray
        val colorBorder = Color.parseColor("#E7E5E4")           // Subtle Stone Border
        val colorHeaderBg = Color.parseColor("#F5F3EF")         // Elegant Warm Header fill
        val colorCardBg = Color.parseColor("#FAFAF8")           // Metric Card Soft fill

        // Reusable TextPaints
        val brandTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorBrandDark
            textSize = 17f
            isFakeBoldText = true
        }

        val brandSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorBrandPrimary
            textSize = 13f
            isFakeBoldText = true
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorSecondaryText
            textSize = 9.5f
        }

        val thinDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorBorder
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMutedText
            textSize = 8.5f
        }

        fun startNewPage(): Canvas {
            currentPageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageIndex).create()
            val page = document.startPage(pageInfo)
            pages.add(page)
            val canvas = page.canvas
            currentCanvas = canvas

            // Crisp White Background
            canvas.drawColor(Color.WHITE)

            curY = 38f
            val tableLeft = marginLeft
            val tableRight = pageWidth - marginRight

            if (currentPageIndex == 1) {
                // Top decorative accent line
                val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = colorBrandPrimary
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                canvas.drawLine(tableLeft, curY, tableRight, curY, accentPaint)
                curY += 16f

                // Header Line 1: App Name & Logo text on right, Report Title on left
                drawBidiText(canvas, "محفظة نبيه  |  Nabih Wallet", tableLeft, curY, contentWidth, brandTitlePaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, reportTitle, tableLeft, curY + 2f, contentWidth, brandSubtitlePaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 24f

                // Header Line 2: Report Metadata (Period on right, Date & Currency on left)
                val periodLine = "الفترة: $periodText"
                val dateLine = "تاريخ الإصدار: $issueDateText  •  العملة: $currencyText"
                drawBidiText(canvas, periodLine, tableLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, dateLine, tableLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 16f

                // Divider line under header
                canvas.drawLine(tableLeft, curY, tableRight, curY, thinDividerPaint)
                curY += 16f
            } else {
                // Continuation Header for subsequent pages
                val sub = "محفظة نبيه  •  $reportTitle"
                drawBidiText(canvas, sub, tableLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, "صفحة $currentPageIndex", tableLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 14f
                canvas.drawLine(tableLeft, curY, tableRight, curY, thinDividerPaint)
                curY += 14f
            }

            return canvas
        }

        fun ensureSpace(requiredHeight: Float): Canvas {
            if (currentCanvas == null || (curY + requiredHeight) > maxContentY) {
                return startNewPage()
            }
            return currentCanvas!!
        }

        fun finish() {
            pages.forEachIndexed { index, page ->
                val canvas = page.canvas
                val tableLeft = marginLeft
                val tableRight = pageWidth - marginRight
                val footerY = pageHeight - 32f

                // Thin footer divider line
                canvas.drawLine(tableLeft, footerY - 8f, tableRight, footerY - 8f, thinDividerPaint)

                // Arabic localized footer
                drawBidiText(canvas, "تم إنشاء هذا التقرير بواسطة محفظة نبيه", tableLeft, footerY, contentWidth, footerPaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, "صفحة ${index + 1} من ${pages.size}", tableLeft, footerY, contentWidth, footerPaint, Layout.Alignment.ALIGN_NORMAL)

                document.finishPage(page)
            }
        }
    }

    /**
     * GENERATE EXPENSE REPORT
     * Includes:
     * - Top modern KPI cards (Total Expenses, Total Income, Net Balance, Transactions Count).
     * - Categorized Spending Table with % and progress feel, plus individual transactions.
     * - Comprehensive Financial Summary Breakdown.
     */
    fun generateExpenseReport(
        context: Context,
        transactions: List<TransactionEntity>,
        periodTitle: String,
        currencySymbol: String = "جنيه",
        startDateStr: String = "",
        endDateStr: String = ""
    ): File {
        val pdfDocument = PdfDocument()
        val issueDateStr = formatArabicFullDate(Date())

        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        val incomeTransactions = transactions.filter { it.type == "INCOME" }

        val targetExpenses = expenseTransactions.ifEmpty { transactions }

        val periodText = when {
            startDateStr.isNotBlank() && endDateStr.isNotBlank() -> "$startDateStr - $endDateStr"
            transactions.isNotEmpty() -> {
                val minD = transactions.minOf { it.dateMillis }
                val maxD = transactions.maxOf { it.dateMillis }
                "${formatDate(minD)} - ${formatDate(maxD)}"
            }
            else -> periodTitle
        }

        val totalExpenses = targetExpenses.sumOf { it.amount }
        val totalIncome = incomeTransactions.sumOf { it.amount }
        val netSavings = totalIncome - totalExpenses
        val totalVolume = if (totalExpenses > 0.0) totalExpenses else 1.0

        val pageManager = ModernPdfPageManager(
            document = pdfDocument,
            reportTitle = "بيان المصروفات والتحليل المالي",
            periodText = periodText,
            issueDateText = issueDateStr,
            currencyText = currencySymbol
        )

        // Palette & Paints
        val textPrimaryColor = Color.parseColor("#1C1917")
        val textSecondaryColor = Color.parseColor("#57534E")
        val borderColor = Color.parseColor("#E7E5E4")
        val tableHeaderBgColor = Color.parseColor("#F5F3EF")
        val rowDividerColor = Color.parseColor("#F5F4F2")
        val terracottaColor = Color.parseColor("#C25E3E")
        val greenColor = Color.parseColor("#2E7D32")

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAF8")
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
        }

        val cardLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 8.5f
            isFakeBoldText = true
        }

        val cardValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 12.5f
            isFakeBoldText = true
        }

        val cardValueExpensePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaColor
            textSize = 12.5f
            isFakeBoldText = true
        }

        val cardValueIncomePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 12.5f
            isFakeBoldText = true
        }

        val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 11.5f
            isFakeBoldText = true
        }

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 9.5f
            isFakeBoldText = true
        }

        val rowSeqPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C857B")
            textSize = 9.5f
        }

        val rowTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 10f
            isFakeBoldText = true
        }

        val rowSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 8.5f
        }

        val rowPctPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 9.5f
        }

        val rowAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 10.5f
            isFakeBoldText = true
        }

        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 11f
            isFakeBoldText = true
        }

        val totalAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaColor
            textSize = 12.5f
            isFakeBoldText = true
        }

        val thinDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val rowDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rowDividerColor
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tableHeaderBgColor
            style = Paint.Style.FILL
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // ---------------------------------------------------------
        // 1. KPI SUMMARY METRIC CARDS (3 Cards Row)
        // ---------------------------------------------------------
        val cardHeight = 48f
        canvas = pageManager.ensureSpace(cardHeight + 22f)

        val cardSpacing = 10f
        val cardWidth = (totalWidth - (2f * cardSpacing)) / 3f

        // Card 1 (Right): Total Expenses
        val card1X = tableLeft + 2f * (cardWidth + cardSpacing)
        val card1Rect = RectF(card1X, pageManager.curY, card1X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBorderPaint)
        drawBidiText(canvas, "إجمالي المصروفات", card1X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "${formatAmount(totalExpenses)} $currencySymbol", card1X, pageManager.curY + 22f, cardWidth.toInt(), cardValueExpensePaint, Layout.Alignment.ALIGN_CENTER)

        // Card 2 (Middle): Total Income or Transactions
        val card2X = tableLeft + cardWidth + cardSpacing
        val card2Rect = RectF(card2X, pageManager.curY, card2X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBorderPaint)
        if (totalIncome > 0) {
            drawBidiText(canvas, "إجمالي الدخل", card2X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(canvas, "${formatAmount(totalIncome)} $currencySymbol", card2X, pageManager.curY + 22f, cardWidth.toInt(), cardValueIncomePaint, Layout.Alignment.ALIGN_CENTER)
        } else {
            drawBidiText(canvas, "عدد العمليات", card2X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(canvas, "${targetExpenses.size}", card2X, pageManager.curY + 22f, cardWidth.toInt(), cardValuePaint, Layout.Alignment.ALIGN_CENTER)
        }

        // Card 3 (Left): Net Savings or Transactions count
        val card3X = tableLeft
        val card3Rect = RectF(card3X, pageManager.curY, card3X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card3Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card3Rect, 8f, 8f, cardBorderPaint)
        if (totalIncome > 0) {
            drawBidiText(canvas, "صافي الفائض / العجز", card3X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
            val netPaint = if (netSavings >= 0) cardValueIncomePaint else cardValueExpensePaint
            val prefix = if (netSavings > 0) "+" else ""
            drawBidiText(canvas, "$prefix${formatAmount(netSavings)} $currencySymbol", card3X, pageManager.curY + 22f, cardWidth.toInt(), netPaint, Layout.Alignment.ALIGN_CENTER)
        } else {
            drawBidiText(canvas, "متوسط المعاملة", card3X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
            val avg = if (targetExpenses.isNotEmpty()) totalExpenses / targetExpenses.size else 0.0
            drawBidiText(canvas, "${formatAmount(avg)} $currencySymbol", card3X, pageManager.curY + 22f, cardWidth.toInt(), cardValuePaint, Layout.Alignment.ALIGN_CENTER)
        }

        pageManager.curY += cardHeight + 18f

        // ---------------------------------------------------------
        // 2. CATEGORY BREAKDOWN TABLE (بند المصروف، المبلغ، النسبة)
        // Grouping by category makes the report concise and professional like the user's reference
        // ---------------------------------------------------------
        val categoryGroups = targetExpenses
            .groupBy { it.category.ifBlank { "أخرى" } }
            .map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                val pct = (sum / totalVolume) * 100.0
                Triple(catName, sum, pct)
            }
            .sortedByDescending { it.second }

        // Section Title: تفاصيل المصروفات حسب الفئات
        canvas = pageManager.ensureSpace(30f)
        drawBidiText(canvas, "تفاصيل المصروفات حسب الفئات", tableLeft, pageManager.curY, totalWidth.toInt(), sectionHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 18f

        // Table Column allocation (RTL order: م -> البيان -> المبلغ -> النسبة -> ملاحظات)
        val colW_seq = 26f
        val colW_pct = 60f
        val colW_amount = 100f
        val colW_notes = 110f
        val colW_item = totalWidth - (colW_seq + colW_pct + colW_amount + colW_notes)

        // Coordinates from Left to Right (LTR canvas coordinates):
        // [Notes] [Percentage] [Amount] [Item Description] [Seq Number]
        val x_notes = tableLeft
        val x_pct = x_notes + colW_notes
        val x_amount = x_pct + colW_pct
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        val headerHeight = 24f
        canvas = pageManager.ensureSpace(headerHeight)

        // Draw styled table header row
        val headerRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + headerHeight)
        canvas.drawRoundRect(headerRect, 4f, 4f, tableHeaderBgPaint)
        canvas.drawRoundRect(headerRect, 4f, 4f, cardBorderPaint)

        val headerTextY = pageManager.curY + 5f
        drawBidiText(canvas, "م", x_seq, headerTextY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "البيان / الفئة", x_item, headerTextY, colW_item.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
        drawBidiText(canvas, "المبلغ ($currencySymbol)", x_amount, headerTextY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "النسبة", x_pct, headerTextY, colW_pct.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "ملاحظات", x_notes, headerTextY, colW_notes.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)

        pageManager.curY += headerHeight + 2f

        // ---------------------------------------------------------
        // 3. TABLE ROWS
        // ---------------------------------------------------------
        if (categoryGroups.isEmpty()) {
            val emptyH = 40f
            canvas = pageManager.ensureSpace(emptyH)
            drawBidiText(canvas, "لا توجد معاملات مسجلة لهذه الفترة", tableLeft, pageManager.curY + 12f, totalWidth.toInt(), rowSubtitlePaint, Layout.Alignment.ALIGN_CENTER)
            pageManager.curY += emptyH
        } else {
            val rowHeight = 30f

            categoryGroups.forEachIndexed { index, (catName, sumAmount, pct) ->
                canvas = pageManager.ensureSpace(rowHeight)

                val seqStr = "${index + 1}"
                val amtStr = formatAmount(sumAmount)
                val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"

                // Zebra striping for even rows
                if (index % 2 == 1) {
                    val zebraRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight)
                    val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FAF9F6")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(zebraRect, zebraPaint)
                }

                val rowY = pageManager.curY + 7f

                // 1. Seq Number (م)
                drawBidiText(canvas, seqStr, x_seq, rowY, colW_seq.toInt(), rowSeqPaint, Layout.Alignment.ALIGN_CENTER)

                // 2. Category Name (البيان)
                drawBidiText(canvas, catName, x_item, rowY, colW_item.toInt(), rowTitlePaint, Layout.Alignment.ALIGN_NORMAL)

                // 3. Amount (المبلغ)
                drawLtrText(canvas, amtStr, x_amount, rowY, colW_amount.toInt(), rowAmountPaint, Layout.Alignment.ALIGN_CENTER)

                // 4. Percentage (النسبة)
                drawLtrText(canvas, pctStr, x_pct, rowY, colW_pct.toInt(), rowPctPaint, Layout.Alignment.ALIGN_CENTER)

                // 5. Notes (ملاحظات)
                val catTxList = targetExpenses.filter { it.category == catName }
                val notesText = if (catTxList.size > 1) "${catTxList.size} عمليات" else catTxList.firstOrNull()?.notes?.trim()?.ifEmpty { "-" } ?: "-"
                drawBidiText(canvas, notesText, x_notes, rowY, colW_notes.toInt(), rowSubtitlePaint, Layout.Alignment.ALIGN_NORMAL)

                // Bottom divider
                canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)

                pageManager.curY += rowHeight
            }

            // ---------------------------------------------------------
            // 4. TOTAL ROW
            // ---------------------------------------------------------
            val totalRowH = 34f
            canvas = pageManager.ensureSpace(totalRowH + 10f)

            val totalRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + totalRowH)
            val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F9F8F5")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(totalRect, 4f, 4f, totalBgPaint)
            canvas.drawRoundRect(totalRect, 4f, 4f, cardBorderPaint)

            val totalTextY = pageManager.curY + 8f
            drawBidiText(canvas, "إجمالي المصروفات", x_item, totalTextY, (colW_item + colW_seq).toInt(), totalLabelPaint, Layout.Alignment.ALIGN_NORMAL)
            drawLtrText(canvas, "${formatAmount(totalExpenses)} $currencySymbol", x_amount, totalTextY, colW_amount.toInt(), totalAmountPaint, Layout.Alignment.ALIGN_CENTER)
            drawLtrText(canvas, "100.0%", x_pct, totalTextY, colW_pct.toInt(), rowPctPaint, Layout.Alignment.ALIGN_CENTER)

            pageManager.curY += totalRowH + 18f
        }

        // ---------------------------------------------------------
        // 5. FINANCIAL SUMMARY BREAKDOWN (الملخص المالي الشامل)
        // Clean card with key analytical insights
        // ---------------------------------------------------------
        val summaryBoxHeight = 110f
        canvas = pageManager.ensureSpace(summaryBoxHeight + 15f)

        drawBidiText(canvas, "الملخص المالي والتحليل", tableLeft, pageManager.curY, totalWidth.toInt(), sectionHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 16f

        val summaryRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + summaryBoxHeight)
        canvas.drawRoundRect(summaryRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(summaryRect, 8f, 8f, cardBorderPaint)

        var sY = pageManager.curY + 12f
        val bulletCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaColor
            style = Paint.Style.FILL
        }
        val summaryTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 9.5f
        }

        val bulletRightMargin = tableRight - 14f
        val textRightMargin = tableLeft
        val summaryTextWidth = (totalWidth - 32f).toInt()

        fun drawSummaryRow(text: String) {
            // Draw bullet dot on the right for RTL layout
            canvas.drawCircle(bulletRightMargin, sY + 6f, 2.5f, bulletCirclePaint)
            drawBidiText(canvas, text, textRightMargin, sY, summaryTextWidth, summaryTextPaint, Layout.Alignment.ALIGN_NORMAL)
            sY += 18f
        }

        // Bullet 1: Total expenses
        drawSummaryRow("إجمالي المصروفات خلال الفترة: ${formatAmount(totalExpenses)} $currencySymbol")

        // Bullet 2: Total income
        if (totalIncome > 0) {
            drawSummaryRow("إجمالي الدخل المسجل: ${formatAmount(totalIncome)} $currencySymbol")
        }

        // Bullet 3: Highest spending category
        val topCategory = categoryGroups.firstOrNull()
        if (topCategory != null) {
            val topPct = String.format(Locale.US, "%.1f", topCategory.third)
            drawSummaryRow("أعلى بند إنفاق: ${topCategory.first} (${formatAmount(topCategory.second)} $currencySymbol بنسبة $topPct%)")
        }

        // Bullet 4: Lowest spending category
        val lowestCategory = categoryGroups.lastOrNull()
        if (lowestCategory != null && lowestCategory != topCategory) {
            val lowPct = String.format(Locale.US, "%.1f", lowestCategory.third)
            drawSummaryRow("أقل بند إنفاق: ${lowestCategory.first} (${formatAmount(lowestCategory.second)} $currencySymbol بنسبة $lowPct%)")
        }

        // Bullet 5: Count of categories and transactions
        drawSummaryRow("عدد الفئات المسجلة: ${categoryGroups.size} فئة  |  إجمالي عدد العمليات: ${targetExpenses.size} عملية")

        pageManager.curY += summaryBoxHeight + 16f

        pageManager.finish()

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val reportFile = File(reportsDir, "Nabih_Expense_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(reportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return reportFile
    }

    /**
     * Overload for backward compatibility
     */
    fun generateDebtsReport(
        context: Context,
        debtsWithPayments: List<DebtWithPayments>,
        currencySymbol: String = "جنيه"
    ): File {
        return generateDebtsReportFromRecords(
            context = context,
            debts = debtsWithPayments.map { it.toDebtRecord() },
            currencySymbol = currencySymbol
        )
    }

    /**
     * GENERATE DEBTS REPORT
     */
    fun generateDebtsReportFromRecords(
        context: Context,
        debts: List<DebtRecord>,
        currencySymbol: String = "جنيه"
    ): File {
        val pdfDocument = PdfDocument()
        val issueDateStr = formatArabicFullDate(Date())

        val totalOwedToMe = debts.filter { it.type == "LENT" }.sumOf { it.remainingAmount }
        val totalIOwe = debts.filter { it.type == "BORROWED" }.sumOf { it.remainingAmount }
        val netDebts = totalOwedToMe - totalIOwe

        val pageManager = ModernPdfPageManager(
            document = pdfDocument,
            reportTitle = "بيان الديون والالتزامات المالية",
            periodText = "كافة السجلات المسجلة",
            issueDateText = issueDateStr,
            currencyText = currencySymbol
        )

        // Palette & Paints
        val textPrimaryColor = Color.parseColor("#1C1917")
        val textSecondaryColor = Color.parseColor("#57534E")
        val borderColor = Color.parseColor("#E7E5E4")
        val tableHeaderBgColor = Color.parseColor("#F5F3EF")
        val rowDividerColor = Color.parseColor("#F5F4F2")
        val terracottaColor = Color.parseColor("#C25E3E")
        val greenColor = Color.parseColor("#2E7D32")

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAF8")
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
        }

        val cardLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 8.5f
            isFakeBoldText = true
        }

        val cardValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 12f
            isFakeBoldText = true
        }

        val cardValueGreenPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenColor
            textSize = 12f
            isFakeBoldText = true
        }

        val cardValueTerracottaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaColor
            textSize = 12f
            isFakeBoldText = true
        }

        val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 11.5f
            isFakeBoldText = true
        }

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 9.5f
            isFakeBoldText = true
        }

        val rowSeqPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C857B")
            textSize = 9.5f
        }

        val rowTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 10f
            isFakeBoldText = true
        }

        val rowSubtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 8.5f
        }

        val rowStatusPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondaryColor
            textSize = 9f
        }

        val rowAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 10.5f
            isFakeBoldText = true
        }

        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimaryColor
            textSize = 11f
            isFakeBoldText = true
        }

        val totalAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaColor
            textSize = 12.5f
            isFakeBoldText = true
        }

        val thinDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val rowDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rowDividerColor
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }

        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = tableHeaderBgColor
            style = Paint.Style.FILL
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // ---------------------------------------------------------
        // 1. SUMMARY METRIC CARDS (3 Cards: Owed to me, I owe, Net)
        // ---------------------------------------------------------
        val cardHeight = 48f
        canvas = pageManager.ensureSpace(cardHeight + 20f)

        val cardSpacing = 10f
        val cardWidth = (totalWidth - (2f * cardSpacing)) / 3f

        // Card 1 (Right): Owed to me
        val card1X = tableLeft + 2f * (cardWidth + cardSpacing)
        val card1Rect = RectF(card1X, pageManager.curY, card1X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card1Rect, 8f, 8f, cardBorderPaint)
        drawBidiText(canvas, "مستحق لي (ديون خارجية)", card1X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "${formatAmount(totalOwedToMe)} $currencySymbol", card1X, pageManager.curY + 22f, cardWidth.toInt(), cardValueGreenPaint, Layout.Alignment.ALIGN_CENTER)

        // Card 2 (Middle): I owe
        val card2X = tableLeft + cardWidth + cardSpacing
        val card2Rect = RectF(card2X, pageManager.curY, card2X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card2Rect, 8f, 8f, cardBorderPaint)
        drawBidiText(canvas, "مستحق علي (التزامات)", card2X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "${formatAmount(totalIOwe)} $currencySymbol", card2X, pageManager.curY + 22f, cardWidth.toInt(), cardValueTerracottaPaint, Layout.Alignment.ALIGN_CENTER)

        // Card 3 (Left): Net Debts
        val card3X = tableLeft
        val card3Rect = RectF(card3X, pageManager.curY, card3X + cardWidth, pageManager.curY + cardHeight)
        canvas.drawRoundRect(card3Rect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(card3Rect, 8f, 8f, cardBorderPaint)
        drawBidiText(canvas, "صافي الرصيد", card3X, pageManager.curY + 7f, cardWidth.toInt(), cardLabelPaint, Layout.Alignment.ALIGN_CENTER)
        val netDebtPaint = if (netDebts >= 0) cardValueGreenPaint else cardValueTerracottaPaint
        val sign = if (netDebts > 0) "+" else ""
        drawBidiText(canvas, "$sign${formatAmount(netDebts)} $currencySymbol", card3X, pageManager.curY + 22f, cardWidth.toInt(), netDebtPaint, Layout.Alignment.ALIGN_CENTER)

        pageManager.curY += cardHeight + 18f

        // ---------------------------------------------------------
        // 2. DATA TABLE
        // ---------------------------------------------------------
        canvas = pageManager.ensureSpace(30f)
        drawBidiText(canvas, "قائمة وسجل المديونيات", tableLeft, pageManager.curY, totalWidth.toInt(), sectionHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 18f

        val colW_seq = 26f
        val colW_amount = 120f
        val colW_status = 85f
        val colW_item = totalWidth - (colW_seq + colW_amount + colW_status)

        val x_status = tableLeft
        val x_amount = x_status + colW_status
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        val headerHeight = 24f
        canvas = pageManager.ensureSpace(headerHeight)

        val headerRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + headerHeight)
        canvas.drawRoundRect(headerRect, 4f, 4f, tableHeaderBgPaint)
        canvas.drawRoundRect(headerRect, 4f, 4f, cardBorderPaint)

        val headerTextY = pageManager.curY + 5f
        drawBidiText(canvas, "م", x_seq, headerTextY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "الطرف والبيان", x_item, headerTextY, colW_item.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
        drawBidiText(canvas, "المبلغ المتبقي ($currencySymbol)", x_amount, headerTextY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "الحالة", x_status, headerTextY, colW_status.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)

        pageManager.curY += headerHeight + 2f

        // ---------------------------------------------------------
        // 3. DATA ROWS
        // ---------------------------------------------------------
        if (debts.isEmpty()) {
            val emptyH = 40f
            canvas = pageManager.ensureSpace(emptyH)
            drawBidiText(canvas, "لا توجد ديون مسجلة", tableLeft, pageManager.curY + 12f, totalWidth.toInt(), rowSubtitlePaint, Layout.Alignment.ALIGN_CENTER)
            pageManager.curY += emptyH
        } else {
            val rowHeight = 36f

            debts.forEachIndexed { index, item ->
                canvas = pageManager.ensureSpace(rowHeight)

                if (index % 2 == 1) {
                    val zebraRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight)
                    val zebraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#FAF9F6")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(zebraRect, zebraPaint)
                }

                val seqStr = "${index + 1}"
                val isOwedToMe = item.type == "LENT"
                val typeTag = if (isOwedToMe) "مستحق لي" else "مستحق علي"
                val dateStr = formatDate(item.lastActivityDate)
                val subtitle = "$typeTag  •  $dateStr"

                val isPaid = item.remainingAmount <= 0.001
                val statusStr = if (isPaid) "مسدد بالكامل" else "متبقي"
                val amtStr = formatAmount(item.remainingAmount)

                val rowY = pageManager.curY + 4f

                // Seq
                drawBidiText(canvas, seqStr, x_seq, rowY + 5f, colW_seq.toInt(), rowSeqPaint, Layout.Alignment.ALIGN_CENTER)

                // Person Name & Subtitle
                drawBidiText(canvas, item.personName, x_item, rowY, colW_item.toInt(), rowTitlePaint, Layout.Alignment.ALIGN_NORMAL)
                drawBidiText(canvas, subtitle, x_item, rowY + 16f, colW_item.toInt(), rowSubtitlePaint, Layout.Alignment.ALIGN_NORMAL)

                // Amount
                drawLtrText(canvas, amtStr, x_amount, rowY + 5f, colW_amount.toInt(), rowAmountPaint, Layout.Alignment.ALIGN_CENTER)

                // Status
                val statusColorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isPaid) greenColor else terracottaColor
                    textSize = 9f
                    isFakeBoldText = true
                }
                drawBidiText(canvas, statusStr, x_status, rowY + 5f, colW_status.toInt(), statusColorPaint, Layout.Alignment.ALIGN_CENTER)

                // Divider
                canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)

                pageManager.curY += rowHeight
            }

            // ---------------------------------------------------------
            // 4. TOTAL ROW
            // ---------------------------------------------------------
            val totalRowH = 34f
            canvas = pageManager.ensureSpace(totalRowH + 10f)

            val totalRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + totalRowH)
            val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F9F8F5")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(totalRect, 4f, 4f, totalBgPaint)
            canvas.drawRoundRect(totalRect, 4f, 4f, cardBorderPaint)

            val totalTextY = pageManager.curY + 8f
            drawBidiText(canvas, "صافي رصيد الديون والالتزامات", x_item, totalTextY, (colW_item + colW_seq).toInt(), totalLabelPaint, Layout.Alignment.ALIGN_NORMAL)
            drawLtrText(canvas, "${formatAmount(netDebts)} $currencySymbol", x_amount, totalTextY, colW_amount.toInt(), totalAmountPaint, Layout.Alignment.ALIGN_CENTER)

            pageManager.curY += totalRowH + 16f
        }

        pageManager.finish()

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val reportFile = File(reportsDir, "Nabih_Debts_Report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(reportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return reportFile
    }
}
