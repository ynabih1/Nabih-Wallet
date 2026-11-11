package com.example.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.backup.BackupPackage
import com.example.ui.WalletViewModel
import com.example.ui.category.CategoryManagementDialog
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: WalletViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isBackingUp by viewModel.isBackingUp.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoring.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val categoryIcons by viewModel.categoryIcons.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showRestoreErrorDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf("") }
    var pendingRestorePackage by remember { mutableStateOf<BackupPackage?>(null) }

    var showCategoryManagementDialog by remember { mutableStateOf(false) }

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
            viewModel.validateBackupFileAsync(
                uri = uri,
                onSuccess = { pkg ->
                    pendingRestorePackage = pkg
                    showRestoreConfirmDialog = true
                },
                onFailure = { msg ->
                    restoreErrorMessage = msg
                    showRestoreErrorDialog = true
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // 1. Categories Card (الفئات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "الفئات" else "Categories",
                icon = Icons.Outlined.Palette
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (isArabic) "تخصيص الأسماء والأيقونات" else "Customize names and icons",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Button(
                        onClick = { showCategoryManagementDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("manage_categories_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = OnSpotifyGreen
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (isArabic) "إدارة وتخصيص الفئات" else "Manage & Customize Categories",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSpotifyGreen
                        )
                    }
                }
            }
        }

        // 2. Backup & Restore Card (النسخ الاحتياطي)
        item {
            SettingsSectionCard(
                title = if (isArabic) "النسخ الاحتياطي" else "Backup & Restore",
                icon = Icons.Outlined.Backup
            ) {
                // Export Backup Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isBackingUp && !isRestoring) {
                            viewModel.exportBackup(context)
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBackingUp) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = XBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "تصدير نسخة احتياطية" else "Export Backup",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isArabic) "حفظ البيانات على الجهاز" else "Save data to device",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = DividerBorderColor,
                    thickness = 0.6.dp
                )

                // Restore Backup Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isBackingUp && !isRestoring) {
                            pickBackupLauncher.launch(arrayOf("*/*"))
                        },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = XBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Upload,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "استعادة نسخة احتياطية" else "Restore Backup",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isArabic) "استرجاع البيانات المحفوظة" else "Restore saved data",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Reset Data Card (إعادة ضبط البيانات)
        item {
            SettingsSectionCard(
                title = if (isArabic) "إعادة ضبط البيانات" else "Reset Data",
                icon = Icons.Outlined.DeleteOutline
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "حذف كافة البيانات المسجلة" else "Delete all recorded data",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    OutlinedButton(
                        onClick = { viewModel.clearAllData() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("clear_all_data_btn"),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = ExpenseRed
                        ),
                        border = BorderStroke(1.dp, DividerBorderColor)
                    ) {
                        Text(
                            text = if (isArabic) "مسح كافة البيانات" else "Clear All Data",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(112.dp))
        }
    }

    if (showRestoreConfirmDialog && pendingRestorePackage != null) {
        val pkg = pendingRestorePackage!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "استعادة نسخة احتياطية" else "Restore Backup",
                    color = TextPrimary,
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
                        color = TextSecondary,
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
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(if (isArabic) "استعادة واستبدال" else "Restore & Replace", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRestoreConfirmDialog = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        )
    }

    if (showRestoreErrorDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreErrorDialog = false },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "خطأ في ملف النسخة الاحتياطية" else "Invalid Backup File",
                    color = ExpenseRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = restoreErrorMessage,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRestoreErrorDialog = false },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(if (isArabic) "حسناً" else "OK", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

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
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}
