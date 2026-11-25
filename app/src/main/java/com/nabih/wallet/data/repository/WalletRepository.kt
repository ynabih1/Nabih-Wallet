package com.nabih.wallet.data.repository

import androidx.room.withTransaction
import com.nabih.wallet.data.local.database.AppDatabase
import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.DebtEntity
import com.nabih.wallet.data.local.entity.DebtPaymentEntity
import com.nabih.wallet.data.local.entity.TransactionEntity
import com.nabih.wallet.data.model.DebtWithPayments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.max

sealed class PaymentResult {
    data object Success : PaymentResult()
    data class Error(val message: String) : PaymentResult()
}

class WalletRepository(private val database: AppDatabase) {

    private val categoryDao = database.categoryDao()
    private val transactionDao = database.transactionDao()
    private val debtDao = database.debtDao()
    private val debtPaymentDao = database.debtPaymentDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()

    val debtsWithPayments: Flow<List<DebtWithPayments>> = combine(
        debtDao.observeAll(),
        debtPaymentDao.observeAll()
    ) { debts, payments ->
        val paymentsByDebt = payments.groupBy { it.debtId }
        debts.map { debt ->
            DebtWithPayments(
                debt = debt,
                payments = paymentsByDebt[debt.id].orEmpty()
            )
        }
    }

    suspend fun ensureDefaultCategories() {
        AppDatabase.seedAndDeduplicateCategories(database)
    }

    // --- Categories ---

