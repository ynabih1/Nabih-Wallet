package com.nabih.wallet.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object FileSaver {

    suspend fun writeTextToUri(context: Context, uri: Uri, content: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                    out.flush()
                } ?: error("تعذر فتح الملف للكتابة")
            }
        }

    suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: error("تعذر فتح الملف للقراءة")
            }
        }
}
