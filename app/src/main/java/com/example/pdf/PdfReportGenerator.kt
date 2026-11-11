package com.example.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtRecord
import com.example.model.FinancialConstants
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Ultra-simple, purely typographic PDF Report Generator for Nabih Wallet (محفظة نبيه).
 * Matches the app's exact dark visual design (#000000 background, #121212 summary card,
 * #2F3336 thin horizontal dividers, Cairo typography, and symmetrical 3-column spacing).
 */
object PdfReportGenerator {

    // Standard A4 Page Dimensions (72 PPI)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 40f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 48f
    private const val CONTENT_LEFT = MARGIN_H
    private const val CONTENT_RIGHT = PAGE_WIDTH - MARGIN_H
    private const val CONTENT_CENTER = PAGE_WIDTH / 2f

    // Exact App Color Palette (Dark Only - Spotify & X Theme)
    private val COLOR_BG = Color.parseColor("#000000")
    private val COLOR_CARD = Color.parseColor("#121212")
    private val COLOR_DIVIDER = Color.parseColor("#2F3336")
    private val COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF")
    private val COLOR_TEXT_SECONDARY = Color.parseColor("#B3B3B3")
    private val COLOR_TEXT_MUTED = Color.parseColor("#71767B")
    private val COLOR_GREEN = Color.parseColor("#1DB954")
    private val COLOR_BLUE = Color.parseColor("#1D9BF0")
    private val COLOR_RED = Color.parseColor("#F4212E")

    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private fun formatAmount(amount: Double): String = numberFormat.format(amount)

