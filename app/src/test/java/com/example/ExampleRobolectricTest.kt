package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.model.DebtWithPayments
import com.example.model.DueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue(appName == "محفظة نبيه" || appName == "Nabih Wallet")
  }

  @Test
  fun `test debt model remaining amount and progress calculations`() {
    val debt = DebtEntity(
        id = 1L,
        type = "OWED_TO_ME",
        personName = "Ahmed Mostafa",
        totalAmount = 600.0,
        lentDateMillis = System.currentTimeMillis() - 86400000L * 10,
        dueDateMillis = System.currentTimeMillis() + 86400000L * 4
    )
    val payments = listOf(
        DebtPaymentEntity(
            id = 1L,
            debtId = 1L,
            amount = 150.0,
            paymentDateMillis = System.currentTimeMillis()
        )
    )
    val debtWithPayments = DebtWithPayments(debt = debt, payments = payments)

    assertEquals(450.0, debtWithPayments.remainingAmount, 0.001)
    assertEquals(0.25f, debtWithPayments.progress, 0.001f)
    assertFalse(debtWithPayments.isFullyPaid)
    assertEquals("AM", debtWithPayments.getInitials())
  }
}
