package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.Formatters
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetSettingDialog(
    currentBudget: Double,
    currencySymbol: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSaveBudget: (Double) -> Unit,
    onClearBudget: () -> Unit
) {
    var amountText by remember {
        mutableStateOf(if (currentBudget > 0) String.format(java.util.Locale.US, "%.0f", currentBudget) else "")
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    val quickPresets = listOf(500.0, 1000.0, 2000.0, 3000.0, 5000.0, 10000.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                .testTag("budget_setting_dialog"),
            color = WarmCardSurface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Simplified without description)
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
                                .size(40.dp)
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

                        Text(
                            text = if (isArabic) "الميزانية الشهرية" else "Monthly Budget",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("budget_dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondaryBrown
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input field for amount
                Text(
                    text = if (isArabic) "حدد سقف المصروفات الشهري" else "Set Monthly Spending Limit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { ch -> ch.isDigit() || ch == '.' }
                        errorMessage = null
                    },
                    placeholder = {
                        Text(
                            text = if (isArabic) "مثال: 3000" else "e.g. 3000",
                            color = TextSecondaryBrown.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        if (currencySymbol.isNotBlank()) {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondaryBrown,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MutedExpenseTerracotta) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BurntOrangePrimary,
                        unfocusedBorderColor = BorderSubtle,
                        focusedContainerColor = WarmBackground,
                        unfocusedContainerColor = WarmBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_amount_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Quick presets (خيارات سريعة)
                Text(
                    text = if (isArabic) "خيارات سريعة" else "Quick options",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryBrown
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPresets.forEach { preset ->
                        val isSelected = amountText.toDoubleOrNull() == preset
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    amountText = String.format(java.util.Locale.US, "%.0f", preset)
                                    errorMessage = null
                                }
                                .testTag("budget_preset_${preset.toInt()}"),
                            color = if (isSelected) BurntOrangePrimary else WarmBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSelected) BurntOrangePrimary else BorderSubtle)
                        ) {
                            Text(
                                text = "${Formatters.formatMoney(preset)} $currencySymbol".trim(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextPrimaryDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val parsed = amountText.toDoubleOrNull()
                            if (parsed == null || parsed <= 0.0) {
                                errorMessage = if (isArabic) "يرجى إدخال مبلغ صحيح أكبر من الصفر" else "Please enter a valid amount greater than zero"
                            } else {
                                onSaveBudget(parsed)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_budget_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                    ) {
                        Text(
                            text = if (isArabic) "حفظ الميزانية" else "Save Budget",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (currentBudget > 0) {
                        OutlinedButton(
                            onClick = {
                                onClearBudget()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("clear_budget_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MutedExpenseTerracotta.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isArabic) "إلغاء الميزانية" else "Clear Budget",
                                color = MutedExpenseTerracotta,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
