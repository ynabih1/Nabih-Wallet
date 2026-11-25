package com.nabih.wallet.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.CategoryExpenseSummary
import com.nabih.wallet.data.model.DebtWithPayments
import com.nabih.wallet.data.model.FinancialCalculator
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.notification.NotificationHelper
import com.nabih.wallet.pdf.PdfExporter
import com.nabih.wallet.ui.components.NabihTimePickerDialog
import com.nabih.wallet.ui.debts.AddEditDebtSheet
import com.nabih.wallet.ui.debts.DebtsScreen
import com.nabih.wallet.ui.debts.ManagePaymentsSheet
import com.nabih.wallet.ui.debts.PayDebtSheet
import com.nabih.wallet.ui.home.AllTransactionsScreen
import com.nabih.wallet.ui.home.HomeScreen
import com.nabih.wallet.ui.reports.CategoryDetailsScreen
import com.nabih.wallet.ui.reports.PdfPreviewBottomSheet
import com.nabih.wallet.ui.reports.ReportsScreen
import com.nabih.wallet.ui.settings.AddEditCategorySheet
import com.nabih.wallet.ui.settings.CategoryManagementScreen
import com.nabih.wallet.ui.settings.SettingsScreen
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.ui.transactions.TransactionBottomSheet

private enum class MainTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    EXPENSES(R.string.nav_expenses, Icons.Rounded.PieChart, Icons.Outlined.PieChart),
    DEBTS(R.string.nav_debts, Icons.Rounded.Group, Icons.Outlined.Group),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
}

private sealed class SubScreen {
    data object None : SubScreen()
    data object AllTransactions : SubScreen()
    data class CategoryDetails(val summary: CategoryExpenseSummary) : SubScreen()
    data object CategoryManagement : SubScreen()
}

private data class TransactionSheetRequest(
    val initialType: String,
    val editingItem: TransactionItem? = null
)

