package com.nabih.wallet.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.data.model.DayActivityPoint
import com.nabih.wallet.data.model.DebtSummary
import com.nabih.wallet.data.model.MonthComparisonTrend
import com.nabih.wallet.data.model.MonthlyComparison
import com.nabih.wallet.data.model.TransactionItem
import com.nabih.wallet.ui.WalletUiState
import com.nabih.wallet.ui.components.EmptyStateCard
import com.nabih.wallet.ui.components.ExpressiveCircularProgress
import com.nabih.wallet.ui.components.ExpressiveSegmentedCardItem
import com.nabih.wallet.ui.components.SectionHeaderRow
import com.nabih.wallet.ui.components.TonalCategoryIcon
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.util.Formatters
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: WalletUiState,
    onOpenReminderSettings: () -> Unit,
    onOpenDebtsTab: () -> Unit,
    onOpenAllTransactions: () -> Unit,
    onTransactionClick: (TransactionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonthName = remember {
        Formatters.monthName(Calendar.getInstance().get(Calendar.MONTH) + 1)
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
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nabih_wallet_logo),
                        contentDescription = stringResource(R.string.cd_app_logo),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            actions = {
                FilledTonalIconButton(
                    onClick = onOpenReminderSettings,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("home_reminder_button"),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (uiState.preferences.dailyReminderEnabled) {
                            Icons.Rounded.NotificationsActive
                        } else {
                            Icons.Rounded.Notifications
                        },
                        contentDescription = stringResource(R.string.cd_notifications)
                    )
                }
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
            item(key = "hero_balance") {
                HeroBalanceCard(
                    netBalance = uiState.netBalance,
                    totalIncome = uiState.totalIncome,
                    totalExpense = uiState.totalExpense
                )
            }

            item(key = "bento_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThisMonthBentoCard(
                        monthName = currentMonthName,
                        comparison = uiState.monthlyComparison,
                        modifier = Modifier.weight(1f)
                    )
                    DebtsBentoCard(
                        summary = uiState.debtSummary,
                        onClick = onOpenDebtsTab,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "last_7_days") {
                Last7DaysActivityCard(days = uiState.last7DaysActivity)
            }

            item(key = "recent_header") {
                SectionHeaderRow(
                    title = stringResource(R.string.recent_transactions),
                    trailing = {
                        if (uiState.transactions.isNotEmpty()) {
                            TextButton(
                                onClick = onOpenAllTransactions,
                                modifier = Modifier.testTag("view_all_transactions_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.view_all),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }

            if (uiState.recentTransactions.isEmpty()) {
                item(key = "empty_recent") {
                    EmptyStateCard(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = stringResource(R.string.no_transactions_yet),
                        subtitle = stringResource(R.string.no_transactions_subtitle)
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.recentTransactions,
                    key = { _, item -> "tx_${item.entity.id}" }
                ) { index, item ->
                    TransactionRowItem(
                        item = item,
                        index = index,
                        count = uiState.recentTransactions.size,
                        onClick = { onTransactionClick(item) }
                    )
                    if (index < uiState.recentTransactions.lastIndex) {
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    netBalance: Double,
    totalIncome: Double,
    totalExpense: Double
) {
    val semantic = LocalWalletSemanticColors.current
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(semantic.heroStart, semantic.heroEnd)
                )
            )
            .padding(22.dp)
            .testTag("hero_balance_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.available_balance),
                style = MaterialTheme.typography.labelLarge,
                color = semantic.heroOnSurfaceMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = Formatters.formatNumber(netBalance),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = semantic.heroOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.currency_egp),
                    style = MaterialTheme.typography.titleMedium,
                    color = semantic.heroOnSurfaceMuted,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroSummaryTile(
                    label = stringResource(R.string.income_label),
                    amountText = Formatters.formatAmount(totalIncome),
                    icon = Icons.Rounded.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                HeroSummaryTile(
                    label = stringResource(R.string.expense_label),
                    amountText = if (totalExpense > 0.0) {
                        Formatters.formatSignedExpense(totalExpense)
                    } else {
                        Formatters.formatAmount(0.0)
                    },
                    icon = Icons.Rounded.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroSummaryTile(
    label: String,
    amountText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val semantic = LocalWalletSemanticColors.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = semantic.heroTileSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = semantic.heroOnSurfaceMuted,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = semantic.heroOnSurfaceMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = semantic.heroOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThisMonthBentoCard(
    monthName: String,
    comparison: MonthlyComparison,
    modifier: Modifier = Modifier
) {
    val semantic = LocalWalletSemanticColors.current
    Surface(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.this_month),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = Formatters.formatNumber(comparison.currentMonthExpense),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ج.م • $monthName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                )
            }

            val (pillBg, pillFg, pillText, pillIcon) = when (comparison.trend) {
                MonthComparisonTrend.LESS -> Quad(
                    semantic.incomeContainer,
                    semantic.onIncomeContainer,
                    "أقل بـ ${comparison.percentChange}%",
                    Icons.Rounded.TrendingDown
                )
                MonthComparisonTrend.MORE -> Quad(
                    semantic.expenseContainer,
                    semantic.onExpenseContainer,
                    "أكثر بـ ${comparison.percentChange}%",
                    Icons.Rounded.TrendingUp
                )
                MonthComparisonTrend.SAME -> Quad(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    "مطابق للشهر السابق",
                    null
                )
                MonthComparisonTrend.NO_PREVIOUS_DATA -> Quad(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    stringResource(R.string.start_of_month),
                    null
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = pillBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (pillIcon != null) {
                        Icon(
                            imageVector = pillIcon,
                            contentDescription = null,
                            tint = pillFg,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = pillText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = pillFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun DebtsBentoCard(
    summary: DebtSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        modifier = modifier
            .height(160.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .testTag("home_debts_bento_card"),
        shape = shape,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.nav_debts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
                ExpressiveCircularProgress(
                    progress = summary.overallProgressRatio.toFloat(),
                    size = 48.dp,
                    strokeWidth = 5.dp,
                    activeColor = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.14f),
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Column {
                val (subLabel, mainValue) = when {
                    !summary.hasAnyDebts || abs(summary.netDebt) < 0.005 ->
                        Pair("الحالة", stringResource(R.string.no_debts_yet))
                    summary.netDebt < 0.0 ->
                        Pair("صافي عليك", "عليك ${Formatters.formatAmount(abs(summary.netDebt))}")
                    else ->
                        Pair("صافي لك", "لك ${Formatters.formatAmount(summary.netDebt)}")
                }
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mainValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun Last7DaysActivityCard(days: List<DayActivityPoint>) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val haptic = LocalHapticFeedback.current
    var selectedIndex by remember(days.size) { mutableIntStateOf((days.size - 1).coerceAtLeast(0)) }
    val safeIndex = selectedIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val selectedDay = days.getOrNull(safeIndex)
    val maxExpense = remember(days) { days.maxOfOrNull { it.totalExpense } ?: 0.0 }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("last_7_days_card"),
        shape = RoundedCornerShape(24.dp),
        color = container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.last_7_days),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedDay != null) {
                        Text(
                            text = Formatters.formatDayOfWeekAndDate(
                                selectedDay.dayOfWeek,
                                selectedDay.dayOfMonth,
                                selectedDay.month
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = Formatters.formatAmount(selectedDay?.totalExpense ?: 0.0),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(124.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEachIndexed { index, point ->
                    val isSelected = index == safeIndex
                    val ratio = if (maxExpense <= 0.0) 0f else (point.totalExpense / maxExpense).toFloat()
                    val targetHeight = if (point.totalExpense <= 0.0) {
                        10.dp
                    } else {
                        (18.dp + (74.dp * ratio)).coerceAtMost(92.dp)
                    }
                    val animatedHeight by animateDpAsState(
                        targetValue = targetHeight,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "bar_height_$index"
                    )
                    val barColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        label = "bar_color_$index"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedIndex = index
                            }
                            .padding(vertical = 4.dp)
                            .testTag("day_bar_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .height(92.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(animatedHeight)
                                    .clip(CircleShape)
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Formatters.weekDayName(point.dayOfWeek),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRowItem(
    item: TransactionItem,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalWalletSemanticColors.current
    val isExpense = item.entity.type == TransactionType.EXPENSE
    val amountText = if (isExpense) {
        Formatters.formatSignedExpense(item.entity.amount)
    } else {
        Formatters.formatSignedIncome(item.entity.amount)
    }
    val amountColor = if (isExpense) semantic.expense else semantic.income

    ExpressiveSegmentedCardItem(
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier.testTag("transaction_item_${item.entity.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TonalCategoryIcon(
                iconKey = item.iconKey,
                colorKey = item.colorKey,
                contentDescription = item.categoryName
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append(Formatters.formatDayMonth(item.entity.timestamp))
                    if (item.entity.note.isNotBlank()) {
                        append(" • ")
                        append(item.entity.note)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                maxLines = 1
            )
        }
    }
}
