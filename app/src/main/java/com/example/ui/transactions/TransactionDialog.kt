package com.example.ui.transactions

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.TransactionEntity
import com.example.model.CategoryItem
import com.example.model.FinancialConstants
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBeigeBackground
import com.example.ui.theme.WarmCardSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDialog(
    initialTransaction: TransactionEntity? = null,
    existingTransactions: List<TransactionEntity> = emptyList(),
    customExpenseCategories: List<CategoryItem>? = null,
    customIncomeCategories: List<CategoryItem>? = null,
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
        notes: String,
        isPinned: Boolean,
        receiptUri: String?,
        receiptMimeType: String?
    ) -> Unit,
    onDelete: ((TransactionEntity) -> Unit)? = null
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: "EXPENSE") }
    var amountString by remember {
        mutableStateOf(if (initialTransaction != null && initialTransaction.amount > 0) initialTransaction.amount.toString() else "")
    }

    val activeExpenseList = customExpenseCategories ?: FinancialConstants.expenseCategories
    val activeIncomeList = customIncomeCategories ?: FinancialConstants.incomeCategories

    val defaultCategory = if (type == "EXPENSE") {
        activeExpenseList.first().let { if (isArabic) it.nameAr else it.nameEn }
    } else {
        activeIncomeList.first().let { if (isArabic) it.nameAr else it.nameEn }
    }

    var selectedCategory by remember {
        mutableStateOf(initialTransaction?.category ?: defaultCategory)
    }

    // Dropdown / Expansion states for Category and Payment Method
    var isCategoryExpanded by remember { mutableStateOf(false) }
    var isPaymentMethodExpanded by remember { mutableStateOf(false) }

    val normalizedSelected = FinancialConstants.normalizeCategoryName(selectedCategory)
    val existingMatch = remember(type, normalizedSelected, existingTransactions) {
        if (initialTransaction != null) null
        else existingTransactions.firstOrNull {
            it.type == type && FinancialConstants.normalizeCategoryName(it.category) == normalizedSelected
        }
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: FinancialConstants.paymentMethods.first().let { if (isArabic) it.nameAr else it.nameEn })
    }

    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var isPinned by remember { mutableStateOf(initialTransaction?.isPinned ?: false) }
    var receiptUri by remember { mutableStateOf(initialTransaction?.receiptUri) }
    var receiptMimeType by remember { mutableStateOf(initialTransaction?.receiptMimeType) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // File picker launcher for receipts (Images or PDF)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
            receiptMimeType = if (uri.toString().endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*"
        }
    }

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
                    text = if (isArabic) {
                        if (initialTransaction == null) "إضافة عملية" else "تعديل العملية"
                    } else {
                        if (initialTransaction == null) "Add Transaction" else "Edit Transaction"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                if (initialTransaction != null && onDelete != null) {
                    IconButton(
                        onClick = {
                            onDelete(initialTransaction)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("delete_transaction_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = if (isArabic) "حذف العملية" else "Delete Transaction",
                            tint = MutedExpenseTerracotta
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Transaction Type Toggle: Expense vs Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmCardSurface)
                        .padding(4.dp)
                ) {
                    val isExpense = type == "EXPENSE"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpense) MutedExpenseTerracotta else Color.Transparent)
                            .clickable {
                                if (type != "EXPENSE") {
                                    type = "EXPENSE"
                                    selectedCategory = activeExpenseList.first().let { if (isArabic) it.nameAr else it.nameEn }
                                    isCategoryExpanded = false
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "المصروفات" else "Expenses",
                            color = if (isExpense) Color.White else TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isExpense) MutedIncomeGreen else Color.Transparent)
                            .clickable {
                                if (type != "INCOME") {
                                    type = "INCOME"
                                    selectedCategory = activeIncomeList.first().let { if (isArabic) it.nameAr else it.nameEn }
                                    isCategoryExpanded = false
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isArabic) "الدخل" else "Income",
                            color = if (!isExpense) Color.White else TextPrimaryDark,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Amount Input Field (Strictly Numeric Keyboard)
                OutlinedTextField(
                    value = amountString,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountString = input
                            errorMessage = null
                        }
                    },
                    label = { Text(if (isArabic) "المبلغ" else "Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { Text(it, color = MutedExpenseTerracotta) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                // 3. Category Selector (Dropdown / Click-to-reveal with black/dark styling)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "التصنيف" else "Category",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val selectedCategoryIcon = FinancialConstants.getCategoryIcon(selectedCategory)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isCategoryExpanded = !isCategoryExpanded
                                if (isCategoryExpanded) isPaymentMethodExpanded = false
                            }
                            .testTag("select_category_trigger"),
                        color = WarmCardSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isCategoryExpanded) 1.5.dp else 1.dp,
                            color = if (isCategoryExpanded) BurntOrangePrimary else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val selectedCatColor = FinancialConstants.getCategoryColor(selectedCategory)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(selectedCatColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedCategoryIcon,
                                        contentDescription = null,
                                        tint = selectedCatColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = selectedCategory,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Icon(
                                imageVector = if (isCategoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isArabic) "توسيع التصنيفات" else "Expand Categories",
                                tint = TextPrimaryDark
                            )
                        }
                    }

                    // Expandable Categories Grid/List
                    AnimatedVisibility(
                        visible = isCategoryExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = WarmBeigeBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            val activeCategories = if (type == "EXPENSE") activeExpenseList else activeIncomeList
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    activeCategories.forEach { cat ->
                                        val catName = if (isArabic) cat.nameAr else cat.nameEn
                                        val isSelected = selectedCategory == catName || selectedCategory == cat.id || selectedCategory == cat.nameAr || selectedCategory == cat.nameEn
                                        val catIcon = FinancialConstants.getCategoryIcon(cat.nameAr)
                                        val catColor = Color(cat.color)

                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedCategory = catName
                                                    isCategoryExpanded = false
                                                }
                                                .testTag("cat_chip_${cat.id}"),
                                            color = if (isSelected) catColor else catColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) catColor else catColor.copy(alpha = 0.35f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) Color.White else catColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = catName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else TextPrimaryDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (existingMatch != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = BurntOrangePrimary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BurntOrangePrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = BurntOrangePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "بند مسجل مسبقاً: ${Formatters.formatMoney(existingMatch.amount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BurntOrangePrimary
                                )
                                Text(
                                    text = "سيتم تجميع المبلغ الجديد في نفس الأيقونة الحالية وتحديث رصيدها",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryBrown
                                )
                            }
                        }
                    }
                }

                // 4. Payment Method Selector (Dropdown / Click-to-reveal)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "طريقة الدفع" else "Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val selectedMethodItem = FinancialConstants.paymentMethods.firstOrNull {
                        it.nameAr == selectedPaymentMethod || it.nameEn == selectedPaymentMethod || it.id == selectedPaymentMethod
                    } ?: FinancialConstants.paymentMethods.first()

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isPaymentMethodExpanded = !isPaymentMethodExpanded
                                if (isPaymentMethodExpanded) isCategoryExpanded = false
                            }
                            .testTag("select_payment_method_trigger"),
                        color = WarmCardSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isPaymentMethodExpanded) 1.5.dp else 1.dp,
                            color = if (isPaymentMethodExpanded) BurntOrangePrimary else BorderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE5E2D8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = selectedMethodItem.icon,
                                        contentDescription = null,
                                        tint = TextPrimaryDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = if (isArabic) selectedMethodItem.nameAr else selectedMethodItem.nameEn,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Icon(
                                imageVector = if (isPaymentMethodExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isArabic) "توسيع طرق الدفع" else "Expand Payment Methods",
                                tint = TextPrimaryDark
                            )
                        }
                    }

                    // Expandable Payment Methods Grid
                    AnimatedVisibility(
                        visible = isPaymentMethodExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            color = WarmBeigeBackground,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FinancialConstants.paymentMethods.forEach { method ->
                                    val methodName = if (isArabic) method.nameAr else method.nameEn
                                    val isSelected = selectedPaymentMethod == methodName || selectedPaymentMethod == method.id || selectedPaymentMethod == method.nameAr || selectedPaymentMethod == method.nameEn

                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedPaymentMethod = methodName
                                                isPaymentMethodExpanded = false
                                            }
                                            .testTag("payment_method_${method.id}"),
                                        color = if (isSelected) DarkOliveCard else WarmCardSurface,
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = method.icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else TextPrimaryDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = methodName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else TextPrimaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isArabic) "ملاحظات" else "Notes") },
                    placeholder = { Text(if (isArabic) "اكتب تفاصيل أو ملاحظات إضافية..." else "Enter notes or details...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // 6. Pin Transaction Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = BurntOrangePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "تثبيت في الأعلى" else "Pin to top",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimaryDark
                        )
                    }

                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BurntOrangePrimary
                        )
                    )
                }

                // 7. Attach Receipt (Image / PDF)
                Column {
                    Text(
                        text = if (isArabic) "إرفاق إيصال صورة أو PDF" else "Attach Receipt Image or PDF",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (receiptUri == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    filePickerLauncher.launch("*/*")
                                }
                                .testTag("attach_receipt_button"),
                            color = WarmCardSurface,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = BurntOrangePrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) "إرفاق إيصال صورة أو PDF" else "Attach receipt Image or PDF",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BurntOrangePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        // Attached file display
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = WarmCardSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = BurntOrangePrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "تم إرفاق إيصال" else "Receipt attached",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimaryDark
                                    )
                                }

                                IconButton(onClick = {
                                    receiptUri = null
                                    receiptMimeType = null
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = if (isArabic) "إزالة الإيصال" else "Remove receipt",
                                        tint = MutedExpenseTerracotta,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountString.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = if (isArabic) "يرجى إدخال مبلغ صحيح أكبر من الصفر" else "Please enter a valid amount greater than zero"
                    } else {
                        onSave(
                            initialTransaction?.id ?: 0,
                            type,
                            amount,
                            selectedCategory,
                            selectedPaymentMethod,
                            initialTransaction?.dateMillis ?: System.currentTimeMillis(),
                            notes.trim(),
                            isPinned,
                            receiptUri,
                            receiptMimeType
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text(
                    text = if (existingMatch != null && initialTransaction == null) {
                        if (isArabic) "إضافة إلى الرصيد الحالي" else "Add to existing balance"
                    } else {
                        if (isArabic) "حفظ" else "Save"
                    },
                    color = Color.White
                )
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
