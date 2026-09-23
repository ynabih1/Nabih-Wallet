package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.model.FinancialConstants
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseLight
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.MutedIncomeGreenLight
import com.example.ui.theme.TextMutedBrown
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmCardSurface
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun formatMoney(amount: Double): String = numberFormat.format(amount)

    fun formatDate(timeMillis: Long, isArabic: Boolean = true): String {
        val pattern = "yyyy/MM/dd"
        val sdf = SimpleDateFormat(pattern, if (isArabic) Locale("ar") else Locale.US)
        return sdf.format(Date(timeMillis))
    }

    fun formatDateShort(timeMillis: Long, isArabic: Boolean = true): String {
        val sdf = SimpleDateFormat("d MMM", if (isArabic) Locale("ar") else Locale.ENGLISH)
        return sdf.format(Date(timeMillis))
    }

    fun formatDateTime(timeMillis: Long, isArabic: Boolean = true): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - timeMillis
        val isToday = diffMillis in 0..86400000L

        val timeSdf = SimpleDateFormat("h:mm a", if (isArabic) Locale("ar") else Locale.ENGLISH)
        val timePart = timeSdf.format(Date(timeMillis))

        return if (isToday) {
            if (isArabic) "اليوم، $timePart" else "Today, $timePart"
        } else {
            val dateSdf = SimpleDateFormat("dd MMM، h:mm a", if (isArabic) Locale("ar") else Locale.ENGLISH)
            dateSdf.format(Date(timeMillis))
        }
    }

    fun formatDateRange(startMillis: Long, endMillis: Long?, isArabic: Boolean = true): String {
        if (endMillis == null || endMillis <= startMillis) {
            return formatDateTime(startMillis, isArabic)
        }
        val startStr = formatDateShort(startMillis, isArabic)
        val endStr = formatDateShort(endMillis, isArabic)
        return "$startStr - $endStr"
    }
}

@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    isArabic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "INCOME"
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) MutedIncomeGreen else MutedExpenseTerracotta
    val categoryIcon = FinancialConstants.getCategoryIcon(transaction.category)
    val categoryColor = FinancialConstants.getCategoryColor(transaction.category)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("transaction_item_${transaction.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.5.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon in soft tinted circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FinancialConstants.normalizeCategoryName(transaction.category),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = if (isArabic) "مثبت" else "Pinned",
                            tint = BurntOrangePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = Formatters.formatDateRange(transaction.dateMillis, transaction.endDateMillis, isArabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryBrown
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Amount
            Text(
                text = "$amountPrefix${Formatters.formatMoney(transaction.amount)} $currencySymbol",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    testTag: String = "empty_state_view"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = WarmCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.BurntOrangeLight.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BurntOrangePrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryBrown,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                androidx.compose.material3.Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = BurntOrangePrimary
                    ),
                    modifier = Modifier.testTag("${testTag}_action_btn")
                ) {
                    Text(
                        text = actionText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WarmLoadingIndicator(
    text: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = BurntOrangePrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            if (text != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryBrown,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

