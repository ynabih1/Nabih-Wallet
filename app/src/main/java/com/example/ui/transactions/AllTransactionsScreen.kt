package com.example.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.ui.WalletViewModel
import com.example.ui.components.DateRangeFilterBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionFilterChip
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.AppBackground
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class TransactionFilterType {
    ALL, EXPENSE, INCOME
}

@Composable
fun AllTransactionsScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val filterStartDate by viewModel.filterStartDate.collectAsStateWithLifecycle()
    val filterEndDate by viewModel.filterEndDate.collectAsStateWithLifecycle()
    val filterPreset by viewModel.filterPreset.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var filterType by remember { mutableStateOf(TransactionFilterType.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var transactionForDateChange by remember { mutableStateOf<TransactionEntity?>(null) }

    val filteredList = remember(allTransactions, filterStartDate, filterEndDate, filterType, searchQuery) {
        allTransactions.filter { tx ->
            val inDateRange = tx.dateMillis in filterStartDate..filterEndDate
            val matchesType = when (filterType) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.EXPENSE -> tx.type == "EXPENSE"
                TransactionFilterType.INCOME -> tx.type == "INCOME"
            }
            val matchesSearch = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true)
            inDateRange && matchesType && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Header with Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_from_all_transactions")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) "جميع المعاملات" else "All Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Range Filter Bar
        DateRangeFilterBar(
            startDateMillis = filterStartDate,
            endDateMillis = filterEndDate,
            selectedPreset = filterPreset,
            transactions = allTransactions,
            isArabic = isArabic,
            onDateRangeSelected = { start, end, preset ->
                viewModel.setDateFilter(start, end, preset)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar (#2A2A2A input field)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "بحث",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            },
            placeholder = {
                Text(
                    text = if (isArabic) "ابحث باسم الفئة..." else "Search by category...",
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            singleLine = true,
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
                .testTag("search_transactions_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Equal-width Filter chips in Spotify pill style (الكل | المصروفات | الدخل)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TransactionFilterChip(
                text = stringResource(R.string.filter_all),
                selected = filterType == TransactionFilterType.ALL,
                onClick = { filterType = TransactionFilterType.ALL },
                testTag = "filter_tx_all",
                modifier = Modifier.weight(1f)
            )

            TransactionFilterChip(
                text = stringResource(R.string.expense),
                selected = filterType == TransactionFilterType.EXPENSE,
                onClick = { filterType = TransactionFilterType.EXPENSE },
                testTag = "filter_tx_expense",
                modifier = Modifier.weight(1f)
            )

            TransactionFilterChip(
                text = stringResource(R.string.income),
                selected = filterType == TransactionFilterType.INCOME,
                onClick = { filterType = TransactionFilterType.INCOME },
                testTag = "filter_tx_income",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.ReceiptLong,
                title = if (isArabic) "لا توجد معاملات في هذه الفترة" else "No transactions in this period",
                description = if (isArabic) "جرب تغيير نطاق التاريخ أو الفلتر لعرض المعاملات المسجلة" else "Try adjusting the date range or filter to view transactions",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        onClick = { onEditTransaction(tx) },
                        onQuickChangeDate = { transactionForDateChange = it }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(112.dp))
                }
            }
        }
    }

    transactionForDateChange?.let { tx ->
        com.example.ui.components.QuickChangeDateDialog(
            transaction = tx,
            isArabic = isArabic,
            currencySymbol = currencySymbol,
            onDismiss = { transactionForDateChange = null },
            onConfirm = { newDate ->
                viewModel.updateTransactionDate(tx, newDate)
            }
        )
    }
}
