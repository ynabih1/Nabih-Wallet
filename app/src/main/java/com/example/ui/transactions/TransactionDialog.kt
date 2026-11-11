package com.example.ui.transactions

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.TransactionEntity
import com.example.model.CategoryItem
import com.example.model.FinancialConstants
import com.example.ui.components.DateFilterUtils
import com.example.ui.components.Formatters
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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    initialTransaction: TransactionEntity? = null,
    initialDebt: DebtEntity? = null,
    initialMode: String = "EXPENSE", // "EXPENSE", "INCOME", or "DEBT"
    @Suppress("UNUSED_PARAMETER") existingTransactions: List<TransactionEntity> = emptyList(),
    customExpenseCategories: List<CategoryItem>? = null,
    customIncomeCategories: List<CategoryItem>? = null,
    @Suppress("UNUSED_PARAMETER") savedPaymentMethods: List<String> = emptyList(),
    @Suppress("UNUSED_PARAMETER") onDeletePaymentMethod: ((String) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onUpdateCategoryIcon: ((categoryName: String, iconKey: String) -> Unit)? = null,
    onAddCustomCategory: ((nameAr: String, nameEn: String, type: String, iconKey: String) -> Unit)? = null,
    isArabic: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        type: String,
        amount: Double,
        category: String,
        paymentMethod: String,
        dateMillis: Long,
        endDateMillis: Long?,
        notes: String,
        isPinned: Boolean,
        receiptUri: String?,
        receiptMimeType: String?
    ) -> Unit,
    onSaveDebt: ((
        id: Long,
        type: String,
        personName: String,
        totalAmount: Double,
        lentDateMillis: Long,
        dueDateMillis: Long?,
        notes: String
    ) -> Unit)? = null,
    onDelete: ((TransactionEntity) -> Unit)? = null,
    onDeleteDebt: ((DebtEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var mode by remember {
        mutableStateOf(
            when {
                initialDebt != null -> "DEBT"
                initialTransaction != null -> initialTransaction.type
                else -> initialMode
            }
        )
    }

    val initialAmountVal = initialTransaction?.amount ?: initialDebt?.totalAmount ?: 0.0
    var amountString by remember {
        mutableStateOf(
            if (initialAmountVal > 0) {
                if (initialAmountVal % 1.0 == 0.0) {
                    initialAmountVal.toLong().toString()
                } else {
                    initialAmountVal.toString()
                }
            } else ""
        )
    }

    val activeExpenseList = customExpenseCategories ?: FinancialConstants.expenseCategories
    val activeIncomeList = customIncomeCategories ?: FinancialConstants.incomeCategories

    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category
                ?: (if (mode == "INCOME") activeIncomeList.firstOrNull()?.nameAr else activeExpenseList.firstOrNull()?.nameAr)
                ?: ""
        )
    }

    val currentCategoryList = if (mode == "INCOME") activeIncomeList else activeExpenseList

    var startDateMillis by remember {
        mutableLongStateOf(
            initialTransaction?.dateMillis
                ?: initialDebt?.lentDateMillis
                ?: DateFilterUtils.getStartOfDay(System.currentTimeMillis())
        )
    }
    var showStartDatePicker by remember { mutableStateOf(false) }

    // Debt-specific states
    var debtType by remember { mutableStateOf(initialDebt?.type ?: "OWED_TO_ME") } // "OWED_TO_ME" or "I_OWE"
    var personName by remember { mutableStateOf(initialDebt?.personName ?: "") }
    var dueDateMillis by remember { mutableStateOf<Long?>(initialDebt?.dueDateMillis) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    val debtNotes by remember { mutableStateOf(initialDebt?.notes ?: initialTransaction?.notes ?: "") }

    val isPinned = initialTransaction?.isPinned ?: false

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(180)
        focusRequester.requestFocus()
    }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = if (mode == "DEBT") {
        parsedAmount > 0.0 && personName.trim().isNotBlank()
    } else {
        parsedAmount > 0.0 && selectedCategory.isNotBlank()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ElevatedSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = DividerBorderColor,
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
            if (mode == "DEBT") {
                // =========================================================================
                // ADD / EDIT DEBT SHEET
                // =========================================================================
                val isOwedToMe = debtType == "OWED_TO_ME"
                val activeAccent = if (isOwedToMe) IncomeGreen else ExpenseRed

                // 1. Top Row: X-Style Tabs (مستحق لي | مستحق علي) + Delete + Close
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // مستحق لي (Owed to Me)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtType = "OWED_TO_ME" }
                                .padding(top = 8.dp)
                                .testTag("debt_type_owed_to_me"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "مستحق لي" else "Owed to Me",
                                fontSize = 14.sp,
                                fontWeight = if (isOwedToMe) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOwedToMe) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (isOwedToMe) XBlue else Color.Transparent)
                            )
                        }

                        // مستحق علي (I Owe)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { debtType = "I_OWE" }
                                .padding(top = 8.dp)
                                .testTag("debt_type_i_owe"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "مستحق علي" else "I Owe",
                                fontSize = 14.sp,
                                fontWeight = if (!isOwedToMe) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isOwedToMe) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (!isOwedToMe) XBlue else Color.Transparent)
                            )
                        }

                        if (initialDebt != null && onDeleteDebt != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(InputChipSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = if (isArabic) "حذف" else "Delete",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(InputChipSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Centered Total Debt Amount Card (إجمالي مبلغ الدين)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, DividerBorderColor),
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "إجمالي مبلغ الدين" else "Total Debt Amount",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
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
                                        if (cleaned.isEmpty() || cleaned.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                                            amountString = cleaned
                                        }
                                    },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("transaction_amount_input")
                                        .width(IntrinsicSize.Min)
                                        .widthIn(min = 68.dp, max = 220.dp),
                                    textStyle = TextStyle(
                                        fontSize = 36.sp,
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
                                                    fontSize = 36.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMuted,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = InputChipSurface
                                ) {
                                    Text(
                                        text = currencySymbol,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                                    .background(InputChipSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = if (isArabic) "مسح" else "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Person Name Input Box (اسم الشخص): #2A2A2A surface
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = InputChipSurface,
                    border = BorderStroke(0.8.dp, DividerBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("debt_person_name_input"),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            cursorBrush = SolidColor(SpotifyGreen),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (personName.isEmpty()) {
                                        Text(
                                            text = if (isArabic) "اسم الشخص" else "Person Name",
                                            fontSize = 14.sp,
                                            color = TextMuted
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Two Side-by-Side Date Cards: تاريخ الدين (Right) & موعد الاستحقاق (Left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Right Card: تاريخ الدين
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showStartDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = InputChipSurface,
                        border = BorderStroke(0.8.dp, DividerBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isArabic) "تاريخ الدين" else "Debt Date",
                                    fontSize = 11.5.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = Formatters.formatDate(startDateMillis, isArabic),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Left Card: موعد الاستحقاق
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDueDatePicker = true },
                        shape = RoundedCornerShape(12.dp),
                        color = InputChipSurface,
                        border = BorderStroke(0.8.dp, DividerBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "موعد الاستحقاق" else "Due Date",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                if (dueDateMillis != null) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = if (isArabic) "إلغاء التاريخ" else "Clear Date",
                                        tint = TextSecondary,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { dueDateMillis = null }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = if (dueDateMillis != null) {
                                    Formatters.formatDate(dueDateMillis!!, isArabic)
                                } else {
                                    if (isArabic) "اختياري - غير محدد" else "Optional - Not set"
                                },
                                fontSize = if (dueDateMillis != null) 13.sp else 11.5.sp,
                                fontWeight = if (dueDateMillis != null) FontWeight.Bold else FontWeight.Normal,
                                color = if (dueDateMillis != null) TextPrimary else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Save Debt Button: #1DB954 background + black text, fully rounded
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = isSaveEnabled) {
                            onSaveDebt?.invoke(
                                initialDebt?.id ?: 0L,
                                debtType,
                                personName.trim(),
                                parsedAmount,
                                startDateMillis,
                                dueDateMillis,
                                debtNotes.trim()
                            )
                        }
                        .testTag("save_transaction_button"),
                    shape = CircleShape,
                    color = if (isSaveEnabled) SpotifyGreen else InputChipSurface,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when {
                                !isSaveEnabled -> if (isArabic) "أدخل الاسم والمبلغ" else "Enter name and amount"
                                initialDebt != null -> if (isArabic) "تحديث الدين" else "Update Debt"
                                else -> {
                                    val formatted = Formatters.formatMoney(parsedAmount)
                                    if (isArabic) "حفظ • $formatted $currencySymbol" else "Save • $formatted $currencySymbol"
                                }
                            },
                            fontSize = 14.sp,
                            fontWeight = if (isSaveEnabled) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSaveEnabled) OnSpotifyGreen else TextMuted
                        )
                    }
                }
            } else {
                // =========================================================================
                // EXPENSE / INCOME SHEET
                // =========================================================================
                val isIncome = mode == "INCOME"
                val activeAccent = if (isIncome) IncomeGreen else ExpenseRed

                // 1. Top Row: X-Style Tabs (مصروف | دخل) + Delete + Close
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // مصروف (Expense)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    mode = "EXPENSE"
                                    val list = activeExpenseList
                                    if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                        selectedCategory = list.firstOrNull()?.nameAr ?: ""
                                    }
                                }
                                .padding(top = 8.dp)
                                .testTag("mode_tab_expense"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "مصروف" else "Expense",
                                fontSize = 14.sp,
                                fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isIncome) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (!isIncome) XBlue else Color.Transparent)
                            )
                        }

                        // دخل (Income)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    mode = "INCOME"
                                    val list = activeIncomeList
                                    if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                        selectedCategory = list.firstOrNull()?.nameAr ?: ""
                                    }
                                }
                                .padding(top = 8.dp)
                                .testTag("mode_tab_income"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "دخل" else "Income",
                                fontSize = 14.sp,
                                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncome) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) XBlue else Color.Transparent)
                            )
                        }

                        if (initialTransaction != null && onDelete != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(InputChipSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = if (isArabic) "حذف" else "Delete",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(InputChipSurface)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = if (isArabic) "إغلاق" else "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Centered Amount Input Card (#121212 surface, 12dp radius)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, DividerBorderColor),
                    shadowElevation = 0.dp
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
                                text = if (isArabic) "المبلغ" else "Amount",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
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
                                        if (cleaned.isEmpty() || cleaned.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                                            amountString = cleaned
                                        }
                                    },
                                    modifier = Modifier
                                        .focusRequester(focusRequester)
                                        .testTag("transaction_amount_input")
                                        .width(IntrinsicSize.Min)
                                        .widthIn(min = 68.dp, max = 220.dp),
                                    textStyle = TextStyle(
                                        fontSize = 36.sp,
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
                                                    fontSize = 36.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMuted,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = CircleShape,
                                    color = InputChipSurface
                                ) {
                                    Text(
                                        text = currencySymbol,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
                                    .background(InputChipSurface)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = if (isArabic) "مسح" else "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Category Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "الفئة" else "Category",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Text(
                        text = selectedCategory,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = XBlue
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val gridColumns = 4
                val totalSlots = currentCategoryList.size + 1
                val rowsCount = (totalSlots + gridColumns - 1) / gridColumns

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    for (rowIndex in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            for (colIndex in 0 until gridColumns) {
                                val itemIndex = rowIndex * gridColumns + colIndex
                                if (itemIndex < currentCategoryList.size) {
                                    val cat = currentCategoryList[itemIndex]
                                    val isSelected = selectedCategory == cat.nameAr ||
                                        selectedCategory == cat.id ||
                                        selectedCategory == cat.nameEn
                                    CategoryPillItem(
                                        category = cat,
                                        isSelected = isSelected,
                                        isArabic = isArabic,
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedCategory = cat.nameAr }
                                    )
                                } else if (itemIndex == currentCategoryList.size) {
                                    Surface(
                                        shape = CircleShape,
                                        color = InputChipSurface,
                                        border = BorderStroke(0.8.dp, DividerBorderColor),
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(CircleShape)
                                            .clickable { showCustomCategoryDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = if (isArabic) "فئة جديدة" else "New Category",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isArabic) "مخصصة" else "Custom",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Date Row (#2A2A2A input box)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showStartDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = InputChipSurface,
                    border = BorderStroke(0.8.dp, DividerBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isArabic) "التاريخ" else "Date",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = Formatters.formatDate(startDateMillis, isArabic),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Save Transaction Button: #1DB954 background + black text, fully rounded
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = isSaveEnabled) {
                            onSave(
                                initialTransaction?.id ?: 0L,
                                mode,
                                parsedAmount,
                                selectedCategory.trim(),
                                "",
                                startDateMillis,
                                null,
                                debtNotes.trim(),
                                isPinned,
                                null,
                                null
                            )
                        }
                        .testTag("save_transaction_button"),
                    shape = CircleShape,
                    color = if (isSaveEnabled) SpotifyGreen else InputChipSurface,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSaveEnabled) {
                            Icon(
                                imageVector = if (initialTransaction != null) Icons.Outlined.Edit else Icons.Outlined.Add,
                                contentDescription = null,
                                tint = OnSpotifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when {
                                initialTransaction != null -> if (isArabic) "تحديث العملية" else "Update Entry"
                                isSaveEnabled -> {
                                    val formatted = Formatters.formatMoney(parsedAmount)
                                    if (isArabic) "حفظ • $formatted $currencySymbol" else "Save • $formatted $currencySymbol"
                                }
                                else -> if (isArabic) "حدد المبلغ" else "Set amount"
                            },
                            fontSize = 14.sp,
                            fontWeight = if (isSaveEnabled) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSaveEnabled) OnSpotifyGreen else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val initialUtc = remember(startDateMillis) {
            DateFilterUtils.localMillisToUtcMidnight(startDateMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = ElevatedSurface),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utc ->
                            startDateMillis = DateFilterUtils.utcMillisToLocalStartOfDay(utc)
                        }
                        showStartDatePicker = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(if (isArabic) "تأكيد" else "Confirm", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showStartDatePicker = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ElevatedSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = SpotifyGreen,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = SpotifyGreen,
                    selectedYearContainerColor = SpotifyGreen,
                    selectedYearContentColor = OnSpotifyGreen,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = SpotifyGreen,
                    selectedDayContentColor = OnSpotifyGreen,
                    todayDateBorderColor = SpotifyGreen,
                    todayContentColor = SpotifyGreen
                )
            )
        }
    }

    // Due Date Picker Dialog (for Debts)
    if (showDueDatePicker) {
        val baseMillis = dueDateMillis ?: System.currentTimeMillis()
        val initialUtc = remember(baseMillis) {
            DateFilterUtils.localMillisToUtcMidnight(baseMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = ElevatedSurface),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utc ->
                            dueDateMillis = DateFilterUtils.utcMillisToLocalStartOfDay(utc)
                        }
                        showDueDatePicker = false
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(if (isArabic) "تأكيد" else "Confirm", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDueDatePicker = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ElevatedSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = SpotifyGreen,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = SpotifyGreen,
                    selectedYearContainerColor = SpotifyGreen,
                    selectedYearContentColor = OnSpotifyGreen,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = SpotifyGreen,
                    selectedDayContentColor = OnSpotifyGreen,
                    todayDateBorderColor = SpotifyGreen,
                    todayContentColor = SpotifyGreen
                )
            )
        }
    }

    // Custom Category Dialog
    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "إضافة فئة جديدة" else "Add New Category",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "اكتب اسم الفئة الجديدة:" else "Enter category name:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = { customCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = InputChipSurface,
                            unfocusedContainerColor = InputChipSurface,
                            focusedBorderColor = SpotifyGreen,
                            unfocusedBorderColor = DividerBorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        placeholder = { Text(if (isArabic) "مثال: هدايا، كتب..." else "e.g., Gifts, Books...", color = TextMuted) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customCategoryName.trim()
                        if (name.isNotBlank()) {
                            val targetType = if (mode == "INCOME") "INCOME" else "EXPENSE"
                            onAddCustomCategory?.invoke(name, name, targetType, "label")
                            selectedCategory = name
                            customCategoryName = ""
                            showCustomCategoryDialog = false
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(if (isArabic) "إضافة" else "Add", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomCategoryDialog = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "تأكيد الحذف" else "Confirm Delete",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isArabic) "هل أنت متأكد من رغبتك في الحذف نهائياً؟" else "Are you sure you want to permanently delete this entry?",
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (initialDebt != null) {
                            onDeleteDebt?.invoke(initialDebt)
                        } else if (initialTransaction != null) {
                            onDelete?.invoke(initialTransaction)
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(if (isArabic) "حذف" else "Delete", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
private fun CategoryPillItem(
    category: CategoryItem,
    isSelected: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Spotify-style filter chip: fully rounded pill, #1DB954 + black text/icon when selected, #2A2A2A + white text/icon when unselected
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) SpotifyGreen else InputChipSurface,
        label = "pill_bg"
    )
    val contentColor = if (isSelected) OnSpotifyGreen else TextPrimary

    Surface(
        shape = CircleShape,
        color = animatedBg,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isArabic) category.nameAr else category.nameEn,
                fontSize = 11.2.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
