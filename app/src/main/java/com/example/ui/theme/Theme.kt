package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NabihLightColorScheme = lightColorScheme(
    primary = BurntOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = BurntOrangeLight,
    onPrimaryContainer = BurntOrangeDark,
    secondary = DarkOliveCard,
    onSecondary = Color.White,
    secondaryContainer = WarmCardSurface,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = MutedIncomeGreen,
    onTertiary = Color.White,
    tertiaryContainer = MutedIncomeGreenLight,
    onTertiaryContainer = MutedIncomeGreen,
    error = MutedExpenseTerracotta,
    onError = Color.White,
    errorContainer = MutedExpenseLight,
    onErrorContainer = MutedExpenseTerracotta,
    background = WarmBeigeBackground,
    onBackground = TextPrimaryDark,
    surface = WarmOffWhiteSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = TextSecondaryBrown,
    outline = BorderSubtle,
    outlineVariant = DividerColor
)

private val NabihDarkColorScheme = darkColorScheme(
    primary = BurntOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = BurntOrangeDark,
    onPrimaryContainer = BurntOrangeLight,
    secondary = WarmCardSurface,
    onSecondary = DarkOliveCard,
    secondaryContainer = DarkOliveCardSecondary,
    onSecondaryContainer = Color.White,
    tertiary = MutedIncomeGreen,
    onTertiary = Color.White,
    background = Color(0xFF22231E),
    onBackground = Color(0xFFEBEAE2),
    surface = Color(0xFF2A2B24),
    onSurface = Color(0xFFEBEAE2),
    surfaceVariant = Color(0xFF35372E),
    onSurfaceVariant = Color(0xFFCBC8BC),
    outline = Color(0xFF4A4C42)
)

@Composable
fun NabihWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NabihDarkColorScheme else NabihLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
