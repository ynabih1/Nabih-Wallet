package com.nabih.wallet.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nabih.wallet.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments ORDER BY paidAt DESC, id DESC")
    fun observeAll(): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paidAt DESC, id DESC")
    fun observeForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paidAt DESC, id DESC")
    suspend fun getForDebt(debtId: Long): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments ORDER BY paidAt DESC, id DESC")
    suspend fun getAll(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: DebtPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<DebtPaymentEntity>)

    @Delete
    suspend fun delete(payment: DebtPaymentEntity)

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM debt_payments")
    suspend fun deleteAll()
}
