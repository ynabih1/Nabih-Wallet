package com.nabih.wallet.ui.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.model.DebtWithPayments
import com.nabih.wallet.ui.components.ExpressiveSegmentedCardItem
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtSheet(
    editingDebt: DebtWithPayments?,
    onDismiss: () -> Unit,
    onSave: (personName: String, type: String, originalAmount: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var debtType by remember(editingDebt) {
        mutableStateOf(editingDebt?.debt?.type ?: DebtType.OWED_BY_ME)
    }
    var personName by remember(editingDebt) {
        mutableStateOf(editingDebt?.debt?.personName ?: "")
    }
    var amountText by remember(editingDebt) {
        mutableStateOf(
            editingDebt?.debt?.originalAmount?.let { Formatters.formatNumber(it).replace(",", "") } ?: ""
        )
    }

    val parsedAmount = Formatters.parseAmountInput(amountText)
    val canSave = personName.trim().isNotEmpty() && (parsedAmount != null && parsedAmount > 0.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingDebt == null) {
                        stringResource(R.string.add_debt_title)
                    } else {
                        stringResource(R.string.edit_debt_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    DebtType.OWED_BY_ME to stringResource(R.string.debt_type_owed_by_me),
                    DebtType.OWED_TO_ME to stringResource(R.string.debt_type_owed_to_me)
                )
                options.forEachIndexed { index, (key, label) ->
                    SegmentedButton(
                        selected = debtType == key,
                        onClick = { debtType = key },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.testTag("debt_type_${key.lowercase()}")
                    ) {
                        Text(label)
                    }
                }
            }

            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(stringResource(R.string.person_name_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_person_name_input")
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.amount_label)) },
                placeholder = {
                    Text(
                        text = "0.00",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_amount_input")
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    onSave(personName.trim(), debtType, amount)
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_debt_button"),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.save_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PayDebtSheet(
    debtItem: DebtWithPayments,
    onDismiss: () -> Unit,
    onConfirmPayment: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember(debtItem.debt.id) { mutableStateOf("") }
    val remaining = debtItem.remainingAmount
    val parsedAmount = Formatters.parseAmountInput(amountText)
    val isFullSelected = parsedAmount != null && kotlin.math.abs(parsedAmount - remaining) < 0.01
    val canSubmit = parsedAmount != null && parsedAmount > 0.0 && parsedAmount <= remaining + 0.004

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pay_installment),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${debtItem.debt.personName} • المتبقي ${Formatters.formatAmount(remaining)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.payment_amount_label)) },
                placeholder = {
                    Text(
                        text = "0.00",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                isError = parsedAmount != null && parsedAmount > remaining + 0.004,
                supportingText = if (parsedAmount != null && parsedAmount > remaining + 0.004) {
                    { Text("المبلغ أكبر من المتبقي المستحق") }
                } else {
                    null
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pay_debt_amount_input")
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(100.0, 200.0, 500.0).forEach { preset ->
                    if (preset <= remaining + 0.004) {
                        AssistChip(
                            onClick = { amountText = preset.toInt().toString() },
                            label = { Text("${preset.toInt()} ج.م") },
                            modifier = Modifier.testTag("pay_chip_${preset.toInt()}")
                        )
                    }
                }

                FilterChip(
                    selected = isFullSelected,
                    onClick = {
                        amountText = Formatters.formatNumber(remaining).replace(",", "")
                    },
                    label = {
                        Text("المتبقي (${Formatters.formatNumber(remaining)})")
                    },
                    leadingIcon = if (isFullSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.testTag("pay_remaining_chip")
                )
            }

            Button(
                onClick = {
                    val value = parsedAmount ?: return@Button
                    onConfirmPayment(value)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_payment_button"),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.confirm_payment),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePaymentsSheet(
    debtItem: DebtWithPayments,
    onDismiss: () -> Unit,
    onDeletePayment: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalWalletSemanticColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.manage_payments_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "إجمالي المسدد: ${Formatters.formatAmount(debtItem.totalPaid)} (${debtItem.payments.size} دفعات)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            if (debtItem.payments.isEmpty()) {
                Text(
                    text = "لا توجد دفعات مسجلة لهذا الدين",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(
                        items = debtItem.payments,
                        key = { _, p -> "manage_pay_${p.id}" }
                    ) { index, payment ->
                        ExpressiveSegmentedCardItem(
                            index = index,
                            count = debtItem.payments.size
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = Formatters.formatAmount(payment.amount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = Formatters.formatFullDate(payment.paidAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Clean red TextButton without pink background
                                TextButton(
                                    onClick = { onDeletePayment(payment.id) },
                                    modifier = Modifier.testTag("delete_payment_${payment.id}")
                                ) {
                                    Text(
                                        text = stringResource(R.string.delete_label),
                                        color = semantic.expense,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("manage_payments_done"),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.done_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
