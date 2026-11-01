package com.example.ui.debts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.ui.components.DateFilterUtils
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    var type by remember { mutableStateOf(initialDebt?.type ?: "OWED_TO_ME") }
    var personName by remember { mutableStateOf(initialDebt?.personName ?: "") }
    var amountString by remember {
        mutableStateOf(
            if (initialDebt != null && initialDebt.totalAmount > 0) {
                if (initialDebt.totalAmount % 1.0 == 0.0) {
                    initialDebt.totalAmount.toLong().toString()
                } else {
                    initialDebt.totalAmount.toString()
                }
            } else ""
        )
    }
    var lentDateMillis by remember {
        mutableLongStateOf(initialDebt?.lentDateMillis ?: DateFilterUtils.getStartOfDay(System.currentTimeMillis()))
    }
    var dueDateMillis by remember { mutableStateOf<Long?>(initialDebt?.dueDateMillis) }
    val notes = initialDebt?.notes ?: ""

    var showLentDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isOwedToMe = type == "OWED_TO_ME"
    val activeAccent = if (isOwedToMe) MutedIncomeGreen else MutedExpenseTerracotta
    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = parsedAmount > 0.0 && personName.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = BorderSubtle,
                width = 40.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Unified Top Row: Debt Type Switcher Pills + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Owed to me Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { type = "OWED_TO_ME" },
                    shape = RoundedCornerShape(13.dp),
                    color = if (isOwedToMe) MutedIncomeGreen else Color(0xFFFAF9F6),
                    border = BorderStroke(0.8.dp, if (isOwedToMe) MutedIncomeGreen else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isOwedToMe) Color.White.copy(alpha = 0.22f) else MutedIncomeGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isOwedToMe) Color.White else MutedIncomeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "مستحق لي" else "Owed to me",
                            fontSize = 13.5.sp,
                            fontWeight = if (isOwedToMe) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isOwedToMe) Color.White else TextPrimaryDark
                        )
                    }
                }

                // I owe Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { type = "I_OWE" },
                    shape = RoundedCornerShape(13.dp),
                    color = if (!isOwedToMe) MutedExpenseTerracotta else Color(0xFFFAF9F6),
                    border = BorderStroke(0.8.dp, if (!isOwedToMe) MutedExpenseTerracotta else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (!isOwedToMe) Color.White.copy(alpha = 0.22f) else MutedExpenseTerracotta.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!isOwedToMe) Color.White else MutedExpenseTerracotta,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "مستحق علي" else "I owe",
                            fontSize = 13.5.sp,
                            fontWeight = if (!isOwedToMe) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (!isOwedToMe) Color.White else TextPrimaryDark
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F1EC))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Centered Amount Input Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFAF9F6),
                border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "إجمالي مبلغ الدين" else "Total Debt Amount",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondaryBrown
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            BasicTextField(
                                value = amountString,
                                onValueChange = { input ->
                                    val cleaned = input.replace(',', '.')
                                    if (cleaned.isEmpty() || cleaned.matches(Regex("^\\d*(\\.\\d{0,2})?$"))) {
                                        amountString = cleaned
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier
                                    .testTag("debt_amount_input")
                                    .width(IntrinsicSize.Min)
                                    .widthIn(min = 68.dp, max = 220.dp),
                                textStyle = TextStyle(
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = activeAccent,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                ),
                                cursorBrush = SolidColor(activeAccent),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (amountString.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                fontSize = 34.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMutedBrown.copy(alpha = 0.32f),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = activeAccent.copy(alpha = 0.12f),
                                border = BorderStroke(0.8.dp, activeAccent.copy(alpha = 0.28f))
                            ) {
                                Text(
                                    text = currencySymbol,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (amountString.isNotBlank()) {
                        IconButton(
                            onClick = { amountString = "" },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDEAE2))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "مسح" else "Clear",
                                tint = TextSecondaryBrown,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Person Name Input Field
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    errorMessage = null
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = activeAccent
                    )
                },
                label = { Text(if (isArabic) "اسم الشخص" else "Person Name") },
                placeholder = { Text(if (isArabic) "أحمد، محمد..." else "John, Alex...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFFAF9F6),
                    unfocusedContainerColor = Color(0xFFFAF9F6),
                    focusedBorderColor = activeAccent,
                    unfocusedBorderColor = BorderSubtle
                ),
                isError = errorMessage != null && personName.isBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_person_name_input")
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage!!,
                    color = MutedExpenseTerracotta,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Dates Row: Debt Date + Optional Due Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Debt Date Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { showLentDatePicker = true },
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFFFAF9F6),
                    border = BorderStroke(0.8.dp, BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "تاريخ الدين" else "Debt Date",
                                fontSize = 11.sp,
                                color = TextSecondaryBrown,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatDate(lentDateMillis, isArabic),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                // Optional Due Date Card
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(13.dp))
                        .clickable { showDueDatePicker = true },
                    shape = RoundedCornerShape(13.dp),
                    color = Color(0xFFFAF9F6),
                    border = BorderStroke(0.8.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = BurntOrangePrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isArabic) "موعد الاستحقاق" else "Due Date",
                                    fontSize = 11.sp,
                                    color = TextSecondaryBrown,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dueDateMillis?.let { Formatters.formatDate(it, isArabic) }
                                    ?: (if (isArabic) "اختياري - غير محدد" else "Optional"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dueDateMillis != null) TextPrimaryDark else TextMutedBrown
                            )
                        }

                        if (dueDateMillis != null) {
                            IconButton(
                                onClick = { dueDateMillis = null },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "مسح تاريخ الاستحقاق" else "Clear due date",
                                    tint = TextSecondaryBrown,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Save Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        val amt = amountString.toDoubleOrNull()
                        if (personName.isBlank()) {
                            errorMessage = if (isArabic) "يرجى إدخال اسم الشخص" else "Please enter person's name"
                        } else if (amt == null || amt <= 0) {
                            errorMessage = if (isArabic) "يرجى إدخال مبلغ صحيح أكبر من الصفر" else "Please enter a valid amount"
                        } else {
                            onSave(
                                initialDebt?.id ?: 0L,
                                type,
                                personName.trim(),
                                amt,
                                lentDateMillis,
                                dueDateMillis,
                                notes,
                                false
                            )
                            onDismiss()
                        }
                    }
                    .testTag("save_debt_button"),
                shape = RoundedCornerShape(14.dp),
                color = if (isSaveEnabled) activeAccent else Color(0xFFF3F1EC),
                border = BorderStroke(0.8.dp, if (isSaveEnabled) activeAccent else BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSaveEnabled) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialDebt != null) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (initialDebt != null) {
                            if (isArabic) "تحديث بيانات الدين" else "Update Debt"
                        } else if (isSaveEnabled) {
                            val formatted = Formatters.formatMoney(parsedAmount)
                            if (isArabic) "حفظ الدين • $formatted $currencySymbol" else "Save Debt • $formatted $currencySymbol"
                        } else {
                            if (isArabic) "أدخل الاسم والمبلغ" else "Enter name & amount"
                        },
                        fontSize = 14.sp,
                        fontWeight = if (isSaveEnabled) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSaveEnabled) Color.White else TextSecondaryBrown
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showLentDatePicker) {
        val initialUtc = remember(lentDateMillis) {
            DateFilterUtils.localMillisToUtcMidnight(lentDateMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showLentDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utc ->
                        lentDateMillis = DateFilterUtils.utcMillisToLocalStartOfDay(utc)
                    }
                    showLentDatePicker = false
                }) {
                    Text(stringResource(R.string.save), color = BurntOrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLentDatePicker = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondaryBrown)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDueDatePicker) {
        val initialUtc = remember(dueDateMillis, lentDateMillis) {
            DateFilterUtils.localMillisToUtcMidnight(dueDateMillis ?: lentDateMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utc ->
                        dueDateMillis = DateFilterUtils.utcMillisToLocalStartOfDay(utc)
                    }
                    showDueDatePicker = false
                }) {
                    Text(stringResource(R.string.save), color = BurntOrangePrimary, fontWeight = FontWeight.Bold)
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
}
