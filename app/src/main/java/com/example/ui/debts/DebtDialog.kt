package com.example.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDialog(
    initialDebt: DebtEntity? = null,
    isArabic: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        type: String,
        personName: String,
        totalAmount: Double,
        lentDateMillis: Long,
        dueDateMillis: Long?,
        notes: String,
        enableReminder: Boolean
    ) -> Unit
) {
    var type by remember { mutableStateOf(initialDebt?.type ?: "OWED_TO_ME") }
    var personName by remember { mutableStateOf(initialDebt?.personName ?: "") }
    var amountString by remember {
        mutableStateOf(if (initialDebt != null && initialDebt.totalAmount > 0) initialDebt.totalAmount.toString() else "")
    }
    var lentDateMillis by remember { mutableStateOf(initialDebt?.lentDateMillis ?: System.currentTimeMillis()) }
    var dueDateMillis by remember { mutableStateOf<Long?>(initialDebt?.dueDateMillis) }
    var notes by remember { mutableStateOf(initialDebt?.notes ?: "") }
    var enableReminder by remember { mutableStateOf(initialDebt?.enableReminder ?: false) }

    var showDueDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDateMillis ?: System.currentTimeMillis()
    )

    if (showDueDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis = datePickerState.selectedDateMillis
                    showDueDatePicker = false
                }) {
                    Text(stringResource(R.string.save), color = BurntOrangePrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondaryBrown)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(horizontal = 8.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (isArabic) {
                    if (initialDebt == null) "إضافة دين" else "تعديل الدين"
                } else {
                    if (initialDebt == null) "Add Debt" else "Edit Debt"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Debt Type Switcher: Owed to me (Lent) vs I owe (Borrowed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmCardSurface)
                        .padding(4.dp)
                ) {
                    val isOwedToMe = type == "OWED_TO_ME"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isOwedToMe) MutedIncomeGreen else Color.Transparent)
                            .clickable { type = "OWED_TO_ME" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "مستحق لي" else "Owed to me",
                            color = if (isOwedToMe) Color.White else TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isOwedToMe) MutedExpenseTerracotta else Color.Transparent)
                            .clickable { type = "I_OWE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "مستحق علي" else "I owe",
                            color = if (!isOwedToMe) Color.White else TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Person's Name
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        errorMessage = null
                    },
                    label = { Text(if (isArabic) "اسم الشخص" else "Person Name") },
                    placeholder = { Text(if (isArabic) "مثال: أحمد، محمد..." else "e.g. John, Alex...") },
                    singleLine = true,
                    isError = errorMessage != null && personName.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_person_name_input")
                )

                // 3. Amount Field (Numeric keyboard)
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountString = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isArabic) "إجمالي المبلغ" else "Total Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MutedExpenseTerracotta) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("debt_amount_input")
                )

                // 4. Optional Due Date
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDueDatePicker = true },
                    color = WarmCardSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isArabic) "تاريخ الاستحقاق (اختياري)" else "Due Date (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondaryBrown
                                )
                                Text(
                                    text = dueDateMillis?.let { Formatters.formatDate(it, isArabic) }
                                        ?: (if (isArabic) "بدون موعد استحقاق محدد" else "No due date set"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }
                        }

                        if (dueDateMillis != null) {
                            IconButton(onClick = { dueDateMillis = null }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = if (isArabic) "مسح تاريخ الاستحقاق" else "Clear due date",
                                    tint = TextSecondaryBrown
                                )
                            }
                        }
                    }
                }

                // 5. Due date reminder toggle
                if (dueDateMillis != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تفعيل التذكير بموعد الاستحقاق" else "Enable due date reminder",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimaryDark
                            )
                        }

                        Switch(
                            checked = enableReminder,
                            onCheckedChange = { enableReminder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BurntOrangePrimary
                            )
                        )
                    }
                }

                // 6. Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isArabic) "ملاحظات" else "Notes") },
                    placeholder = { Text(if (isArabic) "اكتب تفاصيل أو ملاحظات إضافية..." else "Enter details or notes...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountString.toDoubleOrNull()
                    if (personName.isBlank()) {
                        errorMessage = if (isArabic) "يرجى إدخال اسم الشخص" else "Please enter person's name"
                    } else if (amt == null || amt <= 0) {
                        errorMessage = if (isArabic) "يرجى إدخال مبلغ صحيح أكبر من الصفر" else "Please enter a valid amount greater than zero"
                    } else {
                        onSave(
                            initialDebt?.id ?: 0,
                            type,
                            personName.trim(),
                            amt,
                            lentDateMillis,
                            dueDateMillis,
                            notes.trim(),
                            enableReminder
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                modifier = Modifier.testTag("save_debt_button")
            ) {
                Text(if (isArabic) "حفظ" else "Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
