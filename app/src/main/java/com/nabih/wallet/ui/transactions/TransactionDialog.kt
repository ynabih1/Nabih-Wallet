package com.nabih.wallet.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.ui.components.IconLibrary
import com.nabih.wallet.ui.theme.CategoryColors
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionBottomSheet(
    initialType: String,
    editingTransaction: TransactionItem?,
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    preselectedCategoryId: Long?,
    onDismiss: () -> Unit,
    onRequestAddCategory: (type: String) -> Unit,
    onSave: (type: String, amount: Double, categoryId: Long, timestamp: Long, note: String) -> Unit,
    onDelete: ((Long) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(editingTransaction?.entity?.type ?: initialType)
    }
    var amountText by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.entity?.amount?.let { Formatters.formatNumber(it).replace(",", "") } ?: ""
        )
    }
    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.entity?.note ?: "")
    }
    var selectedTimestamp by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.entity?.timestamp ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val availableCategories = if (selectedType == TransactionType.EXPENSE) {
        expenseCategories
    } else {
        incomeCategories
    }

    var selectedCategoryId by remember(editingTransaction, selectedType, availableCategories) {
        val preferred = editingTransaction?.entity?.categoryId
            ?.takeIf { id -> availableCategories.any { it.id == id } }
            ?: preselectedCategoryId?.takeIf { id -> availableCategories.any { it.id == id } }
            ?: availableCategories.firstOrNull()?.id
            ?: 0L
        mutableLongStateOf(preferred)
    }

    LaunchedEffect(preselectedCategoryId, availableCategories) {
        if (preselectedCategoryId != null && availableCategories.any { it.id == preselectedCategoryId }) {
            selectedCategoryId = preselectedCategoryId
        }
    }

    val parsedAmount = Formatters.parseAmountInput(amountText)
    val canSave = parsedAmount != null && parsedAmount > 0.0 && selectedCategoryId > 0L

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingTransaction == null) {
                        stringResource(R.string.add_transaction_title)
                    } else {
                        stringResource(R.string.edit_transaction_title)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            // Type Segmented Control
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    TransactionType.EXPENSE to stringResource(R.string.transaction_type_expense),
                    TransactionType.INCOME to stringResource(R.string.transaction_type_income)
                )
                options.forEachIndexed { index, (typeKey, label) ->
                    SegmentedButton(
                        selected = selectedType == typeKey,
                        onClick = { selectedType = typeKey },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.testTag("tx_type_${typeKey.lowercase()}")
                    ) {
                        Text(label)
                    }
                }
            }

            // Large Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.amount_label)) },
                placeholder = {
                    Text(
                        text = "0.00",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            // Quick Add Chips (+50, +150, +500, +1,200)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(50.0 to "+50", 150.0 to "+150", 500.0 to "+500", 1200.0 to "+1,200").forEach { (delta, label) ->
                    AssistChip(
                        onClick = {
                            val current = Formatters.parseAmountInput(amountText) ?: 0.0
                            val updated = current + delta
                            amountText = if (updated % 1.0 == 0.0) {
                                updated.toLong().toString()
                            } else {
                                Formatters.formatNumber(updated).replace(",", "")
                            }
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("tx_quick_${delta.toInt()}")
                    )
                }
            }

            // Category Section
            Text(
                text = stringResource(R.string.category_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    val accent = CategoryColors.accentColor(category.colorKey, isDark)
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                        leadingIcon = {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = IconLibrary.iconFor(category.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.testTag("tx_category_chip_${category.id}")
                    )
                }

                AssistChip(
                    onClick = { onRequestAddCategory(selectedType) },
                    label = {
                        Text(
                            text = stringResource(R.string.new_category_chip),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("tx_add_new_category_chip")
                )
            }

            // Date Picker Row
            val dateShape = RoundedCornerShape(16.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(dateShape)
                    .clickable { showDatePicker = true }
                    .testTag("tx_date_picker_row"),
                shape = dateShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = Formatters.formatDateWithRelativePrefix(selectedTimestamp),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(R.string.change_date),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Optional Note Field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(stringResource(R.string.note_optional_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            // Save Button
            Button(
                onClick = {
                    val amount = parsedAmount ?: return@Button
                    onSave(selectedType, amount, selectedCategoryId, selectedTimestamp, noteText)
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = CircleShape
            ) {
                Text(
                    text = if (canSave) {
                        stringResource(R.string.save_label)
                    } else {
                        stringResource(R.string.enter_amount_cta)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (editingTransaction != null && onDelete != null) {
                TextButton(
                    onClick = { onDelete(editingTransaction.entity.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_transaction_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = semantic.expense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حذف العملية",
                        color = semantic.expense,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedTimestamp = it }
                        showDatePicker = false
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_label))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
