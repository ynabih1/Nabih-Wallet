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
 * موديل بسيط لسجل دين واحد (مستحق لي أو عليّ). عدّل الحقول دي لو عندك
 * DebtEntity فعلي في قاعدة بيانات Room بأسماء مختلفة.
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
    // إصلاح مشكلة النص العربي/الأرقام المقلوبة (Bidi)
    // ---------------------------------------------------------------
    // canvas.drawText() تطبّق خوارزمية Unicode Bidi تلقائيًا. لما النص
    // يبدأ برقم (LTR ضعيف) ويتبعه كلمة عربية (RTL قوي)، تتفسر الفقرة
    // ككل كـ RTL فتنعكس الأرقام. الحل: نغلّف كل جزء نص بعزل اتجاه
    // صريح عبر BidiFormatter قبل تمريره لـ drawText.

    private val bidiFormatter = BidiFormatter.getInstance(true) // strong RTL context

    /**
     * يرسم نصًا عربيًا (قد يحتوي أرقامًا) مع فرض اتجاه RTL صحيح.
     */
    private fun drawArabicText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.RTL)
        canvas.drawText(wrapped, x, y, paint)
    }

    /**
     * يرسم نصًا رقميًا/إنجليزيًا مع فرض اتجاه LTR صحيح (يمنع انعكاس الأرقام
     * حتى لو كان مجاورًا لنص عربي في نفس السطر).
     */
    private fun drawLtrText(canvas: android.graphics.Canvas, text: String, x: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.LTR)
        canvas.drawText(wrapped, x, y, paint)
    }

    /**
     * يرسم نصًا عربيًا محاذى لليمين (بحيث تنتهي حافته اليمنى عند x).
     */
    private fun drawArabicTextRightAligned(canvas: android.graphics.Canvas, text: String, rightX: Float, y: Float, paint: TextPaint) {
        val wrapped = bidiFormatter.unicodeWrap(text, TextDirectionHeuristicsCompat.RTL)
        val width = paint.measureText(wrapped)
        canvas.drawText(wrapped, rightX - width, y, paint)
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
            Toast.makeText(context, "لا توجد معاملات للتصدير / No transactions to export", Toast.LENGTH_SHORT).show()
            return null
        }

        // خط عربي مخصص (اختياري). لو أضفت ملفات Cairo-Regular.ttf و
        // Cairo-Bold.ttf داخل res/font/ سيتم استخدامها تلقائيًا، وإلا
        // سيتم الرجوع للخط الافتراضي بأمان.
        val arabicRegular = loadArabicTypeface(context, Typeface.NORMAL)
        val arabicBold = loadArabicTypeface(context, Typeface.BOLD)

        val document = PdfDocument()
        val pageWidth = 595 // A4 standard width in points
        val pageHeight = 842 // A4 standard height in points
        val margin = 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // ---------------- ألوان التصميم: لوحة دافئة احترافية ----------------
        val primary = Color.parseColor("#B4622F")        // برتقالي محروق دافئ — رأس الجدول والعناوين
        val primaryDeep = Color.parseColor("#6B4226")     // بني دافئ غامق للعنوان الفرعي
        val rowAlt = Color.parseColor("#FBF7F0")          // صف فاتح متبادل (كريمي هادئ)
        val rowWhite = Color.WHITE
        val totalRowBg = Color.parseColor("#F3E4D3")      // صف الإجمالي
        val borderColor = Color.parseColor("#E7DFD2")
        val textDark = Color.parseColor("#2B2620")
        val textMuted = Color.parseColor("#8A8171")

        val primaryFillPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        val rowAltPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val totalRowPaint = Paint().apply { color = totalRowBg; style = Paint.Style.FILL; isAntiAlias = true }
        val borderPaint = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.7f; isAntiAlias = true }

        val titlePaint = TextPaint().apply {
            color = textDark; textSize = 19f; typeface = arabicBold; isAntiAlias = true
        }
        val subtitlePaint = TextPaint().apply {
            color = primaryDeep; textSize = 12.5f; typeface = arabicBold; isAntiAlias = true
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
            color = textDark; textSize = 14f; typeface = arabicBold; isAntiAlias = true
        }
        val footerPaint = TextPaint().apply {
            color = textMuted; textSize = 9f; typeface = arabicRegular; isAntiAlias = true
        }

        // Calculations
        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        // تجميع المصروفات حسب الفئة (نفس منطق التقرير المرجعي: بيان + مبلغ + نسبة)
        // عمود الملاحظات يعرض الملاحظات الفعلية التي كتبها المستخدم (وليس عدد العمليات)
        data class CategoryRow(val name: String, val amount: Double, val note: String)
        val expenseByCategory = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.categoryName }
            .map { (name, items) ->
                // نجمع الملاحظات الفعلية غير الفارغة لكل عمليات هذه الفئة، بدون تكرار
                val distinctNotes = items.mapNotNull { it.note?.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                val noteText = if (distinctNotes.isEmpty()) "" else distinctNotes.joinToString("، ")
                CategoryRow(name, items.sumOf { it.amount }, noteText)
            }
            .sortedByDescending { it.amount }

        var currentY: Float

        // --- HEADER (بدون اسم التطبيق فوق — يظهر فقط في التذييل أسفل الصفحة) ---
        drawArabicText(canvas, "تقرير: بيان المصروفات الشخصية", margin, margin + 16f, titlePaint)

        currentY = margin + 46f
        drawArabicText(canvas, "الفترة: $periodName", margin, currentY, metaPaint)
        currentY += 19f
        drawArabicText(canvas, "تاريخ إصدار التقرير: ${SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())}", margin, currentY, metaPaint)
        currentY += 19f
        drawArabicText(canvas, "عدد البنود: ${expenseByCategory.size}", margin, currentY, metaPaint)

        currentY += 38f

        // --- TABLE: م | البيان | المبلغ (جنيه) | النسبة | ملاحظات ---
        // ترتيب الأعمدة من اليمين لليسار
        val colNumRightX = pageWidth - margin - 8f
        val colNameRightX = colNumRightX - 30f
        val colAmountRightX = colNameRightX - 190f
        val colPercentRightX = colAmountRightX - 90f
        val colNoteRightX = colPercentRightX - 55f

        val colNumLeftBound = colNumRightX - 30f
        val colNameLeftBound = colAmountRightX
        val colAmountLeftBound = colPercentRightX
        val colPercentLeftBound = colNoteRightX
        val colNoteLeftBound = margin

        val rowHeight = 32f
        val headerRowHeight = 30f

        fun drawTableHeaderRow(y: Float) {
            canvas.drawRect(margin, y, pageWidth - margin, y + headerRowHeight, primaryFillPaint)
            val ty = y + headerRowHeight / 2f + 3.5f
            drawArabicTextRightAligned(canvas, "م", colNumRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "البيان", colNameRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "المبلغ ($currencySymbol)", colAmountRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "النسبة", colPercentRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "ملاحظات", colNoteRightX, ty, headerTextPaint)
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
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, bg)
            }
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, borderPaint)

            val percent = if (totalExpense > 0) (row.amount / totalExpense) * 100.0 else 0.0
            val ty = currentY + rowHeight / 2f + 3.5f

            drawArabicTextRightAligned(canvas, "${index + 1}", colNumRightX, ty, textPaint)
            val nameStr = TextUtils.ellipsize(row.name, boldTextPaint, 175f, TextUtils.TruncateAt.END).toString()
            drawArabicTextRightAligned(canvas, nameStr, colNameRightX, ty, boldTextPaint)
            drawLtrText(canvas, String.format(Locale.US, "%,.2f", row.amount), colAmountRightX - 85f, ty, textPaint)
            drawLtrText(canvas, String.format(Locale.US, "%.1f%%", percent), colPercentRightX - 40f, ty, textPaint)
            val noteStr = TextUtils.ellipsize(row.note, textPaint, 90f, TextUtils.TruncateAt.END).toString()
            drawArabicTextRightAligned(canvas, noteStr, colNoteRightX, ty, textPaint)

            currentY += rowHeight
        }

        // صف الإجمالي
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, totalRowPaint)
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, borderPaint)
        val totalTy = currentY + rowHeight / 2f + 3.5f
        drawArabicTextRightAligned(canvas, "إجمالي المصروفات", colNameRightX, totalTy, boldTextPaint)
        drawLtrText(canvas, String.format(Locale.US, "%,.2f $currencySymbol", totalExpense), colAmountRightX - 100f, totalTy, boldTextPaint)
        currentY += rowHeight + 30f

        // --- قسم الملخص ---
        if (currentY + 140f > maxRowY) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 10f
        }

        // خط فاصل رفيع قبل قسم الملخص
        val dividerPaint = Paint().apply { color = borderColor; strokeWidth = 0.7f; isAntiAlias = true }
        canvas.drawLine(margin, currentY - 12f, pageWidth - margin, currentY - 12f, dividerPaint)

        drawArabicText(canvas, "الملخص", margin, currentY, sectionHeaderPaint)
        currentY += 26f

        val avgMonthly = if (totalExpense > 0) totalExpense / 12.0 else 0.0
        val topRow = expenseByCategory.firstOrNull()
        val bottomRow = expenseByCategory.lastOrNull()

        val summaryLines = mutableListOf(
            "إجمالي المصروفات خلال الفترة: ${String.format(Locale.US, "%,.2f", totalExpense)} $currencySymbol",
            "متوسط الإنفاق الشهري: ${String.format(Locale.US, "%,.2f", avgMonthly)} $currencySymbol / شهر"
        )
        if (totalIncome > 0) {
            summaryLines.add("إجمالي الدخل خلال الفترة: ${String.format(Locale.US, "%,.2f", totalIncome)} $currencySymbol")
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
            // نقطة تمييز صغيرة بلون التصميم بدل نص عادي بحت
            canvas.drawCircle(pageWidth - margin - 3f, currentY - 3.5f, 2.2f, bulletPaint)
            drawArabicText(canvas, line, margin, currentY, textPaint)
            currentY += 21f
        }

        currentY += 16f
        // اسم التطبيق يظهر فقط هنا في التذييل، وليس أعلى الصفحة
        drawArabicText(canvas, "تم إنشاء هذا التقرير بواسطة Nabih Wallet", margin, currentY, footerPaint)

        // --- تذييل رقم الصفحة ---
        drawLtrText(canvas, "Page $pageNumber", margin, pageHeight - 24f, footerPaint)

        document.finishPage(page)

        // Save PDF to Storage
        val fileName = "Nabih_Wallet_Report_${System.currentTimeMillis()}.pdf"
        val pdfFile = File(context.cacheDir, fileName)

        try {
            document.writeTo(FileOutputStream(pdfFile))
            document.close()

            if (share) {
                shareFile(context, pdfFile, "application/pdf")
            } else {
                saveToDownloads(context, pdfFile, fileName, "application/pdf")
            }
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل في حفظ ملف PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    /**
     * يحاول تحميل خط عربي مخصص من res/font (لو تم إضافته).
     * لو غير موجود، يرجع للخط الافتراضي بأمان دون أي كراش.
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
     * Exports transactions to CSV format and saves or shares.
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
        // BOM حتى يفتح إكسل الملف بترميز UTF-8 صحيح ويظهر العربي سليمًا
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

        try {
            csvFile.writeText(sb.toString(), Charsets.UTF_8)

            if (share) {
                shareFile(context, csvFile, "text/csv")
            } else {
                saveToDownloads(context, csvFile, fileName, "text/csv")
            }
            return csvFile
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "فشل حفظ ملف CSV: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }

    /**
     * Generates a styled PDF report for debts (owed to me / I owe) and saves/shares it.
     * نفس اللوحة الدافئة المستخدمة في تقرير المصروفات، من غير اسم التطبيق
     * فوق — يظهر فقط في التذييل أسفل الصفحة.
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

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // ---------------- نفس اللوحة الدافئة المستخدمة في تقرير المصروفات ----------------
        val primary = Color.parseColor("#B4622F")
        val rowAlt = Color.parseColor("#FBF7F0")
        val totalBg = Color.parseColor("#F3E4D3")
        val borderColor = Color.parseColor("#E7DFD2")
        val textDark = Color.parseColor("#2B2620")
        val textMuted = Color.parseColor("#8A8171")
        val greenPositive = Color.parseColor("#4E7A4E")
        val redNegative = Color.parseColor("#B4482F")

        val primaryFillPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        val rowAltPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val totalBgPaint = Paint().apply { color = totalBg; style = Paint.Style.FILL; isAntiAlias = true }
        val cardBgPaint = Paint().apply { color = rowAlt; style = Paint.Style.FILL; isAntiAlias = true }
        val borderPaint = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.7f; isAntiAlias = true }
        val dividerPaint = Paint().apply { color = borderColor; strokeWidth = 0.7f; isAntiAlias = true }

        val titlePaint = TextPaint().apply { color = textDark; textSize = 19f; typeface = arabicBold; isAntiAlias = true }
        val metaPaint = TextPaint().apply { color = textMuted; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true }
        val textPaint = TextPaint().apply { color = textDark; textSize = 10.5f; typeface = arabicRegular; isAntiAlias = true }
        val boldTextPaint = TextPaint().apply { color = textDark; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true }
        val headerTextPaint = TextPaint().apply { color = Color.WHITE; textSize = 10.5f; typeface = arabicBold; isAntiAlias = true }
        val sectionHeaderPaint = TextPaint().apply { color = textDark; textSize = 14f; typeface = arabicBold; isAntiAlias = true }
        val footerPaint = TextPaint().apply { color = textMuted; textSize = 9f; typeface = arabicRegular; isAntiAlias = true }
        val cardLabelPaint = TextPaint().apply { color = textMuted; textSize = 9.5f; typeface = arabicRegular; isAntiAlias = true }
        val positiveAmountPaint = TextPaint().apply { color = greenPositive; textSize = 13f; typeface = arabicBold; isAntiAlias = true }
        val negativeAmountPaint = TextPaint().apply { color = redNegative; textSize = 13f; typeface = arabicBold; isAntiAlias = true }

        val totalOwedToMe = debts.filter { it.type == "LENT" }.sumOf { it.remainingAmount }
        val totalIOwe = debts.filter { it.type == "BORROWED" }.sumOf { it.remainingAmount }
        val net = totalOwedToMe - totalIOwe
        val overdueCount = debts.count { it.isOverdue }
        val sdf = SimpleDateFormat("d MMMM", Locale("ar"))

        var currentY: Float

        // --- HEADER (بدون اسم التطبيق فوق) ---
        drawArabicText(canvas, "تقرير: المديونيات", margin, margin + 16f, titlePaint)

        currentY = margin + 46f
        drawArabicText(canvas, "تاريخ إصدار التقرير: ${SimpleDateFormat("d MMMM yyyy", Locale("ar")).format(Date())}", margin, currentY, metaPaint)
        currentY += 19f
        drawArabicText(canvas, "عدد الأشخاص: ${debts.map { it.personName }.distinct().size}", margin, currentY, metaPaint)

        currentY += 34f

        // --- بطاقتا الملخص العلوي ---
        val cardGap = 12f
        val cardWidth = (pageWidth - (margin * 2) - cardGap) / 2
        val cardHeight = 54f
        val cardRightX = pageWidth - margin - cardWidth  // بطاقة "مستحق لي" على اليمين (RTL)
        val cardLeftX = margin                            // بطاقة "عليّ" على الشمال

        canvas.drawRoundRect(cardRightX, currentY, cardRightX + cardWidth, currentY + cardHeight, 8f, 8f, cardBgPaint)
        drawArabicTextRightAligned(canvas, "إجمالي المستحق لي", cardRightX + cardWidth - 12f, currentY + 22f, cardLabelPaint)
        drawLtrText(canvas, String.format(Locale.US, "%,.2f جنيه", totalOwedToMe), cardRightX + 12f, currentY + 42f, positiveAmountPaint)

        canvas.drawRoundRect(cardLeftX, currentY, cardLeftX + cardWidth, currentY + cardHeight, 8f, 8f, cardBgPaint)
        drawArabicTextRightAligned(canvas, "إجمالي عليّ", cardLeftX + cardWidth - 12f, currentY + 22f, cardLabelPaint)
        drawLtrText(canvas, String.format(Locale.US, "%,.2f جنيه", totalIOwe), cardLeftX + 12f, currentY + 42f, negativeAmountPaint)

        currentY += cardHeight + 30f

        // --- TABLE: الاسم | النوع | المبلغ | آخر عملية ---
        val colNameRightX = pageWidth - margin - 8f
        val colTypeRightX = colNameRightX - 130f
        val colAmountRightX = colTypeRightX - 70f
        val colDateRightX = colAmountRightX - 90f

        val rowHeight = 32f
        val headerRowHeight = 30f

        fun drawTableHeaderRow(y: Float) {
            canvas.drawRect(margin, y, pageWidth - margin, y + headerRowHeight, primaryFillPaint)
            val ty = y + headerRowHeight / 2f + 3.5f
            drawArabicTextRightAligned(canvas, "الاسم", colNameRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "النوع", colTypeRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "المبلغ", colAmountRightX, ty, headerTextPaint)
            drawArabicTextRightAligned(canvas, "آخر عملية", colDateRightX, ty, headerTextPaint)
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
            if (bg != null) canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, bg)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, borderPaint)

            val ty = currentY + rowHeight / 2f + 3.5f
            val typeLabel = if (debt.type == "LENT") "مستحق لي" else "عليّ"
            val typePaint = if (debt.type == "LENT") TextPaint(boldTextPaint).apply { color = greenPositive } else TextPaint(boldTextPaint).apply { color = redNegative }
            val nameStr = TextUtils.ellipsize(debt.personName, boldTextPaint, 110f, TextUtils.TruncateAt.END).toString()

            drawArabicTextRightAligned(canvas, nameStr, colNameRightX, ty, boldTextPaint)
            drawArabicTextRightAligned(canvas, typeLabel, colTypeRightX, ty, typePaint)
            drawLtrText(canvas, String.format(Locale.US, "%,.2f", debt.remainingAmount), colAmountRightX - 65f, ty, textPaint)
            drawArabicTextRightAligned(canvas, sdf.format(Date(debt.lastActivityDate)), colDateRightX, ty, textPaint)

            currentY += rowHeight
        }

        currentY += 30f

        // --- قسم الملخص ---
        if (currentY + 100f > maxRowY) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 10f
        }

        canvas.drawLine(margin, currentY - 12f, pageWidth - margin, currentY - 12f, dividerPaint)
        drawArabicText(canvas, "الملخص", margin, currentY, sectionHeaderPaint)
        currentY += 26f

        val netLabel = if (net >= 0) "الصافي: ${String.format(Locale.US, "%,.2f", net)} جنيه لصالحك"
                        else "الصافي: ${String.format(Locale.US, "%,.2f", -net)} جنيه عليك"
        val summaryLines = listOf(
            netLabel,
            "عدد الديون المتأخرة: $overdueCount"
        )

        val bulletPaint = Paint().apply { color = primary; style = Paint.Style.FILL; isAntiAlias = true }
        summaryLines.forEach { line ->
            canvas.drawCircle(pageWidth - margin - 3f, currentY - 3.5f, 2.2f, bulletPaint)
            drawArabicText(canvas, line, margin, currentY, textPaint)
            currentY += 21f
        }

        currentY += 16f
        drawArabicText(canvas, "تم إنشاء هذا التقرير بواسطة Nabih Wallet", margin, currentY, footerPaint)
        drawLtrText(canvas, "Page $pageNumber", margin, pageHeight - 24f, footerPaint)

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (uri == null) {
                Toast.makeText(context, "فشل إنشاء الملف في مجلد Downloads (تحقق من الصلاحيات)", Toast.LENGTH_LONG).show()
                return
            }

            var writeSucceeded = false
            try {
                val outputStream = resolver.openOutputStream(uri)
                if (outputStream == null) {
                    Toast.makeText(context, "فشل فتح الملف للكتابة (openOutputStream = null)", Toast.LENGTH_LONG).show()
                    resolver.delete(uri, null, null)
                    return
                }
                outputStream.use { os ->
                    sourceFile.inputStream().use { inputStream ->
                        val bytesCopied = inputStream.copyTo(os)
                        os.flush()
                        writeSucceeded = bytesCopied > 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "فشل حفظ الملف: ${e.message}", Toast.LENGTH_LONG).show()
                resolver.delete(uri, null, null)
                return
            }

            if (writeSucceeded) {
                val doneValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(uri, doneValues, null, null)
                Toast.makeText(context, "تم حفظ الملف بنجاح في مجلد Downloads\n$fileName", Toast.LENGTH_LONG).show()
            } else {
                resolver.delete(uri, null, null)
                Toast.makeText(context, "فشل حفظ الملف: لم يتم نسخ أي بيانات", Toast.LENGTH_LONG).show()
            }
        } else {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                if (destFile.exists() && destFile.length() > 0) {
                    Toast.makeText(context, "تم حفظ الملف بنجاح: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل حفظ الملف في: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "فشل حفظ الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
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
        val chooser = Intent.createChooser(intent, "مشاركة التقرير عبر / Share Report via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
