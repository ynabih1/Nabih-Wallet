package com.example.data.repository

import com.example.data.local.dao.DebtDao
import com.example.data.local.dao.DebtPaymentDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.DebtWithPayments
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class WalletRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val debtPaymentDao: DebtPaymentDao
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val pinnedTransactions: Flow<List<TransactionEntity>> = transactionDao.getPinnedTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(15)

    fun getTransactionsBetween(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsBetween(startMillis, endMillis)

    suspend fun getTransactionsBetweenSync(startMillis: Long, endMillis: Long): List<TransactionEntity> =
        transactionDao.getTransactionsBetweenSync(startMillis, endMillis)

    suspend fun getTransactionsByTypeSync(type: String): List<TransactionEntity> =
        transactionDao.getTransactionsByTypeSync(type)

    suspend fun getAllTransactionsSync(): List<TransactionEntity> =
        transactionDao.getAllTransactionsSync()

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        transactionDao.deleteTransactionById(id)

    suspend fun togglePinTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction.copy(isPinned = !transaction.isPinned))
    }

    // Debts + Payments combined reactive flow
    val allDebtsWithPayments: Flow<List<DebtWithPayments>> = combine(
        debtDao.getAllDebts(),
        debtPaymentDao.getAllPayments()
    ) { debts, payments ->
        val paymentsByDebt = payments.groupBy { it.debtId }
        debts.map { debt ->
            val debtPayments = paymentsByDebt[debt.id] ?: emptyList()
            val totalRepaid = debtPayments.sumOf { it.amount }
            val newStatus = when {
                totalRepaid >= debt.totalAmount - 0.001 -> "FULLY_PAID"
                totalRepaid > 0 -> "PARTIALLY_PAID"
                else -> "UNPAID"
            }
            // keep status aligned
            val updatedDebt = if (debt.status != newStatus) debt.copy(status = newStatus) else debt
            DebtWithPayments(
                debt = updatedDebt,
                payments = debtPayments
            )
        }
    }

    suspend fun getAllDebtsWithPaymentsSync(): List<DebtWithPayments> {
        val debts = debtDao.getAllDebtsSync()
        val payments = debtPaymentDao.getAllPaymentsSync()
        val paymentsByDebt = payments.groupBy { it.debtId }
        return debts.map { debt ->
            DebtWithPayments(
                debt = debt,
                payments = paymentsByDebt[debt.id] ?: emptyList()
            )
        }
    }

    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>> =
        debtPaymentDao.getPaymentsForDebt(debtId)

    suspend fun insertDebt(debt: DebtEntity): Long =
        debtDao.insertDebt(debt)

    suspend fun updateDebt(debt: DebtEntity) =
        debtDao.updateDebt(debt)

    suspend fun deleteDebt(debt: DebtEntity) =
        debtDao.deleteDebt(debt)

    suspend fun deleteDebtById(id: Long) =
        debtDao.deleteDebtById(id)

    suspend fun addDebtPayment(payment: DebtPaymentEntity): Long {
        val paymentId = debtPaymentDao.insertPayment(payment)
        // Check and update debt status
        val debt = debtDao.getDebtByIdSync(payment.debtId)
        if (debt != null) {
            val totalRepaid = debtPaymentDao.getRepaidSumForDebtSync(debt.id) ?: 0.0
            val newStatus = when {
                totalRepaid >= debt.totalAmount - 0.001 -> "FULLY_PAID"
                totalRepaid > 0.0 -> "PARTIALLY_PAID"
                else -> "UNPAID"
            }
            if (debt.status != newStatus) {
                debtDao.updateDebt(debt.copy(status = newStatus))
            }
        }
        return paymentId
    }

    suspend fun deleteDebtPayment(payment: DebtPaymentEntity) {
        debtPaymentDao.deletePayment(payment)
        val debt = debtDao.getDebtByIdSync(payment.debtId)
        if (debt != null) {
            val totalRepaid = debtPaymentDao.getRepaidSumForDebtSync(debt.id) ?: 0.0
            val newStatus = when {
                totalRepaid >= debt.totalAmount - 0.001 -> "FULLY_PAID"
                totalRepaid > 0.0 -> "PARTIALLY_PAID"
                else -> "UNPAID"
            }
            if (debt.status != newStatus) {
                debtDao.updateDebt(debt.copy(status = newStatus))
            }
        }
    }

    suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        debtDao.deleteAllDebts()
    }
}
