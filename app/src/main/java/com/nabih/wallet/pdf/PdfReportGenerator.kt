package com.nabih.wallet.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.nabih.wallet.R
import com.nabih.wallet.data.model.CategoryExpenseSummary
import com.nabih.wallet.ui.theme.CategoryColors
import com.nabih.wallet.util.Formatters
import java.io.OutputStream
import kotlin.math.ceil

data class PdfExpenseReportData(
    val periodLabel: String,
    val exportedAtMillis: Long,
    val totalExpense: Double,
    val categories: List<CategoryExpenseSummary>
)

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 44f
    private const val ROW_HEIGHT = 36f
    private const val TABLE_HEADER_HEIGHT = 36f
    private const val TOTAL_ROW_HEIGHT = 40f

    private const val COLOR_BRAND_BLUE = 0xFF0B57D0.toInt()
    private const val COLOR_EXPENSE_RED = 0xFFB3261E.toInt()
    private const val COLOR_TEXT_PRIMARY = 0xFF1A1C22.toInt()
    private const val COLOR_TEXT_SECONDARY = 0xFF5F6368.toInt()
    private const val COLOR_CARD_BG = 0xFFF3F6FC.toInt()
    private const val COLOR_DIVIDER = 0xFFE5EAF2.toInt()
    private const val COLOR_FOOTER_LINE = 0xFFBDC5D4.toInt()

    fun generateToStream(
        context: Context,
        report: PdfExpenseReportData,
        outputStream: OutputStream
    ): Int {
        val document = PdfDocument()
        try {
            val totalPages = renderPages(context, report) { pageNumber, width, height, draw ->
                val pageInfo = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
                val page = document.startPage(pageInfo)
                draw(page.canvas)
                document.finishPage(page)
            }
            document.writeTo(outputStream)
            return totalPages
        } finally {
            document.close()
        }
    }

    internal fun renderPages(
        context: Context,
        report: PdfExpenseReportData,
        onPage: (pageNumber: Int, width: Int, height: Int, draw: (Canvas) -> Unit) -> Unit
    ): Int {
        val regularTypeface = loadTypeface(context, Typeface.NORMAL)
        val boldTypeface = loadTypeface(context, Typeface.BOLD)
        val pageChunks = buildPageChunks(report.categories)
        val totalPages = pageChunks.size

        pageChunks.forEachIndexed { pageIndex, chunk ->
            val pageNumber = pageIndex + 1
            onPage(pageNumber, PAGE_WIDTH, PAGE_HEIGHT) { canvas ->
                canvas.drawColor(Color.WHITE)

                var cursorY = MARGIN_TOP
                if (pageIndex == 0) {
                    cursorY = drawHeader(context, canvas, report, regularTypeface, boldTypeface, cursorY)
                    cursorY += 18f
                } else {
                    cursorY = drawContinuationHeader(canvas, report, boldTypeface, cursorY)
                    cursorY += 14f
                }

                cursorY = drawTableHeader(canvas, boldTypeface, cursorY)

                if (chunk.isEmpty()) {
                    val emptyPaint = createTextPaint(regularTypeface, 12f, COLOR_TEXT_SECONDARY)
                    drawRtlText(
                        canvas = canvas,
                        text = "لا توجد مصروفات مسجلة في هذه الفترة",
                        paint = emptyPaint,
                        x = MARGIN_H,
                        y = cursorY + 18f,
                        width = (PAGE_WIDTH - MARGIN_H * 2).toInt(),
                        alignment = Layout.Alignment.ALIGN_CENTER
                    )
                } else {
                    chunk.forEachIndexed { rowIdx, item ->
                        drawCategoryRow(
                            canvas = canvas,
                            item = item,
                            regularTypeface = regularTypeface,
                            boldTypeface = boldTypeface,
                            topY = cursorY,
                            drawBottomDivider = rowIdx < chunk.lastIndex || pageIndex == pageChunks.lastIndex
                        )
                        cursorY += ROW_HEIGHT
                    }
                }

                if (pageIndex == pageChunks.lastIndex) {
                    drawTotalRow(canvas, report.totalExpense, boldTypeface, cursorY)
                }

                drawFooter(canvas, regularTypeface, pageNumber, totalPages)
            }
        }
        return totalPages
    }

    internal fun buildPageChunks(rows: List<CategoryExpenseSummary>): List<List<CategoryExpenseSummary>> {
        val firstPageRowsCapacity = 15
        val otherPageRowsCapacity = 18
        if (rows.isEmpty()) return listOf(emptyList())
        val pageChunks = mutableListOf<List<CategoryExpenseSummary>>()
        var offset = 0
        while (offset < rows.size) {
            val cap = if (pageChunks.isEmpty()) firstPageRowsCapacity else otherPageRowsCapacity
            val end = (offset + cap).coerceAtMost(rows.size)
            pageChunks.add(rows.subList(offset, end))
            offset = end
        }
        return pageChunks
    }

    private fun loadTypeface(context: Context, style: Int): Typeface {
        val base = runCatching {
            ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic)
        }.getOrNull() ?: Typeface.DEFAULT
        return Typeface.create(base, style)
    }

    private fun createTextPaint(typeface: Typeface, sizePt: Float, color: Int): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = sizePt
            this.color = color
        }
    }

    private fun drawRtlText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        x: Float,
        y: Float,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val safeWidth = width.coerceAtLeast(1)
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(alignment)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .setLineSpacing(2f, 1.0f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height
    }

    private fun drawHeader(
        context: Context,
        canvas: Canvas,
        report: PdfExpenseReportData,
        regularTypeface: Typeface,
        boldTypeface: Typeface,
        startY: Float
    ): Float {
        val contentRight = PAGE_WIDTH - MARGIN_H
        val logoSize = 44

        // Draw Nabih Wallet Logo on Top-Right
        val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_nabih_wallet_logo)
        if (logoDrawable != null) {
            val logoLeft = (contentRight - logoSize).toInt()
            val logoTop = startY.toInt() + 4
            logoDrawable.setBounds(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)
            logoDrawable.draw(canvas)
        }

        // Right-side title stack (next to logo)
        val titleRight = contentRight - logoSize - 12f
        val leftCardWidth = 176f
        val leftCardRight = MARGIN_H + leftCardWidth
        val titleAreaWidth = (titleRight - leftCardRight - 14f).toInt().coerceAtLeast(120)
        val titleAreaX = titleRight - titleAreaWidth

        val brandPaint = createTextPaint(boldTypeface, 11f, COLOR_BRAND_BLUE)
        val reportTitlePaint = createTextPaint(boldTypeface, 14f, COLOR_TEXT_PRIMARY)
        val datePaint = createTextPaint(regularTypeface, 9.5f, COLOR_TEXT_SECONDARY)

        var textY = startY
        textY += drawRtlText(canvas, "Nabih Wallet", brandPaint, titleAreaX, textY, titleAreaWidth) + 3f
        val reportTitle = "تقرير المصروفات — ${report.periodLabel}"
        textY += drawRtlText(canvas, reportTitle, reportTitlePaint, titleAreaX, textY, titleAreaWidth) + 4f
        val exportDateText = "تاريخ التصدير: ${Formatters.formatFullDate(report.exportedAtMillis)}"
        textY += drawRtlText(canvas, exportDateText, datePaint, titleAreaX, textY, titleAreaWidth)

        // Left-side total expenses card (#F3F6FC)
        val cardHeight = 58f
        val cardRect = RectF(MARGIN_H, startY, MARGIN_H + leftCardWidth, startY + cardHeight)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 14f, 14f, cardPaint)

        val labelPaint = createTextPaint(regularTypeface, 9.5f, COLOR_TEXT_SECONDARY)
        val totalAmountPaint = createTextPaint(boldTypeface, 14f, COLOR_EXPENSE_RED)

        drawRtlText(
            canvas = canvas,
            text = "إجمالي المصروفات",
            paint = labelPaint,
            x = MARGIN_H + 10f,
            y = startY + 10f,
            width = (leftCardWidth - 20f).toInt(),
            alignment = Layout.Alignment.ALIGN_CENTER
        )
        drawRtlText(
            canvas = canvas,
            text = Formatters.formatAmount(report.totalExpense),
            paint = totalAmountPaint,
            x = MARGIN_H + 10f,
            y = startY + 28f,
            width = (leftCardWidth - 20f).toInt(),
            alignment = Layout.Alignment.ALIGN_CENTER
        )

        val bottomY = maxOf(textY, startY + cardHeight) + 14f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_H, bottomY, contentRight, bottomY, dividerPaint)
        return bottomY
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        report: PdfExpenseReportData,
        boldTypeface: Typeface,
        startY: Float
    ): Float {
        val paint = createTextPaint(boldTypeface, 12f, COLOR_BRAND_BLUE)
        val h = drawRtlText(
            canvas = canvas,
            text = "Nabih Wallet — تابع تقرير المصروفات (${report.periodLabel})",
            paint = paint,
            x = MARGIN_H,
            y = startY,
            width = (PAGE_WIDTH - MARGIN_H * 2).toInt()
        )
        return startY + h + 8f
    }

    private fun drawTableHeader(
        canvas: Canvas,
        boldTypeface: Typeface,
        topY: Float
    ): Float {
        val contentRight = PAGE_WIDTH - MARGIN_H
        val headerRect = RectF(MARGIN_H, topY, contentRight, topY + TABLE_HEADER_HEIGHT)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 10f, 10f, bgPaint)

        val headerTextPaint = createTextPaint(boldTypeface, 11f, COLOR_TEXT_SECONDARY)
        val textY = topY + 10f

        // 3 columns in RTL: Right = الفئة, Center = النسبة, Left = المبلغ
        drawRtlText(
            canvas = canvas,
            text = "الفئة",
            paint = headerTextPaint,
            x = contentRight - 240f,
            y = textY,
            width = 226
        )
        drawRtlText(
            canvas = canvas,
            text = "النسبة",
            paint = headerTextPaint,
            x = MARGIN_H + 150f,
            y = textY,
            width = 90,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
        drawRtlText(
            canvas = canvas,
            text = "المبلغ",
            paint = headerTextPaint,
            x = MARGIN_H + 14f,
            y = textY,
            width = 136,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )

        return topY + TABLE_HEADER_HEIGHT
    }

    private fun drawCategoryRow(
        canvas: Canvas,
        item: CategoryExpenseSummary,
        regularTypeface: Typeface,
        boldTypeface: Typeface,
        topY: Float,
        drawBottomDivider: Boolean
    ) {
        val contentRight = PAGE_WIDTH - MARGIN_H
        val centerY = topY + ROW_HEIGHT / 2f

        // Colored dot on the right
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CategoryColors.pdfArgb(item.colorKey)
            style = Paint.Style.FILL
        }
        val dotRadius = 4.5f
        val dotCenterX = contentRight - 16f
        canvas.drawCircle(dotCenterX, centerY, dotRadius, dotPaint)

        val catPaint = createTextPaint(boldTypeface, 11f, COLOR_TEXT_PRIMARY)
        val pctPaint = createTextPaint(regularTypeface, 11f, COLOR_TEXT_SECONDARY)
        val amountPaint = createTextPaint(boldTypeface, 11f, COLOR_TEXT_PRIMARY)

        val textY = topY + 10f
        drawRtlText(
            canvas = canvas,
            text = item.categoryName,
            paint = catPaint,
            x = MARGIN_H + 245f,
            y = textY,
            width = (contentRight - 28f - (MARGIN_H + 245f)).toInt().coerceAtLeast(80)
        )
        drawRtlText(
            canvas = canvas,
            text = Formatters.formatPercent(item.percentage),
            paint = pctPaint,
            x = MARGIN_H + 150f,
            y = textY,
            width = 90,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
        drawRtlText(
            canvas = canvas,
            text = Formatters.formatAmount(item.totalAmount),
            paint = amountPaint,
            x = MARGIN_H + 14f,
            y = textY,
            width = 136,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )

        if (drawBottomDivider) {
            val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_DIVIDER
                strokeWidth = 0.8f
            }
            canvas.drawLine(MARGIN_H, topY + ROW_HEIGHT, contentRight, topY + ROW_HEIGHT, dividerPaint)
        }
    }

    private fun drawTotalRow(
        canvas: Canvas,
        totalExpense: Double,
        boldTypeface: Typeface,
        topY: Float
    ) {
        val contentRight = PAGE_WIDTH - MARGIN_H
        val bgRect = RectF(MARGIN_H, topY + 4f, contentRight, topY + TOTAL_ROW_HEIGHT)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(bgRect, 8f, 8f, bgPaint)

        val textY = topY + 14f
        val labelPaint = createTextPaint(boldTypeface, 12f, COLOR_TEXT_PRIMARY)
        val pctPaint = createTextPaint(boldTypeface, 12f, COLOR_BRAND_BLUE)
        val amountPaint = createTextPaint(boldTypeface, 12f, COLOR_TEXT_PRIMARY)

        drawRtlText(
            canvas = canvas,
            text = "الإجمالي",
            paint = labelPaint,
            x = contentRight - 240f,
            y = textY,
            width = 226
        )
        drawRtlText(
            canvas = canvas,
            text = "100%",
            paint = pctPaint,
            x = MARGIN_H + 150f,
            y = textY,
            width = 90,
            alignment = Layout.Alignment.ALIGN_CENTER
        )
        drawRtlText(
            canvas = canvas,
            text = Formatters.formatAmount(totalExpense),
            paint = amountPaint,
            x = MARGIN_H + 14f,
            y = textY,
            width = 136,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )

        // Bottom accent rule
        val accentRule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_FOOTER_LINE
            strokeWidth = 2.5f
        }
        canvas.drawLine(MARGIN_H, topY + TOTAL_ROW_HEIGHT + 4f, contentRight, topY + TOTAL_ROW_HEIGHT + 4f, accentRule)
    }

    private fun drawFooter(
        canvas: Canvas,
        regularTypeface: Typeface,
        pageNumber: Int,
        totalPages: Int
    ) {
        val contentRight = PAGE_WIDTH - MARGIN_H
        val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 8f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_DIVIDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_H, footerY - 8f, contentRight, footerY - 8f, linePaint)

        val footerPaint = createTextPaint(regularTypeface, 9f, COLOR_TEXT_SECONDARY)
        drawRtlText(
            canvas = canvas,
            text = "تم التوليد تلقائيًا بواسطة تطبيق Nabih Wallet",
            paint = footerPaint,
            x = MARGIN_H + 140f,
            y = footerY,
            width = (contentRight - (MARGIN_H + 140f)).toInt()
        )
        drawRtlText(
            canvas = canvas,
            text = "صفحة $pageNumber من $totalPages",
            paint = footerPaint,
            x = MARGIN_H,
            y = footerY,
            width = 130,
            alignment = Layout.Alignment.ALIGN_OPPOSITE
        )
    }
}
