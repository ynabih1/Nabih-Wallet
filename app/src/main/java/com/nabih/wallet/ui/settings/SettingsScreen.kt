package com.nabih.wallet.ui.settings

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nabih.wallet.R
import com.nabih.wallet.data.preferences.ColorPalette
import com.nabih.wallet.data.preferences.ThemeMode
import com.nabih.wallet.ui.WalletUiState
import com.nabih.wallet.ui.components.ExpressiveSegmentedCardItem
import com.nabih.wallet.ui.components.SectionHeaderRow
import com.nabih.wallet.ui.components.TonalSquircleIcon
import com.nabih.wallet.ui.theme.LocalWalletSemanticColors
import com.nabih.wallet.ui.theme.paletteSwatchColor
import com.nabih.wallet.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: WalletUiState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColors: (Boolean) -> Unit,
    onSelectPalette: (ColorPalette) -> Unit,
    onToggleDailyReminder: (Boolean) -> Unit,
    onOpenReminderTimePicker: () -> Unit,
    onOpenCategoryManagement: () -> Unit,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val prefs = uiState.preferences
    val semantic = LocalWalletSemanticColors.current
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicColorActive = prefs.dynamicColorsEnabled && supportsDynamicColor
    val appearanceCardCount = if (supportsDynamicColor) 3 else 2

    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        LargeTopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.largeTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            title = {
                Text(
                    text = stringResource(R.string.nav_settings),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 32.dp
            )
        ) {
            // 1. المظهر والألوان
            item(key = "sec_appearance") {
                SectionHeaderRow(title = stringResource(R.string.settings_appearance_section))
                Spacer(modifier = Modifier.height(6.dp))
            }

            item(key = "theme_mode_card") {
                ExpressiveSegmentedCardItem(
                    index = 0,
                    count = appearanceCardCount
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TonalSquircleIcon(
                                icon = Icons.Rounded.Contrast,
                                contentDescription = null,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.settings_theme_mode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val themeModes = listOf(
                            ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                            ThemeMode.LIGHT to stringResource(R.string.theme_light),
                            ThemeMode.DARK to stringResource(R.string.theme_dark)
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeModes.forEachIndexed { index, (mode, label) ->
                                SegmentedButton(
                                    selected = prefs.themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = themeModes.size
                                    ),
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            if (supportsDynamicColor) {
                item(key = "dynamic_color_card") {
                    ExpressiveSegmentedCardItem(
                        index = 1,
                        count = appearanceCardCount
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TonalSquircleIcon(
                                icon = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                containerColor = semantic.incomeContainer,
                                iconTint = semantic.onIncomeContainer
                            )
                            Text(
                                text = stringResource(R.string.settings_dynamic_colors),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            SettingsExpressiveSwitch(
                                checked = dynamicColorActive,
                                onCheckedChange = onToggleDynamicColors,
                                testTag = "dynamic_colors_switch"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                }
            }

            item(key = "palette_card") {
                ExpressiveSegmentedCardItem(
                    index = appearanceCardCount - 1,
                    count = appearanceCardCount
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .alpha(if (dynamicColorActive) 0.55f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.ColorLens,
                            contentDescription = null,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_color_palette),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorPalette.entries.forEach { palette ->
                                val selected = !dynamicColorActive && prefs.colorPalette == palette
                                val swatchColor = paletteSwatchColor(palette)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .then(
                                            if (selected) {
                                                Modifier.border(
                                                    width = 2.5.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            if (dynamicColorActive) {
                                                onToggleDynamicColors(false)
                                            }
                                            onSelectPalette(palette)
                                        }
                                        .testTag("palette_${palette.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = palette.name,
                                            tint = semantic.heroOnSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 2. الإشعارات
            item(key = "sec_notifications") {
                SectionHeaderRow(title = stringResource(R.string.settings_notifications_section))
                Spacer(modifier = Modifier.height(6.dp))
            }

            item(key = "daily_reminder_card") {
                ExpressiveSegmentedCardItem(
                    index = 0,
                    count = 1
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            containerColor = semantic.expenseContainer,
                            iconTint = semantic.onExpenseContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_daily_reminder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (prefs.dailyReminderEnabled) {
                            AssistChip(
                                onClick = onOpenReminderTimePicker,
                                label = {
                                    Text(
                                        text = Formatters.formatTime12Hour(
                                            prefs.reminderHour,
                                            prefs.reminderMinute
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    leadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                border = null,
                                modifier = Modifier.testTag("reminder_time_chip")
                            )
                        }
                        SettingsExpressiveSwitch(
                            checked = prefs.dailyReminderEnabled,
                            onCheckedChange = onToggleDailyReminder,
                            testTag = "daily_reminder_switch"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 3. التخصيص
            item(key = "sec_customization") {
                SectionHeaderRow(title = stringResource(R.string.settings_customization_section))
                Spacer(modifier = Modifier.height(6.dp))
            }

            item(key = "categories_card") {
                ExpressiveSegmentedCardItem(
                    index = 0,
                    count = 1,
                    onClick = onOpenCategoryManagement,
                    modifier = Modifier.testTag("manage_categories_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.Category,
                            contentDescription = null,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_manage_categories),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = uiState.categories.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 4. البيانات والنسخ الاحتياطي
            item(key = "sec_backup") {
                SectionHeaderRow(title = stringResource(R.string.settings_backup_section))
                Spacer(modifier = Modifier.height(6.dp))
            }

            item(key = "export_backup_card") {
                ExpressiveSegmentedCardItem(
                    index = 0,
                    count = 2,
                    onClick = onExportBackup,
                    modifier = Modifier.testTag("export_backup_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_export_backup),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            item(key = "restore_backup_card") {
                ExpressiveSegmentedCardItem(
                    index = 1,
                    count = 2,
                    onClick = onRestoreBackup,
                    modifier = Modifier.testTag("restore_backup_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.CloudUpload,
                            contentDescription = null,
                            containerColor = semantic.incomeContainer,
                            iconTint = semantic.onIncomeContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_restore_backup),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // 5. التطبيق
            item(key = "sec_app") {
                SectionHeaderRow(title = stringResource(R.string.settings_app_section))
                Spacer(modifier = Modifier.height(6.dp))
            }

            item(key = "about_card") {
                ExpressiveSegmentedCardItem(
                    index = 0,
                    count = 1,
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.testTag("about_app_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TonalSquircleIcon(
                            icon = Icons.Rounded.Info,
                            contentDescription = null,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.settings_about),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = stringResource(R.string.app_version),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Image(
                    painter = painterResource(id = R.drawable.ic_nabih_wallet_logo),
                    contentDescription = stringResource(R.string.cd_app_logo),
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            },
            title = {
                Text(
                    text = "Nabih Wallet — محفظة نبيه",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "تطبيق محفظة شخصية محلي بالكامل لإدارة المصروفات والدخل والديون، وتصدير تقارير PDF عربية والنسخ الاحتياطي الآمن على جهازك.\n\nالإصدار: v3.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
private fun SettingsExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize)
                )
            }
        } else {
            null
        },
        modifier = Modifier.testTag(testTag)
    )
}
