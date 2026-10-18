package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "OWED_TO_ME" (Lent) or "I_OWE" (Borrowed)
    val personName: String,
    val totalAmount: Double,
    val lentDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long? = null,
    val notes: String = "",
    val enableReminder: Boolean = false,
    val status: String = "UNPAID" // "UNPAID", "PARTIALLY_PAID", "FULLY_PAID"
)
