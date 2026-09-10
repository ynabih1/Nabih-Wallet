package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.debts.DebtDialog
import com.example.ui.debts.DebtsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface
import com.example.ui.theme.WarmOffWhiteSurface
import com.example.ui.transactions.AllTransactionsScreen
import com.example.ui.transactions.TransactionDialog

object NabihDestinations {
    const val HOME = "home"
    const val REPORTS = "reports"
    const val DEBTS = "debts"
    const val SETTINGS = "settings"
    const val ALL_TRANSACTIONS = "all_transactions"
}

@Composable
fun NabihApp(
    viewModel: WalletViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val language by viewModel.language.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var currentRoute by remember { mutableStateOf(NabihDestinations.HOME) }

    // Dialog States
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var showDebtDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }

    var showQuickAddChoice by remember { mutableStateOf(false) }

    // Collect feedback messages (e.g. PDF saved successfully, error notifications)
    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            when (feedback) {
                is UserFeedback.Success -> snackbarHostState.showSnackbar(feedback.message)
                is UserFeedback.Error -> snackbarHostState.showSnackbar("⚠️ ${feedback.message}")
            }
        }
    }

    val context = LocalContext.current
    val registryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current

    val localizedContext = remember(language, context) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        context.createConfigurationContext(config)
    }

    // Force Arabic RTL layout when language is Arabic
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val compositionProviders = remember(localizedContext, layoutDirection, registryOwner) {
        val list = mutableListOf<androidx.compose.runtime.ProvidedValue<*>>(
            LocalContext provides localizedContext,
            LocalLayoutDirection provides layoutDirection
        )
        if (registryOwner != null) {
            list.add(androidx.activity.compose.LocalActivityResultRegistryOwner provides registryOwner)
        }
        list.toTypedArray()
    }

    CompositionLocalProvider(*compositionProviders) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // Show bottom navigation on top-level screens
                if (currentRoute != NabihDestinations.ALL_TRANSACTIONS) {
                    NabihBottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            currentRoute = route
                            navController.navigate(route) {
                                popUpTo(NabihDestinations.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onFabClick = {
                            showQuickAddChoice = true
                        }
                    )
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = NabihDestinations.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable(NabihDestinations.HOME) {
                    currentRoute = NabihDestinations.HOME
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToDebts = { filter ->
                            viewModel.setDebtFilter(filter)
                            currentRoute = NabihDestinations.DEBTS
                            navController.navigate(NabihDestinations.DEBTS) {
                                popUpTo(NabihDestinations.HOME) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onNavigateToAllTransactions = {
                            currentRoute = NabihDestinations.ALL_TRANSACTIONS
                            navController.navigate(NabihDestinations.ALL_TRANSACTIONS)
                        },
                        onEditTransaction = { tx ->
                            transactionToEdit = tx
                            showTransactionDialog = true
                        }
                    )
                }

                composable(NabihDestinations.REPORTS) {
                    currentRoute = NabihDestinations.REPORTS
                    ReportsScreen(viewModel = viewModel)
                }

                composable(NabihDestinations.DEBTS) {
                    currentRoute = NabihDestinations.DEBTS
                    DebtsScreen(
                        viewModel = viewModel,
                        onAddNewDebt = {
                            debtToEdit = null
                            showDebtDialog = true
                        },
                        onEditDebt = { debt ->
                            debtToEdit = debt
                            showDebtDialog = true
                        }
                    )
                }

                composable(NabihDestinations.SETTINGS) {
                    currentRoute = NabihDestinations.SETTINGS
                    SettingsScreen(viewModel = viewModel)
                }

                composable(NabihDestinations.ALL_TRANSACTIONS) {
                    currentRoute = NabihDestinations.ALL_TRANSACTIONS
                    AllTransactionsScreen(
                        viewModel = viewModel,
                        onBack = {
                            currentRoute = NabihDestinations.HOME
                            navController.popBackStack()
                        },
                        onEditTransaction = { tx ->
                            transactionToEdit = tx
                            showTransactionDialog = true
                        }
                    )
                }
            }
        }

        // Quick Add Choice Dialog (+)
        if (showQuickAddChoice) {
            AlertDialog(
                onDismissRequest = { showQuickAddChoice = false },
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(horizontal = 8.dp),
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
                title = {
                    Text(
                        text = if (isArabic) "إضافة جديدة" else "Add New",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    showQuickAddChoice = false
                                    transactionToEdit = null
                                    showTransactionDialog = true
                                }
                                .testTag("quick_add_transaction"),
                            color = WarmCardSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BurntOrangePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "إضافة عملية" else "Add Transaction",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = if (isArabic) "تسجيل مصروف أو دخل جديد" else "Record new expense or income",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryBrown
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    showQuickAddChoice = false
                                    debtToEdit = null
                                    showDebtDialog = true
                                }
                                .testTag("quick_add_debt"),
                            color = WarmCardSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BurntOrangePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isArabic) "إضافة دين" else "Add Debt",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimaryDark
                                    )
                                    Text(
                                        text = if (isArabic) "تسجيل دين مستحق لي أو علي" else "Record owed or borrowed debt",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondaryBrown
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showQuickAddChoice = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        // Add / Edit Transaction Dialog
        if (showTransactionDialog) {
            TransactionDialog(
                initialTransaction = transactionToEdit,
                existingTransactions = allTransactions,
                customExpenseCategories = viewModel.getAllExpenseCategories(),
                customIncomeCategories = viewModel.getAllIncomeCategories(),
                isArabic = isArabic,
                currencySymbol = currencySymbol,
                onDismiss = {
                    showTransactionDialog = false
                    transactionToEdit = null
                },
                onSave = { id, type, amount, category, paymentMethod, dateMillis, notes, isPinned, receiptUri, receiptMimeType ->
                    viewModel.saveTransaction(
                        id = id,
                        type = type,
                        amount = amount,
                        category = category,
                        paymentMethod = paymentMethod,
                        dateMillis = dateMillis,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = receiptUri,
                        receiptMimeType = receiptMimeType
                    )
                    showTransactionDialog = false
                    transactionToEdit = null
                },
                onDelete = { tx ->
                    viewModel.deleteTransaction(tx)
                    showTransactionDialog = false
                    transactionToEdit = null
                }
            )
        }

        // Add / Edit Debt Dialog
        if (showDebtDialog) {
            DebtDialog(
                initialDebt = debtToEdit,
                isArabic = isArabic,
                currencySymbol = currencySymbol,
                onDismiss = {
                    showDebtDialog = false
                    debtToEdit = null
                },
                onSave = { id, type, personName, totalAmount, lentDateMillis, dueDateMillis, notes, enableReminder ->
                    viewModel.saveDebt(
                        id = id,
                        type = type,
                        personName = personName,
                        totalAmount = totalAmount,
                        lentDateMillis = lentDateMillis,
                        dueDateMillis = dueDateMillis,
                        notes = notes,
                        enableReminder = enableReminder
                    )
                    showDebtDialog = false
                    debtToEdit = null
                }
            )
        }
    }
}

