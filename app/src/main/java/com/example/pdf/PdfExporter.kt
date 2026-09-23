package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object PdfExporter {

    private const val TAG = "PdfExporter"

    sealed class SaveResult {
        data class Success(val uri: Uri, val fileName: String, val bytes: Long) : SaveResult()
        data class Failure(val errorMessage: String) : SaveResult()
    }

    /**
     * Saves the PDF file directly to the system Downloads folder.
     * Uses MediaStore API with IS_PENDING on Android 10+ (Q+), with strict byte verification (> 0),
     * automatic cleanup of incomplete entries on failure, and comprehensive logging at every step.
     */
    fun saveToDownloads(context: Context, sourceFile: File, targetFileName: String): SaveResult {
        Log.d(TAG, "Starting saveToDownloads for file: $targetFileName, source: ${sourceFile.absolutePath}")

        if (!sourceFile.exists() || sourceFile.length() <= 0) {
            val errorMsg = "الملف المؤقت غير موجود أو فارغ: ${sourceFile.length()} بايت"
            Log.e(TAG, errorMsg)
            return SaveResult.Failure(errorMsg)
        }

        val resolver = context.contentResolver
        var targetUri: Uri? = null

        // 1. Primary Strategy: MediaStore API for Android 10+ (Build.VERSION_CODES.Q and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                // Try Downloads volume first, fallback to external Files table if unsupported by OEM
                val collectionUri = try {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not get VOLUME_EXTERNAL_PRIMARY downloads uri, trying EXTERNAL_CONTENT_URI: ${e.message}")
                    try {
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    } catch (e2: Exception) {
                        Log.w(TAG, "Fallback to MediaStore.Files collection: ${e2.message}")
                        MediaStore.Files.getContentUri("external")
                    }
                }

                Log.d(TAG, "Inserting record into MediaStore collection: $collectionUri")
                targetUri = resolver.insert(collectionUri, contentValues)

                if (targetUri == null) {
                    Log.e(TAG, "resolver.insert returned null for MediaStore collection")
                } else {
                    Log.d(TAG, "Record inserted successfully, targetUri: $targetUri. Opening OutputStream...")
                    var bytesCopied: Long = 0

                    val outputStream = resolver.openOutputStream(targetUri, "w")
                    if (outputStream == null) {
                        Log.e(TAG, "resolver.openOutputStream returned null for uri: $targetUri")
                        try {
                            resolver.delete(targetUri, null, null)
                        } catch (delEx: Exception) {
                            Log.e(TAG, "Failed to delete empty record after null stream", delEx)
                        }
                    } else {
                        outputStream.use { os ->
                            FileInputStream(sourceFile).use { inputStream ->
                                bytesCopied = inputStream.copyTo(os)
                                os.flush()
                            }
                        }

                        Log.d(TAG, "Stream write completed. Bytes copied: $bytesCopied")

                        if (bytesCopied <= 0) {
                            Log.e(TAG, "Zero bytes copied to MediaStore uri: $targetUri. Deleting incomplete record...")
                            try {
                                resolver.delete(targetUri, null, null)
                            } catch (delEx: Exception) {
                                Log.e(TAG, "Failed to delete zero-byte record", delEx)
                            }
                        } else {
                            // Clear IS_PENDING to 0 now that bytes are completely written
                            val finalizeValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.IS_PENDING, 0)
                            }
                            val updatedCount = resolver.update(targetUri, finalizeValues, null, null)
                            Log.d(TAG, "Cleared IS_PENDING to 0. Rows updated: $updatedCount. Success confirmed.")

                            return SaveResult.Success(targetUri, targetFileName, bytesCopied)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during MediaStore save process: ${e.message}", e)
                targetUri?.let { uri ->
                    try {
                        resolver.delete(uri, null, null)
                        Log.d(TAG, "Cleaned up incomplete MediaStore entry: $uri")
                    } catch (delEx: Exception) {
                        Log.e(TAG, "Failed to clean up MediaStore entry: ${delEx.message}")
                    }
                }
            }
        }

        // 2. Secondary Strategy: Direct Public Downloads Directory (Pre-Q or OEM MediaStore Fallback)
        Log.w(TAG, "Attempting direct copy to public Downloads directory...")
        try {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                val created = downloadsDir.mkdirs()
                Log.d(TAG, "Created public Downloads directory: $created")
            }

            val destFile = File(downloadsDir, targetFileName)
            var bytesCopied: Long = 0

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    bytesCopied = input.copyTo(output)
                    output.flush()
                }
            }

            Log.d(TAG, "Direct copy completed. Destination: ${destFile.absolutePath}, bytes: $bytesCopied")

            if (bytesCopied <= 0 || !destFile.exists() || destFile.length() <= 0) {
                if (destFile.exists()) {
                    destFile.delete()
                }
                val errorMsg = "فشل نسخ الملف إلى مجلد التنزيلات: عدد البايتات المنسوخة $bytesCopied"
                Log.e(TAG, errorMsg)
                return SaveResult.Failure(errorMsg)
            }

            // Trigger system media scan so the file appears immediately in the Files app / Downloads
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("application/pdf")
            ) { path, scannedUri ->
                Log.d(TAG, "MediaScanner scanned $path -> $scannedUri")
            }

            val uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )
            } catch (fpEx: Exception) {
                Log.w(TAG, "FileProvider URI resolution failed, using file URI: ${fpEx.message}")
                Uri.fromFile(destFile)
            }

            Log.d(TAG, "Direct fallback succeeded: $destFile ($bytesCopied bytes)")
            return SaveResult.Success(uri, targetFileName, bytesCopied)
        } catch (e: Exception) {
            val errorMsg = "فشل حفظ الملف في مجلد التنزيلات: ${e.localizedMessage ?: e.message}"
            Log.e(TAG, errorMsg, e)
            return SaveResult.Failure(errorMsg)
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
}
