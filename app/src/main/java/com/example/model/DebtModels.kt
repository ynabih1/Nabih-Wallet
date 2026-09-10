package com.example.model

import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import java.util.concurrent.TimeUnit

data class DebtWithPayments(
    val debt: DebtEntity,
    val payments: List<DebtPaymentEntity> = emptyList()
) {
    val totalAmount: Double get() = debt.totalAmount
    val repaidAmount: Double get() = payments.sumOf { it.amount }
    val remainingAmount: Double get() = (debt.totalAmount - repaidAmount).coerceAtLeast(0.0)
    val progress: Float get() = if (debt.totalAmount > 0.0) {
        (repaidAmount / debt.totalAmount).toFloat().coerceIn(0f, 1f)
    } else 0f

    val isFullyPaid: Boolean get() = remainingAmount <= 0.001
    val isPartiallyPaid: Boolean get() = repaidAmount > 0 && !isFullyPaid
    val isUnpaid: Boolean get() = repaidAmount <= 0.001

    fun getInitials(): String {
        val trimmed = debt.personName.trim()
        if (trimmed.isEmpty()) return "?"
        val parts = trimmed.split(" ").filter { it.isNotBlank() }
        return if (parts.size >= 2) {
            "${parts[0].first()}${parts[1].first()}".uppercase()
        } else {
            trimmed.take(2).uppercase()
        }
    }

    fun getDueStatus(nowMillis: Long = System.currentTimeMillis()): DueStatus {
        val due = debt.dueDateMillis ?: return DueStatus.NoDueDate
        val diffMillis = due - nowMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMillis < 0 -> {
                val overdueDays = TimeUnit.MILLISECONDS.toDays(-diffMillis).coerceAtLeast(1)
                DueStatus.Overdue(overdueDays.toInt())
            }
            diffDays == 0L -> DueStatus.DueToday
            else -> DueStatus.DueInDays(diffDays.toInt().coerceAtLeast(1))
        }
    }
}

sealed interface DueStatus {
    data object NoDueDate : DueStatus
    data object DueToday : DueStatus
    data class DueInDays(val days: Int) : DueStatus
    data class Overdue(val days: Int) : DueStatus
}
