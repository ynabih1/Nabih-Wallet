package com.example.ui.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.SouthWest
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
import com.example.ui.components.Formatters
import com.example.ui.components.TransactionRowItem
import androidx.compose.ui.platform.LocalContext
import com.example.ui.notifications.NotificationCenterDialog
import com.example.ui.settings.BudgetSettingDialog
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.DarkOliveCardSecondary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@Composable
fun HomeScreen(
    viewModel: WalletViewModel,
    onNavigateToDebts: (DebtFilter) -> Unit,
    onNavigateToAllTransactions: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Nabih Wallet Logo + Title + Notification Bell
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Warm terracotta square logo
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BurntOrangePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "شعار التطبيق",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Box {
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "التنبيهات والإشعارات",
                            tint = TextPrimaryDark
                        )
                    }

                    if (isNotificationsEnabled && unreadNotificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 6.dp, end = 6.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(BurntOrangePrimary)
                                .border(1.5.dp, WarmBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Dark Olive Card: Total Balance & Income/Expense Sub-cards
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_balance_card"),
                color = DarkOliveCard,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.total_balance),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFB5B7AB)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = Formatters.formatMoney(totalBalance),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Sub-card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkOliveCardSecondary)
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.income),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFA5A79B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+${Formatters.formatMoney(totalIncome)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7CB88B)
                                )
                            }
                        }

                        // Expense Sub-card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DarkOliveCardSecondary)
                                .padding(vertical = 12.dp, horizontal = 14.dp)
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.expense),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFA5A79B)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "-${Formatters.formatMoney(totalExpense)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE58770)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Monthly Budget & Alert Card
        item {
            MonthlyBudgetCard(
                budgetStatus = monthlyBudgetStatus,
                currencySymbol = currencySymbol,
                isArabic = isArabic,
                onOpenBudgetDialog = { showBudgetDialog = true }
            )
        }

        // 4. Two Mini Summary Cards: Owed to me & I owe
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Owed to me
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToDebts(DebtFilter.OWED_TO_ME) }
                        .testTag("owed_to_me_card"),
                    color = WarmCardSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SouthWest,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.owed_to_me),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(totalOwedToMe),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                // I owe
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onNavigateToDebts(DebtFilter.I_OWE) }
                        .testTag("i_owe_card"),
                    color = WarmCardSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NorthEast,
                            contentDescription = null,
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.i_owe),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatMoney(totalIOwe),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }
            }
        }

        // 4. Section Header: Recent transactions + See all
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                TextButton(
                    onClick = onNavigateToAllTransactions,
                    modifier = Modifier.testTag("see_all_transactions_button")
                ) {
                    Text(
                        text = stringResource(R.string.see_all),
                        color = BurntOrangePrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 5. Recent Transactions List
        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = WarmCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_transactions_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryBrown
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.add_first_transaction),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMutedBrown
                        )
                    }
                }
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
            Spacer(modifier = Modifier.height(80.dp)) // Breathing room for bottom bar & FAB
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
            onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
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
