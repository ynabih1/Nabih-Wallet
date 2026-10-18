package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.model.DebtWithPayments
import com.example.model.DueStatus
import com.example.model.toDebtRecord
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

  @Test
  fun `test debt record export mapping`() {
    val debt = DebtEntity(
        id = 1L,
        type = "OWED_TO_ME",
        personName = "أحمد مصطفى",
        totalAmount = 1000.0,
        lentDateMillis = System.currentTimeMillis() - 86400000L * 5,
        dueDateMillis = System.currentTimeMillis() + 86400000L * 5
    )
    val payment = DebtPaymentEntity(
        id = 1L,
        debtId = 1L,
        amount = 400.0,
        paymentDateMillis = System.currentTimeMillis()
    )
    val record = DebtWithPayments(debt = debt, payments = listOf(payment)).toDebtRecord()
    assertEquals("أحمد مصطفى", record.personName)
    assertEquals("LENT", record.type)
    assertEquals(1000.0, record.originalAmount, 0.001)
    assertEquals(600.0, record.remainingAmount, 0.001)
    assertFalse(record.isOverdue)
  }

  @Test
  fun `test system notifications are posted to Android NotificationManager`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    val sentBackup = com.example.notification.NotificationHelper.sendBackupReminder(app, isArabic = true)
    assertTrue(sentBackup)

    val smartItem = com.example.model.SmartNotificationItem(
        id = "budget_warning",
        titleAr = "⚠️ اقتراب من سقف الميزانية",
        titleEn = "⚠️ Approaching Budget Limit",
        messageAr = "تم استهلاك 85% من ميزانيتك الشهرية.",
        messageEn = "You have spent 85% of your monthly budget.",
        severity = com.example.model.NotificationSeverity.WARNING,
        type = "BUDGET"
    )
    val sentSmart = com.example.notification.NotificationHelper.postSmartNotification(app, smartItem, isArabic = true)
    assertTrue(sentSmart)

    val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val shadowNm = org.robolectric.Shadows.shadowOf(notificationManager)
    assertTrue(shadowNm.allNotifications.size >= 2)
  }

  @Test
  fun `test WalletViewModel initializes without NullPointerException`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.ui.WalletViewModel(app)
    assertTrue(viewModel.smartNotifications.value != null)
    assertTrue(viewModel.unreadNotificationCount.value >= 0)
  }
}

