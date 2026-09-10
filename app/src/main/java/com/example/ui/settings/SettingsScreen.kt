package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.WalletViewModel
import com.example.ui.category.CategoryManagementDialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeLight
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface

@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isDebtAlertEnabled by viewModel.isDebtAlertEnabled.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val isArabic = language == "ar"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isArabic) "الإعدادات" else "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        // 1. Preferences Card (التفضيلات) - Language and Appearance
        item {
            SettingsSectionCard(
                title = if (isArabic) "التفضيلات" else "Preferences",
                icon = Icons.Default.Settings
            ) {
                // Language
                Text(
                    text = if (isArabic) "اللغة" else "Language",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryBrown
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SegmentedOptionButton(
                        text = "العربية",
                        isSelected = isArabic,
                        onClick = { viewModel.setLanguage("ar") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_arabic_btn")
                    )
                    SegmentedOptionButton(
                        text = "English",
                        isSelected = !isArabic,
                        onClick = { viewModel.setLanguage("en") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_english_btn")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Appearance (المظهر)
                Text(
                    text = if (isArabic) "المظهر" else "Appearance",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryBrown
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SegmentedOptionButton(
                        text = if (isArabic) "فاتح" else "Light",
                        isSelected = themeMode == "LIGHT",
                        onClick = { viewModel.setThemeMode("LIGHT") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_btn")
                    )
                    SegmentedOptionButton(
                        text = if (isArabic) "داكن" else "Dark",
                        isSelected = themeMode == "DARK",
                        onClick = { viewModel.setThemeMode("DARK") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_dark_btn")
                    )
                    SegmentedOptionButton(
                        text = if (isArabic) "النظام" else "System",
                        isSelected = themeMode == "SYSTEM",
                        onClick = { viewModel.setThemeMode("SYSTEM") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_system_btn")
                    )
                }
            }
        }

        // 2. Notifications Card (الإشعارات والتنبيهات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "الإشعارات والتنبيهات" else "Notifications & Alerts",
                icon = Icons.Default.NotificationsActive
            ) {
                // Master Notifications Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "تفعيل التنبيهات والإشعارات" else "Enable Notifications",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isArabic) "استقبال إشعارات وتنبيهات النظام ومتابعة الحالة المالية" else "Receive system alerts and updates",
                            fontSize = 11.sp,
                            color = TextSecondaryBrown
                        )
                    }

                    Switch(
                        checked = isNotificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BurntOrangePrimary,
                            uncheckedThumbColor = TextSecondaryBrown,
                            uncheckedTrackColor = BorderSubtle
                        ),
                        modifier = Modifier.testTag("settings_master_notifications_switch")
                    )
                }

                AnimatedVisibility(visible = isNotificationsEnabled) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))

                        // Debt Reminders
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "تذكيرات الديون المستحقة" else "Debt Reminders",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isArabic) "تنبيه بالمبالغ والديون المستحقة للسداد أو التحصيل" else "Alerts for payables and receivables",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown
                                )
                            }

                            Switch(
                                checked = isDebtAlertEnabled,
                                onCheckedChange = { viewModel.setDebtAlertEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BurntOrangePrimary,
                                    uncheckedThumbColor = TextSecondaryBrown,
                                    uncheckedTrackColor = BorderSubtle
                                )
                            )
                        }

                        // Daily Log Reminder
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isArabic) "تذكير التسجيل اليومي" else "Daily Expense Reminder",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    text = if (isArabic) "تذكير يومي لتسجيل المصروفات والحفاظ على الدقة" else "Daily prompt to log your expenses",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown
                                )
                            }

                            Switch(
                                checked = isDailyReminderEnabled,
                                onCheckedChange = { viewModel.setDailyReminderEnabled(it) },
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
            }
        }

        // 3. Categories Card (الفئات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "الفئات" else "Categories",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = if (isArabic) "تخصيص أيقونات وأسماء فئات المصروفات والدخل" else "Customize expense and income category icons & names",
                    fontSize = 13.sp,
                    color = TextSecondaryBrown
                )
                Spacer(modifier = Modifier.height(14.dp))
                SegmentedActionButton(
                    text = if (isArabic) "إدارة وتخصيص الفئات" else "Manage Categories",
                    onClick = { showCategoryDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_categories_btn")
                )
            }
        }

        // 4. Reset Data (إعادة ضبط البيانات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "إعادة ضبط البيانات" else "Reset Data",
                icon = Icons.Default.DeleteForever
            ) {
                SegmentedActionButton(
                    text = if (isArabic) "مسح كافة البيانات" else "Reset All Data",
                    textColor = MutedExpenseTerracotta,
                    borderColor = MutedExpenseTerracotta.copy(alpha = 0.35f),
                    onClick = { showResetDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clear_data_btn")
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showCategoryDialog) {
        CategoryManagementDialog(
            expenseCategories = viewModel.getAllExpenseCategories(),
            incomeCategories = viewModel.getAllIncomeCategories(),
            customIconOverrides = categoryIcons,
            isArabic = isArabic,
            onDismiss = { showCategoryDialog = false },
            onUpdateIcon = { catName, iconKey -> viewModel.setCategoryIcon(catName, iconKey) },
            onAddCustomCategory = { nameAr, nameEn, type, iconKey -> viewModel.addCustomCategory(nameAr, nameEn, type, iconKey) },
            onDeleteCustomCategory = { catId -> viewModel.deleteCustomCategory(catId) },
            onResetDefaults = { viewModel.resetCategoryIcons() }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = if (isArabic) "مسح كافة البيانات" else "Clear All Data",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic) "هل أنت متأكد من رغبتك في حذف جميع المعاملات والديون؟ لا يمكن التراجع عن هذا الإجراء." else "Are you sure you want to delete all transactions and debts? This action cannot be undone.",
                    color = TextSecondaryBrown
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedExpenseTerracotta)
                ) {
                    Text(if (isArabic) "حذف" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // Header: Title and Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BurntOrangePrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}

@Composable
fun SegmentedOptionButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) BurntOrangeLight.copy(alpha = 0.35f) else WarmCardSurface
    val borderColor = if (isSelected) BurntOrangePrimary else BorderSubtle
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val textColor = if (isSelected) BurntOrangePrimary else TextPrimaryDark
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSelected) "$text ✓" else text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = fontWeight
            )
        }
    }
}

@Composable
fun SegmentedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = TextPrimaryDark,
    borderColor: Color = BorderSubtle
) {
    Surface(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = WarmCardSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
