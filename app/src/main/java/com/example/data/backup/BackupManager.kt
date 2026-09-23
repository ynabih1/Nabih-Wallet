package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.CategoryItem
import com.example.model.IconLibrary
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupPackage(
    val exportDate: String,
    val exportTimestamp: Long,
    val transactions: List<TransactionEntity>,
    val debts: List<DebtEntity>,
    val debtPayments: List<DebtPaymentEntity>,
    val customCategories: List<CategoryItem>,
    val categoryIcons: Map<String, String>,
    val monthlyBudget: Double
)

object BackupManager {

    /**
     * Exports all user data to a formatted JSON file in Downloads folder.
     * Verified with bytesCopied > 0 before returning success.
     */
    fun exportBackup(
        context: Context,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        debtPayments: List<DebtPaymentEntity>,
        customCategories: List<CategoryItem>,
        categoryIcons: Map<String, String>,
        monthlyBudget: Double
    ): Result<String> {
        return try {
            val rootJson = JSONObject()
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val dateStr = dateFormat.format(Date(now))
            val fullDateStr = fullDateFormat.format(Date(now))

            rootJson.put("app", "Nabih Wallet")
            rootJson.put("schemaVersion", 1)
            rootJson.put("exportDate", fullDateStr)
            rootJson.put("exportTimestamp", now)

            // Transactions
            val txArray = JSONArray()
            for (tx in transactions) {
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("type", tx.type)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("paymentMethod", tx.paymentMethod)
                    put("dateMillis", tx.dateMillis)
                    if (tx.endDateMillis != null) put("endDateMillis", tx.endDateMillis)
                    put("notes", tx.notes)
                    put("isPinned", tx.isPinned)
                    put("receiptUri", tx.receiptUri ?: "")
                    put("receiptMimeType", tx.receiptMimeType ?: "")
                }
                txArray.put(obj)
            }
            rootJson.put("transactions", txArray)

            // Debts
            val debtArray = JSONArray()
            for (debt in debts) {
                val obj = JSONObject().apply {
                    put("id", debt.id)
                    put("type", debt.type)
                    put("personName", debt.personName)
                    put("totalAmount", debt.totalAmount)
                    put("lentDateMillis", debt.lentDateMillis)
                    if (debt.dueDateMillis != null) put("dueDateMillis", debt.dueDateMillis)
                    put("notes", debt.notes)
                    put("enableReminder", debt.enableReminder)
                    put("status", debt.status)
                }
                debtArray.put(obj)
            }
            rootJson.put("debts", debtArray)

            // Debt Payments
            val paymentArray = JSONArray()
            for (p in debtPayments) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("debtId", p.debtId)
                    put("amount", p.amount)
                    put("paymentDateMillis", p.paymentDateMillis)
                    put("note", p.note)
                }
                paymentArray.put(obj)
            }
            rootJson.put("debtPayments", paymentArray)

            // Custom categories
            val catArray = JSONArray()
            for (cat in customCategories) {
                val obj = JSONObject().apply {
                    put("id", cat.id)
                    put("nameAr", cat.nameAr)
                    put("nameEn", cat.nameEn)
                    put("iconKey", cat.iconKey)
                    put("color", cat.color)
                    put("type", cat.type)
                    put("descriptionAr", cat.descriptionAr)
                    put("descriptionEn", cat.descriptionEn)
                }
                catArray.put(obj)
            }
            rootJson.put("customCategories", catArray)

            // Category icons
            val iconsObj = JSONObject()
            for ((k, v) in categoryIcons) {
                iconsObj.put(k, v)
            }
            rootJson.put("categoryIcons", iconsObj)

            // Monthly budget
            rootJson.put("monthlyBudget", monthlyBudget)

            // File Name format: Nabih_Wallet_Backup_YYYY-MM-DD.json
            val fileName = "Nabih_Wallet_Backup_$dateStr.json"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { fos ->
                fos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val savedSuccessfully = saveFileToDownloads(context, tempFile, fileName, "application/json")
            if (savedSuccessfully) {
                Result.success(fileName)
            } else {
                Result.failure(Exception("فشل حفظ ملف النسخة الاحتياطية في التنزيلات"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Validates and parses the backup JSON file from a given content URI.
     */
    fun parseAndValidateBackup(context: Context, uri: Uri): Result<BackupPackage> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(Exception("تعذر قراءة محتوى الملف المختار"))

            if (content.isBlank()) {
                return Result.failure(Exception("ملف النسخة الاحتياطية فارغ"))
            }

            val root = JSONObject(content)

            // Validation: Must contain at least transactions or debts or app signature
            val hasTransactions = root.has("transactions")
            val hasDebts = root.has("debts")
            val hasAppSignature = root.optString("app", "").contains("Nabih", ignoreCase = true)

            if (!hasTransactions && !hasDebts && !hasAppSignature) {
                return Result.failure(Exception("تنسيق الملف غير صالح ولا يطابق هيكل النسخ الاحتياطي للتطبيق"))
            }

            val exportDate = root.optString("exportDate", SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()))
            val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            // Parse Transactions
            val transactions = mutableListOf<TransactionEntity>()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    transactions.add(
                        TransactionEntity(
                            id = obj.optLong("id", 0L),
                            type = obj.optString("type", "EXPENSE"),
                            amount = obj.optDouble("amount", 0.0),
                            category = obj.optString("category", ""),
                            paymentMethod = obj.optString("paymentMethod", ""),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            endDateMillis = if (obj.has("endDateMillis")) obj.optLong("endDateMillis") else null,
                            notes = obj.optString("notes", ""),
                            isPinned = obj.optBoolean("isPinned", false),
                            receiptUri = obj.optString("receiptUri", "").takeIf { it.isNotBlank() },
                            receiptMimeType = obj.optString("receiptMimeType", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }

            // Parse Debts
            val debts = mutableListOf<DebtEntity>()
            val debtArr = root.optJSONArray("debts")
            if (debtArr != null) {
                for (i in 0 until debtArr.length()) {
                    val obj = debtArr.getJSONObject(i)
                    val dueDate = if (obj.has("dueDateMillis") && !obj.isNull("dueDateMillis")) {
                        obj.optLong("dueDateMillis")
                    } else null
                    debts.add(
                        DebtEntity(
                            id = obj.optLong("id", 0L),
                            type = obj.optString("type", "OWED_TO_ME"),
                            personName = obj.optString("personName", "شخص"),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            lentDateMillis = obj.optLong("lentDateMillis", System.currentTimeMillis()),
                            dueDateMillis = dueDate,
                            notes = obj.optString("notes", ""),
                            enableReminder = obj.optBoolean("enableReminder", false),
                            status = obj.optString("status", "UNPAID")
                        )
                    )
                }
            }

            // Parse Debt Payments
            val payments = mutableListOf<DebtPaymentEntity>()
            val payArr = root.optJSONArray("debtPayments")
            if (payArr != null) {
                for (i in 0 until payArr.length()) {
                    val obj = payArr.getJSONObject(i)
                    payments.add(
                        DebtPaymentEntity(
                            id = obj.optLong("id", 0L),
                            debtId = obj.optLong("debtId", 0L),
                            amount = obj.optDouble("amount", 0.0),
                            paymentDateMillis = obj.optLong("paymentDateMillis", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            // Parse Custom Categories
            val customCats = mutableListOf<CategoryItem>()
            val catArr = root.optJSONArray("customCategories")
            if (catArr != null) {
                for (i in 0 until catArr.length()) {
                    val obj = catArr.getJSONObject(i)
                    val id = obj.optString("id", "custom_${System.currentTimeMillis()}_$i")
                    val iconKey = obj.optString("iconKey", "tag")
                    customCats.add(
                        CategoryItem(
                            id = id,
                            nameAr = obj.optString("nameAr", "فئة مخصصة"),
                            nameEn = obj.optString("nameEn", "Custom"),
                            icon = IconLibrary.getIconByKey(iconKey),
                            iconKey = iconKey,
                            color = obj.optLong("color", 0xFFD97757),
                            type = obj.optString("type", "EXPENSE"),
                            descriptionAr = obj.optString("descriptionAr", ""),
                            descriptionEn = obj.optString("descriptionEn", "")
                        )
                    )
                }
            }

            // Parse Category Icons
            val categoryIcons = mutableMapOf<String, String>()
            val iconsObj = root.optJSONObject("categoryIcons")
            if (iconsObj != null) {
                val keys = iconsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    categoryIcons[k] = iconsObj.getString(k)
                }
            }

            val monthlyBudget = root.optDouble("monthlyBudget", 0.0)

            val pkg = BackupPackage(
                exportDate = exportDate,
                exportTimestamp = exportTimestamp,
                transactions = transactions,
                debts = debts,
                debtPayments = payments,
                customCategories = customCats,
                categoryIcons = categoryIcons,
                monthlyBudget = monthlyBudget
            )
            Result.success(pkg)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("فشل قراءة وتفسير ملف النسخة الاحتياطية: ${e.localizedMessage ?: "تنسيق غير صالح"}"))
        }
    }

    private fun saveFileToDownloads(
        context: Context,
        sourceFile: File,
        fileName: String,
        mimeType: String
    ): Boolean {
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
                    } else {
                        resolver.delete(uri, null, null)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { resolver.delete(uri, null, null) } catch (_: Exception) {}
                }
            }
        }

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
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return writeSucceeded
    }
}
