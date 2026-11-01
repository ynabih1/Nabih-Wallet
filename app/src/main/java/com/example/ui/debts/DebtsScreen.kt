package com.example.ui.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.model.DebtWithPayments
import com.example.model.DueStatus
import com.example.ui.DebtFilter
import com.example.ui.WalletViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.MutedExpenseLight
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.MutedIncomeGreenLight
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextOnDarkSecondary
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmAmberWarning
import com.example.ui.theme.WarmCardSurface
import java.util.Locale
import kotlin.math.abs

@Composable
fun DebtsScreen(
    viewModel: WalletViewModel,
    onAddNewDebt: () -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalOwedToMe by viewModel.totalOwedToMe.collectAsStateWithLifecycle()
    val totalIOwe by viewModel.totalIOwe.collectAsStateWithLifecycle()
    val netDebtBalance by viewModel.netDebtBalance.collectAsStateWithLifecycle()
    val currentFilter by viewModel.debtFilter.collectAsStateWithLifecycle()
    val allDebts by viewModel.allDebtsWithPayments.collectAsStateWithLifecycle()
    val filteredDebts by viewModel.filteredDebts.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var searchQuery by remember { mutableStateOf("") }
    var selectedDebtForDetails by remember { mutableStateOf<DebtWithPayments?>(null) }
    var selectedDebtForRepayment by remember { mutableStateOf<DebtWithPayments?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }
    var debtForFullSettleConfirm by remember { mutableStateOf<DebtWithPayments?>(null) }

    val displayedDebts = remember(filteredDebts, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredDebts
        } else {
            filteredDebts.filter {
                it.debt.personName.contains(searchQuery, ignoreCase = true) ||
                    it.debt.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header: Clean Title Only
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.debts_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimaryDark
                )
            }
        }

        // 2. Hero Summary Card (Consistent with Home Total Balance Card)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { viewModel.setDebtFilter(DebtFilter.ALL) },
                shape = RoundedCornerShape(22.dp),
                color = DarkOliveCard,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "صافي موقف الديون" else "Net Debt Position",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextOnDarkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val netPrefix = when {
                                netDebtBalance > 0.005 -> "+"
                                netDebtBalance < -0.005 -> "-"
                                else -> ""
                            }
                            val netColor = when {
                                netDebtBalance > 0.005 -> MutedIncomeGreenLight
                                netDebtBalance < -0.005 -> MutedExpenseLight
                                else -> Color.White
                            }
                            Text(
                                text = "$netPrefix${Formatters.formatMoney(abs(netDebtBalance))} $currencySymbol",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = netColor
                            )
                        }

                        val netStatusText = when {
                            netDebtBalance > 0.005 -> if (isArabic) "لصالحك" else "In your favor"
                            netDebtBalance < -0.005 -> if (isArabic) "التزام عليك" else "You owe net"
                            else -> if (isArabic) "متوازن" else "Balanced"
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = netStatusText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Owed to me Sub-Card (Showing label + debt amount)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setDebtFilter(
                                        if (currentFilter == DebtFilter.OWED_TO_ME) DebtFilter.ALL else DebtFilter.OWED_TO_ME
                                    )
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentFilter == DebtFilter.OWED_TO_ME) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MutedIncomeGreenLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = MutedIncomeGreenLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "مستحق لي" else "Owed to me",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextOnDarkSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${Formatters.formatMoney(totalOwedToMe)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalOwedToMe > 0.005) MutedIncomeGreenLight else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // I owe Sub-Card (Showing label + debt amount)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setDebtFilter(
                                        if (currentFilter == DebtFilter.I_OWE) DebtFilter.ALL else DebtFilter.I_OWE
                                    )
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (currentFilter == DebtFilter.I_OWE) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MutedExpenseLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = MutedExpenseLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "مستحق علي" else "I owe",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextOnDarkSecondary,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${Formatters.formatMoney(totalIOwe)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalIOwe > 0.005) MutedExpenseLight else Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Search Bar (only when multiple debts exist)
        if (allDebts.size > 2) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondaryBrown,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    placeholder = {
                        Text(
                            text = if (isArabic) "ابحث باسم الشخص..." else "Search by person name...",
                            fontSize = 12.5.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = BurntOrangePrimary,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 4. Debts List
        if (displayedDebts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Handshake,
                    title = stringResource(R.string.no_debts_yet),
                    description = if (isArabic) "لا توجد ديون أو مستحقات مالية مسجلة في هذا القسم" else "No debts or receivables recorded in this section",
                    actionText = if (isArabic) "إضافة دين جديد" else "Add New Debt",
                    onAction = onAddNewDebt,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(displayedDebts, key = { it.debt.id }) { debtItem ->
                DebtCardItem(
                    debtItem = debtItem,
                    currencySymbol = currencySymbol,
                    isArabic = isArabic,
                    onClick = { selectedDebtForDetails = debtItem },
                    onLogRepayment = { selectedDebtForRepayment = debtItem },
                    onQuickFullSettle = { debtForFullSettleConfirm = debtItem }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    // Detail & Payment History Dialog
    selectedDebtForDetails?.let { debtWithPayments ->
        val currentDebtItem = allDebts.find { it.debt.id == debtWithPayments.debt.id } ?: debtWithPayments
        DebtDetailsDialog(
            debtItem = currentDebtItem,
            currencySymbol = currencySymbol,
            isArabic = isArabic,
            onDismiss = { selectedDebtForDetails = null },
            onLogRepayment = {
                selectedDebtForRepayment = currentDebtItem
            },
            onEdit = {
                selectedDebtForDetails = null
                onEditDebt(currentDebtItem.debt)
            },
            onDelete = {
                selectedDebtForDetails = null
                debtToDelete = currentDebtItem.debt
            },
            onDeletePayment = { payment ->
                viewModel.deleteDebtPayment(payment)
            }
        )
    }

    // Partial / Custom Repayment Dialog
    selectedDebtForRepayment?.let { debtWithPayments ->
        LogRepaymentDialog(
            debtItem = debtWithPayments,
            currencySymbol = currencySymbol,
            isArabic = isArabic,
            onDismiss = { selectedDebtForRepayment = null },
            onConfirm = { amount, note ->
                viewModel.logDebtPayment(debtWithPayments.debt.id, amount, note)
                selectedDebtForRepayment = null
            }
        )
    }

    // Quick Full Settlement Confirmation Dialog
    debtForFullSettleConfirm?.let { debtWithPayments ->
        AlertDialog(
            onDismissRequest = { debtForFullSettleConfirm = null },
            title = {
                Text(
                    text = if (isArabic) "تأكيد السداد الكامل" else "Confirm Full Settlement",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = if (isArabic) {
                        "هل تريد تسجيل سداد كامل المبلغ المتبقي ${Formatters.formatMoney(debtWithPayments.remainingAmount)} $currencySymbol لحساب \"${debtWithPayments.debt.personName}\"؟"
                    } else {
                        "Record full payment of the remaining ${Formatters.formatMoney(debtWithPayments.remainingAmount)} $currencySymbol for \"${debtWithPayments.debt.personName}\"?"
                    },
                    color = TextSecondaryBrown,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logDebtPayment(
                            debtId = debtWithPayments.debt.id,
                            amount = debtWithPayments.remainingAmount,
                            note = if (isArabic) "سداد كامل" else "Full settlement"
                        )
                        debtForFullSettleConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedIncomeGreen)
                ) {
                    Text(
                        text = if (isArabic) "تأكيد السداد" else "Confirm",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { debtForFullSettleConfirm = null }) {
                    Text(text = stringResource(R.string.cancel), color = TextSecondaryBrown)
                }
            },
            containerColor = WarmCardSurface
        )
    }

    // Delete Confirmation Dialog
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(text = stringResource(R.string.delete_debt), fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
            text = { Text(text = stringResource(R.string.confirm_delete), color = TextSecondaryBrown) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedExpenseTerracotta)
                ) {
                    Text(text = stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) {
                    Text(text = stringResource(R.string.cancel), color = TextSecondaryBrown)
                }
            },
            containerColor = WarmCardSurface
        )
    }
}

@Composable
fun DebtFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) BurntOrangePrimary else MaterialTheme.colorScheme.surface
    val textColor = if (selected) Color.White else TextPrimaryDark
    val border = if (selected) BurntOrangePrimary else BorderSubtle

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(0.8.dp, border),
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DebtCardItem(
    debtItem: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onClick: () -> Unit,
    onLogRepayment: () -> Unit,
    onQuickFullSettle: (() -> Unit)? = null
) {
    val isOwedToMe = debtItem.debt.type == "OWED_TO_ME"
    val accentColor = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta

    val dueStatus = debtItem.getDueStatus()
    val dueText: String? = if (debtItem.isFullyPaid) {
        if (isArabic) "مسدد بالكامل ✓" else "Fully Paid ✓"
    } else {
        when (dueStatus) {
            is DueStatus.NoDueDate -> null
            is DueStatus.DueToday -> if (isArabic) "مستحق اليوم" else "Due today"
            is DueStatus.DueInDays -> if (isArabic) "مستحق خلال ${dueStatus.days} يوم" else "Due in ${dueStatus.days} days"
            is DueStatus.Overdue -> if (isArabic) "متأخر ${dueStatus.days} يوم" else "Overdue ${dueStatus.days} days"
        }
    }

    val dueTextColor = if (debtItem.isFullyPaid) {
        MutedIncomeGreen
    } else {
        when (dueStatus) {
            is DueStatus.Overdue -> MutedExpenseTerracotta
            is DueStatus.DueToday -> WarmAmberWarning
            else -> TextSecondaryBrown
        }
    }

    val dueBgColor = if (debtItem.isFullyPaid) {
        MutedIncomeGreenLight.copy(alpha = 0.6f)
    } else {
        when (dueStatus) {
            is DueStatus.Overdue -> MutedExpenseTerracotta.copy(alpha = 0.1f)
            is DueStatus.DueToday -> WarmAmberWarning.copy(alpha = 0.12f)
            else -> WarmCardSurface
        }
    }

    val pctInt = (debtItem.progress * 100).toInt().coerceIn(0, 100)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("debt_card_${debtItem.debt.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.8.dp, BorderSubtle),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            // Top Row: Avatar + Person Name + Remaining Amount (Semantic Color)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Initials Circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = debtItem.getInitials(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = debtItem.debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (dueText != null) {
                            Spacer(modifier = Modifier.height(4.dp))

                            // Due status pill (hidden when no due date is set)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = dueBgColor
                            ) {
                                Text(
                                    text = dueText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = dueTextColor,
                                    fontWeight = if (debtItem.isFullyPaid || dueStatus is DueStatus.Overdue || dueStatus is DueStatus.DueToday) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount Section
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${Formatters.formatMoney(debtItem.remainingAmount)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isArabic) "من إجمالي" else "of"} ${Formatters.formatMoney(debtItem.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar + Paid Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) {
                        "المسدد: ${Formatters.formatMoney(debtItem.repaidAmount)} $currencySymbol"
                    } else {
                        "Paid: ${Formatters.formatMoney(debtItem.repaidAmount)} $currencySymbol"
                    },
                    fontSize = 11.sp,
                    color = TextSecondaryBrown,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$pctInt%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            LinearProgressIndicator(
                progress = { debtItem.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = Color(0xFFE8E5DC)
            )

            // Bottom action row: Harmonious Equal-Width Repayment & Full Settlement Buttons
            if (!debtItem.isFullyPaid) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Partial / Custom Repayment Button (Secondary Outlined/Tinted Style)
                    Surface(
                        shape = RoundedCornerShape(11.dp),
                        color = WarmCardSurface,
                        border = BorderStroke(0.8.dp, BurntOrangePrimary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { onLogRepayment() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تسجيل سداد" else "Log Payment",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = BurntOrangePrimary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 2. Full Settlement Button (Prominent Final Action)
                    if (onQuickFullSettle != null) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MutedIncomeGreen,
                            border = BorderStroke(0.8.dp, MutedIncomeGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .clickable { onQuickFullSettle() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "سداد بالكامل" else "Settle in Full",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
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
fun LogRepaymentDialog(
    debtItem: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val remaining = debtItem.remainingAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BurntOrangePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = null,
                        tint = BurntOrangePrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.log_repayment),
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Person & Remaining Summary Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(0.8.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = debtItem.debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "${if (isArabic) "المتبقي:" else "Remaining:"} ${Formatters.formatMoney(remaining)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = BurntOrangePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        val cleaned = it.replace(',', '.')
                        if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = cleaned
                            errorMessage = null
                        }
                    },
                    label = { Text(stringResource(R.string.repayment_amount)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MutedExpenseTerracotta) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repayment_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = if (isArabic) "يرجى إدخال مبلغ صحيح" else "Please enter a valid amount"
                    } else if (amt > debtItem.remainingAmount + 0.01) {
                        errorMessage = if (isArabic) "مبلغ السداد لا يمكن أن يتجاوز المبلغ المتبقي" else "Amount cannot exceed remaining balance"
                    } else {
                        onConfirm(amt, "")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                modifier = Modifier.testTag("submit_repayment_button")
            ) {
                Text(stringResource(R.string.save), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondaryBrown)
            }
        },
        containerColor = WarmCardSurface
    )
}

@Composable
fun DebtDetailsDialog(
    debtItem: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onLogRepayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeletePayment: (DebtPaymentEntity) -> Unit
) {
    val isOwedToMe = debtItem.debt.type == "OWED_TO_ME"
    val accentColor = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta
    val typeLabel = if (isOwedToMe) stringResource(R.string.owed_to_me) else stringResource(R.string.i_owe)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = debtItem.getInitials(),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = debtItem.debt.personName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = typeLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل الدين", tint = TextSecondaryBrown)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف الدين", tint = MutedExpenseTerracotta)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 3-Column Financial Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(0.8.dp, BorderSubtle)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = if (isArabic) "الإجمالي" else "Total",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown
                                )
                                Text(
                                    text = "${Formatters.formatMoney(debtItem.totalAmount)} $currencySymbol",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isArabic) "المسدد" else "Paid",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown
                                )
                                Text(
                                    text = "${Formatters.formatMoney(debtItem.repaidAmount)} $currencySymbol",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedIncomeGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isArabic) "المتبقي" else "Remaining",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown
                                )
                                Text(
                                    text = "${Formatters.formatMoney(debtItem.remainingAmount)} $currencySymbol",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { debtItem.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = accentColor,
                            trackColor = Color(0xFFE8E5DC)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dates info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${if (isArabic) "تاريخ التسجيل:" else "Date:"} ${Formatters.formatDate(debtItem.debt.lentDateMillis, isArabic)}",
                        fontSize = 11.5.sp,
                        color = TextSecondaryBrown
                    )
                    if (debtItem.debt.dueDateMillis != null) {
                        Text(
                            text = "${if (isArabic) "الاستحقاق:" else "Due:"} ${Formatters.formatDate(debtItem.debt.dueDateMillis, isArabic)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderSubtle.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(10.dp))

                // Payment History Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.repayment_history),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimaryDark
                    )

                    if (!debtItem.isFullyPaid) {
                        TextButton(
                            onClick = onLogRepayment,
                            modifier = Modifier.testTag("log_repayment_action_button")
                        ) {
                            Text(
                                text = "+ ${stringResource(R.string.log_repayment)}",
                                color = BurntOrangePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (debtItem.payments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_payments_yet),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedBrown,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        debtItem.payments.forEach { payment ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(0.6.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${Formatters.formatMoney(payment.amount)} $currencySymbol",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedIncomeGreen
                                        )
                                        Text(
                                            text = Formatters.formatDate(payment.paymentDateMillis, isArabic),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryBrown,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeletePayment(payment) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الدفعة",
                                            tint = TextMutedBrown,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
            ) {
                Text(if (isArabic) "إغلاق" else "Close", color = Color.White)
            }
        },
        containerColor = WarmCardSurface
    )
}
