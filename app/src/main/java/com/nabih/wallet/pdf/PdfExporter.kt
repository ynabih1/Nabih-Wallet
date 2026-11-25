package com.nabih.wallet.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    suspend fun exportToCacheFile(
        context: Context,
        report: PdfExpenseReportData,
        fileName: String
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
            val targetFile = File(reportsDir, safeName)
            FileOutputStream(targetFile).use { stream ->
                PdfReportGenerator.generateToStream(context, report, stream)
            }
            targetFile
        }
    }

    suspend fun exportToUri(
        context: Context,
        report: PdfExpenseReportData,
        uri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                PdfReportGenerator.generateToStream(context, report, stream)
            } ?: error("تعذر حفظ ملف PDF")
            Unit
        }
    }

    fun sharePdfFile(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تقرير مصروفات Nabih Wallet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة تقرير PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
