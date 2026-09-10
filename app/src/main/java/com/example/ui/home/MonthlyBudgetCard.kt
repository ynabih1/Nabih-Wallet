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
        // Simplified Unset state: Clean and direct
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpenBudgetDialog() }
                .testTag("budget_unset_card"),
            color = WarmCardSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BurntOrangePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = BurntOrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = if (isArabic) "الميزانية الشهرية" else "Monthly Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Surface(
                    color = BurntOrangePrimary,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
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
        // Budget configured state: Simplified, focused on numbers and progress
        val pctPercent = (budgetStatus.percentage * 100).toInt()
        val progressColor = when {
            budgetStatus.isExceeded -> MutedExpenseTerracotta
            budgetStatus.isWarning -> Color(0xFFC97A3E) // Warm Amber Warning
            else -> MutedIncomeGreen
        }

        val progressBackground = progressColor.copy(alpha = 0.15f)

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .animateContentSize()
                .testTag("budget_status_card"),
            color = WarmCardSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                width = if (budgetStatus.isExceeded) 1.5.dp else 1.dp,
                color = if (budgetStatus.isExceeded) MutedExpenseTerracotta.copy(alpha = 0.6f) else BorderSubtle
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header row
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(progressColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = progressColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = if (isArabic) "الميزانية الشهرية" else "Monthly Budget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Badge for percentage
                        Surface(
                            color = progressColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, progressColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "$pctPercent%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = progressColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onOpenBudgetDialog,
                            modifier = Modifier
                                .size(32.dp)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Spent vs Budget row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (isArabic) "المصروف" else "Spent"}: ${Formatters.formatMoney(budgetStatus.spentAmount)} $currencySymbol",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryBrown,
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${if (isArabic) "الميزانية" else "Budget"}: ${Formatters.formatMoney(budgetStatus.budgetAmount)} $currencySymbol",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Remaining or Exceeded row
                if (budgetStatus.isExceeded) {
                    val exceededAmount = budgetStatus.spentAmount - budgetStatus.budgetAmount
                    Text(
                        text = "${if (isArabic) "تجاوزت الميزانية بـ" else "Exceeded by"} ${Formatters.formatMoney(exceededAmount)} $currencySymbol",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MutedExpenseTerracotta,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "${if (isArabic) "المتبقي" else "Remaining"}: ${Formatters.formatMoney(budgetStatus.remainingAmount)} $currencySymbol",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (budgetStatus.isWarning) Color(0xFFC97A3E) else MutedIncomeGreen,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
