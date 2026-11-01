package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtRecord
import com.example.model.FinancialConstants
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Refined Classic Financial PDF Report Generator for Nabih Wallet (محفظة نبيه).
 * Preserves the classic statement layout while removing the Notes column/footer
 * and improving visual polish (crisp borders, balanced 4-column table, and framed summary).
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

    private fun formatArabicFullDateTime(date: Date = Date()): String {
        val datePart = SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(date)
        val timePart = SimpleDateFormat("hh:mm a", Locale("ar")).format(date)
            .replace("AM", "ص")
            .replace("PM", "م")
        return "$datePart - $timePart"
    }

    /**
     * Builds a styled CharSequence with a bold label and normal value.
     */
    private fun buildLabelValueSpan(
        label: String,
        value: String,
        bulletPrefix: String = "",
        bulletColor: Int? = null
    ): CharSequence {
        val sb = SpannableStringBuilder()
        if (bulletPrefix.isNotEmpty()) {
            val bulletStart = sb.length
            sb.append(bulletPrefix)
            val bulletEnd = sb.length
            sb.setSpan(StyleSpan(Typeface.BOLD), bulletStart, bulletEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (bulletColor != null) {
                sb.setSpan(ForegroundColorSpan(bulletColor), bulletStart, bulletEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val labelStart = sb.length
        sb.append(label)
        val labelEnd = sb.length
        sb.setSpan(StyleSpan(Typeface.BOLD), labelStart, labelEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        if (value.isNotEmpty()) {
            sb.append(" ")
            sb.append(value)
        }
        return sb
    }

    /**
     * Renders RTL Arabic text (or Spannable CharSequence) cleanly and accurately using StaticLayout.
     */
    private fun drawBidiText(
        canvas: Canvas,
        text: CharSequence,
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
                .setLineSpacing(2.5f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                textPaint,
                safeWidth,
                align,
                1.15f,
                2.5f,
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
     * Renders LTR text (e.g. numbers, percentages, dates).
     */
    private fun drawLtrText(
        canvas: Canvas,
        text: CharSequence,
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
                .setLineSpacing(2.5f, 1.15f)
                .setIncludePad(false)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                text,
                textPaint,
                safeWidth,
                align,
                1.15f,
                2.5f,
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
     * Multi-page PDF layout manager for standard A4 pages in Refined Classic Statement style.
     */
    private class ClassicPdfPageManager(
        private val document: PdfDocument,
        val reportTitle: String,
        val periodValue: String,
        val issueDateValue: String,
        val itemsCountValue: String = ""
    ) {
        val pageWidth = 595
        val pageHeight = 842

        // Expanded, generous outer page margins for breathing room
        val marginLeft = 52f
        val marginRight = 52f
        val marginTop = 52f
        val contentWidth = (pageWidth - (marginLeft + marginRight)).toInt()

        // Reserved bottom footer area with ample vertical spacing
        val footerTopY = pageHeight - 60f
        val maxContentY = footerTopY - 22f

        var currentPageIndex = 0
        var currentPage: PdfDocument.Page? = null
        var currentCanvas: Canvas? = null
        var curY = marginTop

        // Classic Warm Palette (Nabih Wallet colors)
        val colorMainTitle = Color.parseColor("#2D322B")        // Dark charcoal gray (#2D322B) for formal elegance
        val colorAccentBullet = Color.parseColor("#B85435")     // Warm Terracotta Accent
        val colorMetaText = Color.parseColor("#2D322B")         // Classic Charcoal Metadata
        val colorHeaderCardBg = Color.parseColor("#FCF9F6")     // Soft Warm Cream Header Box
        val colorHeaderCardBorder = Color.parseColor("#E6DCD2") // Subtle Warm Border
        val colorFooterDivider = Color.parseColor("#E6DCD2")    // Subtle Warm Divider
        val colorFooterText = Color.parseColor("#7D7771")       // Calm Muted Gray for Footer

        val reportTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMainTitle
            textSize = 17.5f
            isFakeBoldText = true
        }

        val brandBadgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorAccentBullet
            textSize = 11.5f
            isFakeBoldText = true
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMetaText
            textSize = 11.8f
        }

        val headerBoxBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorHeaderCardBg
            style = Paint.Style.FILL
        }

        val headerBoxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorHeaderCardBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val accentStripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorAccentBullet
            style = Paint.Style.FILL
        }

        val footerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFooterDivider
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
        }

        val footerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFooterText
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        private fun drawPageFooter(canvas: Canvas) {
            val left = marginLeft
            val right = pageWidth - marginRight

            // Subtle divider line separating page content from the bottom footer
            canvas.drawLine(left, footerTopY, right, footerTopY, footerDividerPaint)

            val footerY = footerTopY + 12f

            // Fixed documentation phrase in calm gray at the bottom of every page
            drawBidiText(
                canvas = canvas,
                text = "تم إنشاء هذا التقرير بواسطة Nabih Wallet",
                x = left,
                y = footerY,
                width = contentWidth,
                textPaint = footerTextPaint,
                align = Layout.Alignment.ALIGN_NORMAL
            )

            // Subtle page number aligned opposite
            drawBidiText(
                canvas = canvas,
                text = "صفحة $currentPageIndex",
                x = left,
                y = footerY,
                width = contentWidth,
                textPaint = footerTextPaint,
                align = Layout.Alignment.ALIGN_OPPOSITE
            )
        }

        private fun finishCurrentPageIfOpen() {
            val page = currentPage ?: return
            currentCanvas?.let { drawPageFooter(it) }
            document.finishPage(page)
            currentPage = null
            currentCanvas = null
        }

        fun startNewPage(): Canvas {
            finishCurrentPageIfOpen()
            currentPageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageIndex).create()
            val page = document.startPage(pageInfo)
            currentPage = page
            val canvas = page.canvas
            currentCanvas = canvas

            // Pure White Page Background
            canvas.drawColor(Color.WHITE)

            curY = marginTop
            val tableLeft = marginLeft
            val tableRight = pageWidth - marginRight

            if (currentPageIndex == 1) {
                // 1. Centered Main Report Title in bold #2D322B
                val titleTopY = curY
                val titleHeight = drawBidiText(
                    canvas = canvas,
                    text = reportTitle,
                    x = tableLeft,
                    y = titleTopY,
                    width = contentWidth,
                    textPaint = reportTitlePaint,
                    align = Layout.Alignment.ALIGN_CENTER
                )
                curY = titleTopY + titleHeight + 14f

                // 2. Light, organized card below the title
                val boxTop = curY
                val hasItemsCount = itemsCountValue.isNotBlank()
                val boxHeight = if (hasItemsCount) 92f else 68f
                val boxRect = RectF(tableLeft, boxTop, tableRight, boxTop + boxHeight)
                canvas.drawRoundRect(boxRect, 8f, 8f, headerBoxBgPaint)
                canvas.drawRoundRect(boxRect, 8f, 8f, headerBoxBorderPaint)

                // Right-side vertical accent bar inside header box
                val stripeRect = RectF(tableRight - 4.5f, boxTop + 8f, tableRight - 1f, boxTop + boxHeight - 8f)
                canvas.drawRoundRect(stripeRect, 2f, 2f, accentStripePaint)

                val innerWidth = contentWidth - 32
                val innerX = tableLeft + 16f
                var innerY = boxTop + 14f

                drawBidiText(
                    canvas,
                    buildLabelValueSpan("• تاريخ الإصدار:", issueDateValue, bulletColor = colorAccentBullet),
                    innerX,
                    innerY,
                    innerWidth,
                    metaPaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                innerY += 25f

                drawBidiText(
                    canvas,
                    buildLabelValueSpan("• الفترة الزمنية:", periodValue, bulletColor = colorAccentBullet),
                    innerX,
                    innerY,
                    innerWidth,
                    metaPaint,
                    Layout.Alignment.ALIGN_NORMAL
                )

                if (hasItemsCount) {
                    innerY += 25f
                    drawBidiText(
                        canvas,
                        buildLabelValueSpan("• عدد البنود:", itemsCountValue, bulletColor = colorAccentBullet),
                        innerX,
                        innerY,
                        innerWidth,
                        metaPaint,
                        Layout.Alignment.ALIGN_NORMAL
                    )
                }

                curY = boxTop + boxHeight + 24f
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
            finishCurrentPageIfOpen()
        }
    }

    /**
     * Helper to draw a top-rounded rectangle for the table header.
     */
    private fun drawTopRoundedRect(canvas: Canvas, rect: RectF, radius: Float, paint: Paint) {
        val path = Path().apply {
            val radii = floatArrayOf(
                radius, radius,   // Top-left
                radius, radius,   // Top-right
                0f, 0f,           // Bottom-right
                0f, 0f            // Bottom-left
            )
            addRoundRect(rect, radii, Path.Direction.CW)
        }
        canvas.drawPath(path, paint)
    }

    /**
     * GENERATE CLASSIC EXPENSE REPORT (Without Notes Column or Footer Notes)
     * - Refined header box with metadata lines.
     * - Clean 4-column table (م | البيان | المبلغ | النسبة) with subtle grid lines.
     * - Framed "الملخص" section with clear bullets (• إجمالي، • متوسط، إلخ).
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
        val issueDateStr = formatArabicFullDateTime(Date())

        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        val targetExpenses = expenseTransactions.ifEmpty { transactions }

        val minMillis = targetExpenses.minOfOrNull { it.dateMillis } ?: System.currentTimeMillis()
        val maxMillis = targetExpenses.maxOfOrNull { it.dateMillis } ?: System.currentTimeMillis()
        val diffDays = max(1L, TimeUnit.MILLISECONDS.toDays(maxMillis - minMillis) + 1L)

        val periodValueStr = when {
            startDateStr.isNotBlank() && endDateStr.isNotBlank() -> {
                "من $startDateStr إلى $endDateStr"
            }
            targetExpenses.isNotEmpty() -> {
                val fromStr = formatDate(minMillis)
                val toStr = formatDate(maxMillis)
                val durationLabel = when {
                    diffDays >= 30L -> {
                        val months = max(1L, Math.round(diffDays / 30.0))
                        " - $months شهرًا"
                    }
                    diffDays > 1L -> " - $diffDays يومًا"
                    else -> ""
                }
                "من $fromStr إلى $toStr$durationLabel"
            }
            periodTitle.isNotBlank() -> {
                if (periodTitle.startsWith("عن ") || periodTitle.startsWith("من ")) periodTitle else "عن $periodTitle"
            }
            else -> "كافة المصروفات المسجلة"
        }

        val totalExpenses = targetExpenses.sumOf { it.amount }
        val totalVolume = if (totalExpenses > 0.0) totalExpenses else 1.0

        data class ClassicExpenseRow(
            val title: String,
            val amount: Double,
            val percentage: Double
        )

        val categoryRows = targetExpenses
            .groupBy { FinancialConstants.normalizeCategoryName(it.category).ifBlank { "أخرى" } }
            .map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                val pct = (sum / totalVolume) * 100.0
                ClassicExpenseRow(
                    title = catName,
                    amount = sum,
                    percentage = pct
                )
            }
            .sortedByDescending { it.amount }

        val pageManager = ClassicPdfPageManager(
            document = pdfDocument,
            reportTitle = "تقرير بيان المصروفات الشخصية",
            periodValue = periodValueStr,
            issueDateValue = issueDateStr,
            itemsCountValue = "${categoryRows.size} بنود"
        )

        // Warm Brand Colors
        val colorHeaderBg = Color.parseColor("#B85435")         // Warm Terracotta Header Bar
        val colorZebraBg = Color.parseColor("#F9F3EE")          // Soft Warm Cream Zebra Row
        val colorTotalRowBg = Color.parseColor("#EFE3D8")       // Warm Tinted Total Bar
        val colorRowDivider = Color.parseColor("#E6DCD2")       // Subtle Warm Divider
        val colorColDivider = Color.parseColor("#EDE4DC")       // Subtle Vertical Column Divider
        val colorTableBorder = Color.parseColor("#D8C8B8")      // Crisp Outer Table Border
        val colorTextDark = Color.parseColor("#1E1B18")         // Deep Warm Charcoal Text
        val colorSectionTitle = Color.parseColor("#8C3B20")     // Warm Terracotta-Brown Section Title

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorHeaderBg
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorZebraBg
            style = Paint.Style.FILL
        }
        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTotalRowBg
            style = Paint.Style.FILL
        }
        val rowDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorRowDivider
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val colDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorColDivider
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }
        val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTableBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            isFakeBoldText = true
        }
        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.5f
        }
        val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.8f
            isFakeBoldText = true
        }
        val totalTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 12.2f
            isFakeBoldText = true
        }
        val summaryHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorSectionTitle
            textSize = 14.5f
            isFakeBoldText = true
        }
        val summaryLinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.8f
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // -------------------------------------------------------------------------
        // CLEAN 4-COLUMN TABLE (RTL: م | البيان | المبلغ | النسبة)
        // -------------------------------------------------------------------------
        val colW_seq = 46f
        val colW_amount = 148f
        val colW_pct = 96f
        val colW_item = totalWidth - (colW_seq + colW_amount + colW_pct)

        // LTR X positions (Left -> Right):
        // [النسبة] [المبلغ] [البيان] [م]
        val x_pct = tableLeft
        val x_amount = x_pct + colW_pct
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        fun drawClassicTableHeader(c: Canvas, topY: Float): Float {
            val headerH = 40f
            val headerRect = RectF(tableLeft, topY, tableRight, topY + headerH)
            drawTopRoundedRect(c, headerRect, 7f, headerBgPaint)

            val textY = topY + 11f
            drawBidiText(c, "م", x_seq, textY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "البيان", x_item, textY, (colW_item - 14f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(c, "المبلغ $currencySymbol", x_amount, textY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "النسبة", x_pct, textY, colW_pct.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            return topY + headerH
        }

        var tableStartTopY = pageManager.curY
        pageManager.curY = drawClassicTableHeader(canvas, pageManager.curY)

        val rowHeight = 39f
        if (categoryRows.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            drawBidiText(
                canvas,
                "لا توجد مصروفات مسجلة خلال هذه الفترة",
                tableLeft,
                pageManager.curY + 10.5f,
                totalWidth.toInt(),
                cellTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)
            pageManager.curY += rowHeight
            canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
        } else {
            categoryRows.forEachIndexed { index, row ->
                if (pageManager.curY + rowHeight > pageManager.maxContentY) {
                    // Close border on current page table before starting new page
                    canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
                    canvas = pageManager.startNewPage()
                    tableStartTopY = pageManager.curY
                    pageManager.curY = drawClassicTableHeader(canvas, pageManager.curY)
                }

                val rTop = pageManager.curY
                val rBottom = rTop + rowHeight

                // Alternating rows get soft warm tint
                if (index % 2 == 1) {
                    canvas.drawRect(RectF(tableLeft, rTop, tableRight, rBottom), zebraBgPaint)
                }

                // Subtle vertical column dividers
                canvas.drawLine(x_amount, rTop, x_amount, rBottom, colDividerPaint)
                canvas.drawLine(x_item, rTop, x_item, rBottom, colDividerPaint)
                canvas.drawLine(x_seq, rTop, x_seq, rBottom, colDividerPaint)

                val textY = rTop + 10.5f
                val pctStr = "${String.format(Locale.US, "%.1f", row.percentage)}%"

                // 1. م
                drawLtrText(canvas, "${index + 1}", x_seq, textY, colW_seq.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                // 2. البيان
                drawBidiText(canvas, row.title, x_item, textY, (colW_item - 14f).toInt(), cellBoldPaint, Layout.Alignment.ALIGN_NORMAL)
                // 3. المبلغ
                drawLtrText(canvas, formatAmount(row.amount), x_amount, textY, colW_amount.toInt(), cellBoldPaint, Layout.Alignment.ALIGN_CENTER)
                // 4. النسبة
                drawLtrText(canvas, pctStr, x_pct, textY, colW_pct.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)

                // Thin bottom row divider
                canvas.drawLine(tableLeft, rBottom, tableRight, rBottom, rowDividerPaint)

                pageManager.curY += rowHeight
            }

            // Total Row (إجمالي المصروفات)
            val totalRowH = 42f
            if (pageManager.curY + totalRowH > pageManager.maxContentY) {
                canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
                canvas = pageManager.startNewPage()
                tableStartTopY = pageManager.curY
            }
            val totalTop = pageManager.curY
            val totalBottom = totalTop + totalRowH
            val totalRect = RectF(tableLeft, totalTop, tableRight, totalBottom)
            canvas.drawRect(totalRect, totalBgPaint)

            // Vertical dividers in total row
            canvas.drawLine(x_amount, totalTop, x_amount, totalBottom, colDividerPaint)
            canvas.drawLine(x_item, totalTop, x_item, totalBottom, colDividerPaint)

            val totalTextY = totalTop + 12f
            drawBidiText(
                canvas,
                "إجمالي المصروفات",
                x_item,
                totalTextY,
                (colW_item - 14f).toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_NORMAL
            )
            drawBidiText(
                canvas,
                "${formatAmount(totalExpenses)} $currencySymbol",
                x_amount,
                totalTextY,
                colW_amount.toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            drawLtrText(
                canvas,
                "100%",
                x_pct,
                totalTextY,
                colW_pct.toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )

            pageManager.curY = totalBottom

            // Outer border around the completed table
            canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
        }

        // -------------------------------------------------------------------------
        // FRAMED SUMMARY SECTION (الملخص) WITH CLEAR BULLETS (•) & SPACING
        // -------------------------------------------------------------------------
        pageManager.curY += 30f

        val topCategory = categoryRows.firstOrNull()
        val lowestCategory = categoryRows.lastOrNull()?.takeIf { it != topCategory }
        val bulletCount = 3 + (if (topCategory != null) 1 else 0) + (if (lowestCategory != null) 1 else 0)
        val summaryCardHeight = 52f + (bulletCount * 26f) + 14f

        canvas = pageManager.ensureSpace(summaryCardHeight)

        val summaryTop = pageManager.curY
        val summaryRect = RectF(tableLeft, summaryTop, tableRight, summaryTop + summaryCardHeight)
        canvas.drawRoundRect(summaryRect, 8f, 8f, pageManager.headerBoxBgPaint)
        canvas.drawRoundRect(summaryRect, 8f, 8f, pageManager.headerBoxBorderPaint)

        // Right-side terracotta accent bar inside summary card
        val summaryStripe = RectF(tableRight - 4.5f, summaryTop + 8f, tableRight - 1f, summaryTop + summaryCardHeight - 8f)
        canvas.drawRoundRect(summaryStripe, 2f, 2f, pageManager.accentStripePaint)

        val summaryInnerX = tableLeft + 16f
        val summaryInnerWidth = (totalWidth - 32f).toInt()
        pageManager.curY = summaryTop + 16f

        drawBidiText(
            canvas,
            "الملخص:",
            summaryInnerX,
            pageManager.curY,
            summaryInnerWidth,
            summaryHeaderPaint,
            Layout.Alignment.ALIGN_NORMAL
        )
        pageManager.curY += 29f

        fun drawBulletedSummaryLine(label: String, value: String) {
            val span = buildLabelValueSpan(
                label = label,
                value = value,
                bulletPrefix = "•  ",
                bulletColor = pageManager.colorAccentBullet
            )
            drawBidiText(
                canvas = canvas,
                text = span,
                x = summaryInnerX,
                y = pageManager.curY,
                width = summaryInnerWidth,
                textPaint = summaryLinePaint,
                align = Layout.Alignment.ALIGN_NORMAL
            )
            pageManager.curY += 26f
        }

        // 1. • إجمالي المصروفات خلال الفترة
        drawBulletedSummaryLine(
            label = "إجمالي المصروفات خلال الفترة:",
            value = "${formatAmount(totalExpenses)} $currencySymbol"
        )

        // 2. • متوسط الإنفاق الشهري
        val monthsCount = max(1.0, diffDays / 30.0)
        val monthlyAverage = totalExpenses / monthsCount
        drawBulletedSummaryLine(
            label = "متوسط الإنفاق الشهري:",
            value = "${formatAmount(monthlyAverage)} $currencySymbol / شهر"
        )

        // 3. • متوسط المصروفات اليومية
        val dailyAverage = totalExpenses / diffDays.toDouble()
        drawBulletedSummaryLine(
            label = "متوسط المصروفات اليومية:",
            value = "${formatAmount(dailyAverage)} $currencySymbol / يوم"
        )

        // 4. • أعلى بند إنفاق
        if (topCategory != null) {
            val topPct = String.format(Locale.US, "%.1f", topCategory.percentage)
            drawBulletedSummaryLine(
                label = "أعلى بند إنفاق:",
                value = "${topCategory.title} - $topPct% من الإجمالي"
            )
        }

        // 5. • أقل بند إنفاق
        if (lowestCategory != null) {
            val lowPct = String.format(Locale.US, "%.1f", lowestCategory.percentage)
            drawBulletedSummaryLine(
                label = "أقل بند إنفاق:",
                value = "${lowestCategory.title} - $lowPct% من الإجمالي"
            )
        }

        pageManager.finish()

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val reportFile = File(reportsDir, "Nabih_Expense_Report_${timeStamp}.pdf")
        FileOutputStream(reportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return reportFile
    }

    /**
     * GENERATE CLASSIC DEBTS REPORT (Without Notes Column or Footer Notes)
     */
    fun generateDebtsReportFromRecords(
        context: Context,
        debts: List<DebtRecord>,
        currencySymbol: String = "جنيه"
    ): File {
        val pdfDocument = PdfDocument()
        val issueDateStr = formatArabicFullDateTime(Date())

        val totalOwedToMe = debts.filter { it.type == "LENT" }.sumOf { it.remainingAmount }
        val totalIOwe = debts.filter { it.type == "BORROWED" }.sumOf { it.remainingAmount }
        val totalRemainingAll = debts.sumOf { it.remainingAmount }
        val totalVolume = if (totalRemainingAll > 0.0) totalRemainingAll else 1.0

        val minMillis = debts.minOfOrNull { it.lastActivityDate } ?: System.currentTimeMillis()
        val maxMillis = debts.maxOfOrNull { it.lastActivityDate } ?: System.currentTimeMillis()
        val periodValueStr = if (debts.isNotEmpty()) {
            "من ${formatDate(minMillis)} إلى ${formatDate(maxMillis)}"
        } else {
            "كافة السجلات المسجلة"
        }

        val pageManager = ClassicPdfPageManager(
            document = pdfDocument,
            reportTitle = "تقرير بيان الديون والالتزامات الشخصية",
            periodValue = periodValueStr,
            issueDateValue = issueDateStr,
            itemsCountValue = "${debts.size} بنود"
        )

        val colorHeaderBg = Color.parseColor("#B85435")
        val colorZebraBg = Color.parseColor("#F9F3EE")
        val colorTotalRowBg = Color.parseColor("#EFE3D8")
        val colorRowDivider = Color.parseColor("#E6DCD2")
        val colorColDivider = Color.parseColor("#EDE4DC")
        val colorTableBorder = Color.parseColor("#D8C8B8")
        val colorTextDark = Color.parseColor("#1E1B18")
        val colorSectionTitle = Color.parseColor("#8C3B20")

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorHeaderBg
            style = Paint.Style.FILL
        }
        val zebraBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorZebraBg
            style = Paint.Style.FILL
        }
        val totalBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTotalRowBg
            style = Paint.Style.FILL
        }
        val rowDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorRowDivider
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }
        val colDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorColDivider
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
        }
        val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTableBorder
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12f
            isFakeBoldText = true
        }
        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.5f
        }
        val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.8f
            isFakeBoldText = true
        }
        val totalTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 12.2f
            isFakeBoldText = true
        }
        val summaryHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorSectionTitle
            textSize = 14.5f
            isFakeBoldText = true
        }
        val summaryLinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11.8f
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // Clean 4-Column Table (RTL: م | البيان | المبلغ | النسبة)
        val colW_seq = 46f
        val colW_amount = 148f
        val colW_pct = 96f
        val colW_item = totalWidth - (colW_seq + colW_amount + colW_pct)

        val x_pct = tableLeft
        val x_amount = x_pct + colW_pct
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        fun drawClassicDebtsHeader(c: Canvas, topY: Float): Float {
            val headerH = 40f
            val headerRect = RectF(tableLeft, topY, tableRight, topY + headerH)
            drawTopRoundedRect(c, headerRect, 7f, headerBgPaint)

            val textY = topY + 11f
            drawBidiText(c, "م", x_seq, textY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "البيان", x_item, textY, (colW_item - 14f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(c, "المبلغ $currencySymbol", x_amount, textY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "النسبة", x_pct, textY, colW_pct.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            return topY + headerH
        }

        var tableStartTopY = pageManager.curY
        pageManager.curY = drawClassicDebtsHeader(canvas, pageManager.curY)

        val rowHeight = 39f
        if (debts.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            drawBidiText(
                canvas,
                "لا توجد ديون مسجلة",
                tableLeft,
                pageManager.curY + 10.5f,
                totalWidth.toInt(),
                cellTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)
            pageManager.curY += rowHeight
            canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
        } else {
            debts.forEachIndexed { index, item ->
                if (pageManager.curY + rowHeight > pageManager.maxContentY) {
                    canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
                    canvas = pageManager.startNewPage()
                    tableStartTopY = pageManager.curY
                    pageManager.curY = drawClassicDebtsHeader(canvas, pageManager.curY)
                }

                val rTop = pageManager.curY
                val rBottom = rTop + rowHeight

                if (index % 2 == 1) {
                    canvas.drawRect(RectF(tableLeft, rTop, tableRight, rBottom), zebraBgPaint)
                }

                canvas.drawLine(x_amount, rTop, x_amount, rBottom, colDividerPaint)
                canvas.drawLine(x_item, rTop, x_item, rBottom, colDividerPaint)
                canvas.drawLine(x_seq, rTop, x_seq, rBottom, colDividerPaint)

                val pct = (item.remainingAmount / totalVolume) * 100.0
                val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"
                val typeLabel = if (item.type == "LENT") "له" else "عليه"
                val displayName = "${item.personName} - $typeLabel"

                val textY = rTop + 10.5f
                drawLtrText(canvas, "${index + 1}", x_seq, textY, colW_seq.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, displayName, x_item, textY, (colW_item - 14f).toInt(), cellBoldPaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, formatAmount(item.remainingAmount), x_amount, textY, colW_amount.toInt(), cellBoldPaint, Layout.Alignment.ALIGN_CENTER)
                drawLtrText(canvas, pctStr, x_pct, textY, colW_pct.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)

                canvas.drawLine(tableLeft, rBottom, tableRight, rBottom, rowDividerPaint)
                pageManager.curY += rowHeight
            }

            val totalRowH = 42f
            if (pageManager.curY + totalRowH > pageManager.maxContentY) {
                canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
                canvas = pageManager.startNewPage()
                tableStartTopY = pageManager.curY
            }
            val totalTop = pageManager.curY
            val totalBottom = totalTop + totalRowH
            val totalRect = RectF(tableLeft, totalTop, tableRight, totalBottom)
            canvas.drawRect(totalRect, totalBgPaint)

            canvas.drawLine(x_amount, totalTop, x_amount, totalBottom, colDividerPaint)
            canvas.drawLine(x_item, totalTop, x_item, totalBottom, colDividerPaint)

            val totalTextY = totalTop + 12f
            drawBidiText(
                canvas,
                "إجمالي المديونيات المتبقية",
                x_item,
                totalTextY,
                (colW_item - 14f).toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_NORMAL
            )
            drawBidiText(
                canvas,
                "${formatAmount(totalRemainingAll)} $currencySymbol",
                x_amount,
                totalTextY,
                colW_amount.toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            drawLtrText(
                canvas,
                "100%",
                x_pct,
                totalTextY,
                colW_pct.toInt(),
                totalTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )

            pageManager.curY = totalBottom
            canvas.drawRoundRect(RectF(tableLeft, tableStartTopY, tableRight, pageManager.curY), 7f, 7f, tableBorderPaint)
        }

        // Framed Summary Section (الملخص) with Bullets
        pageManager.curY += 30f
        val summaryCardHeight = 52f + (4 * 26f) + 14f
        canvas = pageManager.ensureSpace(summaryCardHeight)

        val summaryTop = pageManager.curY
        val summaryRect = RectF(tableLeft, summaryTop, tableRight, summaryTop + summaryCardHeight)
        canvas.drawRoundRect(summaryRect, 8f, 8f, pageManager.headerBoxBgPaint)
        canvas.drawRoundRect(summaryRect, 8f, 8f, pageManager.headerBoxBorderPaint)

        val summaryStripe = RectF(tableRight - 4.5f, summaryTop + 8f, tableRight - 1f, summaryTop + summaryCardHeight - 8f)
        canvas.drawRoundRect(summaryStripe, 2f, 2f, pageManager.accentStripePaint)

        val summaryInnerX = tableLeft + 16f
        val summaryInnerWidth = (totalWidth - 32f).toInt()
        pageManager.curY = summaryTop + 16f

        drawBidiText(
            canvas,
            "الملخص:",
            summaryInnerX,
            pageManager.curY,
            summaryInnerWidth,
            summaryHeaderPaint,
            Layout.Alignment.ALIGN_NORMAL
        )
        pageManager.curY += 29f

        fun drawBulletedSummaryLine(label: String, value: String) {
            val span = buildLabelValueSpan(
                label = label,
                value = value,
                bulletPrefix = "•  ",
                bulletColor = pageManager.colorAccentBullet
            )
            drawBidiText(
                canvas = canvas,
                text = span,
                x = summaryInnerX,
                y = pageManager.curY,
                width = summaryInnerWidth,
                textPaint = summaryLinePaint,
                align = Layout.Alignment.ALIGN_NORMAL
            )
            pageManager.curY += 26f
        }

        drawBulletedSummaryLine("إجمالي المبالغ المستحقة لي:", "${formatAmount(totalOwedToMe)} $currencySymbol")
        drawBulletedSummaryLine("إجمالي المبالغ المستحقة علي:", "${formatAmount(totalIOwe)} $currencySymbol")
        drawBulletedSummaryLine("متوسط المديونية لكل سجل:", "${formatAmount(if (debts.isNotEmpty()) totalRemainingAll / debts.size else 0.0)} $currencySymbol")
        drawBulletedSummaryLine("صافي رصيد الديون:", "${formatAmount(totalOwedToMe - totalIOwe)} $currencySymbol")

        pageManager.finish()

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val reportFile = File(reportsDir, "Nabih_Debts_Report_${timeStamp}.pdf")
        FileOutputStream(reportFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return reportFile
    }
}
