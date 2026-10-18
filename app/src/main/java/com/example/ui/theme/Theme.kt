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
    secondary = Color(0xFF383A30),
    onSecondary = Color(0xFFF3F2EB),
    secondaryContainer = Color(0xFF2B2D25),
    onSecondaryContainer = Color(0xFFE5E3D8),
    tertiary = Color(0xFF4FA876),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1E462F),
    onTertiaryContainer = Color(0xFFB4E7C7),
    error = Color(0xFFE2614B),
    onError = Color.White,
    errorContainer = Color(0xFF5A1C12),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF181915),
    onBackground = Color(0xFFECEAE2),
    surface = Color(0xFF22241D),
    onSurface = Color(0xFFECEAE2),
    surfaceVariant = Color(0xFF2D3027),
    onSurfaceVariant = Color(0xFFCBC7B9),
    outline = Color(0xFF45473E),
    outlineVariant = Color(0xFF35372F)
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
