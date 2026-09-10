package com.example.ui.category

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.FinancialConstants
import com.example.model.IconLibrary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BurntOrangeLight
import com.example.ui.theme.BurntOrangePrimary
import com.example.ui.theme.DarkOliveCard
import com.example.ui.theme.MutedExpenseTerracotta
import com.example.ui.theme.MutedIncomeGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryBrown
import com.example.ui.theme.WarmBackground
import com.example.ui.theme.WarmCardSurface

@Composable
fun CategoryManagementDialog(
    expenseCategories: List<CategoryItem>,
    incomeCategories: List<CategoryItem>,
    customIconOverrides: Map<String, String>,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onUpdateIcon: (categoryNameOrId: String, iconKey: String) -> Unit,
    onAddCustomCategory: (nameAr: String, nameEn: String, type: String, iconKey: String) -> Unit,
    onDeleteCustomCategory: ((categoryId: String) -> Unit)? = null,
    onResetDefaults: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    var searchQuery by remember { mutableStateOf("") }

    // Sub-dialog states
    var categoryForIconPicker by remember { mutableStateOf<CategoryItem?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("category_management_dialog"),
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
                            text = if (isArabic) "إدارة وتخصيص الفئات" else "Categories & Customization",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = if (isArabic) "تخصيص الفئات، الأيقونات، والألوان لجميع المعاملات" else "Customize categories, icons, and colors for all transactions",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryBrown
                        )
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

                // 2. Segmented Tabs: Expenses (المصروفات) vs Income (الإيرادات)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(WarmCardSurface)
                        .padding(4.dp)
                ) {
                    val isExpense = selectedTab == "EXPENSE"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = "EXPENSE" },
                        color = if (isExpense) BurntOrangePrimary else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isExpense) Color.White else TextSecondaryBrown,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "المصروفات (${expenseCategories.size})" else "Expenses (${expenseCategories.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) Color.White else TextPrimaryDark
                            )
                        }
                    }

                    val isIncome = selectedTab == "INCOME"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedTab = "INCOME" },
                        color = if (isIncome) MutedIncomeGreen else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isIncome) Color.White else TextSecondaryBrown,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "الإيرادات (${incomeCategories.size})" else "Income (${incomeCategories.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) Color.White else TextPrimaryDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Search Bar for categories
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "بحث في الفئات..." else "Search categories...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryBrown
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondaryBrown,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "مسح",
                                    tint = TextSecondaryBrown,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Quick Action Buttons: Add Category + Reset Defaults
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isArabic) "إضافة فئة جديدة" else "Add New Category",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (customIconOverrides.isNotEmpty()) {
                        TextButton(
                            onClick = { showResetConfirmDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = TextSecondaryBrown,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "استعادة الافتراضي" else "Reset Defaults",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryBrown
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Category List
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

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        val activeIcon = FinancialConstants.getCategoryIcon(category.nameAr, customIconOverrides)
                        val categoryName = if (isArabic) category.nameAr else category.nameEn
                        val customKey = customIconOverrides[category.nameAr] ?: customIconOverrides[category.id]
                        val isCustomized = customKey != null
                        val isCustomCreated = category.id.startsWith("custom_")
                        val catColor = Color(category.color)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { categoryForIconPicker = category }
                                .testTag("manage_cat_${category.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCardSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isCustomized || isCustomCreated) 1.5.dp else 1.dp,
                                color = if (isCustomized || isCustomCreated) BurntOrangePrimary.copy(alpha = 0.5f) else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Category Icon with Colored Container
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(catColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = activeIcon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryDark
                                        )
                                        if (isCustomCreated) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = BurntOrangePrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (isArabic) "مخصصة" else "Custom",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BurntOrangePrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    val desc = if (isArabic) category.descriptionAr else category.descriptionEn
                                    if (desc.isNotBlank()) {
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryBrown,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Edit icon button
                                IconButton(
                                    onClick = { categoryForIconPicker = category },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "تخصيص الأيقونة",
                                        tint = BurntOrangePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete custom category button (if custom created)
                                if (isCustomCreated && onDeleteCustomCategory != null) {
                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الفئة",
                                            tint = MutedExpenseTerracotta,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_category_management_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                ) {
                    Text(
                        text = if (isArabic) "تم والعودة" else "Done",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Sub Dialog: Icon Picker
    categoryForIconPicker?.let { category ->
        val currentKey = customIconOverrides[category.nameAr]
            ?: customIconOverrides[category.id]
            ?: category.iconKey.ifBlank { IconLibrary.findKeyByIcon(category.icon) }

        IconPickerDialog(
            targetCategoryName = if (isArabic) category.nameAr else category.nameEn,
            initialIconKey = currentKey,
            isArabic = isArabic,
            onDismiss = { categoryForIconPicker = null },
            onIconSelected = { newIconKey ->
                onUpdateIcon(category.nameAr, newIconKey)
                categoryForIconPicker = null
            }
        )
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
            title = {
                Text(
                    text = if (isArabic) "حذف الفئة المخصصة؟" else "Delete Custom Category?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic) "هل أنت متأكد من رغبتك في حذف فئة \"${cat.nameAr}\"؟"
                    else "Are you sure you want to delete \"${cat.nameEn}\"?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCustomCategory?.invoke(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedExpenseTerracotta)
                ) {
                    Text(text = if (isArabic) "حذف" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // Sub Dialog: Confirm Reset
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = if (isArabic) "استعادة الأيقونات الافتراضية؟" else "Reset default icons?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isArabic) "سيتم إعادة تعيين جميع أيقونات الفئات إلى الأيقونات الأصلية الافتراضية."
                    else "All category icons will be reset to their default icons."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary)
                ) {
                    Text(text = if (isArabic) "نعم، استعادة" else "Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(text = if (isArabic) "إلغاء" else "Cancel")
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
    var nameEn by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(initialType) }
    var chosenIconKey by remember { mutableStateOf("tag") }
    var selectedColorHex by remember { mutableLongStateOf(0xFF1E293B) }
    var showIconPicker by remember { mutableStateOf(false) }

    val presetColors = listOf(
        0xFFC25E40, // Warm Terracotta
        0xFF388E3C, // Fresh Forest Green
        0xFF2E7D32, // Deep Emerald
        0xFFE06D3B, // Warm Coral Amber
        0xFFC97A3E, // Warm Bronze
        0xFF2E6B9E, // Sapphire Slate
        0xFFA6445B, // Berry Rose
        0xFF00897B, // Teal Pine
        0xFF7E4A82, // Plum Violet
        0xFF1976D2, // Ocean Blue
        0xFF455A64, // Slate Charcoal
        0xFF6D685E  // Warm Taupe
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = WarmBackground,
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
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Category Name
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(if (isArabic) "اسم الفئة (بالعربية)" else "Category Name (Arabic)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // English Name (Optional)
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "الاسم بالإنجليزية (اختياري)" else "English Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle
                Text(
                    text = if (isArabic) "نوع الفئة:" else "Category Type:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text(if (isArabic) "مصروفات" else "Expenses") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text(if (isArabic) "دخل / إيرادات" else "Income") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Color Palette Picker
                Text(
                    text = if (isArabic) "لون الفئة المميز:" else "Category Theme Color:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presetColors) { colorValue ->
                        val isSelected = selectedColorHex == colorValue
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .clickable { selectedColorHex = colorValue }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) TextPrimaryDark else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Icon Selection Button & Preview
                Text(
                    text = if (isArabic) "الأيقونة المحددة:" else "Selected Icon:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showIconPicker = true },
                    color = WarmCardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(selectedColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = IconLibrary.getIconByKey(chosenIconKey),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            val item = IconLibrary.getIconItemByKey(chosenIconKey)
                            Text(
                                text = if (isArabic) (item?.nameAr ?: chosenIconKey) else (item?.nameEn ?: chosenIconKey),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        }
                        Text(
                            text = if (isArabic) "تغيير من المكتبة..." else "Pick from library...",
                            style = MaterialTheme.typography.labelSmall,
                            color = BurntOrangePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = if (isArabic) "إلغاء" else "Cancel")
                    }
                    Button(
                        onClick = {
                            val finalAr = nameAr.trim()
                            val finalEn = nameEn.trim().ifBlank { finalAr }
                            if (finalAr.isNotBlank()) {
                                onAdd(finalAr, finalEn, type, chosenIconKey)
                            }
                        },
                        enabled = nameAr.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = BurntOrangePrimary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text(text = if (isArabic) "إضافة الفئة" else "Add Category", color = Color.White)
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            targetCategoryName = nameAr.ifBlank { if (isArabic) "فئة جديدة" else "New Category" },
            initialIconKey = chosenIconKey,
            isArabic = isArabic,
            onDismiss = { showIconPicker = false },
            onIconSelected = { key ->
                chosenIconKey = key
                showIconPicker = false
            }
        )
    }
}
