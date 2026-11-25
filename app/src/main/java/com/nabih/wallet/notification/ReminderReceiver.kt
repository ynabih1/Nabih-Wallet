package com.nabih.wallet.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nabih.wallet.data.preferences.AppPreferences

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AppPreferences(context).preferences.value
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (prefs.dailyReminderEnabled) {
                    NotificationHelper.scheduleDailyReminder(
                        context = context,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            }

            ACTION_DAILY_REMINDER -> {
                if (prefs.dailyReminderEnabled) {
                    NotificationHelper.showReminderNotification(context)
                    // Reschedule automatically for the next day
                    NotificationHelper.scheduleDailyReminder(
                        context = context,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_REMINDER = "com.nabih.wallet.ACTION_DAILY_REMINDER"
    }
}
