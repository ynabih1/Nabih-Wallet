package com.nabih.wallet.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.local.entity.CategoryEntity
import com.nabih.wallet.data.local.entity.TransactionType
import com.nabih.wallet.ui.components.ExpressiveSegmentedCardItem
import com.nabih.wallet.ui.components.IconLibrary
import com.nabih.wallet.ui.components.SectionHeaderRow
import com.nabih.wallet.ui.components.TonalCategoryIcon
import com.nabih.wallet.ui.theme.CategoryColors
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    expenseCategories: List<CategoryEntity>,
    incomeCategories: List<CategoryEntity>,
    onBack: () -> Unit,
    onAddCategory: (name: String, type: String, iconKey: String, colorKey: String) -> Unit,
    onUpdateCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val displayed = if (selectedType == TransactionType.EXPENSE) {
        expenseCategories
    } else {
        incomeCategories
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
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("categories_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.settings_manage_categories),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    TransactionType.EXPENSE to "المصروفات (${expenseCategories.size})",
                    TransactionType.INCOME to "الإيرادات (${incomeCategories.size})"
                )
                options.forEachIndexed { index, (typeKey, label) ->
                    SegmentedButton(
                        selected = selectedType == typeKey,
                        onClick = { selectedType = typeKey },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        modifier = Modifier.testTag("manage_cat_tab_${typeKey.lowercase()}")
                    ) {
                        Text(label, maxLines = 1)
                    }
                }
            }

            FilledTonalButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_new_category_button"),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.add_new_category_cta),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 32.dp
            )
        ) {
            item(key = "cat_section_header") {
                SectionHeaderRow(
                    title = if (selectedType == TransactionType.EXPENSE) {
                        "فئات المصروفات"
                    } else {
                        "فئات الإيرادات"
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            itemsIndexed(
                items = displayed,
                key = { _, c -> "manage_cat_${c.id}" }
            ) { index, category ->
                ExpressiveSegmentedCardItem(
                    index = index,
                    count = displayed.size,
                    onClick = { editingCategory = category },
                    modifier = Modifier.testTag("manage_cat_item_${category.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalCategoryIcon(
                            iconKey = category.iconKey,
                            colorKey = category.colorKey,
                            contentDescription = category.name
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (index < displayed.lastIndex) {
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditCategorySheet(
            initialType = selectedType,
            editingCategory = null,
            onDismiss = { showAddSheet = false },
            onSave = { name, type, iconKey, colorKey ->
                onAddCategory(name, type, iconKey, colorKey)
                showAddSheet = false
            },
            onDelete = null
        )
    }

    editingCategory?.let { category ->
        AddEditCategorySheet(
            initialType = category.type,
            editingCategory = category,
            onDismiss = { editingCategory = null },
            onSave = { name, type, iconKey, colorKey ->
                onUpdateCategory(
                    category.copy(
                        name = name,
                        type = type,
                        iconKey = iconKey,
                        colorKey = colorKey
                    )
                )
                editingCategory = null
            },
            onDelete = {
                onDeleteCategory(category)
                editingCategory = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditCategorySheet(
    initialType: String,
    editingCategory: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, iconKey: String, colorKey: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val semantic = LocalWalletSemanticColors.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
    var selectedIconKey by remember(editingCategory) {
        mutableStateOf(editingCategory?.iconKey ?: "restaurant")
    }
    var selectedColorKey by remember(editingCategory) {
        mutableStateOf(editingCategory?.colorKey ?: "blue")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (editingCategory == null) "إضافة فئة جديدة" else "تعديل الفئة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "إغلاق")
                }
            }

            // Live preview + name input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TonalCategoryIcon(
                    iconKey = selectedIconKey,
                    colorKey = selectedColorKey,
                    contentDescription = null,
                    boxSize = 52.dp,
                    iconSize = 26.dp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الفئة") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("category_name_input")
                )
            }

            // Color Picker
            Text(
                text = "اللون",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryColors.all.forEach { tone ->
                    val swatch = CategoryColors.accentColor(tone.key, isDark)
                    val isSelected = tone.key == selectedColorKey
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        width = 2.5.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { selectedColorKey = tone.key },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Icon Picker
            Text(
                text = "الأيقونة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconLibrary.all.forEach { option ->
                    val isSelected = option.key == selectedIconKey
                    val shape = RoundedCornerShape(14.dp)
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(shape)
                            .clickable { selectedIconKey = option.key },
                        shape = shape,
                        color = if (isSelected) {
                            CategoryColors.containerColor(selectedColorKey, isDark)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) {
                                    CategoryColors.accentColor(selectedColorKey, isDark)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(name.trim(), editingCategory?.type ?: initialType, selectedIconKey, selectedColorKey)
                },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_category_button"),
                shape = CircleShape
            ) {
                Text(
                    text = stringResource(R.string.save_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            if (editingCategory != null && onDelete != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_category_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = semantic.expense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "حذف الفئة",
                        color = semantic.expense,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
