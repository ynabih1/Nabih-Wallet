package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfExporter {

    sealed class SaveResult {
        data class Success(val uri: Uri, val fileName: String, val bytes: Long) : SaveResult()
        data class Failure(val errorMessage: String) : SaveResult()
    }

    /**
     * Saves the PDF file directly to the system Downloads folder using MediaStore API.
     * Uses IS_PENDING flag and strictly validates bytesCopied > 0 before confirming success.
     */
    fun saveToDownloads(context: Context, sourceFile: File, targetFileName: String): SaveResult {
        if (!sourceFile.exists() || sourceFile.length() <= 0) {
            return SaveResult.Failure("الملف الأصلي غير موجود أو فارغ")
        }

        val resolver = context.contentResolver
        var targetUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                targetUri = resolver.insert(collection, contentValues)
                    ?: return SaveResult.Failure("فشل إنشاء ملف جديد في مجلد التنزيلات")

                var bytesCopied: Long = 0
                resolver.openOutputStream(targetUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        bytesCopied = inputStream.copyTo(outputStream)
                    }
                } ?: return SaveResult.Failure("تعذر فتح مسار الكتابة للملف")

                if (bytesCopied <= 0) {
                    // Cleanup failed entry
                    resolver.delete(targetUri, null, null)
                    return SaveResult.Failure("لم يتم كتابة أي بيانات في الملف - الحجم 0 بايت")
                }

                // Confirm file is ready
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(targetUri, contentValues, null, null)

                return SaveResult.Success(targetUri, targetFileName, bytesCopied)
            } else {
                // Fallback for older Android (pre-Q)
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, targetFileName)
                var bytesCopied: Long = 0
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        bytesCopied = input.copyTo(output)
                    }
                }
                if (bytesCopied <= 0 || !destFile.exists()) {
                    if (destFile.exists()) destFile.delete()
                    return SaveResult.Failure("فشل نسخ الملف إلى مجلد التنزيلات")
                }
                val uri = Uri.fromFile(destFile)
                return SaveResult.Success(uri, targetFileName, bytesCopied)
            }
        } catch (e: Exception) {
            targetUri?.let { uri ->
                try {
                    resolver.delete(uri, null, null)
                } catch (_: Exception) {}
            }
            return SaveResult.Failure("حدث خطأ أثناء حفظ الملف: ${e.localizedMessage ?: e.message}")
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
            e.printStackTrace()
        }
    }
}
