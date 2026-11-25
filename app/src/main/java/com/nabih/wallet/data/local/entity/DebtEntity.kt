package com.nabih.wallet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

object DebtType {
    const val OWED_TO_ME = "OWED_TO_ME"
    const val OWED_BY_ME = "OWED_BY_ME"
}

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val type: String, // DebtType.OWED_TO_ME or DebtType.OWED_BY_ME
    val originalAmount: Double,
    val createdAt: Long
)
