package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Extension properties for backward/forward compatibility
val TransactionEntity.categoryName: String get() = category
val TransactionEntity.date: Long get() = dateMillis
val TransactionEntity.paymentMethodId: String get() = paymentMethod
val TransactionEntity.note: String? get() = notes

/**
 * موديل بسيط لسجل دين واحد (مستحق لي أو عليّ).
 */
data class DebtRecord(
    val personName: String,
    val type: String,          // "LENT" = مستحق لي, "BORROWED" = عليّ
    val originalAmount: Double,
    val remainingAmount: Double,
    val lastActivityDate: Long, // timestamp بالميلي ثانية
    val isOverdue: Boolean = false
)

fun DebtWithPayments.toDebtRecord(): DebtRecord {
    val isOwedToMe = debt.type == "OWED_TO_ME"
    val isOverdue = debt.dueDateMillis?.let { it < System.currentTimeMillis() && !isFullyPaid } ?: false
    return DebtRecord(
        personName = debt.personName,
        type = if (isOwedToMe) "LENT" else "BORROWED",
        originalAmount = debt.totalAmount,
        remainingAmount = remainingAmount,
        lastActivityDate = payments.maxOfOrNull { it.paymentDateMillis } ?: debt.lentDateMillis,
        isOverdue = isOverdue
    )
}

object ExportUtils {

    // ---------------------------------------------------------------
    // معالجة نصوص Bidi واللغة العربية والأرقام
    // ---------------------------------------------------------------
    private val bidiFormatter = BidiFormatter.getInstance(true) // strong RTL context

