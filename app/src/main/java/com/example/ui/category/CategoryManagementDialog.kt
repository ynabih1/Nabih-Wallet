package com.example.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CategoryItem
import com.example.model.IconLibrary
import com.example.ui.theme.DividerBorderColor
import com.example.ui.theme.ElevatedSurface
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.InputChipSurface
import com.example.ui.theme.OnSpotifyGreen
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@Composable
fun CategoryManagementDialog(
    expenseCategories: List<CategoryItem>,
    incomeCategories: List<CategoryItem>,
    @Suppress("UNUSED_PARAMETER") customIconOverrides: Map<String, String> = emptyMap(),
    isArabic: Boolean,
    onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onUpdateIcon: ((categoryNameOrId: String, iconKey: String) -> Unit)? = null,
    onAddCustomCategory: (nameAr: String, nameEn: String, type: String, iconKey: String) -> Unit,
    onDeleteCustomCategory: ((categoryId: String) -> Unit)? = null,
    @Suppress("UNUSED_PARAMETER") onResetDefaults: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var searchQuery by remember { mutableStateOf("") }

    // Sub-dialog states
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .testTag("category_management_dialog"),
            color = ElevatedSurface,
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 0.dp
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
                            text = if (isArabic) "إدارة وتخصيص الفئات" else "Category Management",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isArabic) "إضافة وإدارة فئات المصروفات والإيرادات" else "Manage expense and income categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(InputChipSurface, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "إغلاق",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. X-Style Top Tabs: No background, bold white selected text with thick #1D9BF0 bottom indicator, #71767B unselected text
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val isExpense = selectedTab == "EXPENSE"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "EXPENSE" }
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "المصروفات • ${expenseCategories.size}" else "Expenses • ${expenseCategories.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                                color = if (isExpense) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (isExpense) XBlue else Color.Transparent)
                            )
                        }

                        val isIncome = selectedTab == "INCOME"
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = "INCOME" }
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isArabic) "الإيرادات • ${incomeCategories.size}" else "Income • ${incomeCategories.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                                color = if (isIncome) TextPrimary else TextMuted,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.75f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(if (isIncome) XBlue else Color.Transparent)
                            )
                        }
                    }
                    HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Search Bar for categories (#2A2A2A input field)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "بحث في الفئات..." else "Search categories...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "مسح",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputChipSurface,
                        unfocusedContainerColor = InputChipSurface,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = DividerBorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Quick Action Button: Add Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = OnSpotifyGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = OnSpotifyGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "إضافة فئة جديدة" else "Add New Category",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = OnSpotifyGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Category List (Flat rows with #2F3336 divider, 24dp outlined icons without colored backgrounds)
                val rawCategories = if (selectedTab == "EXPENSE") expenseCategories else incomeCategories
                val filteredCategories = remember(rawCategories, searchQuery) {
                    if (searchQuery.isBlank()) rawCategories
                    else {
                        rawCategories.filter {
                            it.nameAr.contains(searchQuery.trim(), ignoreCase = true) ||
                            it.nameEn.contains(searchQuery.trim(), ignoreCase = true) ||
                            it.descriptionAr.contains(searchQuery.trim(), ignoreCase = true)
                        }
                    }
                }

                if (filteredCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = if (isArabic) "لا توجد فئات حالياً" else "No categories found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isArabic) "انقر على \"إضافة فئة جديدة\" لإنشاء فئاتك الخاصة" else "Click \"Add New Category\" to create your own",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(filteredCategories, key = { it.id }) { category ->
                            val categoryName = if (isArabic) category.nameAr else category.nameEn
                            val isCustomCreated = category.id.startsWith("custom_")

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manage_cat_${category.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 24dp outlined icon with NO colored background circle
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = categoryName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            if (isCustomCreated) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isArabic) "مخصصة" else "Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = XBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        val desc = if (isArabic) category.descriptionAr else category.descriptionEn
                                        if (desc.isNotBlank()) {
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Delete custom category button (if custom created)
                                    if (isCustomCreated && onDeleteCustomCategory != null) {
                                        IconButton(
                                            onClick = { categoryToDelete = category },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.DeleteOutline,
                                                contentDescription = "حذف الفئة",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = DividerBorderColor, thickness = 0.6.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Done Button (Primary: #1DB954 background + black text, fully rounded)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("close_category_management_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = OnSpotifyGreen
                    )
                ) {
                    Text(
                        text = if (isArabic) "تم" else "Done",
                        color = OnSpotifyGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Sub Dialog: Add Custom Category
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            initialType = selectedTab,
            isArabic = isArabic,
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { nameAr, nameEn, type, iconKey ->
                onAddCustomCategory(nameAr, nameEn, type, iconKey)
                showAddCategoryDialog = false
            }
        )
    }

    // Sub Dialog: Confirm Delete
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = ElevatedSurface,
            title = {
                Text(
                    text = if (isArabic) "حذف الفئة المخصصة؟" else "Delete Custom Category?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isArabic) "هل أنت متأكد من رغبتك في حذف فئة \"${cat.nameAr}\"؟"
                    else "Are you sure you want to delete \"${cat.nameEn}\"?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomCategory?.invoke(cat.id)
                        categoryToDelete = null
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExpenseRed,
                        contentColor = TextPrimary
                    )
                ) {
                    Text(text = if (isArabic) "حذف" else "Delete", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoryToDelete = null },
                    shape = CircleShape,
                    border = BorderStroke(1.dp, DividerBorderColor)
                ) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                }
            }
        )
    }
}

@Composable
fun AddCategoryDialog(
    initialType: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAdd: (nameAr: String, nameEn: String, type: String, iconKey: String) -> Unit
) {
    var nameAr by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var selectedColorHex by remember { mutableLongStateOf(0xFF1DB954) }
    var selectedIconKey by remember { mutableStateOf("tag") }

    val presetIcons = listOf(
        "tag", "fastfood", "grocery", "shopping_bag", "home", "car",
        "receipt", "hospital", "school", "games", "baby", "fitness",
        "gift", "build", "gadgets", "laptop", "money", "wallet"
    )

    // The 6-color palette in exact specified order
    val presetColors = listOf(
        0xFF1DB954L,
        0xFF1D9BF0L,
        0xFF7856FFL,
        0xFFFF7A00L,
        0xFFF91880L,
        0xFFFFD400L
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ElevatedSurface,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = if (isArabic) "إضافة فئة جديدة" else "Add New Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Name
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "اسم الفئة" else "Category Name", color = TextSecondary) },
                    placeholder = { Text(if (isArabic) "مثال: هدايا، كتب، صيانة..." else "e.g., Gifts, Books, Repairs...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = InputChipSurface,
                        unfocusedContainerColor = InputChipSurface,
                        focusedBorderColor = SpotifyGreen,
                        unfocusedBorderColor = DividerBorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle (Spotify pills)
                Text(
                    text = if (isArabic) "نوع الفئة:" else "Category Type:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isExpSelected = type == "EXPENSE"
                    Surface(
                        shape = CircleShape,
                        color = if (isExpSelected) SpotifyGreen else InputChipSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable { type = "EXPENSE" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "مصروفات" else "Expenses",
                                color = if (isExpSelected) OnSpotifyGreen else TextPrimary,
                                fontWeight = if (isExpSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }

                    val isIncSelected = type == "INCOME"
                    Surface(
                        shape = CircleShape,
                        color = if (isIncSelected) SpotifyGreen else InputChipSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(CircleShape)
                            .clickable { type = "INCOME" }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isArabic) "دخل" else "Income",
                                color = if (isIncSelected) OnSpotifyGreen else TextPrimary,
                                fontWeight = if (isIncSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Icon Picker
                Text(
                    text = if (isArabic) "أيقونة الفئة:" else "Category Icon:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetIcons) { iconKey ->
                        val isSelected = selectedIconKey == iconKey
                        val iconItem = IconLibrary.getIconByKey(iconKey)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) SpotifyGreen else InputChipSurface)
                                .clickable { selectedIconKey = iconKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconItem,
                                contentDescription = null,
                                tint = if (isSelected) OnSpotifyGreen else TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Palette Picker
                Text(
                    text = if (isArabic) "لون الفئة المميز:" else "Category Theme Color:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetColors) { colorValue ->
                        val isSelected = selectedColorHex == colorValue
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .clickable { selectedColorHex = colorValue }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = OnSpotifyGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, DividerBorderColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (isArabic) "إلغاء" else "Cancel", color = TextPrimary)
                    }
                    Button(
                        onClick = {
                            val finalAr = nameAr.trim()
                            if (finalAr.isNotBlank()) {
                                onAdd(finalAr, finalAr, type, selectedIconKey)
                            }
                        },
                        enabled = nameAr.isNotBlank(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpotifyGreen,
                            contentColor = OnSpotifyGreen,
                            disabledContainerColor = InputChipSurface,
                            disabledContentColor = TextMuted
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(text = if (isArabic) "إضافة الفئة" else "Add Category", color = if (nameAr.isNotBlank()) OnSpotifyGreen else TextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
