package com.nabih.wallet.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.ui.components.EmptyStateCard
import java.util.Calendar

private enum class TransactionTypeFilter(val label: String) {
    ALL("الكل"),
    EXPENSE("مصروفات"),
    INCOME("إيرادات")
}

private enum class TransactionPeriodFilter(val label: String) {
    ALL("كل الفترات"),
    THIS_WEEK("آخر 7 أيام"),
    THIS_MONTH("هذا الشهر"),
    LAST_3_MONTHS("آخر 3 شهور")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTransactionsScreen(
    transactions: List<TransactionItem>,
    onBack: () -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TransactionTypeFilter.ALL) }
    var periodFilter by remember { mutableStateOf(TransactionPeriodFilter.ALL) }

    val filtered = remember(transactions, searchQuery, typeFilter, periodFilter) {
        val now = System.currentTimeMillis()
        val cutoffMillis: Long? = when (periodFilter) {
            TransactionPeriodFilter.ALL -> null
            TransactionPeriodFilter.THIS_WEEK -> now - 7L * 24L * 60L * 60L * 1000L
            TransactionPeriodFilter.THIS_MONTH -> Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            TransactionPeriodFilter.LAST_3_MONTHS -> Calendar.getInstance().apply {
                add(Calendar.MONTH, -3)
            }.timeInMillis
        }

        val query = searchQuery.trim().lowercase()
        transactions.filter { item ->
            val matchesType = when (typeFilter) {
                TransactionTypeFilter.ALL -> true
                TransactionTypeFilter.EXPENSE -> item.entity.type == TransactionType.EXPENSE
                TransactionTypeFilter.INCOME -> item.entity.type == TransactionType.INCOME
            }
            val matchesPeriod = cutoffMillis == null || item.entity.timestamp >= cutoffMillis
            val matchesQuery = query.isEmpty() ||
                item.categoryName.lowercase().contains(query) ||
                item.entity.note.lowercase().contains(query)
            matchesType && matchesPeriod && matchesQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("all_tx_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.all_transactions_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("all_tx_search_input"),
                placeholder = { Text(stringResource(R.string.search_transactions_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionTypeFilter.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = typeFilter == option,
                        onClick = { typeFilter = option },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransactionTypeFilter.entries.size
                        ),
                        modifier = Modifier.testTag("all_tx_filter_${option.name.lowercase()}")
                    ) {
                        Text(option.label, maxLines = 1)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionPeriodFilter.entries.forEach { period ->
                    FilterChip(
                        selected = periodFilter == period,
                        onClick = { periodFilter = period },
                        label = { Text(period.label) },
                        modifier = Modifier.testTag("period_chip_${period.name.lowercase()}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 32.dp
            )
        ) {
            if (filtered.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.SearchOff,
                        title = "لا توجد عمليات مطابقة",
                        subtitle = "جرّب تغيير البحث أو الفلاتر لعرض عمليات أخرى"
                    )
                }
            } else {
                itemsIndexed(
                    items = filtered,
                    key = { _, item -> "all_tx_${item.entity.id}" }
                ) { index, item ->
                    TransactionRowItem(
                        item = item,
                        index = index,
                        count = filtered.size,
                        onClick = { onTransactionClick(item) }
                    )
                    if (index < filtered.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}
