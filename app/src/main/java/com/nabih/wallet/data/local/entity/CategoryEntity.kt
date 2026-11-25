package com.nabih.wallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object TransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // TransactionType.EXPENSE or TransactionType.INCOME
    val iconKey: String,
    val colorKey: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
