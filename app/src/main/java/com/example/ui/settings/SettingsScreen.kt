package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.ui.category.CategoryManagementDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupPackage
import com.example.model.CategoryItem
import com.example.model.FinancialConstants
import com.example.model.IconCategoryGroup
import com.example.model.IconLibrary
import com.example.ui.WalletViewModel
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseLight
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.MutedIncomeGreenLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isDebtAlertEnabled by viewModel.isDebtAlertEnabled.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val paymentMethods by viewModel.allPaymentMethods.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreErrorDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf("") }
    var pendingRestorePackage by remember { mutableStateOf<BackupPackage?>(null) }

    var showCategoryManagementDialog by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }

    val expenseCategories = remember(customCategories, categoryIcons) {
        viewModel.getAllExpenseCategories()
    }
    val incomeCategories = remember(customCategories, categoryIcons) {
        viewModel.getAllIncomeCategories()
    }

    val isArabic = language == "ar"

    val pickBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = viewModel.validateBackupFile(context, uri)
            result.fold(
                onSuccess = { pkg ->
                    pendingRestorePackage = pkg
                    showRestoreConfirmDialog = true
                },
                onFailure = { err ->
                    restoreErrorMessage = err.localizedMessage ?: if (isArabic) "ملف النسخة الاحتياطية غير صالح أو تالف" else "Invalid backup file"
                    showRestoreErrorDialog = true
                }
            )
        }
    }

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

        // 1. Categories Card (الفئات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "الفئات" else "Categories",
                icon = Icons.Default.Palette
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isArabic) "تخصيص أيقونات وأسماء فئات المصروفات والدخل" else "Customize icons and names of expense and income categories",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown
                    )

                    Button(
                        onClick = { showCategoryManagementDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manage_categories_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF3F1EC),
                            contentColor = TextPrimaryDark
                        ),
                        border = BorderStroke(1.dp, BorderSubtle),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إدارة وتخصيص الفئات" else "Manage & Customize Categories",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // 2. Notifications Card (الإشعارات والتنبيهات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "الإشعارات والتنبيهات" else "Notifications & Alerts",
                icon = Icons.Default.NotificationsActive
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) "استقبال إشعارات وتنبيهات النظام ومتابعة الحالة المالية" else "Receive system notifications and track financial status",
                                style = MaterialTheme.typography.bodySmall,
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(
                                color = BorderSubtle.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )

                            // Debt Reminders
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) "تذكيرات الديون المستحقة" else "Debt Reminders",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isArabic) "تنبيه بالمبالغ والديون المستحقة للسداد أو التحصيل" else "Alert for due receivables and payables",
                                        style = MaterialTheme.typography.bodySmall,
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
                                        text = if (isArabic) "تذكير التسجيل اليومي" else "Daily Reminder",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isArabic) "تذكير يومي لتسجيل المصروفات والحفاظ على الدقة" else "Daily reminder to log daily expenses and maintain accuracy",
                                        style = MaterialTheme.typography.bodySmall,
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
        }

        // 4. Backup & Restore Card (النسخ الاحتياطي)
        item {
            SettingsSectionCard(
                title = if (isArabic) "النسخ الاحتياطي" else "Backup & Restore",
                icon = Icons.Default.Backup
            ) {
                // Export Backup Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBackingUp && !isRestoring) {
                            viewModel.exportBackup(context)
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BurntOrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (isArabic) "تصدير نسخة احتياطية" else "Export Backup",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = BorderSubtle.copy(alpha = 0.6f)
                )

                // Restore Backup Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBackingUp && !isRestoring) {
                            pickBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4A5568)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Upload,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = if (isArabic) "استعادة نسخة احتياطية" else "Restore Backup",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Reset Data Card (إعادة ضبط البيانات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "إعادة ضبط البيانات" else "Reset Data",
                icon = Icons.Default.DeleteOutline
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showClearDataConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_data_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MutedExpenseTerracotta.copy(alpha = 0.05f),
                            contentColor = MutedExpenseTerracotta
                        ),
                        border = BorderStroke(1.dp, MutedExpenseTerracotta.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = if (isArabic) "مسح كافة البيانات" else "Clear All Data",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MutedExpenseTerracotta
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showRestoreConfirmDialog && pendingRestorePackage != null) {
        val pkg = pendingRestorePackage!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "استعادة نسخة احتياطية" else "Restore Backup",
                    color = TextPrimaryDark,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic)
                            "سيتم استبدال البيانات الحالية بالكامل بالبيانات المستعادة من النسخة الاحتياطية:\n" +
                            "• تاريخ النسخة: ${pkg.exportDate}\n" +
                            "• عدد المعاملات: ${pkg.transactions.size}\n" +
                            "• عدد الديون: ${pkg.debts.size}\n\n" +
                            "هل أنت متأكد من الاستمرار؟ لا يمكن التراجع عن هذا الإجراء."
                        else
                            "All current data will be completely replaced with data from backup:\n" +
                            "• Export Date: ${pkg.exportDate}\n" +
                            "• Transactions: ${pkg.transactions.size}\n" +
                            "• Debts: ${pkg.debts.size}\n\n" +
                            "Are you sure you want to proceed? This action cannot be undone.",
                        color = TextSecondaryBrown,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackup(context, pkg)
                        showRestoreConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                ) {
                    Text(if (isArabic) "استعادة واستبدال" else "Restore & Replace", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
        )
    }

    if (showRestoreErrorDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreErrorDialog = false },
            title = {
                Text(
                    text = if (isArabic) "خطأ في ملف النسخة الاحتياطية" else "Invalid Backup File",
                    color = MutedExpenseTerracotta,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = restoreErrorMessage,
                    color = TextSecondaryBrown,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRestoreErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                ) {
                    Text(if (isArabic) "حسناً" else "OK", color = Color.White)
                }
            }
        )
    }

    // ==================== Category Management & Reset Data Dialogs ====================

    if (showCategoryManagementDialog) {
        CategoryManagementDialog(
            expenseCategories = expenseCategories,
            incomeCategories = incomeCategories,
            customIconOverrides = categoryIcons,
            isArabic = isArabic,
            onDismiss = { showCategoryManagementDialog = false },
            onUpdateIcon = { catId, iconKey ->
                viewModel.setCategoryIcon(catId, iconKey)
            },
            onAddCustomCategory = { nameAr, nameEn, type, iconKey ->
                viewModel.addCustomCategory(nameAr, nameEn, type, iconKey)
            },
            onDeleteCustomCategory = { catId ->
                viewModel.deleteCustomCategory(catId)
            },
            onResetDefaults = {
                viewModel.resetCategoryIcons()
            }
        )
    }

    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إعادة ضبط البيانات" else "Reset All Data",
                    color = MutedExpenseTerracotta,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic)
                        "هل أنت متأكد تماماً من رغبتك في مسح كافة البيانات؟ سيتم حذف جميع المعاملات، الديون، وسجل السداد نهائياً والبدء من جديد. لا يمكن التراجع عن هذا الإجراء."
                    else
                        "Are you completely sure you want to reset all data? All transactions, debts, and payments will be permanently deleted. This cannot be undone.",
                    color = TextPrimaryDark,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedExpenseTerracotta),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isArabic) "مسح كافة البيانات" else "Clear All Data", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
