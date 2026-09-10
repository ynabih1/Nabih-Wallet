package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isPinned: Boolean = false,
    val receiptUri: String? = null,
    val receiptMimeType: String? = null
)
