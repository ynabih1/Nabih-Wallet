package com.example.data.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import com.example.pdf.PdfExporter
import com.example.pdf.PdfReportGenerator

// Extension properties for backward/forward compatibility
val TransactionEntity.categoryName: String get() = category
val TransactionEntity.date: Long get() = dateMillis
val TransactionEntity.paymentMethodId: String get() = paymentMethod
val TransactionEntity.note: String? get() = notes

/**
 * موديل لسجل دين واحد (مستحق لي أو عليّ).
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

    /**
     * Generates a styled PDF report for transactions and saves or shares it.
     * Delegates document rendering to PdfReportGenerator, and saving to
     * Downloads to PdfExporter (MediaStore API).
     */
    fun exportPdfReport(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String,
        periodName: String,
        startDateStr: String = "",
        endDateStr: String = "",
        share: Boolean = false
    ): PdfExporter.SaveResult {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد معاملات للتصدير", Toast.LENGTH_SHORT).show()
            return PdfExporter.SaveResult.Failure("لا توجد معاملات للتصدير")
        }

        return try {
            val pdfFile = PdfReportGenerator.generateExpenseReport(
                context = context,
                transactions = transactions,
                periodTitle = periodName,
                currencySymbol = currencySymbol,
                startDateStr = startDateStr,
                endDateStr = endDateStr
            )

            if (share) {
                PdfExporter.sharePdf(context, pdfFile, "تقرير المعاملات - محفظة نبيه")
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                } catch (e: Exception) {
                    Uri.fromFile(pdfFile)
                }
                PdfExporter.SaveResult.Success(uri, pdfFile.name, pdfFile.length())
            } else {
                PdfExporter.saveToDownloads(context, pdfFile, pdfFile.name)
            }
        } catch (e: Exception) {
            PdfExporter.SaveResult.Failure("فشل في إنشاء أو حفظ ملف PDF: ${e.message}")
        }
    }

    /**
     * Generates a styled PDF report for debts and saves or shares it.
     */
    fun exportDebtsPdfReport(
        context: Context,
        debts: List<DebtRecord>,
        currencySymbol: String = "جنيه",
        share: Boolean = false
    ): PdfExporter.SaveResult {
        if (debts.isEmpty()) {
            Toast.makeText(context, "لا توجد مديونيات للتصدير", Toast.LENGTH_SHORT).show()
            return PdfExporter.SaveResult.Failure("لا توجد مديونيات للتصدير")
        }

        return try {
            val pdfFile = PdfReportGenerator.generateDebtsReportFromRecords(
                context = context,
                debts = debts,
                currencySymbol = currencySymbol
            )

            if (share) {
                PdfExporter.sharePdf(context, pdfFile, "تقرير الديون - محفظة نبيه")
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                } catch (e: Exception) {
                    Uri.fromFile(pdfFile)
                }
                PdfExporter.SaveResult.Success(uri, pdfFile.name, pdfFile.length())
            } else {
                PdfExporter.saveToDownloads(context, pdfFile, pdfFile.name)
            }
        } catch (e: Exception) {
            PdfExporter.SaveResult.Failure("فشل في إنشاء أو حفظ ملف PDF: ${e.message}")
        }
    }

    /**
     * Overload to allow exporting directly from List<DebtWithPayments>.
     */
    @JvmName("exportDebtsWithPaymentsPdfReport")
    fun exportDebtsPdfReport(
        context: Context,
        debtsWithPayments: List<DebtWithPayments>,
        currencySymbol: String = "جنيه",
        share: Boolean = false
    ): PdfExporter.SaveResult {
        return exportDebtsPdfReport(
            context = context,
            debts = debtsWithPayments.map { it.toDebtRecord() },
            currencySymbol = currencySymbol,
            share = share
        )
    }
}