@Composable
fun NabihBottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onFabClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bottom_nav_bar"),
        color = WarmOffWhiteSurface,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Home Tab
            BottomNavItem(
                icon = if (currentRoute == NabihDestinations.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                label = stringResource(R.string.nav_home),
                isSelected = currentRoute == NabihDestinations.HOME,
                onClick = { onNavigate(NabihDestinations.HOME) },
                testTag = "nav_item_home",
                modifier = Modifier.weight(1f)
            )

            // 2. Reports Tab
            BottomNavItem(
                icon = if (currentRoute == NabihDestinations.REPORTS) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                label = stringResource(R.string.nav_reports),
                isSelected = currentRoute == NabihDestinations.REPORTS,
                onClick = { onNavigate(NabihDestinations.REPORTS) },
                testTag = "nav_item_reports",
                modifier = Modifier.weight(1f)
            )

            // Center FAB (+)
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(BurntOrangePrimary)
                        .clickable { onFabClick() }
                        .testTag("main_add_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_add),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // 3. Debts Tab
            BottomNavItem(
                icon = if (currentRoute == NabihDestinations.DEBTS) Icons.Filled.Group else Icons.Outlined.Group,
                label = stringResource(R.string.nav_debts),
                isSelected = currentRoute == NabihDestinations.DEBTS,
                onClick = { onNavigate(NabihDestinations.DEBTS) },
                testTag = "nav_item_debts",
                modifier = Modifier.weight(1f)
            )

            // 4. Settings Tab
            BottomNavItem(
                icon = if (currentRoute == NabihDestinations.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                label = stringResource(R.string.nav_settings),
                isSelected = currentRoute == NabihDestinations.SETTINGS,
                onClick = { onNavigate(NabihDestinations.SETTINGS) },
                testTag = "nav_item_settings",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isSelected) BurntOrangePrimary else TextSecondaryBrown
    val textColor = if (isSelected) BurntOrangePrimary else TextSecondaryBrown
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}
