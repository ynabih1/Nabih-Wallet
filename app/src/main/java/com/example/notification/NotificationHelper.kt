package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_BUDGET = "channel_budget_alerts"
    const val CHANNEL_DEBTS = "channel_debt_reminders"
    const val CHANNEL_DAILY = "channel_daily_reminders"

    private const val NOTIFICATION_ID_BUDGET = 1001
    private const val NOTIFICATION_ID_DEBT = 1002
    private const val NOTIFICATION_ID_DAILY = 1003
    private const val NOTIFICATION_ID_TEST = 1004

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "تنبيهات الميزانية (Budget Alerts)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات عند اقتراب أو تجاوز سقف الميزانية الشهرية"
                enableVibration(true)
            }

            val debtChannel = NotificationChannel(
                CHANNEL_DEBTS,
                "تذكيرات الديون (Debt Reminders)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكيرات بمتابعة الديون المستحقة لك وعليك"
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "تذكير التسجيل اليومي (Daily Reminder)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تذكيرات لتسجيل المصروفات اليومية"
            }

            notificationManager.createNotificationChannels(listOf(budgetChannel, debtChannel, dailyChannel))
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String
    ): Boolean {
        createNotificationChannels(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun sendBudgetAlert(context: Context, title: String, message: String): Boolean {
        return sendNotification(context, CHANNEL_BUDGET, NOTIFICATION_ID_BUDGET, title, message)
    }

    fun sendDebtReminder(context: Context, title: String, message: String): Boolean {
        return sendNotification(context, CHANNEL_DEBTS, NOTIFICATION_ID_DEBT, title, message)
    }

    fun sendDailyReminder(context: Context, title: String, message: String): Boolean {
        return sendNotification(context, CHANNEL_DAILY, NOTIFICATION_ID_DAILY, title, message)
    }

    fun sendTestNotification(context: Context, isArabic: Boolean): Boolean {
        val title = if (isArabic) "محفظة نبيه - إشعار تجريبي" else "Nabih Wallet - Test Alert"
        val msg = if (isArabic) {
            "تم تفعيل نظام الإشعارات والتنبيهات الذكية بنجاح! ستصلك تنبيهات الميزانية والديون بانتظام."
        } else {
            "Smart notifications enabled successfully! You'll receive budget and debt alerts regularly."
        }
        return sendNotification(context, CHANNEL_BUDGET, NOTIFICATION_ID_TEST, title, msg)
    }
}
