package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods ORDER BY id ASC")
    fun getAllPaymentMethods(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods ORDER BY id ASC")
    suspend fun getAllPaymentMethodsSync(): List<PaymentMethodEntity>

    @Query("SELECT * FROM payment_methods WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getByName(name: String): PaymentMethodEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPaymentMethod(paymentMethod: PaymentMethodEntity): Long

    @Delete
    suspend fun deletePaymentMethod(paymentMethod: PaymentMethodEntity)

    @Query("DELETE FROM payment_methods WHERE id = :id")
    suspend fun deletePaymentMethodById(id: Long)
}