@Composable
fun NabihApp(viewModel: WalletViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val semantic = LocalWalletSemanticColors.current

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var subScreen by remember { mutableStateOf<SubScreen>(SubScreen.None) }
    var fabExpanded by remember { mutableStateOf(false) }

    // Modal sheets & dialogs state
    var transactionSheetRequest by remember { mutableStateOf<TransactionSheetRequest?>(null) }
    var quickAddCategoryType by remember { mutableStateOf<String?>(null) }
    var preselectedCategoryId by remember { mutableStateOf<Long?>(null) }

    var showAddDebtSheet by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtWithPayments?>(null) }
    var payingDebtId by remember { mutableStateOf<Long?>(null) }
    var managingPaymentsDebtId by remember { mutableStateOf<Long?>(null) }
    var deletingDebt by remember { mutableStateOf<DebtWithPayments?>(null) }

    var showPdfPreview by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    // Show snackbars reactively
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    // SAF & Permission Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.restoreBackupFromUri(uri)
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) viewModel.savePdfToUri(uri)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminderEnabled(true)
        } else {
            viewModel.postMessage("يرجى منح إذن الإشعارات لتفعيل التذكير اليومي")
        }
    }

    fun requestToggleDailyReminder(enabled: Boolean) {
        if (!enabled) {
            viewModel.setDailyReminderEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setDailyReminderEnabled(true)
        }
    }

    // Close FAB menu on back press if open
    BackHandler(enabled = fabExpanded) {
        fabExpanded = false
    }

    val showFab = subScreen == SubScreen.None && currentTab != MainTab.SETTINGS

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                MainTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            fabExpanded = false
                            subScreen = SubScreen.None
                            currentTab = tab
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val activeSub = subScreen) {
                SubScreen.None -> {
                    when (currentTab) {
                        MainTab.HOME -> HomeScreen(
                            uiState = uiState,
                            onOpenReminderSettings = {
                                if (uiState.preferences.dailyReminderEnabled) {
                                    showReminderTimePicker = true
                                } else {
                                    currentTab = MainTab.SETTINGS
                                }
                            },
                            onOpenDebtsTab = { currentTab = MainTab.DEBTS },
                            onOpenAllTransactions = { subScreen = SubScreen.AllTransactions },
                            onTransactionClick = { item ->
                                preselectedCategoryId = null
                                transactionSheetRequest = TransactionSheetRequest(
                                    initialType = item.entity.type,
                                    editingItem = item
                                )
                            }
                        )

                        MainTab.EXPENSES -> ReportsScreen(
                            uiState = uiState,
                            onSelectMonth = { viewModel.selectReportMonth(it) },
                            onCategoryClick = { summary ->
                                subScreen = SubScreen.CategoryDetails(summary)
                            },
                            onOpenPdfPreview = { showPdfPreview = true }
                        )

                        MainTab.DEBTS -> DebtsScreen(
                            uiState = uiState,
                            onPayDebt = { debt -> payingDebtId = debt.debt.id },
                            onManagePayments = { debt -> managingPaymentsDebtId = debt.debt.id },
                            onEditDebt = { debt -> editingDebt = debt },
                            onDeleteDebt = { debt -> deletingDebt = debt }
                        )

                        MainTab.SETTINGS -> SettingsScreen(
                            uiState = uiState,
                            onSelectThemeMode = { viewModel.setThemeMode(it) },
                            onToggleDynamicColors = { viewModel.setDynamicColorsEnabled(it) },
                            onSelectPalette = { viewModel.setColorPalette(it) },
                            onToggleDailyReminder = { requestToggleDailyReminder(it) },
                            onOpenReminderTimePicker = { showReminderTimePicker = true },
                            onOpenCategoryManagement = { subScreen = SubScreen.CategoryManagement },
                            onExportBackup = {
                                exportBackupLauncher.launch("nabih_wallet_backup_${System.currentTimeMillis()}.json")
                            },
                            onRestoreBackup = {
                                restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            }
                        )
                    }
                }

                SubScreen.AllTransactions -> {
                    AllTransactionsScreen(
                        transactions = uiState.transactions,
                        onBack = { subScreen = SubScreen.None },
                        onTransactionClick = { item ->
                            preselectedCategoryId = null
                            transactionSheetRequest = TransactionSheetRequest(
                                initialType = item.entity.type,
                                editingItem = item
                            )
                        }
                    )
                }

                is SubScreen.CategoryDetails -> {
                    val catTx = remember(uiState.transactions, uiState.selectedReportMonth, activeSub.summary.categoryId) {
                        uiState.transactions.filter { tx ->
                            tx.entity.type == TransactionType.EXPENSE &&
                                tx.entity.categoryId == activeSub.summary.categoryId &&
                                (uiState.selectedReportMonth == null ||
                                    FinancialCalculator.yearMonthOf(tx.entity.timestamp) == uiState.selectedReportMonth)
                        }
                    }
                    CategoryDetailsScreen(
                        categoryName = activeSub.summary.categoryName,
                        iconKey = activeSub.summary.iconKey,
                        colorKey = activeSub.summary.colorKey,
                        transactions = catTx,
                        onBack = { subScreen = SubScreen.None },
                        onTransactionClick = { item ->
                            preselectedCategoryId = null
                            transactionSheetRequest = TransactionSheetRequest(
                                initialType = item.entity.type,
                                editingItem = item
                            )
                        }
                    )
                }

                SubScreen.CategoryManagement -> {
                    CategoryManagementScreen(
                        expenseCategories = uiState.expenseCategories,
                        incomeCategories = uiState.incomeCategories,
                        onBack = { subScreen = SubScreen.None },
                        onAddCategory = { name, type, iconKey, colorKey ->
                            viewModel.addCategory(name, type, iconKey, colorKey)
                        },
                        onUpdateCategory = { viewModel.updateCategory(it) },
                        onDeleteCategory = { viewModel.deleteCategory(it) }
                    )
                }
            }

            // Scrim when FAB menu is open
            AnimatedVisibility(
                visible = fabExpanded && showFab,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { fabExpanded = false }
                )
            }

            // Expressive Squircle FAB + Spring FAB Menu (Bottom-End -> Bottom-Left in RTL)
            if (showFab) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FabMenuPillItem(
                                label = stringResource(R.string.fab_add_debt),
                                icon = Icons.Rounded.Handshake,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                testTag = "fab_menu_debt",
                                onClick = {
                                    fabExpanded = false
                                    showAddDebtSheet = true
                                }
                            )
                            FabMenuPillItem(
                                label = stringResource(R.string.fab_add_income),
                                icon = Icons.Rounded.TrendingUp,
                                containerColor = semantic.incomeContainer,
                                contentColor = semantic.onIncomeContainer,
                                testTag = "fab_menu_income",
                                onClick = {
                                    fabExpanded = false
                                    preselectedCategoryId = null
                                    transactionSheetRequest = TransactionSheetRequest(
                                        initialType = TransactionType.INCOME
                                    )
                                }
                            )
                            FabMenuPillItem(
                                label = stringResource(R.string.fab_add_expense),
                                icon = Icons.Rounded.ReceiptLong,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                testTag = "fab_menu_expense",
                                onClick = {
                                    fabExpanded = false
                                    preselectedCategoryId = null
                                    transactionSheetRequest = TransactionSheetRequest(
                                        initialType = TransactionType.EXPENSE
                                    )
                                }
                            )
                        }
                    }

                    val fabCorner by animateDpAsState(
                        targetValue = if (fabExpanded) 28.dp else 16.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "fab_corner"
                    )
                    val iconRotation by animateFloatAsState(
                        targetValue = if (fabExpanded) 45f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "fab_rotation"
                    )
                    val fabContainerColor by animateColorAsState(
                        targetValue = if (fabExpanded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        label = "fab_bg"
                    )
                    val fabContentColor by animateColorAsState(
                        targetValue = if (fabExpanded) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        label = "fab_fg"
                    )

                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            fabExpanded = !fabExpanded
                        },
                        shape = RoundedCornerShape(fabCorner),
                        containerColor = fabContainerColor,
                        contentColor = fabContentColor,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 6.dp
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("main_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = if (fabExpanded) {
                                stringResource(R.string.cd_close_add_menu)
                            } else {
                                stringResource(R.string.cd_open_add_menu)
                            },
                            modifier = Modifier
                                .size(26.dp)
                                .rotate(iconRotation)
                        )
                    }
                }
            }
        }
    }

    // --- Modal Bottom Sheets & Dialogs ---

    transactionSheetRequest?.let { req ->
        TransactionBottomSheet(
            initialType = req.initialType,
            editingTransaction = req.editingItem,
            expenseCategories = uiState.expenseCategories,
            incomeCategories = uiState.incomeCategories,
            preselectedCategoryId = preselectedCategoryId,
            onDismiss = {
                transactionSheetRequest = null
                preselectedCategoryId = null
            },
            onRequestAddCategory = { type ->
                quickAddCategoryType = type
            },
            onSave = { type, amount, categoryId, timestamp, note ->
                viewModel.saveTransaction(
                    editingId = req.editingItem?.entity?.id,
                    type = type,
                    amount = amount,
                    categoryId = categoryId,
                    timestamp = timestamp,
                    note = note
                )
                transactionSheetRequest = null
                preselectedCategoryId = null
            },
            onDelete = if (req.editingItem != null) {
                { id ->
                    viewModel.deleteTransaction(id)
                    transactionSheetRequest = null
                    preselectedCategoryId = null
                }
            } else {
                null
            }
        )
    }

    quickAddCategoryType?.let { type ->
        AddEditCategorySheet(
            initialType = type,
            editingCategory = null,
            onDismiss = { quickAddCategoryType = null },
            onSave = { name, catType, iconKey, colorKey ->
                viewModel.addCategory(name, catType, iconKey, colorKey) { newId ->
                    preselectedCategoryId = newId
                }
                quickAddCategoryType = null
            },
            onDelete = null
        )
    }

    if (showAddDebtSheet || editingDebt != null) {
        AddEditDebtSheet(
            editingDebt = editingDebt,
            onDismiss = {
                showAddDebtSheet = false
                editingDebt = null
            },
            onSave = { personName, type, amount ->
                viewModel.saveDebt(
                    editingId = editingDebt?.debt?.id,
                    personName = personName,
                    type = type,
                    originalAmount = amount
                ) { success ->
                    if (success) {
                        showAddDebtSheet = false
                        editingDebt = null
                    }
                }
            }
        )
    }

    payingDebtId?.let { debtId ->
        val latestDebt = uiState.debts.firstOrNull { it.debt.id == debtId }
        if (latestDebt != null) {
            PayDebtSheet(
                debtItem = latestDebt,
                onDismiss = { payingDebtId = null },
                onConfirmPayment = { amount ->
                    viewModel.addDebtPayment(debtId, amount) {
                        payingDebtId = null
                    }
                }
            )
        } else {
            payingDebtId = null
        }
    }

    managingPaymentsDebtId?.let { debtId ->
        val latestDebt = uiState.debts.firstOrNull { it.debt.id == debtId }
        if (latestDebt != null) {
            ManagePaymentsSheet(
                debtItem = latestDebt,
                onDismiss = { managingPaymentsDebtId = null },
                onDeletePayment = { paymentId ->
                    viewModel.deleteDebtPayment(paymentId)
                }
            )
        } else {
            managingPaymentsDebtId = null
        }
    }

    deletingDebt?.let { debt ->
        AlertDialog(
            onDismissRequest = { deletingDebt = null },
            title = { Text("حذف الدين", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من حذف دين \"${debt.debt.personName}\" وجميع دفعاته المسجلة؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debt.debt.id)
                        deletingDebt = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.delete_label),
                        color = semantic.expense,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingDebt = null }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        )
    }

    if (showPdfPreview) {
        val report = remember(uiState.selectedReportMonth, uiState.expensesByCategory, uiState.reportTotalExpense) {
            viewModel.buildCurrentPdfReport()
        }
        PdfPreviewBottomSheet(
            report = report,
            onDismiss = { showPdfPreview = false },
            onSharePdf = {
                showPdfPreview = false
                viewModel.exportAndSharePdf { file ->
                    PdfExporter.sharePdfFile(context, file)
                }
            },
            onSavePdfDirect = {
                showPdfPreview = false
                savePdfLauncher.launch("nabih_wallet_expenses_${System.currentTimeMillis()}.pdf")
            }
        )
    }

    if (showReminderTimePicker) {
        NabihTimePickerDialog(
            initialHour = uiState.preferences.reminderHour,
            initialMinute = uiState.preferences.reminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.setReminderTime(hour, minute)
                showReminderTimePicker = false
            }
        )
    }
}

@Composable
private fun FabMenuPillItem(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}
