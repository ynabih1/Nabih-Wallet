package com.example.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.IconCategoryGroup
import com.example.model.IconItem
import com.example.model.IconLibrary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    targetCategoryName: String,
    initialIconKey: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onIconSelected: (selectedIconKey: String) -> Unit
) {
    var selectedKey by remember { mutableStateOf(initialIconKey) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(IconCategoryGroup.ALL) }

    val filteredIcons = remember(searchQuery, selectedGroup) {
        IconLibrary.icons.filter { item ->
            val matchesGroup = selectedGroup == IconCategoryGroup.ALL || item.group == selectedGroup
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.nameAr.contains(searchQuery.trim(), ignoreCase = true) ||
                item.nameEn.contains(searchQuery.trim(), ignoreCase = true) ||
                item.key.contains(searchQuery.trim(), ignoreCase = true)
            }
            matchesGroup && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("icon_picker_dialog"),
            color = WarmBackground,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isArabic) "مكتبة الأيقونات" else "Icon Library",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (targetCategoryName.isNotBlank()) {
                            Text(
                                text = if (isArabic) "تخصيص أيقونة: $targetCategoryName" else "Customize icon for: $targetCategoryName",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryBrown
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(BorderSubtle.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Active Preview Card
                val activeIconItem = remember(selectedKey) {
                    IconLibrary.getIconItemByKey(selectedKey)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = WarmCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BurntOrangePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeIconItem?.icon ?: IconLibrary.getIconByKey(selectedKey),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isArabic) "الأيقونة المختارة حالياً" else "Selected Icon",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondaryBrown
                            )
                            Text(
                                text = if (isArabic) (activeIconItem?.nameAr ?: selectedKey) else (activeIconItem?.nameEn ?: selectedKey),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("icon_search_input"),
                    placeholder = {
                        Text(
                            text = if (isArabic) "بحث في الأيقونات: طعام، سيارة، منزل..." else "Search icons: food, car, home...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondaryBrown
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = TextSecondaryBrown
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = WarmCardSurface,
                        unfocusedContainerColor = WarmCardSurface,
                        focusedBorderColor = BurntOrangePrimary,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Category Group Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconCategoryGroup.values().forEach { group ->
                        val isSelected = selectedGroup == group
                        val title = if (isArabic) group.titleAr else group.titleEn

                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedGroup = group },
                            label = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = DarkOliveCard,
                                selectedLabelColor = Color.White,
                                containerColor = WarmCardSurface,
                                labelColor = TextPrimaryDark
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) DarkOliveCard else BorderSubtle
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Grid of Icons
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredIcons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد أيقونات تطابق بحثك" else "No icons found matching your query",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondaryBrown,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 64.dp),
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredIcons, key = { it.key }) { item ->
                                val isSelected = selectedKey == item.key
                                IconGridItem(
                                    item = item,
                                    isSelected = isSelected,
                                    isArabic = isArabic,
                                    onClick = { selectedKey = item.key }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            color = TextPrimaryDark
                        )
                    }

                    Button(
                        onClick = {
                            onIconSelected(selectedKey)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_icon_selection_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "تأكيد الأيقونة" else "Select Icon",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconGridItem(
    item: IconItem,
    isSelected: Boolean,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("icon_option_${item.key}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) BurntOrangePrimary.copy(alpha = 0.15f) else WarmCardSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) BurntOrangePrimary else BorderSubtle
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = if (isArabic) item.nameAr else item.nameEn,
                tint = if (isSelected) BurntOrangePrimary else DarkOliveCard,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = if (isArabic) item.nameAr else item.nameEn,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) BurntOrangePrimary else TextSecondaryBrown,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