    suspend fun addCategory(
        name: String,
        type: String,
        iconKey: String,
        colorKey: String
    ): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "اسم الفئة مطلوب" }
        val existing = categoryDao.getAll()
        val duplicate = existing.firstOrNull { it.type == type && it.name.trim() == trimmed }
        if (duplicate != null) {
            return duplicate.id
        }
        val nextSort = (existing.maxOfOrNull { it.sortOrder } ?: 0) + 1
        return categoryDao.insert(
            CategoryEntity(
                name = trimmed,
                type = type,
                iconKey = iconKey,
                colorKey = colorKey,
                isDefault = false,
                sortOrder = nextSort
            )
        )
    }

    suspend fun updateCategory(category: CategoryEntity) {
        val trimmed = category.name.trim()
        require(trimmed.isNotEmpty()) { "اسم الفئة مطلوب" }
        categoryDao.update(category.copy(name = trimmed))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    // --- Transactions (strictly independent from debts) ---

    suspend fun addTransaction(
        type: String,
        amount: Double,
        categoryId: Long,
        timestamp: Long,
        note: String = ""
    ): Long {
        require(amount > 0.0) { "يجب أن يكون المبلغ أكبر من صفر" }
        val category = categoryDao.getById(categoryId)
        return transactionDao.insert(
            TransactionEntity(
                type = type,
                amount = amount,
                categoryId = categoryId,
                categoryNameSnapshot = category?.name ?: "أخرى",
                categoryIconKeySnapshot = category?.iconKey ?: "more_horiz",
                categoryColorKeySnapshot = category?.colorKey ?: "gray",
                timestamp = timestamp,
                note = note.trim()
            )
        )
    }

    suspend fun updateTransaction(
        id: Long,
        type: String,
        amount: Double,
        categoryId: Long,
        timestamp: Long,
        note: String = ""
    ) {
        require(amount > 0.0) { "يجب أن يكون المبلغ أكبر من صفر" }
        val category = categoryDao.getById(categoryId)
        transactionDao.update(
            TransactionEntity(
                id = id,
                type = type,
                amount = amount,
                categoryId = categoryId,
                categoryNameSnapshot = category?.name ?: "أخرى",
                categoryIconKeySnapshot = category?.iconKey ?: "more_horiz",
                categoryColorKeySnapshot = category?.colorKey ?: "gray",
                timestamp = timestamp,
                note = note.trim()
            )
        )
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    // --- Debts (strictly isolated from net balance & monthly expenses) ---

    suspend fun addDebt(
        personName: String,
        type: String,
        originalAmount: Double,
        createdAt: Long = System.currentTimeMillis()
    ): Long {
        val trimmed = personName.trim()
        require(trimmed.isNotEmpty()) { "اسم الشخص مطلوب" }
        require(originalAmount > 0.0) { "يجب أن يكون المبلغ أكبر من صفر" }
        return debtDao.insert(
            DebtEntity(
                personName = trimmed,
                type = type,
                originalAmount = originalAmount,
                createdAt = createdAt
            )
        )
    }

    suspend fun updateDebt(
        id: Long,
        personName: String,
        type: String,
        originalAmount: Double
    ): PaymentResult {
        val trimmed = personName.trim()
        if (trimmed.isEmpty()) return PaymentResult.Error("يرجى إدخال اسم الشخص")
        if (originalAmount <= 0.0) return PaymentResult.Error("يجب أن يكون المبلغ أكبر من صفر")

        val existing = debtDao.getById(id) ?: return PaymentResult.Error("الدين غير موجود")
        val paidSum = debtPaymentDao.getForDebt(id).sumOf { it.amount }
        if (originalAmount + 0.004 < paidSum) {
            return PaymentResult.Error("المبلغ الأصلي لا يمكن أن يكون أقل من إجمالي المسدد")
        }

        debtDao.update(
            existing.copy(
                personName = trimmed,
                type = type,
                originalAmount = originalAmount
            )
        )
        return PaymentResult.Success
    }

    suspend fun deleteDebt(id: Long) {
        debtDao.deleteById(id)
    }

    suspend fun addDebtPayment(
        debtId: Long,
        amount: Double,
        paidAt: Long = System.currentTimeMillis()
    ): PaymentResult {
        if (amount <= 0.0) {
            return PaymentResult.Error("يجب إدخال مبلغ أكبر من صفر")
        }
        val debt = debtDao.getById(debtId) ?: return PaymentResult.Error("الدين غير موجود")
        val existingPaid = debtPaymentDao.getForDebt(debtId).sumOf { it.amount }
        val remaining = max(0.0, debt.originalAmount - existingPaid)
        if (amount > remaining + 0.004) {
            return PaymentResult.Error("المبلغ أكبر من المتبقي المستحق")
        }
        val clampedAmount = amount.coerceAtMost(remaining)
        debtPaymentDao.insert(
            DebtPaymentEntity(
                debtId = debtId,
                amount = clampedAmount,
                paidAt = paidAt
            )
        )
        return PaymentResult.Success
    }

    suspend fun deleteDebtPayment(paymentId: Long) {
        debtPaymentDao.deleteById(paymentId)
    }

    // --- Backup snapshots & atomic restore ---

    suspend fun getAllCategoriesSnapshot(): List<CategoryEntity> = categoryDao.getAll()
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> = transactionDao.getAll()
    suspend fun getAllDebtsSnapshot(): List<DebtEntity> = debtDao.getAll()
    suspend fun getAllDebtPaymentsSnapshot(): List<DebtPaymentEntity> = debtPaymentDao.getAll()

    suspend fun restoreSnapshotAtomically(
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        debts: List<DebtEntity>,
        debtPayments: List<DebtPaymentEntity>
    ) {
        database.withTransaction {
            debtPaymentDao.deleteAll()
            debtDao.deleteAll()
            transactionDao.deleteAll()
            categoryDao.deleteAll()

            if (categories.isNotEmpty()) {
                categoryDao.insertAll(categories)
            } else {
                categoryDao.insertAll(AppDatabase.defaultCategories())
            }
            if (transactions.isNotEmpty()) {
                transactionDao.insertAll(transactions)
            }
            if (debts.isNotEmpty()) {
                debtDao.insertAll(debts)
            }
            if (debtPayments.isNotEmpty()) {
                debtPaymentDao.insertAll(debtPayments)
            }
        }
    }
}
