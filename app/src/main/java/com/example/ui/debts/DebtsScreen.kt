package com.example.ui.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.model.DebtWithPayments
import com.example.ui.WalletViewModel
import com.example.ui.components.EmptyStateView
import com.example.ui.components.Formatters
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import java.util.Locale
import kotlin.math.abs

@Composable
fun DebtsScreen(
    viewModel: WalletViewModel,
    onAddNewDebt: () -> Unit,
    onEditDebt: (DebtEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDebts by viewModel.allDebtsWithPayments.collectAsStateWithLifecycle()
    val totalOwedToMe by viewModel.totalOwedToMe.collectAsStateWithLifecycle()
    val totalIOwe by viewModel.totalIOwe.collectAsStateWithLifecycle()
    val netDebtBalance by viewModel.netDebtBalance.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val isArabic = language == "ar"
    val currencySymbol = viewModel.getCurrencySymbol()

    var debtForRepayment by remember { mutableStateOf<DebtWithPayments?>(null) }
    var debtForHistory by remember { mutableStateOf<DebtWithPayments?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }

    // Keep history dialog synced with latest updates from allDebts
    val activeHistoryDebt = remember(debtForHistory, allDebts) {
        debtForHistory?.let { current ->
            allDebts.find { it.debt.id == current.debt.id }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Top Summary Card: #121212 surface, 12dp radius, zero shadow
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debts_summary_card"),
                shape = RoundedCornerShape(12.dp),
                color = CardSurface,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isArabic) "صافي الديون" else "Net Debts",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val netPrefix = when {
                        netDebtBalance > 0.005 -> "+"
                        netDebtBalance < -0.005 -> "-"
                        else -> ""
                    }
                    Text(
                        text = "$netPrefix${Formatters.formatMoney(abs(netDebtBalance))} $currencySymbol",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // مستحق لي (Owed to Me)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = InputChipSurface,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    tint = IncomeGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.owed_to_me),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${Formatters.formatMoney(totalOwedToMe)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // مستحق علي (I Owe)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = InputChipSurface,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowDownward,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.i_owe),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${Formatters.formatMoney(totalIOwe)} $currencySymbol",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Debts List (Flat rows on #000000 with #2F3336 divider) or Empty State
        if (allDebts.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Outlined.Handshake,
                    title = if (isArabic) "لا توجد ديون" else "No debts",
                    description = if (isArabic) "لا توجد ديون أو مستحقات مالية مسجلة في هذا القسم" else "No debts or financial dues recorded in this section",
                    actionText = if (isArabic) "إضافة دين جديد" else "Add New Debt",
                    onAction = onAddNewDebt,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(allDebts, key = { it.debt.id }) { item ->
                DebtItemCard(
                    debtWithPayments = item,
                    currencySymbol = currencySymbol,
                    isArabic = isArabic,
                    onRepayClick = { debtForRepayment = item },
                    onEditClick = { onEditDebt(item.debt) },
                    onHistoryClick = { debtForHistory = item },
                    onDeleteClick = { debtToDelete = item.debt }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Repayment Dialog (تسديد / تحصيل)
    debtForRepayment?.let { item ->
        RepaymentDialog(
            debtWithPayments = item,
            currencySymbol = currencySymbol,
            isArabic = isArabic,
            onDismiss = { debtForRepayment = null },
            onConfirmPayment = { amount, note, updateWallet ->
                viewModel.logDebtPayment(
                    debt = item.debt,
                    amount = amount,
                    note = note,
                    updateWalletBalance = updateWallet
                )
                debtForRepayment = null
            }
        )
    }

    // Payment History Dialog (سجل الدفعات)
    activeHistoryDebt?.let { item ->
        PaymentHistoryDialog(
            debtWithPayments = item,
            currencySymbol = currencySymbol,
            isArabic = isArabic,
            onDismiss = { debtForHistory = null },
            onDeletePayment = { payment ->
                viewModel.deleteDebtPayment(payment)
            }
        )
    }

    // Delete Debt Confirmation Dialog
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "حذف الدين" else "Delete Debt",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isArabic) {
                        "هل أنت متأكد من رغبتك في حذف سجل الدين الخاص بـ \"${debt.personName}\"؟"
                    } else {
                        "Are you sure you want to delete the debt record for \"${debt.personName}\"?"
                    },
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = TextPrimary
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (isArabic) "حذف" else "Delete",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { debtToDelete = null },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(
                        text = if (isArabic) "إلغاء" else "Cancel",
                        color = TextPrimary
                    )
                }
            }
        )
    }
}

@Composable
private fun DebtItemCard(
    debtWithPayments: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onRepayClick: () -> Unit,
    onEditClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val debt = debtWithPayments.debt
    val isOwedToMe = debt.type == "OWED_TO_ME"
    val semanticColor = if (isOwedToMe) IncomeGreen else ExpenseRed

    var showMenu by remember { mutableStateOf(false) }

    // Flat row on #000000 black background with #2F3336 thin divider
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .testTag("debt_card_${debt.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Outlined 24dp icon with NO colored background circle
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                // Person Name + Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.personName,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Formatters.formatDate(debt.lentDateMillis, isArabic),
                            fontSize = 11.5.sp,
                            color = TextSecondary
                        )
                        if (debt.dueDateMillis != null) {
                            Text(
                                text = "•",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                            Text(
                                text = if (isArabic) {
                                    "الاستحقاق: ${Formatters.formatDate(debt.dueDateMillis, true)}"
                                } else {
                                    "Due: ${Formatters.formatDate(debt.dueDateMillis, false)}"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = XBlue
                            )
                        }
                    }
                }

                // Remaining & Total Amount Column
                Column(horizontalAlignment = Alignment.End) {
                    if (debtWithPayments.isFullyPaid) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isArabic) "مسدد بالكامل" else "Paid",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreen
                            )
                        }
                    } else {
                        Text(
                            text = "${Formatters.formatMoney(debtWithPayments.remainingAmount)} $currencySymbol",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = semanticColor
                        )
                        Text(
                            text = if (isArabic) {
                                "الإجمالي: ${Formatters.formatMoney(debt.totalAmount)}"
                            } else {
                                "Total: ${Formatters.formatMoney(debt.totalAmount)}"
                            },
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 3-Dot Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = if (isArabic) "خيارات الدين" else "Debt Options",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = ElevatedSurface
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "تعديل الدين" else "Edit Debt",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "سجل الدفعات" else "Payment History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onHistoryClick()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "حذف" else "Delete",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ExpenseRed
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Notes if present
            if (debt.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = debt.notes,
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Progress Bar if partially or fully paid
            if (debtWithPayments.repaidAmount > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) {
                            "تم سداد ${Formatters.formatMoney(debtWithPayments.repaidAmount)} من ${Formatters.formatMoney(debt.totalAmount)} $currencySymbol"
                        } else {
                            "Paid ${Formatters.formatMoney(debtWithPayments.repaidAmount)} of ${Formatters.formatMoney(debt.totalAmount)} $currencySymbol"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.0f", debtWithPayments.progress * 100)}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = semanticColor
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(InputChipSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(debtWithPayments.progress.coerceIn(0.03f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(semanticColor)
                    )
                }
            }

            // Quick Action Button: تسديد / تحصيل (Secondary button: #2F3336 border and white text, fully rounded)
            if (!debtWithPayments.isFullyPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = CircleShape,
                    color = AppBackground,
                    border = BorderStroke(1.dp, DividerBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(CircleShape)
                        .clickable { onRepayClick() }
                        .testTag("repay_debt_btn_${debt.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تسديد / تحصيل" else "Repay / Collect",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Thin flat divider #2F3336 between debt rows
        HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
    }
}

@Composable
private fun RepaymentDialog(
    debtWithPayments: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Double, note: String, updateWalletBalance: Boolean) -> Unit
) {
    val remaining = debtWithPayments.remainingAmount

    var amountInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var updateWalletBalance by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = if (isArabic) "تسديد / تحصيل دفعة" else "Record Repayment",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${debtWithPayments.debt.personName} • ${if (isArabic) "المتبقي:" else "Remaining:"} ${Formatters.formatMoney(remaining)} $currencySymbol",
                    fontSize = 12.5.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { input ->
                        val cleaned = input.replace(',', '.')
                        if (cleaned.isEmpty() || cleaned.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            amountInput = cleaned
                            errorText = null
                        }
                    },
                    label = {
                        Text(if (isArabic) "مبلغ الدفعة" else "Payment Amount", color = TextSecondary)
                    },
                    suffix = {
                        Text(currencySymbol, fontWeight = FontWeight.Bold, color = TextSecondary)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputChipSurface,
                        unfocusedContainerColor = InputChipSurface,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = DividerBorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("repayment_amount_input")
                )

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        fontSize = 12.sp,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Checkbox: تحديث رصيد المحفظة تلقائياً بهذه العملية
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(0.6.dp, DividerBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { updateWalletBalance = !updateWalletBalance }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = updateWalletBalance,
                            onCheckedChange = { updateWalletBalance = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SpotifyGreen,
                                checkmarkColor = OnSpotifyGreen,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Text(
                            text = if (isArabic) "تحديث رصيد المحفظة تلقائياً بهذه العملية" else "Automatically update wallet balance with this payment",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }

                // Distinct Action Buttons: تسجيل سداد (Secondary) & سداد بالكامل (Primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val parsed = amountInput.toDoubleOrNull() ?: 0.0
                            when {
                                parsed <= 0.0 -> {
                                    errorText = if (isArabic) "يرجى إدخال مبلغ صحيح" else "Please enter a valid amount"
                                }
                                parsed > remaining + 0.005 -> {
                                    errorText = if (isArabic) "المبلغ يتجاوز المتبقي" else "Amount exceeds remaining balance"
                                }
                                else -> {
                                    onConfirmPayment(parsed, noteInput, updateWalletBalance)
                                }
                            }
                        },
                        shape = CircleShape,
                        border = BorderStroke(1.dp, DividerBorderColor),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تسجيل سداد" else "Log Payment",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            onConfirmPayment(remaining, noteInput, updateWalletBalance)
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = OnSpotifyGreen
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("full_repayment_btn")
                    ) {
                        Text(
                            text = if (isArabic) "سداد بالكامل" else "Pay in Full",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSpotifyGreen
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                border = BorderStroke(1.dp, DividerBorderColor)
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun PaymentHistoryDialog(
    debtWithPayments: DebtWithPayments,
    currencySymbol: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onDeletePayment: (DebtPaymentEntity) -> Unit
) {
    val payments = debtWithPayments.payments

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = if (isArabic) "سجل الدفعات" else "Payment History",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = debtWithPayments.debt.personName,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            if (payments.isEmpty()) {
                Text(
                    text = if (isArabic) "لا توجد دفعات مسجلة لهذا الدين حتى الآن." else "No payments recorded for this debt yet.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    payments.forEach { payment ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurface,
                            border = BorderStroke(0.6.dp, DividerBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${Formatters.formatMoney(payment.amount)} $currencySymbol",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = Formatters.formatDate(payment.paymentDateMillis, isArabic),
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                    if (payment.note.isNotBlank()) {
                                        Text(
                                            text = payment.note,
                                            fontSize = 11.5.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDeletePayment(payment) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = if (isArabic) "حذف الدفعة" else "Delete Payment",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                border = BorderStroke(1.dp, DividerBorderColor)
            ) {
                Text(
                    text = if (isArabic) "إغلاق" else "Close",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
