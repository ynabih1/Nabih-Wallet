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
import com.example.model.SmartNotificationItem

object NotificationHelper {
    const val CHANNEL_BUDGET = "channel_budget_alerts_v2"
    const val CHANNEL_DEBTS = "channel_debt_reminders_v2"
    const val CHANNEL_DAILY = "channel_daily_reminders_v2"
    const val CHANNEL_BACKUP = "channel_backup_reminders_v2"
    const val CHANNEL_INSIGHTS = "channel_smart_insights_v2"

    private const val NOTIFICATION_ID_BUDGET = 1001
    private const val NOTIFICATION_ID_DEBT = 1002
    private const val NOTIFICATION_ID_DAILY = 1003
    private const val NOTIFICATION_ID_BACKUP = 1005

    private const val BRAND_COLOR_INT = 0xFFB85435.toInt()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "تنبيهات الميزانية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات عند اقتراب أو تجاوز سقف الميزانية الشهرية"
                enableVibration(true)
            }

            val debtChannel = NotificationChannel(
                CHANNEL_DEBTS,
                "تذكيرات الديون المستحقة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكيرات بمتابعة الديون المستحقة لك وعليك"
                enableVibration(true)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY,
                "تذكير التسجيل اليومي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكيرات لتسجيل المصروفات اليومية"
                enableVibration(true)
            }

            val backupChannel = NotificationChannel(
                CHANNEL_BACKUP,
                "تذكير النسخ الاحتياطي",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكير دوري بحفظ نسخة احتياطية من بياناتك"
            }

            val insightsChannel = NotificationChannel(
                CHANNEL_INSIGHTS,
                "التنبيهات والرؤى الذكية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات تحليل المصروفات والفئات الأعلى إنفاقاً"
            }

            notificationManager.createNotificationChannels(
                listOf(budgetChannel, debtChannel, dailyChannel, backupChannel, insightsChannel)
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return runtimeGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_wallet)
            .setColor(BRAND_COLOR_INT)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun notificationIdForSmartItem(itemId: String): Int {
        return when (itemId) {
            "budget_exceeded" -> 2001
            "budget_warning" -> 2002
            "debts_owed_to_me" -> 2003
            "debts_i_owe" -> 2004
            "daily_logging_reminder" -> 2005
            "top_category_insight" -> 2006
            else -> 2000 + (itemId.hashCode() and 0x7FF)
        }
    }

    fun channelForSmartItem(type: String): String {
        return when (type) {
            "BUDGET" -> CHANNEL_BUDGET
            "DEBT" -> CHANNEL_DEBTS
            "DAILY" -> CHANNEL_DAILY
            "CATEGORY" -> CHANNEL_INSIGHTS
            else -> CHANNEL_BUDGET
        }
    }

    fun postSmartNotification(
        context: Context,
        item: SmartNotificationItem,
        isArabic: Boolean
    ): Boolean {
        val title = if (isArabic) item.titleAr else item.titleEn
        val message = if (isArabic) item.messageAr else item.messageEn
        val channelId = channelForSmartItem(item.type)
        val notifId = notificationIdForSmartItem(item.id)
        return sendNotification(context, channelId, notifId, title, message)
    }

    fun cancelSmartNotification(context: Context, itemId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForSmartItem(itemId))
        } catch (_: Exception) {
        }
    }

    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
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

    fun sendBackupReminder(context: Context, isArabic: Boolean): Boolean {
        val title = if (isArabic) "محفظة نبيه - تذكير بالنسخ الاحتياطي" else "Nabih Wallet - Backup Reminder"
        val msg = if (isArabic) {
            "حان موعد إنشاء نسخة احتياطية من معاملاتك وديونك لحماية بياناتك من الضياع."
        } else {
            "Time to create a backup of your transactions and debts to keep your data safe."
        }
        return sendNotification(context, CHANNEL_BACKUP, NOTIFICATION_ID_BACKUP, title, msg)
    }
}
