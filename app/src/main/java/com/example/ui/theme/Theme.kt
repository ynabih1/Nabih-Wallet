package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NabihDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = OnSpotifyGreen,
    primaryContainer = InputChipSurface,
    onPrimaryContainer = TextPrimary,
    secondary = XBlue,
    onSecondary = TextPrimary,
    secondaryContainer = InputChipSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = IncomeGreen,
    onTertiary = OnSpotifyGreen,
    tertiaryContainer = CardSurface,
    onTertiaryContainer = IncomeGreen,
    error = ExpenseRed,
    onError = TextPrimary,
    errorContainer = CardSurface,
    onErrorContainer = ExpenseRed,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = ElevatedSurface,
    surfaceContainerHighest = InputChipSurface,
    outline = DividerBorderColor,
    outlineVariant = DividerBorderColor
)

@Composable
fun NabihWalletTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NabihDarkColorScheme,
        typography = Typography,
        content = content
    )
}
