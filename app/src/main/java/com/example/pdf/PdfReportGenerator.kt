package com.example.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import com.example.model.FinancialConstants
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
     * Helper to render RTL Arabic text cleanly and accurately using StaticLayout with exact vertical positioning
     */
    private fun drawBidiText(
        canvas: android.graphics.Canvas,
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
     * Helper to render LTR English text (e.g. Left-aligned brand title)
     */
    private fun drawLtrText(
        canvas: android.graphics.Canvas,
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
     * Multi-page PDF layout manager for A4 matching the exact original design
     */
    private class ClassicPdfPageManager(
        private val document: PdfDocument,
        val reportSubtitle: String,
        val periodText: String,
        val issueDateText: String,
        val itemCount: Int
    ) {
        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 48f
        val marginRight = 48f
        val contentWidth = pageWidth - (marginLeft + marginRight).toInt()
        val bottomMargin = 48f
        val maxContentY = pageHeight - bottomMargin

        var currentPageIndex = 0
        val pages = mutableListOf<PdfDocument.Page>()
        var currentCanvas: android.graphics.Canvas? = null
        var curY = 56f

        private val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        // Top Main Header: "Nabih Wallet" (Larger, bold, wide letter spacing)
        private val mainBrandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B2D42")
            textSize = 24f
            isFakeBoldText = true
            letterSpacing = 0.04f
        }

        // Subtitle: "تقرير: بيان المصروفات الشخصية"
        private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B86548")
            textSize = 12.5f
            isFakeBoldText = true
        }

        // Metadata: Period, Date, Count
        private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#555555")
            textSize = 10.5f
        }

        private val footerPagePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A0A0A0")
            textSize = 8.5f
        }

        fun startNewPage(): android.graphics.Canvas {
            currentPageIndex++
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageIndex).create()
            val page = document.startPage(pageInfo)
            pages.add(page)
            val canvas = page.canvas
            currentCanvas = canvas

            // Pure White Background
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            curY = 56f

            if (currentPageIndex == 1) {
                // 1. "Nabih Wallet" (Header aligned to the LEFT)
                drawLtrText(canvas, "Nabih Wallet", marginLeft, curY, contentWidth, mainBrandPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 34f

                // 2. "تقرير: بيان المصروفات الشخصية"
                drawBidiText(canvas, "تقرير: $reportSubtitle", marginLeft, curY, contentWidth, subtitlePaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 26f

                // 3. الفترة
                drawBidiText(canvas, "الفترة: $periodText", marginLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 20f

                // 4. تاريخ إصدار التقرير
                drawBidiText(canvas, "تاريخ إصدار التقرير: $issueDateText", marginLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 20f

                // 5. عدد البنود
                drawBidiText(canvas, "عدد البنود: $itemCount", marginLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 32f
            } else {
                val continuationHeader = "Nabih Wallet - $reportSubtitle (صفحة $currentPageIndex)"
                drawBidiText(canvas, continuationHeader, marginLeft, curY, contentWidth, metaPaint, Layout.Alignment.ALIGN_NORMAL)
                curY += 32f
            }

            return canvas
        }

        fun ensureSpace(requiredHeight: Float): android.graphics.Canvas {
            if (currentCanvas == null || (curY + requiredHeight) > maxContentY) {
                return startNewPage()
            }
            return currentCanvas!!
        }

        fun finish() {
            pages.forEachIndexed { index, page ->
                val canvas = page.canvas
                val footerY = pageHeight - 28f
                val footerText = "صفحة ${index + 1} من ${pages.size}"
                drawBidiText(canvas, footerText, marginLeft, footerY, contentWidth, footerPagePaint, Layout.Alignment.ALIGN_CENTER)
                document.finishPage(page)
            }
        }
    }

    /**
     * GENERATE EXPENSE REPORT (Matches exact design from Screenshot with perfected layout)
     */
    fun generateExpenseReport(
        context: Context,
        transactions: List<TransactionEntity>,
        periodTitle: String,
        currencySymbol: String = "جنيه"
    ): File {
        val pdfDocument = PdfDocument()
        val issueDateStr = formatArabicFullDate(Date())

        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        val targetList = if (expenseTransactions.isNotEmpty()) expenseTransactions else transactions

        // Compute Date Range for "الفترة"
        val periodText = if (targetList.isNotEmpty()) {
            val minD = targetList.minOf { it.dateMillis }
            val maxD = targetList.maxOf { it.dateMillis }
            "من ${formatDate(minD)} إلى ${formatDate(maxD)}"
        } else {
            periodTitle
        }

        // Aggregate by Category
        val categoryGroups = targetList
            .groupBy { FinancialConstants.normalizeCategoryName(it.category) }
            .map { (catName, items) ->
                val sum = items.sumOf { it.amount }
                catName to sum
            }
            .sortedByDescending { it.second }

        val totalExpenses = targetList.sumOf { it.amount }
        val totalVolume = if (totalExpenses > 0) totalExpenses else 1.0

        val pageManager = ClassicPdfPageManager(
            document = pdfDocument,
            reportSubtitle = "بيان المصروفات الشخصية",
            periodText = periodText,
            issueDateText = issueDateStr,
            itemCount = categoryGroups.size
        )

        // Palette matching screenshot exactly
        val tableHeaderBg = Color.parseColor("#D47053")  // Terracotta header
        val totalRowBg = Color.parseColor("#F4EBE1")    // Warm light beige footer
        val borderColor = Color.parseColor("#EDE7E0")   // Clean light border
        val textDark = Color.parseColor("#2B2D42")
        val terracottaText = Color.parseColor("#B86548")
        val textNote = Color.parseColor("#9E9E9E")
        val white = Color.WHITE

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 10.5f
            isFakeBoldText = true
        }

        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10f
        }

        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10.5f
            isFakeBoldText = true
        }

        val totalAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaText
            textSize = 10.5f
            isFakeBoldText = true
        }

        val summaryHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 12.5f
            isFakeBoldText = true
        }

        val summaryBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10.5f
        }

        val footerNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textNote
            textSize = 9f
        }

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = 0.8f
        }

        var canvas = pageManager.startNewPage()

        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalTableWidth = pageManager.contentWidth.toFloat()

        // 4 Columns matching screenshot (RTL from Right to Left):
        // 1. م (Index): 35f (Rightmost)
        // 2. البيان (Category): 215f
        // 3. المبلغ: 145f
        // 4. النسبة: 104f (Leftmost)
        val colW_m = 35f
        val colW_bayan = 215f
        val colW_amount = 145f
        val colW_pct = totalTableWidth - (colW_m + colW_bayan + colW_amount)

        // Coordinates from Left to Right on Canvas:
        val x_pct = tableLeft
        val x_amount = x_pct + colW_pct
        val x_bayan = x_amount + colW_amount
        val x_m = x_bayan + colW_bayan

        val headerHeight = 32f
        val rowHeight = 30f

        canvas = pageManager.ensureSpace(headerHeight)

        // Draw Terracotta Table Header
        fillPaint.color = tableHeaderBg
        canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + headerHeight, fillPaint)

        // Header text in RTL columns with centered vertical alignment
        val headerTextY = pageManager.curY + 8f
        drawBidiText(canvas, "م", x_m, headerTextY, colW_m.toInt(), headerTextPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "البيان", x_bayan + 12f, headerTextY, (colW_bayan - 24f).toInt(), headerTextPaint, Layout.Alignment.ALIGN_NORMAL)
        drawBidiText(canvas, "المبلغ", x_amount, headerTextY, colW_amount.toInt(), headerTextPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "النسبة", x_pct + 12f, headerTextY, (colW_pct - 24f).toInt(), headerTextPaint, Layout.Alignment.ALIGN_NORMAL)

        pageManager.curY += headerHeight

        if (categoryGroups.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            fillPaint.color = white
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)
            drawBidiText(canvas, "لا توجد مصروفات مسجلة لهذه الفترة", tableLeft, pageManager.curY + 8f, pageManager.contentWidth, cellTextPaint, Layout.Alignment.ALIGN_CENTER)
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)
            pageManager.curY += rowHeight
        } else {
            categoryGroups.forEachIndexed { index, (catName, sum) ->
                canvas = pageManager.ensureSpace(rowHeight)

                // White row background
                fillPaint.color = white
                canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)

                // Outer border and vertical column separators
                canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_m, pageManager.curY, x_m, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_bayan, pageManager.curY, x_bayan, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_amount, pageManager.curY, x_amount, pageManager.curY + rowHeight, borderPaint)

                val pct = (sum / totalVolume) * 100.0
                val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"
                val amtStr = formatAmount(sum)
                val rowTextY = pageManager.curY + 7.5f

                drawBidiText(canvas, "${index + 1}", x_m, rowTextY, colW_m.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, catName, x_bayan + 12f, rowTextY, (colW_bayan - 24f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)
                drawBidiText(canvas, amtStr, x_amount, rowTextY, colW_amount.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, pctStr, x_pct + 12f, rowTextY, (colW_pct - 24f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)

                pageManager.curY += rowHeight
            }

            // Total Summary Row at Bottom of Table
            canvas = pageManager.ensureSpace(rowHeight)
            fillPaint.color = totalRowBg
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)

            // Outer border of total row
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)

            val totalRowTextY = pageManager.curY + 7.5f
            drawBidiText(canvas, "إجمالي المصروفات", x_bayan + 12f, totalRowTextY, (colW_bayan + colW_m - 24f).toInt(), totalLabelPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(canvas, "${formatAmount(totalExpenses)} $currencySymbol", x_pct + 12f, totalRowTextY, (colW_pct + colW_amount - 24f).toInt(), totalAmountPaint, Layout.Alignment.ALIGN_NORMAL)

            pageManager.curY += rowHeight + 28f
        }

        // Summary Section ("الملخص") exactly matching screenshot
        val summarySpaceNeeded = 140f
        canvas = pageManager.ensureSpace(summarySpaceNeeded)

        drawBidiText(canvas, "الملخص", tableLeft, pageManager.curY, pageManager.contentWidth, summaryHeadingPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 24f

        val sum1 = "إجمالي المصروفات خلال الفترة: ${formatAmount(totalExpenses)} $currencySymbol"
        drawBidiText(canvas, sum1, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        // Average Monthly Calculation
        val monthsCount = if (targetList.isNotEmpty()) {
            val minD = targetList.minOf { it.dateMillis }
            val maxD = targetList.maxOf { it.dateMillis }
            val daysDiff = ((maxD - minD) / (1000L * 60 * 60 * 24)).coerceAtLeast(1)
            val months = daysDiff / 30.0
            if (months < 1.0) 1.0 else months
        } else 1.0
        val monthlyAvg = totalExpenses / monthsCount
        val sum2 = "متوسط الإنفاق الشهري: ${formatAmount(monthlyAvg)} $currencySymbol / شهر"
        drawBidiText(canvas, sum2, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        if (categoryGroups.isNotEmpty() && totalExpenses > 0) {
            val topCategory = categoryGroups.first()
            val topPct = (topCategory.second / totalExpenses) * 100.0
            val sum3 = "أعلى بند إنفاق: ${topCategory.first} (${String.format(Locale.US, "%.1f", topPct)}% من الإجمالي)"
            drawBidiText(canvas, sum3, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
            pageManager.curY += 19f
        }

        pageManager.curY += 14f

        // Footer note matching screenshot
        val footerNote = "ملاحظة: هذا التقرير تم إنشاؤه من بيانات تطبيق نبيه."
        drawBidiText(canvas, footerNote, tableLeft, pageManager.curY, pageManager.contentWidth, footerNotePaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 24f

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
     * GENERATE DEBTS REPORT (Matching original design)
     */
    fun generateDebtsReport(
        context: Context,
        debtsWithPayments: List<DebtWithPayments>,
        currencySymbol: String = "جنيه"
    ): File {
        val pdfDocument = PdfDocument()
        val issueDateStr = formatArabicFullDate(Date())

        val totalOwedToMe = debtsWithPayments.filter { it.debt.type == "OWED_TO_ME" }.sumOf { it.remainingAmount }
        val totalIOwe = debtsWithPayments.filter { it.debt.type == "I_OWE" }.sumOf { it.remainingAmount }
        val netDebts = totalOwedToMe - totalIOwe
        val totalVolume = (totalOwedToMe + totalIOwe).coerceAtLeast(1.0)

        val pageManager = ClassicPdfPageManager(
            document = pdfDocument,
            reportSubtitle = "بيان الديون والالتزامات المالية",
            periodText = "كافة السجلات المسجلة",
            issueDateText = issueDateStr,
            itemCount = debtsWithPayments.size
        )

        val tableHeaderBg = Color.parseColor("#D47053")
        val totalRowBg = Color.parseColor("#F4EBE1")
        val borderColor = Color.parseColor("#EDE7E0")
        val textDark = Color.parseColor("#2B2D42")
        val terracottaText = Color.parseColor("#B86548")
        val textNote = Color.parseColor("#9E9E9E")
        val white = Color.WHITE

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = white
            textSize = 10.5f
            isFakeBoldText = true
        }

        val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10f
        }

        val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10.5f
            isFakeBoldText = true
        }

        val totalAmountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = terracottaText
            textSize = 10.5f
            isFakeBoldText = true
        }

        val summaryHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 12.5f
            isFakeBoldText = true
        }

        val summaryBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10.5f
        }

        val footerNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textNote
            textSize = 9f
        }

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = 0.8f
        }

        var canvas = pageManager.startNewPage()

        val tableLeft = pageManager.marginLeft
        val tableRight = pageManager.pageWidth - pageManager.marginRight
        val totalTableWidth = pageManager.contentWidth.toFloat()

        // 4 Columns for Debts:
        // 1. م (Index): 35f
        // 2. الطرف (Person / Type): 215f
        // 3. المبلغ: 145f
        // 4. النسبة / الحالة: 104f
        val colW_m = 35f
        val colW_bayan = 215f
        val colW_amount = 145f
        val colW_pct = totalTableWidth - (colW_m + colW_bayan + colW_amount)

        val x_pct = tableLeft
        val x_amount = x_pct + colW_pct
        val x_bayan = x_amount + colW_amount
        val x_m = x_bayan + colW_bayan

        val headerHeight = 32f
        val rowHeight = 30f

        canvas = pageManager.ensureSpace(headerHeight)

        fillPaint.color = tableHeaderBg
        canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + headerHeight, fillPaint)

        val headerTextY = pageManager.curY + 8f
        drawBidiText(canvas, "م", x_m, headerTextY, colW_m.toInt(), headerTextPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "الطرف", x_bayan + 12f, headerTextY, (colW_bayan - 24f).toInt(), headerTextPaint, Layout.Alignment.ALIGN_NORMAL)
        drawBidiText(canvas, "المبلغ", x_amount, headerTextY, colW_amount.toInt(), headerTextPaint, Layout.Alignment.ALIGN_CENTER)
        drawBidiText(canvas, "النسبة", x_pct + 12f, headerTextY, (colW_pct - 24f).toInt(), headerTextPaint, Layout.Alignment.ALIGN_NORMAL)

        pageManager.curY += headerHeight

        if (debtsWithPayments.isEmpty()) {
            canvas = pageManager.ensureSpace(rowHeight)
            fillPaint.color = white
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)
            drawBidiText(canvas, "لا توجد ديون مسجلة", tableLeft, pageManager.curY + 8f, pageManager.contentWidth, cellTextPaint, Layout.Alignment.ALIGN_CENTER)
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)
            pageManager.curY += rowHeight
        } else {
            debtsWithPayments.forEachIndexed { index, item ->
                canvas = pageManager.ensureSpace(rowHeight)

                fillPaint.color = white
                canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)

                canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_m, pageManager.curY, x_m, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_bayan, pageManager.curY, x_bayan, pageManager.curY + rowHeight, borderPaint)
                canvas.drawLine(x_amount, pageManager.curY, x_amount, pageManager.curY + rowHeight, borderPaint)

                val isOwedToMe = item.debt.type == "OWED_TO_ME"
                val typeTag = if (isOwedToMe) "(مستحق لي)" else "(مستحق علي)"
                val personDisplay = "${item.debt.personName} $typeTag"
                val pct = (item.remainingAmount / totalVolume) * 100.0
                val pctStr = "${String.format(Locale.US, "%.1f", pct)}%"
                val amtStr = formatAmount(item.remainingAmount)
                val rowTextY = pageManager.curY + 7.5f

                drawBidiText(canvas, "${index + 1}", x_m, rowTextY, colW_m.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, personDisplay, x_bayan + 12f, rowTextY, (colW_bayan - 24f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)
                drawBidiText(canvas, amtStr, x_amount, rowTextY, colW_amount.toInt(), cellTextPaint, Layout.Alignment.ALIGN_CENTER)
                drawBidiText(canvas, pctStr, x_pct + 12f, rowTextY, (colW_pct - 24f).toInt(), cellTextPaint, Layout.Alignment.ALIGN_NORMAL)

                pageManager.curY += rowHeight
            }

            // Total Summary Row
            canvas = pageManager.ensureSpace(rowHeight)
            fillPaint.color = totalRowBg
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, fillPaint)
            canvas.drawRect(tableLeft, pageManager.curY, tableRight, pageManager.curY + rowHeight, borderPaint)

            val totalRowTextY = pageManager.curY + 7.5f
            drawBidiText(canvas, "صافي رصيد الديون", x_bayan + 12f, totalRowTextY, (colW_bayan + colW_m - 24f).toInt(), totalLabelPaint, Layout.Alignment.ALIGN_NORMAL)
            drawBidiText(canvas, "${formatAmount(netDebts)} $currencySymbol", x_pct + 12f, totalRowTextY, (colW_pct + colW_amount - 24f).toInt(), totalAmountPaint, Layout.Alignment.ALIGN_NORMAL)

            pageManager.curY += rowHeight + 28f
        }

        // الملخص
        val summarySpaceNeeded = 140f
        canvas = pageManager.ensureSpace(summarySpaceNeeded)

        drawBidiText(canvas, "الملخص", tableLeft, pageManager.curY, pageManager.contentWidth, summaryHeadingPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 24f

        val sum1 = "إجمالي الديون المستحقة لي: ${formatAmount(totalOwedToMe)} $currencySymbol"
        drawBidiText(canvas, sum1, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        val sum2 = "إجمالي الديون المستحقة علي: ${formatAmount(totalIOwe)} $currencySymbol"
        drawBidiText(canvas, sum2, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        val sum3 = "صافي رصيد الديون: ${formatAmount(netDebts)} $currencySymbol"
        drawBidiText(canvas, sum3, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        val fullyPaidCount = debtsWithPayments.count { it.isFullyPaid }
        val sum4 = "عدد السجلات المسددة بالكامل: $fullyPaidCount من أصل ${debtsWithPayments.size}"
        drawBidiText(canvas, sum4, tableLeft, pageManager.curY, pageManager.contentWidth, summaryBodyPaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 19f

        pageManager.curY += 14f

        val footerNote = "ملاحظة: هذا التقرير تم إنشاؤه من بيانات تطبيق نبيه."
        drawBidiText(canvas, footerNote, tableLeft, pageManager.curY, pageManager.contentWidth, footerNotePaint, Layout.Alignment.ALIGN_NORMAL)
        pageManager.curY += 24f

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
