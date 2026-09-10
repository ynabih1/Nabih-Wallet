package com.example.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.ui.WalletViewModel
import com.example.ui.components.TransactionRowItem
import com.example.ui.debts.DebtFilterChip
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface

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
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol = viewModel.getCurrencySymbol()
    val isArabic = language == "ar"

    var filterType by remember { mutableStateOf(TransactionFilterType.ALL) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = allTransactions.filter { tx ->
        val matchesType = when (filterType) {
            TransactionFilterType.ALL -> true
            TransactionFilterType.EXPENSE -> tx.type == "EXPENSE"
            TransactionFilterType.INCOME -> tx.type == "INCOME"
        }
        val matchesSearch = searchQuery.isBlank() ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = TextPrimaryDark
                )
            }
            Text(
                text = stringResource(R.string.recent_transactions),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = TextSecondaryBrown)
            },
            placeholder = { Text(stringResource(R.string.notes_hint)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_transactions_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips (All / Expense / Income)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DebtFilterChip(
                text = stringResource(R.string.filter_all),
                selected = filterType == TransactionFilterType.ALL,
                onClick = { filterType = TransactionFilterType.ALL },
                testTag = "filter_tx_all"
            )

            DebtFilterChip(
                text = stringResource(R.string.expense),
                selected = filterType == TransactionFilterType.EXPENSE,
                onClick = { filterType = TransactionFilterType.EXPENSE },
                testTag = "filter_tx_expense"
            )

            DebtFilterChip(
                text = stringResource(R.string.income),
                selected = filterType == TransactionFilterType.INCOME,
                onClick = { filterType = TransactionFilterType.INCOME },
                testTag = "filter_tx_income"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredList.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = WarmCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_transactions_yet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondaryBrown
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { tx ->
                    TransactionRowItem(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        isArabic = isArabic,
                        onClick = { onEditTransaction(tx) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
