package com.nabih.wallet.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["timestamp"]),
        Index(value = ["type"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // TransactionType.EXPENSE or TransactionType.INCOME
    val amount: Double,
    val categoryId: Long,
    val categoryNameSnapshot: String,
    val categoryIconKeySnapshot: String = "category",
    val categoryColorKeySnapshot: String = "blue",
    val timestamp: Long,
    val note: String = ""
)
