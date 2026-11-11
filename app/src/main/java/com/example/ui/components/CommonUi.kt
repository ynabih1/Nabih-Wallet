package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.model.FinancialConstants
import com.example.ui.theme.AppBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRowItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    isArabic: Boolean,
    onClick: () -> Unit,
    onQuickChangeDate: ((TransactionEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == "INCOME"
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val categoryIcon = FinancialConstants.getCategoryIcon(transaction.category)

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppBackground)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = {
                    if (onQuickChangeDate != null) {
                        onQuickChangeDate(transaction)
                    } else {
                        showMenu = true
                    }
                }
            )
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flat 24dp outlined icon with NO colored background box/circle
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = FinancialConstants.normalizeCategoryName(transaction.category),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (transaction.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = if (isArabic) "مثبت" else "Pinned",
                            tint = XBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = Formatters.formatDate(transaction.dateMillis, isArabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$amountPrefix${Formatters.formatMoney(transaction.amount)} $currencySymbol",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                fontSize = 14.sp
            )

            // Quick overflow options button
            if (onQuickChangeDate != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = if (isArabic) "خيارات إضافية" else "Options",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = ElevatedSurface
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "تغيير التاريخ" else "Change Date",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onQuickChangeDate(transaction)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isArabic) "تعديل العملية" else "Edit Transaction",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onClick()
                            }
                        )
                    }
                }
            }
        }

        // Thin flat divider #2F3336 between rows
        HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
    }
}

@Composable
fun TransactionFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") activeColor: Color = SpotifyGreen
) {
    // Spotify-style filter chip: fully rounded pill, #1DB954 + black text when selected, #2A2A2A + white text when unselected
    val bg = if (selected) SpotifyGreen else InputChipSurface
    val textColor = if (selected) OnSpotifyGreen else TextPrimary

    Surface(
        shape = CircleShape,
        color = bg,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickChangeDateDialog(
    transaction: TransactionEntity,
    isArabic: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (newDateMillis: Long) -> Unit
) {
    var selectedDateMillis by remember { mutableLongStateOf(transaction.dateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElevatedSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isArabic) "تغيير التاريخ" else "Change Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Transaction preview row
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardSurface,
                    border = BorderStroke(0.6.dp, DividerBorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = FinancialConstants.normalizeCategoryName(transaction.category),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${Formatters.formatMoney(transaction.amount)} $currencySymbol",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (transaction.type == "INCOME") IncomeGreen else ExpenseRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isArabic) "اختر التاريخ:" else "Select date:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Single Date Picker Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(10.dp),
                    color = InputChipSurface,
                    border = BorderStroke(0.8.dp, DividerBorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isArabic) "التاريخ:" else "Date:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = Formatters.formatDate(selectedDateMillis, isArabic),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedDateMillis)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen,
                    contentColor = OnSpotifyGreen
                ),
                shape = CircleShape
            ) {
                Text(
                    text = if (isArabic) "حفظ التغيير" else "Save Change",
                    color = OnSpotifyGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = CircleShape,
                border = BorderStroke(1.dp, DividerBorderColor)
            ) {
                Text(
                    text = if (isArabic) "إلغاء" else "Cancel",
                    color = TextPrimary
                )
            }
        }
    )

    if (showDatePicker) {
        val initialUtc = remember(selectedDateMillis) {
            DateFilterUtils.localMillisToUtcMidnight(selectedDateMillis)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtc)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = DatePickerDefaults.colors(containerColor = ElevatedSurface),
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utc ->
                            selectedDateMillis = DateFilterUtils.utcMillisToLocalStartOfDay(utc)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    ),
                    shape = CircleShape
                ) {
                    Text(if (isArabic) "تأكيد" else "Confirm", color = OnSpotifyGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDatePicker = false },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ElevatedSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = SpotifyGreen,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextPrimary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = SpotifyGreen,
                    selectedYearContainerColor = SpotifyGreen,
                    selectedYearContentColor = OnSpotifyGreen,
                    dayContentColor = TextPrimary,
                    selectedDayContainerColor = SpotifyGreen,
                    selectedDayContentColor = OnSpotifyGreen,
                    todayDateBorderColor = SpotifyGreen,
                    todayContentColor = SpotifyGreen
                )
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
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    ),
                    modifier = Modifier.testTag("${testTag}_action_btn")
                ) {
                    Text(
                        text = actionText,
                        color = OnSpotifyGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
