package com.example.ui.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.ui.DebtFilter
import com.example.ui.WalletViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.components.TransactionRowItem
import com.example.ui.notifications.NotificationCenterDialog
import com.example.ui.settings.BudgetSettingDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: WalletViewModel,
    onNavigateToDebts: (DebtFilter) -> Unit,
    onNavigateToAllTransactions: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onAddNewTransaction: () -> Unit = {},
    onAddNewDebt: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val totalOwedToMe by viewModel.totalOwedToMe.collectAsStateWithLifecycle()
    val totalIOwe by viewModel.totalIOwe.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val monthlyBudgetStatus by viewModel.monthlyBudgetStatus.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val smartNotifications by viewModel.smartNotifications.collectAsStateWithLifecycle()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val isDebtAlertEnabled by viewModel.isDebtAlertEnabled.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"
    val context = LocalContext.current

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationPermissionGranted()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Logo, App Name & Notification Bell
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "شعار التطبيق",
                        tint = BurntOrangePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                // Notification Bell Button with badge
                Box {
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "التنبيهات والإشعارات",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (isNotificationsEnabled && unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(BurntOrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Total Balance Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_balance_card"),
                shape = RoundedCornerShape(20.dp),
                color = DarkOliveCard
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.total_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnDarkSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${Formatters.formatMoney(totalBalance)} $currencySymbol",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.income),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDarkSecondary
                            )
                            Text(
                                text = "+${Formatters.formatMoney(totalIncome)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MutedIncomeGreenLight
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.expense),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDarkSecondary
                            )
                            Text(
                                text = "-${Formatters.formatMoney(totalExpense)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MutedExpenseLight
                            )
                        }
                    }
                }
            }
        }

        // 4. Monthly Budget & Alert Card
        item {
            MonthlyBudgetCard(
                budgetStatus = monthlyBudgetStatus,
                currencySymbol = currencySymbol,
                isArabic = isArabic,
                onOpenBudgetDialog = { showBudgetDialog = true }
            )
        }

        // 5. Section Header: Recent transactions + See all pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BurntOrangePrimary.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigateToAllTransactions() }
                        .testTag("see_all_transactions_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.see_all),
                            color = BurntOrangePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 7. Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = stringResource(R.string.no_transactions_yet),
                    description = stringResource(R.string.add_first_transaction),
                    actionText = if (isArabic) "إضافة عملية" else "Add Entry",
                    onAction = onAddNewTransaction,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(recentTransactions, key = { it.id }) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    currencySymbol = currencySymbol,
                    isArabic = isArabic,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp)) // Breathing room for bottom bar & FAB
        }
    }

    if (showNotificationsDialog) {
        NotificationCenterDialog(
            notifications = smartNotifications,
            isNotificationsEnabled = isNotificationsEnabled,
            isBudgetAlertEnabled = monthlyBudgetStatus.isAlertEnabled,
            isDebtAlertEnabled = isDebtAlertEnabled,
            isDailyReminderEnabled = isDailyReminderEnabled,
            isArabic = isArabic,
            onDismiss = { showNotificationsDialog = false },
            onToggleNotifications = { enabled ->
                viewModel.setNotificationsEnabled(enabled)
                if (enabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                    !com.example.notification.NotificationHelper.hasNotificationPermission(context)
                ) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onToggleBudgetAlerts = { viewModel.setBudgetAlertEnabled(it) },
            onToggleDebtAlerts = { viewModel.setDebtAlertEnabled(it) },
            onToggleDailyReminder = { viewModel.setDailyReminderEnabled(it) },
            onNavigateToBudget = { showBudgetDialog = true },
            onNavigateToDebts = { onNavigateToDebts(DebtFilter.ALL) }
        )
    }

    if (showBudgetDialog) {
        BudgetSettingDialog(
            currentBudget = monthlyBudgetStatus.budgetAmount,
            currencySymbol = currencySymbol,
            isArabic = isArabic,
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { amount ->
                viewModel.setMonthlyBudget(amount)
            },
            onClearBudget = {
                viewModel.setMonthlyBudget(0.0)
            }
        )
    }
}

