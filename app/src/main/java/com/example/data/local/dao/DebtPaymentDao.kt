package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDateMillis DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDateMillis DESC")
    suspend fun getPaymentsForDebtSync(debtId: Long): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments ORDER BY paymentDateMillis DESC")
    fun getAllPayments(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments ORDER BY paymentDateMillis DESC")
    suspend fun getAllPaymentsSync(): List<DebtPaymentEntity>

    @Query("SELECT SUM(amount) FROM debt_payments WHERE debtId = :debtId")
    fun getRepaidSumForDebt(debtId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM debt_payments WHERE debtId = :debtId")
    suspend fun getRepaidSumForDebtSync(debtId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: DebtPaymentEntity)

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)
}
