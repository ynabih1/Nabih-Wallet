package com.example.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.debts.DebtsScreen
import com.example.ui.home.HomeScreen
import com.example.ui.reports.ReportsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AppBackground
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.ui.transactions.AllTransactionsScreen
import com.example.ui.transactions.TransactionDialog
import java.util.Locale

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
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }
    var dialogInitialMode by remember { mutableStateOf("EXPENSE") }

    // Collect feedback messages (e.g. PDF saved successfully, error notifications)
    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            when (feedback) {
                is UserFeedback.Success -> {
                    if (feedback.actionLabel != null && feedback.action != null) {
                        val result = snackbarHostState.showSnackbar(
                            message = feedback.message,
                            actionLabel = feedback.actionLabel,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            feedback.action.invoke()
                        }
                    } else {
                        snackbarHostState.showSnackbar(feedback.message)
                    }
                }
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
            containerColor = AppBackground,
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ElevatedSurface,
                        contentColor = TextPrimary,
                        actionColor = XBlue
                    )
                }
            },
            bottomBar = {
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
                            transactionToEdit = null
                            debtToEdit = null
                            dialogInitialMode = if (currentRoute == NabihDestinations.DEBTS) "DEBT" else "EXPENSE"
                            showTransactionDialog = true
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
                        onNavigateToAllTransactions = {
                            currentRoute = NabihDestinations.ALL_TRANSACTIONS
                            navController.navigate(NabihDestinations.ALL_TRANSACTIONS)
                        },
                        onEditTransaction = { tx ->
                            debtToEdit = null
                            transactionToEdit = tx
                            dialogInitialMode = tx.type
                            showTransactionDialog = true
                        },
                        onAddNewTransaction = {
                            debtToEdit = null
                            transactionToEdit = null
                            dialogInitialMode = "EXPENSE"
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
                            transactionToEdit = null
                            debtToEdit = null
                            dialogInitialMode = "DEBT"
                            showTransactionDialog = true
                        },
                        onEditDebt = { debt ->
                            transactionToEdit = null
                            debtToEdit = debt
                            dialogInitialMode = "DEBT"
                            showTransactionDialog = true
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
                            debtToEdit = null
                            transactionToEdit = tx
                            dialogInitialMode = tx.type
                            showTransactionDialog = true
                        }
                    )
                }
            }
        }

        // Add / Edit Transaction or Debt Dialog
        if (showTransactionDialog) {
            TransactionDialog(
                initialTransaction = transactionToEdit,
                initialDebt = debtToEdit,
                initialMode = dialogInitialMode,
                existingTransactions = allTransactions,
                customExpenseCategories = viewModel.getAllExpenseCategories(),
                customIncomeCategories = viewModel.getAllIncomeCategories(),
                isArabic = isArabic,
                currencySymbol = currencySymbol,
                onDismiss = {
                    showTransactionDialog = false
                    transactionToEdit = null
                    debtToEdit = null
                },
                onSave = { id, type, amount, category, _, dateMillis, endDateMillis, notes, isPinned, receiptUri, receiptMimeType ->
                    viewModel.saveTransaction(
                        id = id,
                        type = type,
                        amount = amount,
                        category = category,
                        paymentMethod = "",
                        dateMillis = dateMillis,
                        endDateMillis = endDateMillis,
                        notes = notes,
                        isPinned = isPinned,
                        receiptUri = receiptUri,
                        receiptMimeType = receiptMimeType
                    )
                    showTransactionDialog = false
                    transactionToEdit = null
                    debtToEdit = null
                },
                onDelete = { tx ->
                    viewModel.deleteTransaction(tx)
                    showTransactionDialog = false
                    transactionToEdit = null
                    debtToEdit = null
                },
                onSaveDebt = { id, type, personName, totalAmount, lentDateMillis, dueDateMillis, notes ->
                    viewModel.saveDebt(
                        id = id,
                        type = type,
                        personName = personName,
                        totalAmount = totalAmount,
                        lentDateMillis = lentDateMillis,
                        dueDateMillis = dueDateMillis,
                        notes = notes
                    )
                    showTransactionDialog = false
                    transactionToEdit = null
                    debtToEdit = null
                },
                onDeleteDebt = { debt ->
                    viewModel.deleteDebt(debt)
                    showTransactionDialog = false
                    transactionToEdit = null
                    debtToEdit = null
                },
                onAddCustomCategory = { nameAr, nameEn, type, iconKey ->
                    viewModel.addCustomCategory(nameAr, nameEn, type, iconKey)
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
        color = AppBackground,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thin top border #2F3336 in X style
            HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                // Balanced Left & Right Wings with equal weight so the + button sits at the exact 50% center
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start Wing (Right in RTL): Home + Reports
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            icon = if (currentRoute == NabihDestinations.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            label = stringResource(R.string.nav_home),
                            isSelected = currentRoute == NabihDestinations.HOME,
                            onClick = { onNavigate(NabihDestinations.HOME) },
                            testTag = "nav_item_home",
                            modifier = Modifier.weight(1f)
                        )

                        BottomNavItem(
                            icon = if (currentRoute == NabihDestinations.REPORTS) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                            label = stringResource(R.string.nav_reports),
                            isSelected = currentRoute == NabihDestinations.REPORTS,
                            onClick = { onNavigate(NabihDestinations.REPORTS) },
                            testTag = "nav_item_reports",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Exact Center Space reserved for the + button
                    Spacer(modifier = Modifier.width(68.dp))

                    // End Wing (Left in RTL): Debts + Settings
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavItem(
                            icon = if (currentRoute == NabihDestinations.DEBTS) Icons.Filled.Group else Icons.Outlined.Group,
                            label = stringResource(R.string.nav_debts),
                            isSelected = currentRoute == NabihDestinations.DEBTS,
                            onClick = { onNavigate(NabihDestinations.DEBTS) },
                            testTag = "nav_item_debts",
                            modifier = Modifier.weight(1f)
                        )

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

                // Exact Center (+) Button: #1DB954 circle with black + icon
                Surface(
                    shape = CircleShape,
                    shadowElevation = 0.dp,
                    color = SpotifyGreen,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onFabClick() }
                        .testTag("main_add_fab")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.nav_add),
                            tint = OnSpotifyGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
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
    // X-style icons-only navigation: filled white when selected, outlined muted gray (#71767B) when unselected
    val iconColor = if (isSelected) TextPrimary else TextMuted

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
