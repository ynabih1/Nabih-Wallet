package com.example.ui.debts

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface
import java.util.Locale

@Composable
fun DebtsScreen(
    viewModel: WalletViewModel,
    onAddNewDebt: () -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalOwedToMe by viewModel.totalOwedToMe.collectAsStateWithLifecycle()
    val totalIOwe by viewModel.totalIOwe.collectAsStateWithLifecycle()
    val currentFilter by viewModel.debtFilter.collectAsStateWithLifecycle()
    val filteredDebts by viewModel.filteredDebts.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var selectedDebtForDetails by remember { mutableStateOf<DebtWithPayments?>(null) }
    var selectedDebtForRepayment by remember { mutableStateOf<DebtWithPayments?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: "Debts" + Terracotta "+" Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.debts_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                IconButton(
                    onClick = onAddNewDebt,
                    modifier = Modifier.testTag("add_debt_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة دين جديد",
                        tint = BurntOrangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 2. Summary Cards Row: Owed to me (Green) & I owe (Terracotta)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Owed to me Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.owed_to_me),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${Formatters.formatMoney(totalOwedToMe)} $currencySymbol",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MutedIncomeGreen
                        )
                    }
                }

                // I owe Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.i_owe),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${Formatters.formatMoney(totalIOwe)} $currencySymbol",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MutedExpenseTerracotta
                        )
                    }
                }
            }
        }

        // 3. Filter Chips: All, Owed to me, I owe
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DebtFilterChip(
                    text = stringResource(R.string.filter_all),
                    selected = currentFilter == DebtFilter.ALL,
                    onClick = { viewModel.setDebtFilter(DebtFilter.ALL) },
                    testTag = "filter_all"
                )

                DebtFilterChip(
                    text = stringResource(R.string.filter_owed_to_me),
                    selected = currentFilter == DebtFilter.OWED_TO_ME,
                    onClick = { viewModel.setDebtFilter(DebtFilter.OWED_TO_ME) },
                    testTag = "filter_owed_to_me"
                )

                DebtFilterChip(
                    text = stringResource(R.string.filter_i_owe),
                    selected = currentFilter == DebtFilter.I_OWE,
                    onClick = { viewModel.setDebtFilter(DebtFilter.I_OWE) },
                    testTag = "filter_i_owe"
                )
            }
        }

        // 4. Debts List
        if (filteredDebts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Handshake,
                    title = stringResource(R.string.no_debts_yet),
                    description = if (isArabic) "لا توجد ديون أو مستحقات مالية مسجلة في هذا القسم" else "No debts or receivables recorded in this section",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(filteredDebts, key = { it.debt.id }) { debtItem ->
                DebtCardItem(
                    debtItem = debtItem,
                    currencySymbol = currencySymbol,
                    isArabic = isArabic,
                    onClick = { selectedDebtForDetails = debtItem },
                    onLogRepayment = { selectedDebtForRepayment = debtItem }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Detail & Payment History Dialog
    selectedDebtForDetails?.let { debtWithPayments ->
        // Keep updated state
        val currentDebtItem = filteredDebts.find { it.debt.id == debtWithPayments.debt.id } ?: debtWithPayments
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

    // Partial Repayment Dialog
    selectedDebtForRepayment?.let { debtWithPayments ->
        LogRepaymentDialog(
            debtItem = debtWithPayments,
            currencySymbol = currencySymbol,
            onDismiss = { selectedDebtForRepayment = null },
            onConfirm = { amount, note ->
                viewModel.logDebtPayment(debtWithPayments.debt.id, amount, note)
                selectedDebtForRepayment = null
            }
        )
    }

    // Delete Confirmation Dialog
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text(text = stringResource(R.string.delete_debt), color = TextPrimaryDark) },
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
    testTag: String
) {
    val bg = if (selected) BurntOrangePrimary else MaterialTheme.colorScheme.surface
    val textColor = if (selected) Color.White else TextPrimaryDark
    val border = if (selected) BurntOrangePrimary else BorderSubtle

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun DebtCardItem(
    debtItem: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onClick: () -> Unit,
    onLogRepayment: () -> Unit
) {
    val isOwedToMe = debtItem.debt.type == "OWED_TO_ME"
    val avatarBg = if (isOwedToMe) BurntOrangePrimary else Color(0xFF4A5568)
    val amountColor = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta
    val progressColor = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta
    val dueStatus = debtItem.getDueStatus()

    val dueText = when (dueStatus) {
        is DueStatus.NoDueDate -> if (isArabic) "بدون موعد استحقاق" else "No due date"
        is DueStatus.DueToday -> if (isArabic) "مستحق اليوم" else "Due today"
        is DueStatus.DueInDays -> if (isArabic) "مستحق خلال ${dueStatus.days} أيام" else "Due in ${dueStatus.days} days"
        is DueStatus.Overdue -> if (isArabic) "متأخر ${dueStatus.days} يوم" else "Overdue ${dueStatus.days} days"
    }

    val dueTextColor = when (dueStatus) {
        is DueStatus.Overdue -> MutedExpenseTerracotta
        is DueStatus.DueToday -> com.example.ui.theme.WarmAmberWarning
        else -> TextSecondaryBrown
    }

    val dueBgColor = when (dueStatus) {
        is DueStatus.Overdue -> MutedExpenseTerracotta.copy(alpha = 0.1f)
        is DueStatus.DueToday -> com.example.ui.theme.WarmAmberWarning.copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("debt_card_${debtItem.debt.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderSubtle),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = debtItem.getInitials(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = debtItem.debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(3.dp))

                        // Due status pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = dueBgColor
                        ) {
                            Text(
                                text = dueText,
                                style = MaterialTheme.typography.labelSmall,
                                color = dueTextColor,
                                fontWeight = if (dueStatus is DueStatus.Overdue || dueStatus is DueStatus.DueToday) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount Section
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${Formatters.formatMoney(debtItem.remainingAmount)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${if (isArabic) "من إجمالي" else "of"} ${Formatters.formatMoney(debtItem.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { debtItem.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color(0xFFE8E5DC)
            )

            // Bottom action row: Quick repayment button if not fully paid
            if (!debtItem.isFullyPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = BurntOrangePrimary.copy(alpha = 0.1f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onLogRepayment() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isArabic) "تسجيل سداد" else "Log Payment",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BurntOrangePrimary
                            )
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
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = stringResource(R.string.log_repayment),
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.remaining, Formatters.formatMoney(debtItem.remainingAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryBrown
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.repayment_amount)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MutedExpenseTerracotta) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repayment_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.notes)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "يرجى إدخال مبلغ صحيح"
                    } else if (amt > debtItem.remainingAmount + 0.01) {
                        errorMessage = "مبلغ السداد لا يمكن أن يتجاوز المبلغ المتبقي"
                    } else {
                        onConfirm(amt, noteText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                modifier = Modifier.testTag("submit_repayment_button")
            ) {
                Text(stringResource(R.string.save), color = Color.White)
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
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = debtItem.debt.personName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
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
                val isOwedToMe = debtItem.debt.type == "OWED_TO_ME"
                val typeLabel = if (isOwedToMe) stringResource(R.string.owed_to_me) else stringResource(R.string.i_owe)

                Text(
                    text = "${stringResource(R.string.debt_type)}: $typeLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondaryBrown
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.total_amount)}: ${Formatters.formatMoney(debtItem.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.remaining, Formatters.formatMoney(debtItem.remainingAmount)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta
                )

                if (debtItem.debt.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${stringResource(R.string.notes)}: ${debtItem.debt.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                                fontWeight = FontWeight.SemiBold
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
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = Formatters.formatMoney(payment.amount),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedIncomeGreen
                                        )
                                        Text(
                                            text = Formatters.formatDate(payment.paymentDateMillis, isArabic),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryBrown
                                        )
                                        if (payment.note.isNotBlank()) {
                                            Text(
                                                text = payment.note,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedBrown
                                            )
                                        }
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
                Text(stringResource(R.string.save), color = Color.White)
            }
        },
        containerColor = WarmCardSurface
    )
}
