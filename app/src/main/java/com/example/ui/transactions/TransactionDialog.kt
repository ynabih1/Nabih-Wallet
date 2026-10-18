package com.example.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.local.entity.TransactionEntity
import com.example.model.CategoryItem
import com.example.model.FinancialConstants
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeLight
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseLight
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.MutedIncomeGreenLight
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    initialTransaction: TransactionEntity? = null,
    existingTransactions: List<TransactionEntity> = emptyList(),
    customExpenseCategories: List<CategoryItem>? = null,
    customIncomeCategories: List<CategoryItem>? = null,
    savedPaymentMethods: List<String> = emptyList(),
    onDeletePaymentMethod: ((String) -> Unit)? = null,
    onUpdateCategoryIcon: ((categoryName: String, iconKey: String) -> Unit)? = null,
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
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var type by remember { mutableStateOf(initialTransaction?.type ?: "EXPENSE") }
    var amountString by remember {
        mutableStateOf(
            if (initialTransaction != null && initialTransaction.amount > 0) {
                if (initialTransaction.amount % 1.0 == 0.0) {
                    initialTransaction.amount.toLong().toString()
                } else {
                    initialTransaction.amount.toString()
                }
            } else ""
        )
    }

    val activeExpenseList = customExpenseCategories ?: FinancialConstants.expenseCategories
    val activeIncomeList = customIncomeCategories ?: FinancialConstants.incomeCategories

    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: (if (type == "EXPENSE") activeExpenseList.firstOrNull()?.nameAr else activeIncomeList.firstOrNull()?.nameAr) ?: ""
        )
    }

    // All categories available with improved display layout
    val currentCategoryList = if (type == "EXPENSE") activeExpenseList else activeIncomeList

    val selectedDateMillis = remember {
        initialTransaction?.dateMillis ?: System.currentTimeMillis()
    }
    val notes = initialTransaction?.notes ?: ""
    val isPinned = initialTransaction?.isPinned ?: false
    var receiptUri by remember { mutableStateOf(initialTransaction?.receiptUri) }
    var receiptMimeType by remember { mutableStateOf(initialTransaction?.receiptMimeType) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
            receiptMimeType = "image/*"
        }
    }

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Auto-focus amount field when bottom sheet opens
    LaunchedEffect(Unit) {
        delay(180)
        focusRequester.requestFocus()
    }

    val parsedAmount = amountString.toDoubleOrNull() ?: 0.0
    val isSaveEnabled = parsedAmount > 0.0 && selectedCategory.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = BorderSubtle,
                width = 38.dp,
                height = 4.dp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Unified Top Row: Type Switcher Pills + Delete (if editing) + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isExpense = type == "EXPENSE"
                val isIncome = type == "INCOME"

                // Expense Pill Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            type = "EXPENSE"
                            val list = activeExpenseList
                            if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                selectedCategory = list.firstOrNull()?.nameAr ?: ""
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isExpense) BurntOrangePrimary else Color(0xFFFAF9F6),
                    border = BorderStroke(0.6.dp, if (isExpense) BurntOrangePrimary else BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isExpense) Color.White.copy(alpha = 0.22f) else BurntOrangePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isExpense) Color.White else BurntOrangePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "مصروف" else "Expense",
                            fontSize = 13.sp,
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isExpense) Color.White else TextPrimaryDark
                        )
                    }
                }

                // Income Pill Button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            type = "INCOME"
                            val list = activeIncomeList
                            if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                selectedCategory = list.firstOrNull()?.nameAr ?: ""
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isIncome) MutedIncomeGreen else Color(0xFFFAF9F6),
                    border = BorderStroke(0.6.dp, if (isIncome) MutedIncomeGreen else BorderSubtle),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(if (isIncome) Color.White.copy(alpha = 0.22f) else MutedIncomeGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isIncome) Color.White else MutedIncomeGreen,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "دخل" else "Income",
                            fontSize = 13.sp,
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isIncome) Color.White else TextPrimaryDark
                        )
                    }
                }

                if (initialTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(MutedExpenseLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف" else "Delete",
                            tint = MutedExpenseTerracotta,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFF3F1EC))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Compact & Balanced Centered Amount Input Card
            val activeAccent = if (type == "EXPENSE") BurntOrangePrimary else MutedIncomeGreen
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFAF9F6),
                border = BorderStroke(0.7.dp, activeAccent.copy(alpha = 0.22f)),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "المبلغ" else "Amount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryBrown
                        )

                        Spacer(modifier = Modifier.height(2.dp))

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
                                shape = RoundedCornerShape(8.dp),
                                color = activeAccent.copy(alpha = 0.12f),
                                border = BorderStroke(0.7.dp, activeAccent.copy(alpha = 0.28f))
                            ) {
                                Text(
                                    text = currencySymbol,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = activeAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (amountString.isNotBlank()) {
                        IconButton(
                            onClick = { amountString = "" },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(26.dp)
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

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Clean Category Header & Symmetrical 4-Column Category Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "الفئة" else "Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = selectedCategory,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = activeAccent
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Symmetrical 4-Column Grid (15 Expense + 1 Custom = 4x4; 7 Income + 1 Custom = 2x4)
            val gridColumns = 4
            val totalSlots = currentCategoryList.size + 1 // +1 for "+ مخصصة" button
            val rowsCount = (totalSlots + gridColumns - 1) / gridColumns

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (rowIndex in 0 until rowsCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                // Add Custom Category Cell matching exact pill dimensions
                                Surface(
                                    shape = RoundedCornerShape(11.dp),
                                    color = Color(0xFFFAF9F6),
                                    border = BorderStroke(0.6.dp, BurntOrangePrimary.copy(alpha = 0.35f)),
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(11.dp))
                                        .clickable { showCustomCategoryDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(BurntOrangePrimary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = if (isArabic) "فئة جديدة" else "New Category",
                                                tint = BurntOrangePrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isArabic) "مخصصة" else "Custom",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BurntOrangePrimary,
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

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Compact Bill / Receipt Attachment Row
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (receiptUri == null) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                color = if (receiptUri != null) MutedIncomeGreenLight.copy(alpha = 0.45f) else Color(0xFFFAF9F6),
                border = BorderStroke(
                    0.6.dp,
                    if (receiptUri != null) MutedIncomeGreen.copy(alpha = 0.4f) else BorderSubtle
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (receiptUri != null) MutedIncomeGreen.copy(alpha = 0.18f)
                                    else Color(0xFFEDEAE2)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (receiptUri != null) MutedIncomeGreen else TextSecondaryBrown,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (receiptUri != null) {
                                if (isArabic) "فاتورة مرفقة • اضغط للتغيير" else "Bill attached • Tap to change"
                            } else {
                                if (isArabic) "إرفاق صورة الفاتورة" else "Attach bill photo"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (receiptUri != null) FontWeight.Bold else FontWeight.Medium,
                            color = if (receiptUri != null) MutedIncomeGreen else TextPrimaryDark
                        )
                    }

                    if (receiptUri != null) {
                        IconButton(
                            onClick = {
                                receiptUri = null
                                receiptMimeType = null
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isArabic) "إزالة" else "Remove",
                                tint = MutedExpenseTerracotta,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = TextSecondaryBrown,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Save Button
            val buttonColor = if (type == "EXPENSE") BurntOrangePrimary else MutedIncomeGreen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = isSaveEnabled) {
                        onSave(
                            initialTransaction?.id ?: 0L,
                            type,
                            parsedAmount,
                            selectedCategory.trim(),
                            "",
                            selectedDateMillis,
                            null,
                            notes.trim(),
                            isPinned,
                            receiptUri,
                            receiptMimeType
                        )
                    }
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSaveEnabled) buttonColor else Color(0xFFF3F1EC),
                border = BorderStroke(0.6.dp, if (isSaveEnabled) buttonColor else BorderSubtle),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isSaveEnabled) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialTransaction != null) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (initialTransaction != null) {
                            if (isArabic) "تحديث" else "Update"
                        } else if (isSaveEnabled) {
                            val formatted = Formatters.formatMoney(parsedAmount)
                            if (isArabic) "حفظ • $formatted $currencySymbol" else "Save • $formatted $currencySymbol"
                        } else {
                            if (isArabic) "حدد المبلغ" else "Set amount"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = if (isSaveEnabled) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSaveEnabled) Color.White else TextSecondaryBrown
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }

    // Custom Category Dialog
    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إضافة فئة جديدة" else "Add New Category",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isArabic) "اكتب اسم الفئة الجديدة:" else "Enter category name:",
                        fontSize = 13.sp,
                        color = TextSecondaryBrown
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customCategoryName,
                        onValueChange = { customCategoryName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text(if (isArabic) "مثال: هدايا، كتب..." else "e.g., Gifts, Books...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = customCategoryName.trim()
                        if (name.isNotBlank()) {
                            onAddCustomCategory?.invoke(name, name, type, "label")
                            selectedCategory = name
                            customCategoryName = ""
                            showCustomCategoryDialog = false
                        }
                    }
                ) {
                    Text(if (isArabic) "إضافة" else "Add", color = BurntOrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && initialTransaction != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تأكيد الحذف" else "Confirm Delete",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            },
            text = {
                Text(
                    text = if (isArabic) "هل أنت متأكد من رغبتك في حذف هذه المعاملة نهائياً؟" else "Are you sure you want to permanently delete this transaction?",
                    fontSize = 13.5.sp,
                    color = TextSecondaryBrown
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(initialTransaction)
                    }
                ) {
                    Text(if (isArabic) "حذف" else "Delete", color = MutedExpenseTerracotta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
        )
    }
}

// -------------------------------------------------------------------------
// Sub-component: Uniform Grid Category Pill Item
// -------------------------------------------------------------------------
@Composable
private fun CategoryPillItem(
    category: CategoryItem,
    isSelected: Boolean,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val catColor = Color(category.color)
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) catColor else Color(0xFFFAF9F6),
        label = "pill_bg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) catColor else BorderSubtle,
        label = "pill_border"
    )

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = animatedBg,
        border = BorderStroke(0.6.dp, animatedBorder),
        shadowElevation = 0.dp,
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(11.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else catColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else catColor,
                    modifier = Modifier.size(12.5.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isArabic) category.nameAr else category.nameEn,
                fontSize = 11.2.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color.White else TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
