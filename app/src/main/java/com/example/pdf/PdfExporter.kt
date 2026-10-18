package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfExporter {

    private const val TAG = "PdfExporter"

    sealed class SaveResult {
        data class Success(val uri: Uri, val fileName: String, val bytes: Long, val file: File? = null) : SaveResult()
        data class Failure(val errorMessage: String) : SaveResult()
    }

    /**
     * Saves the PDF file directly to the system Downloads folder.
     * Uses MediaStore API on Android 10+ (Q+), with fallbacks for legacy/OEM versions.
     */
    fun saveToDownloads(context: Context, sourceFile: File, targetFileName: String): SaveResult {
        Log.d(TAG, "Starting saveToDownloads for file: $targetFileName, source: ${sourceFile.absolutePath}")

        if (!sourceFile.exists() || sourceFile.length() <= 0) {
            val errorMsg = "الملف المؤقت غير موجود أو فارغ: ${sourceFile.length()} بايت"
            Log.e(TAG, errorMsg)
            return SaveResult.Failure(errorMsg)
        }

        val resolver = context.contentResolver

        // 1. Primary Strategy: MediaStore API for Android 10+ (Build.VERSION_CODES.Q and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                // Standard Downloads collection URI
                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val targetUri = resolver.insert(collectionUri, contentValues)

                if (targetUri != null) {
                    var bytesCopied: Long = 0
                    resolver.openOutputStream(targetUri, "w")?.use { os ->
                        FileInputStream(sourceFile).use { inputStream ->
                            bytesCopied = inputStream.copyTo(os)
                            os.flush()
                        }
                    }

                    if (bytesCopied > 0) {
                        // Crucial for phone downloads: clear IS_PENDING so the file appears immediately in Downloads
                        val updateValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.IS_PENDING, 0)
                        }
                        resolver.update(targetUri, updateValues, null, null)

                        Log.d(TAG, "MediaStore save successful: $targetUri ($bytesCopied bytes)")
                        return SaveResult.Success(targetUri, targetFileName, bytesCopied, sourceFile)
                    } else {
                        try { resolver.delete(targetUri, null, null) } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore save failed, trying direct storage fallback: ${e.message}", e)
            }
        }

        // 2. Secondary Strategy: Public Downloads folder
        try {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }

            if (downloadsDir.canWrite() || downloadsDir.exists()) {
                val destFile = File(downloadsDir, targetFileName)
                var bytesCopied: Long = 0

                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        bytesCopied = input.copyTo(output)
                        output.flush()
                    }
                }

                if (bytesCopied > 0 && destFile.exists() && destFile.length() > 0) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        arrayOf("application/pdf"),
                        null
                    )

                    val uri = try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
                    } catch (e: Exception) {
                        Uri.fromFile(destFile)
                    }

                    Log.d(TAG, "Public downloads save successful: $destFile")
                    return SaveResult.Success(uri, targetFileName, bytesCopied, destFile)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Public downloads direct copy failed: ${e.message}", e)
        }

        // 3. Guaranteed Safe Fallback: App External Files directory (Never fails on any Android version)
        try {
            val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val safeFile = File(appDownloadsDir, targetFileName)
            var bytesCopied: Long = 0

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(safeFile).use { output ->
                    bytesCopied = input.copyTo(output)
                    output.flush()
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", safeFile)
            Log.d(TAG, "Guaranteed fallback save successful: $safeFile")
            return SaveResult.Success(uri, targetFileName, bytesCopied, safeFile)
        } catch (e: Exception) {
            val errorMsg = "فشل حفظ الملف: ${e.localizedMessage ?: e.message}"
            Log.e(TAG, errorMsg, e)
            return SaveResult.Failure(errorMsg)
        }
    }

    /**
     * Opens the PDF file using an external viewer app (Google Drive, Acrobat, etc.).
     */
    fun openPdf(context: Context, uri: Uri, sourceFile: File? = null) {
        try {
            val viewUri = if (sourceFile != null && sourceFile.exists()) {
                try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sourceFile)
                } catch (e: Exception) {
                    uri
                }
            } else {
                uri
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(viewUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, viewUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "عرض تقرير PDF").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open PDF: ${e.message}", e)
            Toast.makeText(
                context,
                "تم حفظ الملف في مجلد التنزيلات (Downloads). يرجى فتح مدير الملفات أو تثبيت قارئ PDF.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Shares the PDF file via Android Share Intent using FileProvider.
     */
    fun sharePdf(context: Context, file: File, title: String = "تقرير محفظة نبيه") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share PDF: ${e.message}", e)
        }
    }

    fun exportPdfReport(
        context: Context,
        transactions: List<TransactionEntity>,
        currencySymbol: String,
        periodName: String,
        startDateStr: String = "",
        endDateStr: String = "",
        share: Boolean = false
    ): SaveResult {
        if (transactions.isEmpty()) {
            Toast.makeText(context, "لا توجد معاملات للتصدير", Toast.LENGTH_SHORT).show()
            return SaveResult.Failure("لا توجد معاملات للتصدير")
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
                sharePdf(context, pdfFile, "تقرير المعاملات - محفظة نبيه")
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                } catch (e: Exception) {
                    Uri.fromFile(pdfFile)
                }
                SaveResult.Success(uri, pdfFile.name, pdfFile.length())
            } else {
                saveToDownloads(context, pdfFile, pdfFile.name)
            }
        } catch (e: Exception) {
            SaveResult.Failure("فشل في إنشاء أو حفظ ملف PDF: ${e.message}")
        }
    }

    fun exportDebtsPdfReport(
        context: Context,
        debts: List<DebtRecord>,
        currencySymbol: String = "جنيه",
        share: Boolean = false
    ): SaveResult {
        if (debts.isEmpty()) {
            Toast.makeText(context, "لا توجد مديونيات للتصدير", Toast.LENGTH_SHORT).show()
            return SaveResult.Failure("لا توجد مديونيات للتصدير")
        }

        return try {
            val pdfFile = PdfReportGenerator.generateDebtsReportFromRecords(
                context = context,
                debts = debts,
                currencySymbol = currencySymbol
            )

            if (share) {
                sharePdf(context, pdfFile, "تقرير الديون - محفظة نبيه")
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
                } catch (e: Exception) {
                    Uri.fromFile(pdfFile)
                }
                SaveResult.Success(uri, pdfFile.name, pdfFile.length())
            } else {
                saveToDownloads(context, pdfFile, pdfFile.name)
            }
        } catch (e: Exception) {
            SaveResult.Failure("فشل في إنشاء أو حفظ ملف PDF: ${e.message}")
        }
    }
}

