package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
 * Classic, Clean Financial PDF Report Generator for Nabih Wallet (محفظة نبيه).
 * Features:
 * - Balanced page margins and comfortable vertical spacing between all sections.
 * - Right-aligned classic header block with bold labels.
 * - Classic 5-column table (م | البيان | المبلغ (جنيه) | النسبة | ملاحظات) with warm brand colors.
 * - Formatted "الملخص" section with clear bullet points (• إجمالي، • متوسط، إلخ) and bold labels.
 * - Dedicated Notes/Remarks block anchored at the very bottom of the page (page footer).
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
     * Looks up default category description for the "ملاحظات" column when a transaction has no custom note.
     */
    private fun getDefaultCategoryNote(categoryName: String): String {
        val allCats = FinancialConstants.expenseCategories + FinancialConstants.incomeCategories
        val match = allCats.firstOrNull {
            it.nameAr == categoryName || it.nameEn.equals(categoryName, ignoreCase = true) || it.id == categoryName
        }
        return match?.descriptionAr?.takeIf { it.isNotBlank() } ?: "مصروفات عامة"
    }

    /**
     * Multi-page PDF layout manager for standard A4 pages in Classic Statement style.
     * Reserves a dedicated footer zone at the very bottom of every page for the Notes section.
     */
    private class ClassicPdfPageManager(
        private val document: PdfDocument,
        val reportSubtitle: String,
        val periodValue: String,
        val issueDateValue: String,
        val itemsCountValue: String
    ) {
        val pageWidth = 595
        val pageHeight = 842

        // Comfortable, balanced horizontal & vertical page margins
        val marginLeft = 46f
        val marginRight = 46f
        val marginTop = 48f
        val contentWidth = (pageWidth - (marginLeft + marginRight)).toInt()

        // Reserved bottom footer area for the Notes block (تذييل الصفحة - الأسفل تماماً)
        val footerTopY = pageHeight - 62f
        val maxContentY = footerTopY - 18f

        var currentPageIndex = 0
        var currentPage: PdfDocument.Page? = null
        var currentCanvas: Canvas? = null
        var curY = marginTop

        // Classic Warm Palette (Nabih Wallet colors)
        val colorTitleText = Color.parseColor("#8C3B20")        // Deep Warm Terracotta-Brown Title
        val colorAccentBullet = Color.parseColor("#B85435")     // Warm Terracotta Bullet
        val colorMetaText = Color.parseColor("#2B2623")         // Classic Charcoal-Brown Metadata
        val colorFooterDivider = Color.parseColor("#E6DCD2")    // Subtle Warm Divider
        val colorFooterNote = Color.parseColor("#6E665E")       // Readable Muted Brown for Footer Note

        val reportTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTitleText
            textSize = 15f
            isFakeBoldText = true
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorMetaText
            textSize = 11f
        }

        val footerDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFooterDivider
            strokeWidth = 0.9f
            style = Paint.Style.STROKE
        }

        val footerNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorFooterNote
            textSize = 9.5f
        }

        private fun drawPageFooter(canvas: Canvas) {
            val left = marginLeft
            val right = pageWidth - marginRight

            // Subtle divider line separating page content from the bottom footer
            canvas.drawLine(left, footerTopY, right, footerTopY, footerDividerPaint)

            // Notes field anchored at the very bottom of the page (تذييل الصفحة)
            val noteSpan = buildLabelValueSpan(
                label = "الملاحظات:",
                value = "هذا التقرير تم إنشاؤه من بيانات محفظة نبيه."
            )
            drawBidiText(
                canvas = canvas,
                text = noteSpan,
                x = left,
                y = footerTopY + 10f,
                width = contentWidth,
                textPaint = footerNotePaint,
                align = Layout.Alignment.ALIGN_NORMAL
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

            if (currentPageIndex == 1) {
                // Classic Top-Right Header Block with comfortable line spacing
                drawBidiText(
                    canvas,
                    reportSubtitle,
                    tableLeft,
                    curY,
                    contentWidth,
                    reportTitlePaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                curY += 28f

                drawBidiText(
                    canvas,
                    buildLabelValueSpan("• الفترة:", periodValue, bulletColor = colorAccentBullet),
                    tableLeft,
                    curY,
                    contentWidth,
                    metaPaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                curY += 21f

                drawBidiText(
                    canvas,
                    buildLabelValueSpan("• تاريخ إصدار التقرير:", issueDateValue, bulletColor = colorAccentBullet),
                    tableLeft,
                    curY,
                    contentWidth,
                    metaPaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                curY += 21f

                drawBidiText(
                    canvas,
                    buildLabelValueSpan("• عدد البنود:", itemsCountValue, bulletColor = colorAccentBullet),
                    tableLeft,
                    curY,
                    contentWidth,
                    metaPaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                curY += 26f
            } else {
                drawBidiText(
                    canvas,
                    reportSubtitle,
                    tableLeft,
                    curY,
                    contentWidth,
                    reportTitlePaint,
                    Layout.Alignment.ALIGN_NORMAL
                )
                curY += 26f
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
     * GENERATE CLASSIC EXPENSE REPORT
     * - Clean header with well-spaced metadata lines.
     * - Harmonious 5-column table with comfortable cell padding and row height.
     * - Bulleted "الملخص" section (• إجمالي، • متوسط، إلخ).
     * - Notes block anchored at the very bottom footer of the page.
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
                        " ($months شهرًا)"
                    }
                    diffDays > 1L -> " ($diffDays يومًا)"
                    else -> ""
                }
                "من $fromStr إلى $toStr$durationLabel"
            }
            else -> periodTitle
        }

        val totalExpenses = targetExpenses.sumOf { it.amount }
        val totalVolume = if (totalExpenses > 0.0) totalExpenses else 1.0

        data class ClassicExpenseRow(
            val title: String,
            val amount: Double,
            val percentage: Double,
            val notes: String
        )

        val categoryRows = targetExpenses
            .groupBy { it.category.ifBlank { "أخرى" } }
            .map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                val pct = (sum / totalVolume) * 100.0
                val userNotes = items
                    .mapNotNull { it.notes.trim().takeIf { n -> n.isNotEmpty() } }
                    .distinct()
                    .joinToString(" / ")
                val noteText = userNotes.ifEmpty { getDefaultCategoryNote(catName) }
                ClassicExpenseRow(
                    title = catName,
                    amount = sum,
                    percentage = pct,
                    notes = noteText
                )
            }
            .sortedByDescending { it.amount }

        val pageManager = ClassicPdfPageManager(
            document = pdfDocument,
            reportSubtitle = "تقرير: بيان المصروفات الشخصية",
            periodValue = periodValueStr,
            issueDateValue = issueDateStr,
            itemsCountValue = "${categoryRows.size} بنود"
        )

        // Warm Brand Colors
        val colorHeaderBg = Color.parseColor("#B85435")         // Warm Terracotta Header Bar
        val colorZebraBg = Color.parseColor("#F9F3EE")          // Soft Warm Cream Zebra Row
        val colorTotalRowBg = Color.parseColor("#EFE3D8")       // Warm Tinted Total Bar
        val colorRowDivider = Color.parseColor("#E6DCD2")       // Subtle Warm Divider
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

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            isFakeBoldText = true
        }
        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 10.5f
        }
        val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 10.5f
            isFakeBoldText = true
        }
        val totalTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11f
            isFakeBoldText = true
        }
        val summaryHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorSectionTitle
            textSize = 13.5f
            isFakeBoldText = true
        }
        val summaryLinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11f
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // -------------------------------------------------------------------------
        // CLASSIC 5-COLUMN TABLE (RTL: م | البيان | المبلغ (جنيه) | النسبة | ملاحظات)
        // -------------------------------------------------------------------------
        val colW_seq = 38f
        val colW_amount = 118f
        val colW_pct = 76f
        val colW_notes = 132f
        val colW_item = totalWidth - (colW_seq + colW_amount + colW_pct + colW_notes)

        // LTR X positions (Left -> Right):
        // [ملاحظات] [النسبة] [المبلغ] [البيان] [م]
        val x_notes = tableLeft
        val x_pct = x_notes + colW_notes
        val x_amount = x_pct + colW_pct
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        fun drawClassicTableHeader(c: Canvas, topY: Float): Float {
            val headerH = 34f
            val headerRect = RectF(tableLeft, topY, tableRight, topY + headerH)
            c.drawRect(headerRect, headerBgPaint)

            val textY = topY + 9f
            drawBidiText(c, "م", x_seq, textY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "البيان", x_item, textY, (colW_item - 10f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(c, "المبلغ ($currencySymbol)", x_amount, textY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "النسبة", x_pct, textY, colW_pct.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "ملاحظات", x_notes + 8f, textY, (colW_notes - 14f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            return topY + headerH
        }

        pageManager.curY = drawClassicTableHeader(canvas, pageManager.curY)

        val rowHeight = 34f
        if (categoryRows.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            drawBidiText(
                canvas,
                "لا توجد مصروفات مسجلة خلال هذه الفترة",
                tableLeft,
                pageManager.curY + 9f,
                totalWidth.toInt(),
                cellTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)
            pageManager.curY += rowHeight
        } else {
            categoryRows.forEachIndexed { index, row ->
                if (pageManager.curY + rowHeight > pageManager.maxContentY) {
                    canvas = pageManager.startNewPage()
                    pageManager.curY = drawClassicTableHeader(canvas, pageManager.curY)
                }

                val rTop = pageManager.curY
                val rBottom = rTop + rowHeight

                // Alternating rows get soft warm tint
                if (index % 2 == 1) {
                    canvas.drawRect(RectF(tableLeft, rTop, tableRight, rBottom), zebraBgPaint)
                }

                val textY = rTop + 9f
                val pctStr = "${String.format(Locale.US, "%.1f", row.percentage)}%"

                // 1. م
                drawLtrText(canvas, "${index + 1}", x_seq, textY, colW_seq.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                // 2. البيان
                drawBidiText(canvas, row.title, x_item, textY, (colW_item - 10f).toInt(), cellBoldPaint, Layout.Alignment.ALIGN_NORMAL)
                // 3. المبلغ
                drawLtrText(canvas, formatAmount(row.amount), x_amount, textY, colW_amount.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                // 4. النسبة
                drawLtrText(canvas, pctStr, x_pct, textY, colW_pct.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                // 5. ملاحظات
                val noteDisplay = if (row.notes.length > 26) row.notes.take(24) + "…" else row.notes
                drawBidiText(canvas, noteDisplay, x_notes + 8f, textY, (colW_notes - 14f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)

                // Thin bottom row divider
                canvas.drawLine(tableLeft, rBottom, tableRight, rBottom, rowDividerPaint)

                pageManager.curY += rowHeight
            }

            // Total Row (إجمالي المصروفات)
            val totalRowH = 36f
            canvas = pageManager.ensureSpace(totalRowH + 10f)
            val totalRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + totalRowH)
            canvas.drawRect(totalRect, totalBgPaint)

            val totalTextY = pageManager.curY + 10f
            drawBidiText(
                canvas,
                "إجمالي المصروفات",
                x_item,
                totalTextY,
                (colW_item - 10f).toInt(),
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

            pageManager.curY += totalRowH
        }

        // -------------------------------------------------------------------------
        // CLASSIC SUMMARY SECTION (الملخص) WITH CLEAR BULLETS (•) & SPACING
        // -------------------------------------------------------------------------
        pageManager.curY += 36f
        canvas = pageManager.ensureSpace(165f)

        drawBidiText(
            canvas,
            "الملخص:",
            tableLeft,
            pageManager.curY,
            totalWidth.toInt(),
            summaryHeaderPaint,
            Layout.Alignment.ALIGN_NORMAL
        )
        pageManager.curY += 26f

        val bulletIndentRight = 6f
        val summaryWidth = (totalWidth - bulletIndentRight).toInt()

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
                x = tableLeft,
                y = pageManager.curY,
                width = summaryWidth,
                textPaint = summaryLinePaint,
                align = Layout.Alignment.ALIGN_NORMAL
            )
            pageManager.curY += 23f
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
        val topCategory = categoryRows.firstOrNull()
        if (topCategory != null) {
            val topPct = String.format(Locale.US, "%.1f", topCategory.percentage)
            drawBulletedSummaryLine(
                label = "أعلى بند إنفاق:",
                value = "${topCategory.title} ($topPct% من الإجمالي)"
            )
        }

        // 5. • أقل بند إنفاق
        val lowestCategory = categoryRows.lastOrNull()
        if (lowestCategory != null && lowestCategory != topCategory) {
            val lowPct = String.format(Locale.US, "%.1f", lowestCategory.percentage)
            drawBulletedSummaryLine(
                label = "أقل بند إنفاق:",
                value = "${lowestCategory.title} ($lowPct% من الإجمالي)"
            )
        }

        // Finish page (automatically draws the Notes block at the very bottom footer of the page)
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
     * GENERATE CLASSIC DEBTS REPORT
     * Uses the same organized layout, bulleted summary, and bottom-anchored footer notes.
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
            reportSubtitle = "تقرير: بيان الديون والالتزامات الشخصية",
            periodValue = periodValueStr,
            issueDateValue = issueDateStr,
            itemsCountValue = "${debts.size} بنود"
        )

        val colorHeaderBg = Color.parseColor("#B85435")
        val colorZebraBg = Color.parseColor("#F9F3EE")
        val colorTotalRowBg = Color.parseColor("#EFE3D8")
        val colorRowDivider = Color.parseColor("#E6DCD2")
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

        val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 10.5f
            isFakeBoldText = true
        }
        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 10.5f
        }
        val cellBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 10.5f
            isFakeBoldText = true
        }
        val totalTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11f
            isFakeBoldText = true
        }
        val summaryHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorSectionTitle
            textSize = 13.5f
            isFakeBoldText = true
        }
        val summaryLinePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTextDark
            textSize = 11f
        }

        var canvas = pageManager.startNewPage()
        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalWidth = pageManager.contentWidth.toFloat()

        // Classic 5-Column Table (RTL: م | البيان | المبلغ (جنيه) | النسبة | ملاحظات)
        val colW_seq = 38f
        val colW_amount = 118f
        val colW_pct = 76f
        val colW_notes = 132f
        val colW_item = totalWidth - (colW_seq + colW_amount + colW_pct + colW_notes)

        val x_notes = tableLeft
        val x_pct = x_notes + colW_notes
        val x_amount = x_pct + colW_pct
        val x_item = x_amount + colW_amount
        val x_seq = x_item + colW_item

        fun drawClassicDebtsHeader(c: Canvas, topY: Float): Float {
            val headerH = 34f
            val headerRect = RectF(tableLeft, topY, tableRight, topY + headerH)
            c.drawRect(headerRect, headerBgPaint)

            val textY = topY + 9f
            drawBidiText(c, "م", x_seq, textY, colW_seq.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "البيان", x_item, textY, (colW_item - 10f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(c, "المبلغ ($currencySymbol)", x_amount, textY, colW_amount.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "النسبة", x_pct, textY, colW_pct.toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_CENTER)
            drawBidiText(c, "ملاحظات", x_notes + 8f, textY, (colW_notes - 14f).toInt(), tableHeaderPaint, Layout.Alignment.ALIGN_NORMAL)
            return topY + headerH
        }

        pageManager.curY = drawClassicDebtsHeader(canvas, pageManager.curY)

        val rowHeight = 34f
        if (debts.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            drawBidiText(
                canvas,
                "لا توجد ديون مسجلة",
                tableLeft,
                pageManager.curY + 9f,
                totalWidth.toInt(),
                cellTextPaint,
                Layout.Alignment.ALIGN_CENTER
            )
            canvas.drawLine(tableLeft, pageManager.curY + rowHeight, tableRight, pageManager.curY + rowHeight, rowDividerPaint)
            pageManager.curY += rowHeight
        } else {
            debts.forEachIndexed { index, item ->
                if (pageManager.curY + rowHeight > pageManager.maxContentY) {
                    canvas = pageManager.startNewPage()
                    pageManager.curY = drawClassicDebtsHeader(canvas, pageManager.curY)
                }

                val rTop = pageManager.curY
                val rBottom = rTop + rowHeight

                if (index % 2 == 1) {
                    canvas.drawRect(RectF(tableLeft, rTop, tableRight, rBottom), zebraBgPaint)
                }

                val pct = (item.remainingAmount / totalVolume) * 100.0
                val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"
                val typeNote = if (item.type == "LENT") "مستحق لي" else "مستحق علي"
                val statusNote = if (item.remainingAmount <= 0.001) "مسدد بالكامل" else typeNote

                val textY = rTop + 9f
                drawLtrText(canvas, "${index + 1}", x_seq, textY, colW_seq.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, item.personName, x_item, textY, (colW_item - 10f).toInt(), cellBoldPaint, Layout.Alignment.ALIGN_NORMAL)
                drawLtrText(canvas, formatAmount(item.remainingAmount), x_amount, textY, colW_amount.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawLtrText(canvas, pctStr, x_pct, textY, colW_pct.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, statusNote, x_notes + 8f, textY, (colW_notes - 14f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)

                canvas.drawLine(tableLeft, rBottom, tableRight, rBottom, rowDividerPaint)
                pageManager.curY += rowHeight
            }

            val totalRowH = 36f
            canvas = pageManager.ensureSpace(totalRowH + 10f)
            val totalRect = RectF(tableLeft, pageManager.curY, tableRight, pageManager.curY + totalRowH)
            canvas.drawRect(totalRect, totalBgPaint)

            val totalTextY = pageManager.curY + 10f
            drawBidiText(
                canvas,
                "إجمالي المديونيات المتبقية",
                x_item,
                totalTextY,
                (colW_item - 10f).toInt(),
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

            pageManager.curY += totalRowH
        }

        // Summary Section (الملخص) with Bullets
        pageManager.curY += 36f
        canvas = pageManager.ensureSpace(140f)

        drawBidiText(
            canvas,
            "الملخص:",
            tableLeft,
            pageManager.curY,
            totalWidth.toInt(),
            summaryHeaderPaint,
            Layout.Alignment.ALIGN_NORMAL
        )
        pageManager.curY += 26f

        val bulletIndentRight = 6f
        val summaryWidth = (totalWidth - bulletIndentRight).toInt()

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
                x = tableLeft,
                y = pageManager.curY,
                width = summaryWidth,
                textPaint = summaryLinePaint,
                align = Layout.Alignment.ALIGN_NORMAL
            )
            pageManager.curY += 23f
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
