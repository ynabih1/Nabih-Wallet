package com.example.data.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.CategoryItem
import com.example.model.FinancialConstants
import com.example.model.IconLibrary
import com.example.ui.components.MonthlyCalculationUtils
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
    val categoryIcons: Map<String, String>
)

data class BackupExportResult(
    val fileName: String,
    val localFile: File,
    val savedToPublicDownloads: Boolean
)

object BackupManager {

    /**
     * Consolidates duplicate transactions of the same type and category within the same month.
     */
    fun consolidateSameMonthTransactions(transactions: List<TransactionEntity>): List<TransactionEntity> {
        val grouped = transactions.groupBy { tx ->
            val normalizedCat = FinancialConstants.normalizeCategoryName(tx.category)
            val ym = MonthlyCalculationUtils.getYearMonth(tx.dateMillis)
            Triple(tx.type, normalizedCat, ym)
        }

        val consolidated = mutableListOf<TransactionEntity>()
        for ((key, group) in grouped) {
            val normalizedCat = key.second
            if (group.size == 1) {
                val single = group.first()
                consolidated.add(
                    single.copy(
                        category = normalizedCat,
                        endDateMillis = null
                    )
                )
            } else {
                val primary = group.maxByOrNull { it.dateMillis } ?: group.first()
                val totalAmount = group.sumOf { it.amount }
                val latestDate = group.maxOf { it.dateMillis }
                val mergedNotes = group.map { it.notes.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .joinToString(" | ")
                val anyPinned = group.any { it.isPinned }
                consolidated.add(
                    primary.copy(
                        category = normalizedCat,
                        amount = totalAmount,
                        dateMillis = latestDate,
                        endDateMillis = null,
                        notes = mergedNotes,
                        isPinned = anyPinned
                    )
                )
            }
        }
        return consolidated.sortedByDescending { it.dateMillis }
    }

    /**
     * Builds the standardized JSON string for backup package.
     */
    fun buildBackupJsonString(
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        debtPayments: List<DebtPaymentEntity>,
        customCategories: List<CategoryItem>,
        categoryIcons: Map<String, String>
    ): String {
        val rootJson = JSONObject()
        val now = System.currentTimeMillis()
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fullDateStr = fullDateFormat.format(Date(now))

        rootJson.put("app", "Nabih Wallet")
        rootJson.put("schemaVersion", 2)
        rootJson.put("exportDate", fullDateStr)
        rootJson.put("exportTimestamp", now)

        // Transactions
        val txArray = JSONArray()
        for (tx in transactions) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("type", tx.type)
                put("amount", tx.amount)
                put("category", FinancialConstants.normalizeCategoryName(tx.category))
                put("paymentMethod", tx.paymentMethod)
                put("dateMillis", tx.dateMillis)
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

        return rootJson.toString(2)
    }

    /**
     * Exports all user data to a formatted JSON file in Downloads folder AND internal backup storage.
     */
    fun exportBackup(
        context: Context,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        debtPayments: List<DebtPaymentEntity>,
        customCategories: List<CategoryItem>,
        categoryIcons: Map<String, String>
    ): Result<BackupExportResult> {
        return try {
            val appContext = context.applicationContext
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val dateStr = dateFormat.format(Date(now))
            val jsonString = buildBackupJsonString(
                transactions, debts, debtPayments, customCategories, categoryIcons
            )

            val fileName = "Nabih_Wallet_Backup_$dateStr.json"

            // 1. Always save a guaranteed local copy inside app storage (works on all Android versions with 0 permissions)
            val backupsDir = File(appContext.filesDir, "backups").apply { if (!exists()) mkdirs() }
            val latestInternalFile = File(backupsDir, "latest_backup.json")
            FileOutputStream(latestInternalFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val externalAppDownloads = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: backupsDir
            if (!externalAppDownloads.exists()) externalAppDownloads.mkdirs()
            val localBackupFile = File(externalAppDownloads, fileName)
            FileOutputStream(localBackupFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            // 2. Also save to public Downloads folder
            val savedToPublic = saveFileToDownloads(appContext, localBackupFile, fileName, "application/json")

            Result.success(
                BackupExportResult(
                    fileName = fileName,
                    localFile = localBackupFile,
                    savedToPublicDownloads = savedToPublic
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Writes backup JSON directly to a user-chosen document URI (from CreateDocument launcher).
     */
    fun exportBackupToUri(
        context: Context,
        targetUri: Uri,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        debtPayments: List<DebtPaymentEntity>,
        customCategories: List<CategoryItem>,
        categoryIcons: Map<String, String>
    ): Result<Unit> {
        return try {
            val appContext = context.applicationContext
            val jsonString = buildBackupJsonString(
                transactions, debts, debtPayments, customCategories, categoryIcons
            )
            appContext.contentResolver.openOutputStream(targetUri)?.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
            } ?: return Result.failure(Exception("تعذر فتح الملف للكتابة"))
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Shares the exported backup file via Android Share Sheet so user can send or save it anywhere.
     */
    fun shareBackupFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context.applicationContext,
                "${context.applicationContext.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Validates and parses the backup JSON file from a given content URI.
     */
    fun parseAndValidateBackup(context: Context, uri: Uri): Result<BackupPackage> {
        return try {
            val appContext = context.applicationContext
            val rawContent = appContext.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(Exception("تعذر قراءة محتوى الملف المختار"))

            parseBackupJsonString(rawContent)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("تعذر قراءة النسخة: ${e.localizedMessage ?: "تنسيق غير صالح"}"))
        }
    }

    /**
     * Parses and validates backup JSON content directly from a String.
     */
    fun parseBackupJsonString(rawContent: String): Result<BackupPackage> {
        return try {
            val content = rawContent.removePrefix("\uFEFF").trim()
            if (content.isBlank()) {
                return Result.failure(Exception("ملف النسخة الاحتياطية فارغ"))
            }

            // Handle root JSON Array or Object
            val root: JSONObject = if (content.startsWith("[")) {
                val array = JSONArray(content)
                JSONObject().apply {
                    put("transactions", array)
                }
            } else {
                JSONObject(content)
            }

            val hasTransactions = root.has("transactions")
            val hasDebts = root.has("debts")
            val hasAppSignature = root.optString("app", "").contains("Nabih", ignoreCase = true)
            val hasSchema = root.has("schemaVersion") || root.has("customCategories")

            if (!hasTransactions && !hasDebts && !hasAppSignature && !hasSchema) {
                return Result.failure(Exception("تنسيق الملف غير صالح ولا يطابق هيكل النسخ الاحتياطي"))
            }

            val exportDate = root.optString("exportDate", SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()))
            val exportTimestamp = root.optLong("exportTimestamp", System.currentTimeMillis())

            // Parse Transactions (single day only, no date ranges, and consolidate same-month duplicates)
            val rawTransactions = mutableListOf<TransactionEntity>()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    val rawCat = obj.optString("category", "")
                    rawTransactions.add(
                        TransactionEntity(
                            id = 0L, // Let Room auto-generate fresh IDs on restore to prevent primary key conflicts
                            type = obj.optString("type", "EXPENSE"),
                            amount = obj.optDouble("amount", 0.0),
                            category = FinancialConstants.normalizeCategoryName(rawCat),
                            paymentMethod = obj.optString("paymentMethod", ""),
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            endDateMillis = null,
                            notes = obj.optString("notes", ""),
                            isPinned = obj.optBoolean("isPinned", false),
                            receiptUri = null,
                            receiptMimeType = null
                        )
                    )
                }
            }
            val transactions = consolidateSameMonthTransactions(rawTransactions)

            // Parse Debts (ensure every debt has a unique positive ID so foreign keys from debt_payments never fail)
            val debts = mutableListOf<DebtEntity>()
            val debtArr = root.optJSONArray("debts")
            val usedDebtIds = mutableSetOf<Long>()
            var nextGeneratedDebtId = 1L
            if (debtArr != null) {
                for (i in 0 until debtArr.length()) {
                    val obj = debtArr.getJSONObject(i)
                    val dueDate = if (obj.has("dueDateMillis") && !obj.isNull("dueDateMillis")) {
                        obj.optLong("dueDateMillis")
                    } else null
                    var debtId = obj.optLong("id", 0L)
                    if (debtId <= 0L || debtId in usedDebtIds) {
                        while (nextGeneratedDebtId in usedDebtIds) {
                            nextGeneratedDebtId++
                        }
                        debtId = nextGeneratedDebtId
                    }
                    usedDebtIds.add(debtId)

                    debts.add(
                        DebtEntity(
                            id = debtId,
                            type = obj.optString("type", "OWED_TO_ME"),
                            personName = obj.optString("personName", "شخص"),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            lentDateMillis = obj.optLong("lentDateMillis", System.currentTimeMillis()),
                            dueDateMillis = dueDate,
                            notes = obj.optString("notes", ""),
                            enableReminder = false,
                            status = obj.optString("status", "UNPAID")
                        )
                    )
                }
            }

            // Parse Debt Payments (only include payments whose debtId exists in usedDebtIds to avoid ForeignKey crash)
            val payments = mutableListOf<DebtPaymentEntity>()
            val payArr = root.optJSONArray("debtPayments")
            if (payArr != null) {
                for (i in 0 until payArr.length()) {
                    val obj = payArr.getJSONObject(i)
                    val debtId = obj.optLong("debtId", 0L)
                    if (debtId in usedDebtIds) {
                        payments.add(
                            DebtPaymentEntity(
                                id = 0L, // Auto-generate payment ID cleanly
                                debtId = debtId,
                                amount = obj.optDouble("amount", 0.0),
                                paymentDateMillis = obj.optLong("paymentDateMillis", System.currentTimeMillis()),
                                note = obj.optString("note", "")
                            )
                        )
                    }
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

            val pkg = BackupPackage(
                exportDate = exportDate,
                exportTimestamp = exportTimestamp,
                transactions = transactions,
                debts = debts,
                debtPayments = payments,
                customCategories = customCats,
                categoryIcons = categoryIcons
            )
            Result.success(pkg)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("تعذر قراءة النسخة: ${e.localizedMessage ?: "تنسيق غير صالح"}"))
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
            val uri = try {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                null
            }

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
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                if (destFile.exists() && destFile.length() > 0) {
                    writeSucceeded = true
                    MediaScannerConnection.scanFile(
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
