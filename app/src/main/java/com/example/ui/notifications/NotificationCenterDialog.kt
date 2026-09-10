package com.example.ui.notifications

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.NotificationSeverity
import com.example.model.SmartNotificationItem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeLight
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@Composable
fun NotificationCenterDialog(
    notifications: List<SmartNotificationItem>,
    isNotificationsEnabled: Boolean,
    isBudgetAlertEnabled: Boolean,
    isDebtAlertEnabled: Boolean,
    isDailyReminderEnabled: Boolean,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onToggleDebtAlerts: (Boolean) -> Unit,
    onToggleDailyReminder: (Boolean) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToDebts: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("notification_center_dialog"),
            color = WarmCardSurface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BurntOrangePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "مركز الإشعارات والتنبيهات" else "Notification Center",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                if (notifications.isNotEmpty() && isNotificationsEnabled) {
                                    Surface(
                                        color = BurntOrangePrimary,
                                        shape = CircleShape,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${notifications.size}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (isArabic) "متابعة الميزانية والديون والتذكيرات" else "Budget, debt tracking & reminders",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryBrown
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("notification_dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondaryBrown
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = WarmBackground,
                    contentColor = BurntOrangePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = BurntOrangePrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                text = if (isArabic) "التنبيهات الذكية (${notifications.size})" else "Smart Alerts (${notifications.size})",
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 0) BurntOrangePrimary else TextSecondaryBrown,
                                fontSize = 13.sp
                            )
                        }
                    )

                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                text = if (isArabic) "إعدادات التنبيه" else "Preferences",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == 1) BurntOrangePrimary else TextSecondaryBrown,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                if (selectedTabIndex == 0) {
                    // Smart Alerts Tab
                    if (!isNotificationsEnabled) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = WarmBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = TextSecondaryBrown,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (isArabic) "الإشعارات معطلة حالياً" else "Notifications are disabled",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isArabic) "قم بتفعيل الإشعارات لتلقي تنبيهات الميزانية والديون." else "Enable notifications to receive budget and debt alerts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryBrown,
                                    lineHeight = 18.sp
                                )
                                Button(
                                    onClick = { onToggleNotifications(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "تفعيل الإشعارات الآن" else "Enable Notifications Now",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    } else if (notifications.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            color = WarmBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MutedIncomeGreen,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (isArabic) "محفظتك بحالة ممتازة! 🎉" else "Your wallet is in great shape! 🎉",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isArabic) "لا توجد أي تحذيرات أو متأخرات معلقة. جميع العمليات والميزانية في المسار الصحيح." else "No pending warnings or overdue alerts. Everything is on track.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondaryBrown,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(notifications, key = { it.id }) { item ->
                                SmartNotificationCard(
                                    item = item,
                                    isArabic = isArabic,
                                    onActionClick = {
                                        when (item.type) {
                                            "BUDGET" -> {
                                                onDismiss()
                                                onNavigateToBudget()
                                            }
                                            "DEBT" -> {
                                                onDismiss()
                                                onNavigateToDebts()
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Preferences Tab
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Master toggle
                        NotificationPreferenceRow(
                            title = if (isArabic) "تفعيل جميع الإشعارات" else "Enable All Notifications",
                            subtitle = if (isArabic) "المفتاح الرئيسي لجميع تنبيهات وتذكيرات المحفظة" else "Master switch for all wallet alerts",
                            checked = isNotificationsEnabled,
                            onCheckedChange = onToggleNotifications,
                            icon = Icons.Default.NotificationsActive
                        )

                        AnimatedVisibility(visible = isNotificationsEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                NotificationPreferenceRow(
                                    title = if (isArabic) "تنبيهات الميزانية (80% وتجاوز الحد)" else "Budget Alerts (80% & Exceeded)",
                                    subtitle = if (isArabic) "إشعار فوري عند الاقتراب من السقف الشهري" else "Instant alert when near or exceeding budget",
                                    checked = isBudgetAlertEnabled,
                                    onCheckedChange = onToggleBudgetAlerts,
                                    icon = Icons.Default.Savings
                                )

                                NotificationPreferenceRow(
                                    title = if (isArabic) "تذكيرات الديون المستحقة" else "Debt Reminders",
                                    subtitle = if (isArabic) "متابعة المبالغ المستحقة لك وعليك بانتظام" else "Regular tracking for receivables and payables",
                                    checked = isDebtAlertEnabled,
                                    onCheckedChange = onToggleDebtAlerts,
                                    icon = Icons.Default.Handshake
                                )

                                NotificationPreferenceRow(
                                    title = if (isArabic) "تذكير التسجيل اليومي" else "Daily Expense Log Reminder",
                                    subtitle = if (isArabic) "تذكير لتسجيل المصروفات والحفاظ على الدقة" else "Reminds you to log expenses daily",
                                    checked = isDailyReminderEnabled,
                                    onCheckedChange = onToggleDailyReminder,
                                    icon = Icons.Default.Today
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartNotificationCard(
    item: SmartNotificationItem,
    isArabic: Boolean,
    onActionClick: () -> Unit
) {
    val (bgColor, borderColor, iconTint, icon) = when (item.severity) {
        NotificationSeverity.ALERT -> Quadruple(
            MutedExpenseTerracotta.copy(alpha = 0.12f),
            MutedExpenseTerracotta.copy(alpha = 0.4f),
            MutedExpenseTerracotta,
            Icons.Default.WarningAmber
        )
        NotificationSeverity.WARNING -> Quadruple(
            Color(0xFFC97A3E).copy(alpha = 0.12f),
            Color(0xFFC97A3E).copy(alpha = 0.4f),
            Color(0xFFC97A3E),
            Icons.Default.WarningAmber
        )
        NotificationSeverity.INFO -> when (item.type) {
            "BUDGET" -> Quadruple(
                BurntOrangeLight.copy(alpha = 0.3f),
                BurntOrangePrimary.copy(alpha = 0.3f),
                BurntOrangePrimary,
                Icons.Default.Savings
            )
            "DEBT" -> Quadruple(
                MutedIncomeGreen.copy(alpha = 0.12f),
                MutedIncomeGreen.copy(alpha = 0.3f),
                MutedIncomeGreen,
                Icons.Default.Handshake
            )
            "CATEGORY" -> Quadruple(
                Color(0xFF3F51B5).copy(alpha = 0.1f),
                Color(0xFF3F51B5).copy(alpha = 0.3f),
                Color(0xFF3F51B5),
                Icons.Default.PieChart
            )
            else -> Quadruple(
                WarmBackground,
                BorderSubtle,
                BurntOrangePrimary,
                Icons.Default.Info
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onActionClick() }
            .testTag("notification_card_${item.id}"),
        color = bgColor,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isArabic) item.titleAr else item.titleEn,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isArabic) item.messageAr else item.messageEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryBrown,
                    lineHeight = 16.sp,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
fun NotificationPreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = WarmBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) BurntOrangePrimary else TextSecondaryBrown,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        fontSize = 13.sp
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown,
                        fontSize = 11.sp
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BurntOrangePrimary,
                    uncheckedThumbColor = TextSecondaryBrown,
                    uncheckedTrackColor = BorderSubtle
                )
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