    private fun formatDate(millis: Long): String {
        return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(millis))
    }

    private fun formatIssueDate(date: Date = Date()): String {
        return SimpleDateFormat("yyyy/MM/dd", Locale.US).format(date)
    }

    private data class Fonts(
        val regular: Typeface,
        val bold: Typeface
    )

    private fun loadFonts(context: Context): Fonts {
        val base = try {
            ResourcesCompat.getFont(context, R.font.cairo)
                ?: Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        } catch (_: Exception) {
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        return Fonts(
            regular = Typeface.create(base, Typeface.NORMAL),
            bold = Typeface.create(base, Typeface.BOLD)
        )
    }

    private fun createPaint(
        typeface: Typeface,
        sizeSp: Float,
        color: Int,
        align: Paint.Align
    ): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.typeface = typeface
            this.textSize = sizeSp
            this.color = color
            this.textAlign = align
        }
    }

    private fun drawCenteredBaselineText(
        canvas: Canvas,
        text: String,
        x: Float,
        centerY: Float,
        paint: TextPaint
    ) {
        val baseline = centerY - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, x, baseline, paint)
    }

    private fun drawThinDivider(canvas: Canvas, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            color = COLOR_DIVIDER
        }
        canvas.drawLine(CONTENT_LEFT, y, CONTENT_RIGHT, y, paint)
    }

    private fun drawHeader(
        canvas: Canvas,
        fonts: Fonts,
        title: String,
        periodText: String,
        issueDate: String
    ): Float {
        var y = MARGIN_TOP

        val titlePaint = createPaint(fonts.bold, 18f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)
        drawCenteredBaselineText(canvas, title, CONTENT_RIGHT, y + 12f, titlePaint)
        y += 32f

        val periodPaint = createPaint(fonts.regular, 10f, COLOR_TEXT_SECONDARY, Paint.Align.RIGHT)
        val datePaint = createPaint(fonts.regular, 9.5f, COLOR_TEXT_MUTED, Paint.Align.LEFT)

        drawCenteredBaselineText(canvas, periodText, CONTENT_RIGHT, y + 8f, periodPaint)
        drawCenteredBaselineText(canvas, "التاريخ: $issueDate", CONTENT_LEFT, y + 8f, datePaint)
        y += 24f

        drawThinDivider(canvas, y)
        return y + 22f
    }

    private fun drawFooter(canvas: Canvas, fonts: Fonts, pageNumber: Int) {
        val footerTop = PAGE_HEIGHT - 38f
        drawThinDivider(canvas, footerTop)

        val footerPaint = createPaint(fonts.regular, 9f, COLOR_TEXT_MUTED, Paint.Align.CENTER)
        drawCenteredBaselineText(canvas, "صفحة $pageNumber", CONTENT_CENTER, footerTop + 18f, footerPaint)
    }

    private data class CategoryStatRow(
        val name: String,
        val percentage: Double,
        val amount: Double
    )

    /**
     * Generates a very simple, well-spaced Expense PDF Report matching the app's dark design.
     * Contains only:
     * 1. Header (Title, Period, Date)
     * 2. Total Expenses Summary Card (#121212)
     * 3. Expenses by Category (Category, Percentage, Amount)
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
        val fonts = loadFonts(context)

        val expenses = transactions.filter { it.type == "EXPENSE" }
        val totalExpenses = expenses.sumOf { it.amount }

        val minDateMillis = expenses.minOfOrNull { it.dateMillis }
            ?: transactions.minOfOrNull { it.dateMillis }
            ?: System.currentTimeMillis()
        val maxDateMillis = expenses.maxOfOrNull { it.dateMillis }
            ?: transactions.maxOfOrNull { it.dateMillis }
            ?: System.currentTimeMillis()

        val resolvedStartStr = startDateStr.ifBlank { formatDate(minDateMillis) }
        val resolvedEndStr = endDateStr.ifBlank { formatDate(maxDateMillis) }

        val categoryRows = expenses
            .groupBy { FinancialConstants.normalizeCategoryName(it.category) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .map { (name, sum) ->
                val pct = if (totalExpenses > 0.0) (sum / totalExpenses) * 100.0 else 0.0
                CategoryStatRow(
                    name = name,
                    percentage = pct,
                    amount = sum
                )
            }

        val issueDate = formatIssueDate()
        val periodMeta = "الفترة: $periodTitle ($resolvedStartStr - $resolvedEndStr)"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val bgPaint = Paint().apply { color = COLOR_BG }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        fun startNewPage(): Float {
            drawFooter(canvas, fonts, pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)
            return drawHeader(canvas, fonts, "تقرير المصروفات", periodMeta, issueDate)
        }

        var currentY = drawHeader(canvas, fonts, "تقرير المصروفات", periodMeta, issueDate)

        // 1. Simple Summary Card (#121212 surface, 12dp radius — only Total Expenses)
        val summaryCardH = 72f
        val summaryRect = RectF(CONTENT_LEFT, currentY, CONTENT_RIGHT, currentY + summaryCardH)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = COLOR_CARD
        }
        canvas.drawRoundRect(summaryRect, 12f, 12f, cardPaint)

        val labelPaint = createPaint(fonts.regular, 10.5f, COLOR_TEXT_SECONDARY, Paint.Align.RIGHT)
        val bigTotalPaint = createPaint(fonts.bold, 20f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)

        drawCenteredBaselineText(
            canvas = canvas,
            text = "إجمالي المصروفات",
            x = CONTENT_RIGHT - 20f,
            centerY = currentY + 22f,
            paint = labelPaint
        )
        drawCenteredBaselineText(
            canvas = canvas,
            text = "\u200F${formatAmount(totalExpenses)} $currencySymbol",
            x = CONTENT_RIGHT - 20f,
            centerY = currentY + 49f,
            paint = bigTotalPaint
        )

        currentY += summaryCardH + 28f

        // 2. Section: المصروفات حسب الفئة
        val sectionTitlePaint = createPaint(fonts.bold, 13f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)
        drawCenteredBaselineText(
            canvas = canvas,
            text = "المصروفات حسب الفئة",
            x = CONTENT_RIGHT,
            centerY = currentY + 10f,
            paint = sectionTitlePaint
        )
        currentY += 28f

        fun drawCategoryColumnsHeader(y: Float): Float {
            val colRightPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.RIGHT)
            val colCenterPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.CENTER)
            val colLeftPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.LEFT)

            val centerY = y + 12f
            drawCenteredBaselineText(canvas, "الفئة", CONTENT_RIGHT, centerY, colRightPaint)
            drawCenteredBaselineText(canvas, "النسبة", CONTENT_CENTER, centerY, colCenterPaint)
            drawCenteredBaselineText(canvas, "المبلغ", CONTENT_LEFT, centerY, colLeftPaint)

            drawThinDivider(canvas, y + 26f)
            return y + 26f
        }

        currentY = drawCategoryColumnsHeader(currentY)

        val rowHeight = 38f
        val namePaint = createPaint(fonts.bold, 11f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)
        val pctPaint = createPaint(fonts.regular, 10.5f, COLOR_TEXT_SECONDARY, Paint.Align.CENTER)
        val amountPaint = createPaint(fonts.bold, 11f, COLOR_TEXT_PRIMARY, Paint.Align.LEFT)

        if (categoryRows.isEmpty()) {
            val emptyPaint = createPaint(fonts.regular, 10.5f, COLOR_TEXT_MUTED, Paint.Align.CENTER)
            drawCenteredBaselineText(
                canvas = canvas,
                text = "لا توجد مصروفات مسجلة في هذه الفترة",
                x = CONTENT_CENTER,
                centerY = currentY + rowHeight / 2f,
                paint = emptyPaint
            )
            currentY += rowHeight
            drawThinDivider(canvas, currentY)
        } else {
            for (row in categoryRows) {
                if (currentY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 40f) {
                    currentY = startNewPage()
                    currentY = drawCategoryColumnsHeader(currentY)
                }

                val rowCenterY = currentY + rowHeight / 2f
                val pctStr = String.format(Locale.US, "%.1f%%", row.percentage)
                val amtStr = "\u200F${formatAmount(row.amount)} $currencySymbol"

                drawCenteredBaselineText(canvas, row.name, CONTENT_RIGHT, rowCenterY, namePaint)
                drawCenteredBaselineText(canvas, pctStr, CONTENT_CENTER, rowCenterY, pctPaint)
                drawCenteredBaselineText(canvas, amtStr, CONTENT_LEFT, rowCenterY, amountPaint)

                currentY += rowHeight
                drawThinDivider(canvas, currentY)
            }

            // Total Row
            if (currentY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 20f) {
                currentY = startNewPage()
            }

            val totalCenterY = currentY + rowHeight / 2f
            val totalRightPaint = createPaint(fonts.bold, 11.5f, COLOR_GREEN, Paint.Align.RIGHT)
            val totalCenterPaint = createPaint(fonts.bold, 10.5f, COLOR_GREEN, Paint.Align.CENTER)
            val totalLeftPaint = createPaint(fonts.bold, 11.5f, COLOR_GREEN, Paint.Align.LEFT)

            drawCenteredBaselineText(canvas, "الإجمالي", CONTENT_RIGHT, totalCenterY, totalRightPaint)
            drawCenteredBaselineText(canvas, "100%", CONTENT_CENTER, totalCenterY, totalCenterPaint)
            drawCenteredBaselineText(
                canvas = canvas,
                text = "\u200F${formatAmount(totalExpenses)} $currencySymbol",
                x = CONTENT_LEFT,
                centerY = totalCenterY,
                paint = totalLeftPaint
            )

            currentY += rowHeight
            drawThinDivider(canvas, currentY)
        }

        drawFooter(canvas, fonts, pageNumber)
        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Nabih_Expenses_Report_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
            out.flush()
        }
        pdfDocument.close()

        return file
    }

    /**
     * Generates a very simple, well-spaced Debts PDF Report matching the app's dark design.
     */
    fun generateDebtsReportFromRecords(
        context: Context,
        debts: List<DebtRecord>,
        currencySymbol: String = "جنيه"
    ): File {
        val pdfDocument = PdfDocument()
        val fonts = loadFonts(context)

        val owedToMeList = debts.filter { it.type == "LENT" || it.type == "OWED_TO_ME" }
        val iOweList = debts.filter { it.type != "LENT" && it.type != "OWED_TO_ME" }

        val totalOwedToMe = owedToMeList.sumOf { it.remainingAmount }
        val totalIOwe = iOweList.sumOf { it.remainingAmount }
        val netPosition = totalOwedToMe - totalIOwe

        val issueDate = formatIssueDate()
        val periodMeta = "مستحق لي: ${formatAmount(totalOwedToMe)} $currencySymbol   •   عليّ: ${formatAmount(totalIOwe)} $currencySymbol"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val bgPaint = Paint().apply { color = COLOR_BG }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        fun startNewPage(): Float {
            drawFooter(canvas, fonts, pageNumber)
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)
            return drawHeader(canvas, fonts, "تقرير الديون", periodMeta, issueDate)
        }

        var currentY = drawHeader(canvas, fonts, "تقرير الديون", periodMeta, issueDate)

        // 1. Simple Summary Card (#121212 surface, 12dp radius — Net Debts)
        val summaryCardH = 72f
        val summaryRect = RectF(CONTENT_LEFT, currentY, CONTENT_RIGHT, currentY + summaryCardH)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = COLOR_CARD
        }
        canvas.drawRoundRect(summaryRect, 12f, 12f, cardPaint)

        val labelPaint = createPaint(fonts.regular, 10.5f, COLOR_TEXT_SECONDARY, Paint.Align.RIGHT)
        val bigTotalPaint = createPaint(fonts.bold, 20f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)

        val netPrefix = when {
            netPosition > 0.0 -> "+"
            netPosition < 0.0 -> "-"
            else -> ""
        }

        drawCenteredBaselineText(
            canvas = canvas,
            text = "صافي الديون",
            x = CONTENT_RIGHT - 20f,
            centerY = currentY + 22f,
            paint = labelPaint
        )
        drawCenteredBaselineText(
            canvas = canvas,
            text = "\u200F$netPrefix${formatAmount(abs(netPosition))} $currencySymbol",
            x = CONTENT_RIGHT - 20f,
            centerY = currentY + 49f,
            paint = bigTotalPaint
        )

        currentY += summaryCardH + 28f

        // 2. Section: قائمة الديون (Symmetrical 3-column layout: الاسم | النوع | المتبقي)
        val sectionTitlePaint = createPaint(fonts.bold, 13f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)
        drawCenteredBaselineText(
            canvas = canvas,
            text = "قائمة الديون",
            x = CONTENT_RIGHT,
            centerY = currentY + 10f,
            paint = sectionTitlePaint
        )
        currentY += 28f

        fun drawDebtsColumnsHeader(y: Float): Float {
            val colRightPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.RIGHT)
            val colCenterPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.CENTER)
            val colLeftPaint = createPaint(fonts.bold, 9.5f, COLOR_TEXT_MUTED, Paint.Align.LEFT)

            val centerY = y + 12f
            drawCenteredBaselineText(canvas, "الاسم", CONTENT_RIGHT, centerY, colRightPaint)
            drawCenteredBaselineText(canvas, "النوع", CONTENT_CENTER, centerY, colCenterPaint)
            drawCenteredBaselineText(canvas, "المبلغ المتبقي", CONTENT_LEFT, centerY, colLeftPaint)

            drawThinDivider(canvas, y + 26f)
            return y + 26f
        }

        currentY = drawDebtsColumnsHeader(currentY)

        val rowHeight = 38f
        val namePaint = createPaint(fonts.bold, 11f, COLOR_TEXT_PRIMARY, Paint.Align.RIGHT)
        val greenCenterPaint = createPaint(fonts.bold, 10.5f, COLOR_GREEN, Paint.Align.CENTER)
        val redCenterPaint = createPaint(fonts.bold, 10.5f, COLOR_RED, Paint.Align.CENTER)
        val greenLeftPaint = createPaint(fonts.bold, 11f, COLOR_GREEN, Paint.Align.LEFT)
        val redLeftPaint = createPaint(fonts.bold, 11f, COLOR_RED, Paint.Align.LEFT)

        val sortedDebts = debts.sortedByDescending { it.remainingAmount }
        for (debt in sortedDebts) {
            if (currentY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM - 30f) {
                currentY = startNewPage()
                currentY = drawDebtsColumnsHeader(currentY)
            }

            val rowCenterY = currentY + rowHeight / 2f
            val isOwedToMe = debt.type == "LENT" || debt.type == "OWED_TO_ME"
            val typeText = if (isOwedToMe) "مستحق لي" else "عليّ"
            val centerPaint = if (isOwedToMe) greenCenterPaint else redCenterPaint
            val leftPaint = if (isOwedToMe) greenLeftPaint else redLeftPaint

            drawCenteredBaselineText(canvas, debt.personName, CONTENT_RIGHT, rowCenterY, namePaint)
            drawCenteredBaselineText(canvas, typeText, CONTENT_CENTER, rowCenterY, centerPaint)
            drawCenteredBaselineText(
                canvas = canvas,
                text = "\u200F${formatAmount(debt.remainingAmount)} $currencySymbol",
                x = CONTENT_LEFT,
                centerY = rowCenterY,
                paint = leftPaint
            )

            currentY += rowHeight
            drawThinDivider(canvas, currentY)
        }

        drawFooter(canvas, fonts, pageNumber)
        pdfDocument.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val fileName = "Nabih_Debts_Report_${System.currentTimeMillis()}.pdf"
        val file = File(reportsDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
            out.flush()
        }
        pdfDocument.close()

        return file
    }
}
