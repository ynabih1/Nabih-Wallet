package com.nabih.wallet.data.backup

import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.DebtEntity
import com.nabih.wallet.data.local.entity.DebtPaymentEntity
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.local.entity.TransactionEntity
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.repository.WalletRepository
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val debts: List<DebtEntity>,
    val debtPayments: List<DebtPaymentEntity>
)

sealed class BackupRestoreResult {
    data class Success(
        val categoriesCount: Int,
        val transactionsCount: Int,
        val debtsCount: Int,
        val paymentsCount: Int
    ) : BackupRestoreResult()

    data class Error(val message: String) : BackupRestoreResult()
}

class BackupManager(private val repository: WalletRepository) {

    suspend fun createBackupJson(): String {
        val payload = BackupPayload(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            exportedAt = System.currentTimeMillis(),
            categories = repository.getAllCategoriesSnapshot(),
            transactions = repository.getAllTransactionsSnapshot(),
            debts = repository.getAllDebtsSnapshot(),
            debtPayments = repository.getAllDebtPaymentsSnapshot()
        )
        return serialize(payload)
    }

    suspend fun restoreFromJson(rawJson: String): BackupRestoreResult {
        return try {
            val payload = parseAndValidate(rawJson)
            repository.restoreSnapshotAtomically(
                categories = payload.categories,
                transactions = payload.transactions,
                debts = payload.debts,
                debtPayments = payload.debtPayments
            )
            BackupRestoreResult.Success(
                categoriesCount = payload.categories.size,
                transactionsCount = payload.transactions.size,
                debtsCount = payload.debts.size,
                paymentsCount = payload.debtPayments.size
            )
        } catch (e: IllegalArgumentException) {
            BackupRestoreResult.Error(e.message ?: "ملف النسخة الاحتياطية غير صالح")
        } catch (e: Exception) {
            BackupRestoreResult.Error("تعذر قراءة ملف النسخة الاحتياطية")
        }
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun serialize(payload: BackupPayload): String {
            val root = JSONObject()
            root.put("app", "Nabih Wallet")
            root.put("schemaVersion", payload.schemaVersion)
            root.put("exportedAt", payload.exportedAt)

            val categoriesArr = JSONArray()
            payload.categories.forEach { c ->
                categoriesArr.put(
                    JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("type", c.type)
                        put("iconKey", c.iconKey)
                        put("colorKey", c.colorKey)
                        put("isDefault", c.isDefault)
                        put("sortOrder", c.sortOrder)
                    }
                )
            }
            root.put("categories", categoriesArr)

            val txArr = JSONArray()
            payload.transactions.forEach { t ->
                txArr.put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("type", t.type)
                        put("amount", t.amount)
                        put("categoryId", t.categoryId)
                        put("categoryNameSnapshot", t.categoryNameSnapshot)
                        put("categoryIconKeySnapshot", t.categoryIconKeySnapshot)
                        put("categoryColorKeySnapshot", t.categoryColorKeySnapshot)
                        put("timestamp", t.timestamp)
                        put("note", t.note)
                    }
                )
            }
            root.put("transactions", txArr)

            val debtsArr = JSONArray()
            payload.debts.forEach { d ->
                debtsArr.put(
                    JSONObject().apply {
                        put("id", d.id)
                        put("personName", d.personName)
                        put("type", d.type)
                        put("originalAmount", d.originalAmount)
                        put("createdAt", d.createdAt)
                    }
                )
            }
            root.put("debts", debtsArr)

            val paymentsArr = JSONArray()
            payload.debtPayments.forEach { p ->
                paymentsArr.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("debtId", p.debtId)
                        put("amount", p.amount)
                        put("paidAt", p.paidAt)
                    }
                )
            }
            root.put("debtPayments", paymentsArr)

            return root.toString(2)
        }

        fun parseAndValidate(rawJson: String): BackupPayload {
            if (rawJson.isBlank()) {
                throw IllegalArgumentException("ملف النسخة الاحتياطية فارغ")
            }
            val root = JSONObject(rawJson)
            if (!root.has("schemaVersion")) {
                throw IllegalArgumentException("ملف غير صالح: رقم الإصدار مفقود")
            }
            val schemaVersion = root.getInt("schemaVersion")
            if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
                throw IllegalArgumentException("إصدار النسخة الاحتياطية غير مدعوم ($schemaVersion)")
            }

            if (!root.has("categories") || !root.has("transactions") || !root.has("debts") || !root.has("debtPayments")) {
                throw IllegalArgumentException("هيكل ملف النسخة الاحتياطية غير مكتمل")
            }

            val categories = mutableListOf<CategoryEntity>()
            val categoriesArr = root.getJSONArray("categories")
            for (i in 0 until categoriesArr.length()) {
                val obj = categoriesArr.getJSONObject(i)
                val type = obj.getString("type")
                require(type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                    "نوع فئة غير صالح"
                }
                val name = obj.getString("name").trim()
                require(name.isNotEmpty()) { "اسم فئة فارغ في النسخة الاحتياطية" }
                categories.add(
                    CategoryEntity(
                        id = obj.getLong("id"),
                        name = name,
                        type = type,
                        iconKey = obj.optString("iconKey", "more_horiz"),
                        colorKey = obj.optString("colorKey", "gray"),
                        isDefault = obj.optBoolean("isDefault", false),
                        sortOrder = obj.optInt("sortOrder", i)
                    )
                )
            }

            val transactions = mutableListOf<TransactionEntity>()
            val txArr = root.getJSONArray("transactions")
            for (i in 0 until txArr.length()) {
                val obj = txArr.getJSONObject(i)
                val type = obj.getString("type")
                require(type == TransactionType.EXPENSE || type == TransactionType.INCOME) {
                    "نوع عملية غير صالح"
                }
                val amount = obj.getDouble("amount")
                require(amount > 0.0 && amount.isFinite()) { "مبلغ عملية غير صالح" }
                transactions.add(
                    TransactionEntity(
                        id = obj.getLong("id"),
                        type = type,
                        amount = amount,
                        categoryId = obj.getLong("categoryId"),
                        categoryNameSnapshot = obj.optString("categoryNameSnapshot", "أخرى"),
                        categoryIconKeySnapshot = obj.optString("categoryIconKeySnapshot", "more_horiz"),
                        categoryColorKeySnapshot = obj.optString("categoryColorKeySnapshot", "gray"),
                        timestamp = obj.getLong("timestamp"),
                        note = obj.optString("note", "")
                    )
                )
            }

            val debts = mutableListOf<DebtEntity>()
            val debtIds = HashSet<Long>()
            val debtsArr = root.getJSONArray("debts")
            for (i in 0 until debtsArr.length()) {
                val obj = debtsArr.getJSONObject(i)
                val type = obj.getString("type")
                require(type == DebtType.OWED_TO_ME || type == DebtType.OWED_BY_ME) {
                    "نوع دين غير صالح"
                }
                val personName = obj.getString("personName").trim()
                require(personName.isNotEmpty()) { "اسم شخص فارغ في الديون" }
                val amount = obj.getDouble("originalAmount")
                require(amount > 0.0 && amount.isFinite()) { "مبلغ دين غير صالح" }
                val id = obj.getLong("id")
                debtIds.add(id)
                debts.add(
                    DebtEntity(
                        id = id,
                        personName = personName,
                        type = type,
                        originalAmount = amount,
                        createdAt = obj.getLong("createdAt")
                    )
                )
            }

            val debtPayments = mutableListOf<DebtPaymentEntity>()
            val paymentsArr = root.getJSONArray("debtPayments")
            for (i in 0 until paymentsArr.length()) {
                val obj = paymentsArr.getJSONObject(i)
                val debtId = obj.getLong("debtId")
                require(debtId in debtIds) { "دفعة دين مرتبطة بدين غير موجود" }
                val amount = obj.getDouble("amount")
                require(amount > 0.0 && amount.isFinite()) { "مبلغ دفعة غير صالح" }
                debtPayments.add(
                    DebtPaymentEntity(
                        id = obj.getLong("id"),
                        debtId = debtId,
                        amount = amount,
                        paidAt = obj.getLong("paidAt")
                    )
                )
            }

            return BackupPayload(
                schemaVersion = schemaVersion,
                exportedAt = root.optLong("exportedAt", System.currentTimeMillis()),
                categories = categories,
                transactions = transactions,
                debts = debts,
                debtPayments = debtPayments
            )
        }
    }
}
