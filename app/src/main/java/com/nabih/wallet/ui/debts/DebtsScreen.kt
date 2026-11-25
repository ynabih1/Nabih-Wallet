package com.nabih.wallet.ui.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.DebtType
import com.nabih.wallet.data.model.DebtSummary
import com.nabih.wallet.data.model.DebtWithPayments
import com.nabih.wallet.ui.WalletUiState
import com.nabih.wallet.ui.components.EmptyStateCard
import com.nabih.wallet.ui.components.ExpressiveLinearProgressIndicator
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.util.Formatters
import kotlin.math.abs

private enum class DebtFilter(val label: String) {
    ALL("الكل"),
    OWED_TO_ME("مستحق لي"),
    OWED_BY_ME("مستحق علي")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    uiState: WalletUiState,
    onPayDebt: (DebtWithPayments) -> Unit,
    onManagePayments: (DebtWithPayments) -> Unit,
    onEditDebt: (DebtWithPayments) -> Unit,
    onDeleteDebt: (DebtWithPayments) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var filter by remember { mutableStateOf(DebtFilter.ALL) }

    val filteredDebts = remember(uiState.debts, filter) {
        when (filter) {
            DebtFilter.ALL -> uiState.debts
            DebtFilter.OWED_TO_ME -> uiState.debts.filter { it.debt.type == DebtType.OWED_TO_ME }
            DebtFilter.OWED_BY_ME -> uiState.debts.filter { it.debt.type == DebtType.OWED_BY_ME }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        LargeTopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            title = {
                Text(
                    text = stringResource(R.string.nav_debts),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "debts_summary") {
                DebtsSummaryCard(summary = uiState.debtSummary)
            }

            item(key = "debts_filter") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DebtFilter.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = filter == option,
                            onClick = { filter = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DebtFilter.entries.size
                            ),
                            modifier = Modifier.testTag("debt_filter_${option.name.lowercase()}")
                        ) {
                            Text(option.label, maxLines = 1)
                        }
                    }
                }
            }

            if (filteredDebts.isEmpty()) {
                item(key = "empty_debts") {
                    EmptyStateCard(
                        icon = Icons.Rounded.Group,
                        title = stringResource(R.string.no_debts_yet),
                        subtitle = "الديون منفصلة تمامًا عن الرصيد العام والمصروفات، اضغط + لإضافة دين جديد"
                    )
                }
            } else {
                items(
                    items = filteredDebts,
                    key = { "debt_${it.debt.id}" }
                ) { item ->
                    DebtCard(
                        item = item,
                        onPayClick = { onPayDebt(item) },
                        onManagePaymentsClick = { onManagePayments(item) },
                        onEditClick = { onEditDebt(item) },
                        onDeleteClick = { onDeleteDebt(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtsSummaryCard(summary: DebtSummary) {
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    val (netText, netColor) = when {
        !summary.hasAnyDebts || abs(summary.netDebt) < 0.005 ->
            Pair(stringResource(R.string.no_debts_yet), MaterialTheme.colorScheme.onSurface)
        summary.netDebt < 0.0 ->
            Pair("عليك ${Formatters.formatAmount(abs(summary.netDebt))}", semantic.expense)
        else ->
            Pair("لك ${Formatters.formatAmount(summary.netDebt)}", semantic.income)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debts_summary_card"),
        shape = RoundedCornerShape(28.dp),
        color = container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.net_debts),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = netText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = semantic.incomeContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = null,
                                tint = semantic.onIncomeContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.owed_to_me),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = semantic.onIncomeContainer
                            )
                        }
                        Text(
                            text = Formatters.formatAmount(summary.totalOwedToMeRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = semantic.onIncomeContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    color = semantic.expenseContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = null,
                                tint = semantic.onExpenseContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.owed_by_me),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = semantic.onExpenseContainer
                            )
                        }
                        Text(
                            text = Formatters.formatAmount(summary.totalOwedByMeRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = semantic.onExpenseContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DebtCard(
    item: DebtWithPayments,
    onPayClick: () -> Unit,
    onManagePaymentsClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val remainingColor = if (item.debt.type == DebtType.OWED_BY_ME) {
        semantic.expense
    } else {
        semantic.income
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${item.debt.id}"),
        shape = RoundedCornerShape(28.dp),
        color = container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Clean Header: Squircle Avatar + Person Name + MoreVert menu ONLY
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = Formatters.initialsFor(item.debt.personName),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = item.debt.personName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("debt_menu_${item.debt.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "خيارات الدين"
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_debt_title)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.delete_debt),
                                    color = semantic.expense
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = semantic.expense
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    }
                }
            }

            // Remaining amount + original total
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = Formatters.formatAmount(item.remainingAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isFullyPaid) semantic.income else remainingColor
                )
                Text(
                    text = "المتبقي من ${Formatters.formatNumber(item.debt.originalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            // Progress bar + paid summary
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExpressiveLinearProgressIndicator(
                    progress = item.progressRatio.toFloat(),
                    activeColor = semantic.debtProgress,
                    height = 8.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "تم سداد ${Formatters.formatAmount(item.totalPaid)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatPercentInt(item.progressPercent),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = semantic.debtProgress
                    )
                }
            }

            // Pay CTA or Fully Paid Badge
            if (item.isFullyPaid) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    color = semantic.incomeContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = semantic.onIncomeContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.fully_paid),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = semantic.onIncomeContainer
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onPayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pay_debt_button_${item.debt.id}"),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pay_installment),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Embedded Payment History
            if (item.payments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.payment_history),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = item.payments.size.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    AssistChip(
                        onClick = onManagePaymentsClick,
                        label = { Text(stringResource(R.string.manage_payments)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("manage_payments_${item.debt.id}")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.payments.take(3).forEach { payment ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = semantic.income,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = Formatters.formatFullDate(payment.paidAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = Formatters.formatAmount(payment.amount),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (item.payments.size > 3) {
                    TextButton(
                        onClick = onManagePaymentsClick,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "عرض الكل (${item.payments.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
