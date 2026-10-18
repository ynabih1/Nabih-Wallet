package com.example.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MonthlyBudgetStatus
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmAmberWarning
import com.example.ui.theme.WarmCardSurface

@Composable
fun MonthlyBudgetCard(
    budgetStatus: MonthlyBudgetStatus,
    currencySymbol: String,
    isArabic: Boolean,
    onOpenBudgetDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!budgetStatus.isSet) {
        // Unset State: Inviting, elegant banner
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenBudgetDialog() }
                .testTag("budget_unset_card"),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(0.5.dp, BorderSubtle),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BurntOrangePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = BurntOrangePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isArabic) "الميزانية الشهرية" else "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isArabic) "حدد ميزانية شهرية لمتابعة سقف المصروفات" else "Set a monthly budget to track spending limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Surface(
                    color = BurntOrangePrimary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isArabic) "تعيين" else "Set",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        // Budget configured state: Polished data view with progress bar
        val pctPercent = (budgetStatus.percentage * 100).toInt()
        val progressColor = when {
            budgetStatus.isExceeded -> MutedExpenseTerracotta
            budgetStatus.isWarning -> WarmAmberWarning
            else -> MutedIncomeGreen
        }

        val progressBackground = progressColor.copy(alpha = 0.12f)

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .animateContentSize()
                .testTag("budget_status_card"),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                width = if (budgetStatus.isExceeded) 1.dp else 0.5.dp,
                color = if (budgetStatus.isExceeded) MutedExpenseTerracotta.copy(alpha = 0.5f) else BorderSubtle
            ),
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header row: Title + percentage pill + edit icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(progressColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (budgetStatus.isExceeded) Icons.Default.WarningAmber else Icons.Default.Savings,
                                contentDescription = null,
                                tint = progressColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isArabic) "الميزانية الشهرية" else "Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = if (isArabic) "الهدف الشهري" else "Monthly Target",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryBrown,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Badge for percentage
                        Surface(
                            color = progressColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, progressColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$pctPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                fontSize = 11.5.sp
                            )
                        }

                        IconButton(
                            onClick = onOpenBudgetDialog,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WarmCardSurface)
                                .testTag("edit_budget_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل الميزانية",
                                tint = TextSecondaryBrown,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(progressBackground)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(budgetStatus.percentage.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(progressColor)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spent vs Budget row in soft capsule surface
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isArabic) "المصروف" else "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryBrown
                        )
                        Text(
                            text = "${Formatters.formatMoney(budgetStatus.spentAmount)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isArabic) "الميزانية الكاملة" else "Total Budget",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryBrown
                        )
                        Text(
                            text = "${Formatters.formatMoney(budgetStatus.budgetAmount)} $currencySymbol",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Remaining or Exceeded row banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (budgetStatus.isExceeded) MutedExpenseTerracotta.copy(alpha = 0.08f) else progressColor.copy(alpha = 0.07f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (budgetStatus.isExceeded) {
                            val exceededAmount = budgetStatus.spentAmount - budgetStatus.budgetAmount
                            Text(
                                text = if (isArabic) "تجاوزت الميزانية بـ" else "Exceeded budget by",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedExpenseTerracotta,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "${Formatters.formatMoney(exceededAmount)} $currencySymbol",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MutedExpenseTerracotta,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = if (isArabic) "المتبقي للاستهلاك" else "Remaining to spend",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryBrown,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "${Formatters.formatMoney(budgetStatus.remainingAmount)} $currencySymbol",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (budgetStatus.isWarning) WarmAmberWarning else MutedIncomeGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