    /**
     * يرسم نصًا عربيًا مع فرض اتجاه RTL صحيح، محاذى لليمين عند rightX
     */
    private fun drawArabicTextRightAligned(canvas: android.graphics.Canvas, text: String, rightX: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.RTL)
        val width = paint.measureText(wrapped)
        canvas.drawText(wrapped, rightX - width, y, paint)
    }

    /**
     * يرسم نصًا عربيًا في المنتصف حول centerX
     */
    private fun drawArabicTextCentered(canvas: android.graphics.Canvas, text: String, centerX: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.RTL)
        val width = paint.measureText(wrapped)
        canvas.drawText(wrapped, centerX - (width / 2f), y, paint)
    }

    /**
     * يرسم نصًا رقميًا/إنجليزيًا مع فرض اتجاه LTR في المنتصف حول centerX
     */
    private fun drawLtrTextCentered(canvas: android.graphics.Canvas, text: String, centerX: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)
        val width = paint.measureText(wrapped)
        canvas.drawText(wrapped, centerX - (width / 2f), y, paint)
    }

    /**
     * Generates a styled PDF report for transactions and saves/shares it.
     */
    fun exportPdfReport(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String,
        periodName: String,
        startDateStr: String = "",
        endDateStr: String = "",
        share: Boolean = false
    ): File? {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد معاملات للتصدير", Toast.LENGTH_SHORT).show()
            return null
        }

        val arabicRegular = loadArabicTypeface(context, Typeface.NORMAL)
        val arabicBold = loadArabicTypeface(context, Typeface.BOLD)

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 40f
        val contentRightX = pageWidth - margin // 555f
        val contentLeftX = margin // 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // ---------------- ألوان التصميم: لوحة دافئة احترافية ----------------
        val primary = Color.parseColor("#B4622F")        // برتقالي محروق دافئ — رأس الجدول والعناوين
        val rowAlt = Color.parseColor("#FBF7F0")          // صف فاتح متبادل
        val totalRowBg = Color.parseColor("#F3E4D3")      // صف الإجمالي
        val borderColor = Color.parseColor("#E7DFD2")
        val textDark = Color.parseColor("#2B2620")
        val textMuted = Color.parseColor("#8A8171")

        val primaryFillPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        val rowAltPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val totalRowPaint = Paint().apply { color = totalRowBg; style = Paint.Style.FILL; isAntiAlias = true }
        val borderPaint = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.7f; isAntiAlias = true }

        val titlePaint = TextPaint().apply {
            color = textDark; textSize = 18.5f; typeface = arabicBold; isAntiAlias = true
        }
        val metaPaint = TextPaint().apply {
            color = textMuted; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true
        }
        val textPaint = TextPaint().apply {
            color = textDark; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true
        }
        val boldTextPaint = TextPaint().apply {
            color = textDark; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true
        }
        val headerTextPaint = TextPaint().apply {
            color = Color.WHITE; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true
        }
        val sectionHeaderPaint = TextPaint().apply {
            color = textDark; textSize = 13.5f; typeface = arabicBold; isAntiAlias = true
        }
        val footerPaint = TextPaint().apply {
            color = textMuted; textSize = 9f; typeface = arabicRegular; isAntiAlias = true
        }

        // Calculations
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        data class CategoryRow(val name: String, val amount: Double, val note: String)
        val expenseByCategory = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.categoryName }
            .map { (name, items) ->
                val distinctNotes = items.mapNotNull { it.note?.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                val noteText = if (distinctNotes.isEmpty()) "" else distinctNotes.joinToString("، ")
                CategoryRow(name, items.sumOf { it.amount }, noteText)
            }
            .sortedByDescending { it.amount }

        var currentY: Float

        // --- HEADER (محاذاة يمين كاملة لتقرير عربي احترافي) ---
        drawArabicTextRightAligned(canvas, "تقرير: بيان المصروفات الشخصية", contentRightX, margin + 16f, titlePaint)

        currentY = margin + 44f
        drawArabicTextRightAligned(canvas, "الفترة: $periodName", contentRightX, currentY, metaPaint)
        currentY += 19f
        drawArabicTextRightAligned(canvas, "تاريخ إصدار التقرير: ${SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())}", contentRightX, currentY, metaPaint)
        currentY += 19f
        drawArabicTextRightAligned(canvas, "عدد البنود: ${expenseByCategory.size}", contentRightX, currentY, metaPaint)

        currentY += 34f

        // --- أعمدة الجدول (RTL دقيق مع مسافات متناسقة) ---
        // عرض الجدول الكلي = 515f (من 40 إلى 555)
        // العمود 1 (م): 555 إلى 525 (عرض 30) -> المركز 540
        // العمود 2 (البيان): 525 إلى 365 (عرض 160) -> محاذاة يمين عند 517
        // العمود 3 (المبلغ): 365 إلى 265 (عرض 100) -> المركز 315
        // العمود 4 (النسبة): 265 إلى 205 (عرض 60) -> المركز 235
        // العمود 5 (ملاحظات): 205 إلى 40 (عرض 165) -> محاذاة يمين عند 197

        val col1Center = 540f
        val col2Right = 517f
        val col3Center = 315f
        val col4Center = 235f
        val col5Right = 197f

        val rowHeight = 32f
        val headerRowHeight = 30f

        val amountHeader = if (currencySymbol.isNotBlank()) "المبلغ ($currencySymbol)" else "المبلغ"

        fun drawTableHeaderRow(y: Float) {
            canvas.drawRect(contentLeftX, y, contentRightX, y + headerRowHeight, primaryFillPaint)
            val ty = y + headerRowHeight / 2f + 3.5f
            drawArabicTextCentered(canvas, "م", col1Center, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "البيان", col2Right, ty, headerTextPaint)
            drawArabicTextCentered(canvas, amountHeader, col3Center, ty, headerTextPaint)
            drawArabicTextCentered(canvas, "النسبة", col4Center, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "ملاحظات", col5Right, ty, headerTextPaint)
        }

        drawTableHeaderRow(currentY)
        currentY += headerRowHeight

        val maxRowY = pageHeight - 60f

        expenseByCategory.forEachIndexed { index, row ->
            if (currentY + rowHeight > maxRowY) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin + 10f
                drawTableHeaderRow(currentY)
                currentY += headerRowHeight
            }

            val bg = if (index % 2 == 1) rowAltPaint else null
            if (bg != null) {
                canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, bg)
            }
            canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, borderPaint)

            val percent = if (totalExpense > 0) (row.amount / totalExpense) * 100.0 else 0.0
            val ty = currentY + rowHeight / 2f + 3.5f

            // 1. م
            drawArabicTextCentered(canvas, "${index + 1}", col1Center, ty, textPaint)
            // 2. البيان
            val nameStr = TextUtils.ellipsize(row.name, boldTextPaint, 144f, TextUtils.TruncateAt.END).toString()
            drawArabicTextRightAligned(canvas, nameStr, col2Right, ty, boldTextPaint)
            // 3. المبلغ
            val amountStr = String.format(Locale.US, "%,.2f", row.amount)
            drawLtrTextCentered(canvas, amountStr, col3Center, ty, textPaint)
            // 4. النسبة
            val pctStr = String.format(Locale.US, "%.1f%%", percent)
            drawLtrTextCentered(canvas, pctStr, col4Center, ty, textPaint)
            // 5. ملاحظات
            val noteDisplay = row.note.ifBlank { "-" }
            val noteStr = TextUtils.ellipsize(noteDisplay, textPaint, 150f, TextUtils.TruncateAt.END).toString()
            drawArabicTextRightAligned(canvas, noteStr, col5Right, ty, textPaint)

            currentY += rowHeight
        }

        // صف الإجمالي
        canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, totalRowPaint)
        canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, borderPaint)
        val totalTy = currentY + rowHeight / 2f + 3.5f
        drawArabicTextRightAligned(canvas, "إجمالي المصروفات", col2Right, totalTy, boldTextPaint)
        val totalAmountFormatted = String.format(Locale.US, "%,.2f", totalExpense)
        drawLtrTextCentered(canvas, totalAmountFormatted, col3Center, totalTy, boldTextPaint)
        currentY += rowHeight + 26f

        // --- قسم الملخص ---
        if (currentY + 140f > maxRowY) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 10f
        }

        val dividerPaint = Paint().apply { color = borderColor; strokeWidth = 0.7f; isAntiAlias = true }
        canvas.drawLine(contentLeftX, currentY - 10f, contentRightX, currentY - 10f, dividerPaint)

        drawArabicTextRightAligned(canvas, "الملخص", contentRightX, currentY, sectionHeaderPaint)
        currentY += 24f

        val avgMonthly = if (totalExpense > 0) totalExpense / 12.0 else 0.0
        val topRow = expenseByCategory.firstOrNull()
        val bottomRow = expenseByCategory.lastOrNull()
        val currSuffix = if (currencySymbol.isNotBlank()) " $currencySymbol" else ""

        val summaryLines = mutableListOf(
            "إجمالي المصروفات خلال الفترة: ${String.format(Locale.US, "%,.2f", totalExpense)}$currSuffix",
            "متوسط الإنفاق الشهري: ${String.format(Locale.US, "%,.2f", avgMonthly)}$currSuffix / شهر"
        )
        if (totalIncome > 0) {
            summaryLines.add("إجمالي الدخل خلال الفترة: ${String.format(Locale.US, "%,.2f", totalIncome)}$currSuffix")
        }
        if (topRow != null) {
            val topPct = if (totalExpense > 0) (topRow.amount / totalExpense) * 100.0 else 0.0
            summaryLines.add("أعلى بند إنفاق: ${topRow.name} (${String.format(Locale.US, "%.1f", topPct)}% من الإجمالي)")
        }
        if (bottomRow != null && bottomRow != topRow) {
            val botPct = if (totalExpense > 0) (bottomRow.amount / totalExpense) * 100.0 else 0.0
            summaryLines.add("أقل بند إنفاق: ${bottomRow.name} (${String.format(Locale.US, "%.1f", botPct)}% من الإجمالي)")
        }

        val bulletPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        summaryLines.forEach { line ->
            // نقطة التعداد بجوار بداية السطر العربي من اليمين مباشرة
            canvas.drawCircle(contentRightX - 4f, currentY - 3.5f, 2.2f, bulletPaint)
            drawArabicTextRightAligned(canvas, line, contentRightX - 14f, currentY, textPaint)
            currentY += 20f
        }

        currentY += 16f
        drawArabicTextCentered(canvas, "تم إنشاء هذا التقرير بواسطة Nabih Wallet", pageWidth / 2f, currentY, footerPaint)
        drawLtrTextCentered(canvas, "Page $pageNumber", pageWidth / 2f, pageHeight - 24f, footerPaint)

        document.finishPage(page)

        val fileName = "Nabih_Wallet_Report_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        return try {
            document.writeTo(FileOutputStream(pdfFile))
            document.close()

            if (share) {
                shareFile(context, pdfFile, "application/pdf")
            } else {
                saveToDownloads(context, pdfFile, fileName, "application/pdf")
            }
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في حفظ ملف PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * تحميل خط عربي مخصص من res/font.
     */
    private fun loadArabicTypeface(context: Context, style: Int): Typeface {
        return try {
            val resId = context.resources.getIdentifier(
                if (style == Typeface.BOLD) "cairo_bold" else "cairo_regular",
                "font",
                context.packageName
            )
            if (resId != 0) {
                androidx.core.content.res.ResourcesCompat.getFont(context, resId) ?: Typeface.create(Typeface.DEFAULT, style)
            } else {
                Typeface.create(Typeface.DEFAULT, style)
            }
        } catch (e: Exception) {
            Typeface.create(Typeface.DEFAULT, style)
        }
    }

    /**
     * تصدير المعاملات إلى CSV
     */
    fun exportCsvReport(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String,
        share: Boolean = false
    ): File? {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد معاملات للتصدير", Toast.LENGTH_SHORT).show()
            return null
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csvHeader = "ID,Type,Category,Amount,Currency,Date,Payment Method,Note\n"

        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.append(csvHeader)

        for (t in transactions) {
            val noteEscaped = (t.note ?: "").replace("\"", "\"\"")
            val catEscaped = t.categoryName.replace("\"", "\"\"")
            val line = "${t.id},\"${t.type}\",\"$catEscaped\",${t.amount},\"$currencySymbol\",\"${sdf.format(Date(t.date))}\",\"${t.paymentMethodId}\",\"$noteEscaped\"\n"
            sb.append(line)
        }

        val fileName = "Nabih_Wallet_${System.currentTimeMillis()}.csv"
        val csvFile = File(context.cacheDir, fileName)

        return try {
            csvFile.writeText(sb.toString(), Charsets.UTF_8)
            if (share) {
                shareFile(context, csvFile, "text/csv")
            } else {
                saveToDownloads(context, csvFile, fileName, "text/csv")
            }
            csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل حفظ ملف CSV: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Generates a styled PDF report for debts (owed to me / I owe) and saves/shares it.
     */
    fun exportDebtsPdfReport(
        context: Context,
        debts: List<DebtRecord>,
        share: Boolean = false
    ): File? {
        if (debts.isEmpty()) {
            Toast.makeText(context, "لا توجد مديونيات للتصدير", Toast.LENGTH_SHORT).show()
            return null
        }

        val arabicRegular = loadArabicTypeface(context, Typeface.NORMAL)
        val arabicBold = loadArabicTypeface(context, Typeface.BOLD)

        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val contentRightX = pageWidth - margin // 555f
        val contentLeftX = margin // 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val primary = Color.parseColor("#B4622F")
        val rowAlt = Color.parseColor("#FBF7F0")
        val borderColor = Color.parseColor("#E7DFD2")
        val textDark = Color.parseColor("#2B2620")
        val textMuted = Color.parseColor("#8A8171")
        val greenPositive = Color.parseColor("#4E7A4E")
        val redNegative = Color.parseColor("#B4482F")

        val primaryFillPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        val rowAltPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val cardBgPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val borderPaint = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.7f; isAntiAlias = true }
        val dividerPaint = Paint().apply { color = borderColor; strokeWidth = 0.7f; isAntiAlias = true }

        val titlePaint = TextPaint().apply { color = textDark; textSize = 18.5f; typeface = arabicBold; isAntiAlias = true }
        val metaPaint = TextPaint().apply { color = textMuted; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true }
        val textPaint = TextPaint().apply { color = textDark; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true }
        val boldTextPaint = TextPaint().apply { color = textDark; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true }
        val headerTextPaint = TextPaint().apply { color = Color.WHITE; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true }
        val sectionHeaderPaint = TextPaint().apply { color = textDark; textSize = 13.5f; typeface = arabicBold; isAntiAlias = true }
        val footerPaint = TextPaint().apply { color = textMuted; textSize = 9f; typeface = arabicRegular; isAntiAlias = true }
        val cardLabelPaint = TextPaint().apply { color = textMuted; textSize = 9.5f; typeface = arabicRegular; isAntiAlias = true }
        val positiveAmountPaint = TextPaint().apply { color = greenPositive; textSize = 12.5f; typeface = arabicBold; isAntiAlias = true }
        val negativeAmountPaint = TextPaint().apply { color = redNegative; textSize = 12.5f; typeface = arabicBold; isAntiAlias = true }

        val totalOwedToMe = debts.filter { it.type == "LENT" }.sumOf { it.remainingAmount }
        val totalIOwe = debts.filter { it.type == "BORROWED" }.sumOf { it.remainingAmount }
        val net = totalOwedToMe - totalIOwe
        val overdueCount = debts.count { it.isOverdue }
        val sdf = SimpleDateFormat("d MMMM", Locale("ar"))

        var currentY: Float

        // --- HEADER (محاذاة يمين كاملة) ---
        drawArabicTextRightAligned(canvas, "تقرير: المديونيات", contentRightX, margin + 16f, titlePaint)

        currentY = margin + 44f
        drawArabicTextRightAligned(canvas, "تاريخ إصدار التقرير: ${SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())}", contentRightX, currentY, metaPaint)
        currentY += 19f
        drawArabicTextRightAligned(canvas, "عدد الأشخاص: ${debts.map { it.personName }.distinct().size}", contentRightX, currentY, metaPaint)

        currentY += 34f

        // --- بطاقتا الملخص العلوي ---
        val cardGap = 12f
        val cardWidth = (pageWidth - (margin * 2) - cardGap) / 2
        val cardHeight = 54f
        val cardRightX = contentRightX - cardWidth  // بطاقة "مستحق لي" على اليمين (RTL)
        val cardLeftX = contentLeftX               // بطاقة "عليّ" على الشمال

        // بطاقة مستحق لي (يمين)
        canvas.drawRoundRect(cardRightX, currentY, cardRightX + cardWidth, currentY + cardHeight, 8f, 8f, cardBgPaint)
        drawArabicTextRightAligned(canvas, "إجمالي المستحق لي", cardRightX + cardWidth - 12f, currentY + 20f, cardLabelPaint)
        drawLtrTextCentered(canvas, String.format(Locale.US, "%,.2f", totalOwedToMe), cardRightX + (cardWidth / 2f), currentY + 40f, positiveAmountPaint)

        // بطاقة إجمالي عليّ (شمال)
        canvas.drawRoundRect(cardLeftX, currentY, cardLeftX + cardWidth, currentY + cardHeight, 8f, 8f, cardBgPaint)
        drawArabicTextRightAligned(canvas, "إجمالي عليّ", cardLeftX + cardWidth - 12f, currentY + 20f, cardLabelPaint)
        drawLtrTextCentered(canvas, String.format(Locale.US, "%,.2f", totalIOwe), cardLeftX + (cardWidth / 2f), currentY + 40f, negativeAmountPaint)

        currentY += cardHeight + 28f

        // --- أعمدة جدول الديون (515f) ---
        // 1. الاسم: 555 إلى 365 (عرض 190) -> محاذاة يمين عند 547
        // 2. النوع: 365 إلى 275 (عرض 90) -> المركز 320
        // 3. المبلغ: 275 إلى 165 (عرض 110) -> المركز 220
        // 4. آخر عملية: 165 إلى 40 (عرض 125) -> المركز 102.5
        val colDebtNameRight = 547f
        val colDebtTypeCenter = 320f
        val colDebtAmountCenter = 220f
        val colDebtDateCenter = 102.5f

        val rowHeight = 32f
        val headerRowHeight = 30f

        fun drawTableHeaderRow(y: Float) {
            canvas.drawRect(contentLeftX, y, contentRightX, y + headerRowHeight, primaryFillPaint)
            val ty = y + headerRowHeight / 2f + 3.5f
            drawArabicTextRightAligned(canvas, "الاسم", colDebtNameRight, ty, headerTextPaint)
            drawArabicTextCentered(canvas, "النوع", colDebtTypeCenter, ty, headerTextPaint)
            drawArabicTextCentered(canvas, "المبلغ", colDebtAmountCenter, ty, headerTextPaint)
            drawArabicTextCentered(canvas, "آخر عملية", colDebtDateCenter, ty, headerTextPaint)
        }

        drawTableHeaderRow(currentY)
        currentY += headerRowHeight

        val maxRowY = pageHeight - 60f
        val sortedDebts = debts.sortedByDescending { it.remainingAmount }

        sortedDebts.forEachIndexed { index, debt ->
            if (currentY + rowHeight > maxRowY) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin + 10f
                drawTableHeaderRow(currentY)
                currentY += headerRowHeight
            }

            val bg = if (index % 2 == 1) rowAltPaint else null
            if (bg != null) canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, bg)
            canvas.drawRect(contentLeftX, currentY, contentRightX, currentY + rowHeight, borderPaint)

            val ty = currentY + rowHeight / 2f + 3.5f
            val typeLabel = if (debt.type == "LENT") "مستحق لي" else "عليّ"
            val typePaint = if (debt.type == "LENT") TextPaint(boldTextPaint).apply { color = greenPositive } else TextPaint(boldTextPaint).apply { color = redNegative }
            val nameStr = TextUtils.ellipsize(debt.personName, boldTextPaint, 175f, TextUtils.TruncateAt.END).toString()

            drawArabicTextRightAligned(canvas, nameStr, colDebtNameRight, ty, boldTextPaint)
            drawArabicTextCentered(canvas, typeLabel, colDebtTypeCenter, ty, typePaint)
            drawLtrTextCentered(canvas, String.format(Locale.US, "%,.2f", debt.remainingAmount), colDebtAmountCenter, ty, textPaint)
            drawArabicTextCentered(canvas, sdf.format(Date(debt.lastActivityDate)), colDebtDateCenter, ty, textPaint)

            currentY += rowHeight
        }

        currentY += 26f

        // --- قسم الملخص ---
        if (currentY + 100f > maxRowY) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 10f
        }

        canvas.drawLine(contentLeftX, currentY - 10f, contentRightX, currentY - 10f, dividerPaint)
        drawArabicTextRightAligned(canvas, "الملخص", contentRightX, currentY, sectionHeaderPaint)
        currentY += 24f

        val netLabel = if (net >= 0) "الصافي: ${String.format(Locale.US, "%,.2f", net)} لصالحك"
                       else "الصافي: ${String.format(Locale.US, "%,.2f", -net)} عليك"
        val summaryLines = listOf(
            netLabel,
            "عدد الديون المتأخرة: $overdueCount"
        )

        val bulletPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        summaryLines.forEach { line ->
            canvas.drawCircle(contentRightX - 4f, currentY - 3.5f, 2.2f, bulletPaint)
            drawArabicTextRightAligned(canvas, line, contentRightX - 14f, currentY, textPaint)
            currentY += 20f
        }

        currentY += 16f
        drawArabicTextCentered(canvas, "تم إنشاء هذا التقرير بواسطة Nabih Wallet", pageWidth / 2f, currentY, footerPaint)
        drawLtrTextCentered(canvas, "Page $pageNumber", pageWidth / 2f, pageHeight - 24f, footerPaint)

        document.finishPage(page)

        val fileName = "Nabih_Wallet_Debts_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        return try {
            document.writeTo(FileOutputStream(pdfFile))
            document.close()
            if (share) shareFile(context, pdfFile, "application/pdf") else saveToDownloads(context, pdfFile, fileName, "application/pdf")
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في حفظ ملف PDF: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun saveToDownloads(context: Context, sourceFile: File, fileName: String, mimeType: String) {
        var savedUri: Uri? = null
        var writeSucceeded = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                try {
                    val outputStream = resolver.openOutputStream(uri)
                    if (outputStream != null) {
                        outputStream.use { os ->
                            sourceFile.inputStream().use { inputStream ->
                                val bytesCopied = inputStream.copyTo(os)
                                os.flush()
                                writeSucceeded = bytesCopied > 0
                            }
                        }
                    }
                    if (writeSucceeded) {
                        val doneValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                        resolver.update(uri, doneValues, null, null)
                        savedUri = uri
                    } else {
                        resolver.delete(uri, null, null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                }
            }
        }

        // مسار احتياطي موثوق للأجهزة والأنظمة المختلفة
        if (!writeSucceeded) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                if (destFile.exists() && destFile.length() > 0) {
                    writeSucceeded = true
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf(mimeType),
                        null
                    )
                    savedUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        destFile
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (writeSucceeded) {
            Toast.makeText(context, "تم حفظ الملف بنجاح في مجلد Downloads\n$fileName", Toast.LENGTH_LONG).show()

            // فتح الملف تلقائياً لتمكين المستخدم من الاطلاع عليه فوراً
            try {
                val openUri = savedUri ?: FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    sourceFile
                )
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(openUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (e: Exception) {
                // إذا لم يتوفر تطبيق لعرض PDF، فالرسالة قد تم إظهارها
            }
        } else {
            Toast.makeText(context, "فشل حفظ الملف في مجلد Downloads", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "مشاركة التقرير عبر")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
