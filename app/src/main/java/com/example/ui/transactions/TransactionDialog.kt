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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PushPin
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

private data class QuickPaymentMethod(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val icon: ImageVector
)

private val DefaultPaymentMethods = listOf(
    QuickPaymentMethod("cash", "كاش", "Cash", Icons.Default.MonetizationOn),
    QuickPaymentMethod("card", "بطاقة بنكية", "Card", Icons.Default.CreditCard),
    QuickPaymentMethod("wallet", "محفظة إلكترونية", "E-Wallet", Icons.Default.Devices),
    QuickPaymentMethod("transfer", "تحويل بنكي", "Transfer", Icons.Default.AccountBalance)
)

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
    val currentCategoryList = if (type == "EXPENSE") activeExpenseList else activeIncomeList

    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: currentCategoryList.firstOrNull()?.nameAr ?: ""
        )
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: "كاش")
    }

    var selectedDateMillis by remember {
        mutableLongStateOf(initialTransaction?.dateMillis ?: System.currentTimeMillis())
    }
    var selectedEndDateMillis by remember {
        mutableStateOf<Long?>(initialTransaction?.endDateMillis)
    }
    var isRangeMode by remember {
        mutableStateOf(initialTransaction?.endDateMillis != null)
    }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var isPinned by remember { mutableStateOf(initialTransaction?.isPinned ?: false) }
    var receiptUri by remember { mutableStateOf(initialTransaction?.receiptUri) }
    var receiptMimeType by remember { mutableStateOf(initialTransaction?.receiptMimeType) }

    var showExtraDetails by remember {
        mutableStateOf(
            initialTransaction != null && (
                initialTransaction.notes.isNotBlank() ||
                initialTransaction.isPinned ||
                initialTransaction.receiptUri != null ||
                initialTransaction.endDateMillis != null
            )
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryName by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
            receiptMimeType = "image/*"
        }
    }

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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTransaction != null) {
                        if (isArabic) "تعديل المعاملة" else "Edit Transaction"
                    } else {
                        if (isArabic) "تسجيل معاملة سريعة" else "Quick Transaction"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = if (isArabic) "إغلاق" else "Close",
                        tint = TextSecondaryBrown,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Type Switch (مصروف / دخل)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F4F6),
                border = BorderStroke(0.8.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isExpense = type == "EXPENSE"
                    val isIncome = type == "INCOME"

                    // Expense Option
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                type = "EXPENSE"
                                val list = activeExpenseList
                                if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                    selectedCategory = list.firstOrNull()?.nameAr ?: ""
                                }
                            },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isExpense) MutedExpenseTerracotta else Color.Transparent,
                        shadowElevation = if (isExpense) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isExpense) Color.White else TextSecondaryBrown,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "مصروف" else "Expense",
                                fontSize = 13.5.sp,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpense) Color.White else TextSecondaryBrown
                            )
                        }
                    }

                    // Income Option
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .clickable {
                                type = "INCOME"
                                val list = activeIncomeList
                                if (list.none { it.nameAr == selectedCategory || it.id == selectedCategory }) {
                                    selectedCategory = list.firstOrNull()?.nameAr ?: ""
                                }
                            },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isIncome) MutedIncomeGreen else Color.Transparent,
                        shadowElevation = if (isIncome) 1.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isIncome) Color.White else TextSecondaryBrown,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "دخل" else "Income",
                                fontSize = 13.5.sp,
                                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncome) Color.White else TextSecondaryBrown
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Big Centered Amount Input with Currency Symbol
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (type == "EXPENSE") MutedExpenseLight.copy(alpha = 0.4f) else MutedIncomeGreenLight.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isArabic) "المبلغ" else "Amount",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryBrown
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
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
                                .widthIn(min = 60.dp, max = 240.dp),
                            textStyle = TextStyle(
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (type == "EXPENSE") MutedExpenseTerracotta else MutedIncomeGreen,
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
                            cursorBrush = SolidColor(if (type == "EXPENSE") MutedExpenseTerracotta else MutedIncomeGreen),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    if (amountString.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMutedBrown.copy(alpha = 0.4f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = currencySymbol,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryBrown
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. One-tap Category Selection (Grid/Row of Circular Icons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "اختر الفئة" else "Select Category",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Text(
                    text = selectedCategory.ifBlank { if (isArabic) "لم يتم التحديد" else "None" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (type == "EXPENSE") MutedExpenseTerracotta else MutedIncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Smooth Horizontal Category Carousel
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(currentCategoryList) { cat ->
                    val isSelected = selectedCategory == cat.nameAr || selectedCategory == cat.id || selectedCategory == cat.nameEn
                    val catColor = Color(cat.color)

                    CategoryCircleItem(
                        category = cat,
                        isSelected = isSelected,
                        catColor = catColor,
                        isArabic = isArabic,
                        onClick = {
                            selectedCategory = cat.nameAr
                        }
                    )
                }

                // Add Custom Category Chip
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(62.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCustomCategoryDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(WarmCardSurface)
                                .border(1.dp, BorderSubtle, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = if (isArabic) "فئة جديدة" else "New Category",
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isArabic) "+ مخصصة" else "+ Custom",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondaryBrown,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Payment Method Capsules (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "طريقة الدفع" else "Payment Method",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DefaultPaymentMethods.forEach { method ->
                    val isSelected = selectedPaymentMethod == method.nameAr || selectedPaymentMethod == method.nameEn || selectedPaymentMethod == method.id
                    val chipBg = if (isSelected) BurntOrangePrimary.copy(alpha = 0.12f) else Color.White
                    val chipBorder = if (isSelected) BurntOrangePrimary else BorderSubtle

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedPaymentMethod = method.nameAr },
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = method.icon,
                                contentDescription = null,
                                tint = if (isSelected) BurntOrangePrimary else TextSecondaryBrown,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) method.nameAr else method.nameEn,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BurntOrangePrimary else TextPrimaryDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Collapsible Extra Details (Notes, Date, Pin)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showExtraDetails = !showExtraDetails },
                shape = RoundedCornerShape(12.dp),
                color = WarmCardSurface.copy(alpha = 0.6f),
                border = BorderStroke(0.8.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = TextSecondaryBrown,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تفاصيل إضافية (التاريخ، الملاحظات، تثبيت)" else "Extra Details (Date, Notes, Pin)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )
                    }

                    Icon(
                        imageVector = if (showExtraDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextSecondaryBrown,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = showExtraDetails,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date / Range Selector Section
                    val formattedStartDate = remember(selectedDateMillis) {
                        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(selectedDateMillis))
                    }
                    val formattedEndDate = remember(selectedEndDateMillis) {
                        selectedEndDateMillis?.let {
                            SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(it))
                        } ?: (if (isArabic) "اختر تاريخ النهاية" else "Select end date")
                    }

                    // Mode Toggle: Single Date vs Date Range
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "تحديد نطاق زمني (فترة)" else "Date Range (Period)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimaryDark
                        )

                        Switch(
                            checked = isRangeMode,
                            onCheckedChange = { enabled ->
                                isRangeMode = enabled
                                if (!enabled) {
                                    selectedEndDateMillis = null
                                } else if (selectedEndDateMillis == null) {
                                    selectedEndDateMillis = selectedDateMillis
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BurntOrangePrimary
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (!isRangeMode) {
                        // Single Date Selector
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showStartDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "تاريخ المعاملة:" else "Transaction Date:",
                                    fontSize = 12.sp,
                                    color = TextSecondaryBrown
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formattedStartDate,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Range Selector: From Date -> To Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showStartDatePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = if (isArabic) "من تاريخ:" else "From Date:",
                                        fontSize = 11.sp,
                                        color = TextSecondaryBrown
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formattedStartDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = BurntOrangePrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            // End Date
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showEndDatePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = if (isArabic) "إلى تاريخ:" else "To Date:",
                                        fontSize = 11.sp,
                                        color = TextSecondaryBrown
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formattedEndDate,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedEndDateMillis != null) TextPrimaryDark else BurntOrangePrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = BurntOrangePrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Notes Field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (isArabic) "ملاحظات أو تفاصيل اختيارية..." else "Optional notes or details...",
                                fontSize = 12.sp,
                                color = TextMutedBrown
                            )
                        },
                        textStyle = TextStyle(fontSize = 12.5.sp, color = TextPrimaryDark),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BurntOrangePrimary,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        maxLines = 2
                    )

                    // Pin toggle & Receipt attachment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pin Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = isPinned,
                                onCheckedChange = { isPinned = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BurntOrangePrimary
                                ),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isArabic) "تثبيت في الرئيسية" else "Pin on Home",
                                fontSize = 12.sp,
                                color = TextPrimaryDark
                            )
                        }

                        // Attach Receipt Photo
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, BorderSubtle),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = if (receiptUri != null) MutedIncomeGreen else TextSecondaryBrown,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (receiptUri != null) {
                                    if (isArabic) "مرفق إيصال ✓" else "Receipt ✓"
                                } else {
                                    if (isArabic) "إرفاق إيصال" else "Add Receipt"
                                },
                                fontSize = 11.5.sp,
                                color = if (receiptUri != null) MutedIncomeGreen else TextSecondaryBrown
                            )
                        }
                    }

                    // Delete option if editing existing transaction
                    if (initialTransaction != null && onDelete != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MutedExpenseTerracotta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "حذف هذه المعاملة" else "Delete Transaction",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedExpenseTerracotta
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Big Prominent Save Button
            Button(
                onClick = {
                    if (isSaveEnabled) {
                        val finalEndDate = if (isRangeMode) selectedEndDateMillis else null
                        onSave(
                            initialTransaction?.id ?: 0L,
                            type,
                            parsedAmount,
                            selectedCategory.trim(),
                            selectedPaymentMethod.trim(),
                            selectedDateMillis,
                            finalEndDate,
                            notes.trim(),
                            isPinned,
                            receiptUri,
                            receiptMimeType
                        )
                    }
                },
                enabled = isSaveEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurntOrangePrimary,
                    disabledContainerColor = Color(0xFFE5E7EB)
                )
            ) {
                Text(
                    text = if (initialTransaction != null) {
                        if (isArabic) "تحديث المعاملة" else "Update Transaction"
                    } else {
                        if (isArabic) "تسجيل المعاملة" else "Save Transaction"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSaveEnabled) Color.White else TextMutedBrown
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(
                                    utcCal.get(Calendar.YEAR),
                                    utcCal.get(Calendar.MONTH),
                                    utcCal.get(Calendar.DAY_OF_MONTH),
                                    12, 0, 0
                                )
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDateMillis = localCal.timeInMillis
                            if (isRangeMode && selectedEndDateMillis != null && selectedEndDateMillis!! < selectedDateMillis) {
                                selectedEndDateMillis = selectedDateMillis
                            }
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text(if (isArabic) "تأكيد" else "OK", color = BurntOrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End Date Picker Dialog (for Range)
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEndDateMillis ?: selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val localCal = Calendar.getInstance().apply {
                                set(
                                    utcCal.get(Calendar.YEAR),
                                    utcCal.get(Calendar.MONTH),
                                    utcCal.get(Calendar.DAY_OF_MONTH),
                                    12, 0, 0
                                )
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endMillis = localCal.timeInMillis
                            selectedEndDateMillis = if (endMillis >= selectedDateMillis) endMillis else selectedDateMillis
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text(if (isArabic) "تأكيد" else "OK", color = BurntOrangePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextSecondaryBrown)
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
// Sub-component: Category Circle Item for Horizontal/Grid Selection
// -------------------------------------------------------------------------
@Composable
private fun CategoryCircleItem(
    category: CategoryItem,
    isSelected: Boolean,
    catColor: Color,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) catColor.copy(alpha = 0.22f) else catColor.copy(alpha = 0.08f),
        label = "cat_bg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(62.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(animatedBg)
                .border(
                    width = if (isSelected) 2.5.dp else 0.8.dp,
                    color = if (isSelected) catColor else BorderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.size(22.dp)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(catColor)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = if (isArabic) category.nameAr else category.nameEn,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimaryDark else TextSecondaryBrown,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
