package com.nabih.wallet.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.nabih.wallet.data.preferences.ColorPalette

@Immutable
data class WalletSemanticColors(
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val debtProgress: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val heroTileSurface: Color,
    val heroOnSurface: Color,
    val heroOnSurfaceMuted: Color,
    val pdfCardSurface: Color,
    val pdfBorder: Color
)

private val PureWhite = Color(0xFFFFFFFF)

val LightWalletSemanticColors = WalletSemanticColors(
    income = Color(0xFF146C2E),
    incomeContainer = Color(0xFFC4EED0),
    onIncomeContainer = Color(0xFF072711),
    expense = Color(0xFFB3261E),
    expenseContainer = Color(0xFFFFDAD6),
    onExpenseContainer = Color(0xFF410E0B),
    debtProgress = Color(0xFF146C2E),
    heroStart = Color(0xFF0B57D0),
    heroEnd = Color(0xFF1A73E8),
    heroTileSurface = Color(0x28FFFFFF),
    heroOnSurface = PureWhite,
    heroOnSurfaceMuted = Color(0xD9FFFFFF),
    pdfCardSurface = Color(0xFFF3F6FC),
    pdfBorder = Color(0xFFE1E7F0)
)

val DarkWalletSemanticColors = WalletSemanticColors(
    income = Color(0xFF6DD58C),
    incomeContainer = Color(0xFF0F5223),
    onIncomeContainer = Color(0xFFC4EED0),
    expense = Color(0xFFF2B8B5),
    expenseContainer = Color(0xFF7D1D17),
    onExpenseContainer = Color(0xFFFFDAD6),
    debtProgress = Color(0xFF6DD58C),
    heroStart = Color(0xFF0842A0),
    heroEnd = Color(0xFF0B57D0),
    heroTileSurface = Color(0x24FFFFFF),
    heroOnSurface = PureWhite,
    heroOnSurfaceMuted = Color(0xD9FFFFFF),
    pdfCardSurface = Color(0xFF1E222B),
    pdfBorder = Color(0xFF333846)
)

val LocalWalletSemanticColors = staticCompositionLocalOf { LightWalletSemanticColors }

// Palette Swatch preview colors for Settings
val PaletteSwatchPixelBlue = Color(0xFF0B57D0)
val PaletteSwatchEmerald = Color(0xFF006C4C)
val PaletteSwatchTerracotta = Color(0xFF9C413B)
val PaletteSwatchIndigo = Color(0xFF4C56AF)

fun paletteSwatchColor(palette: ColorPalette): Color = when (palette) {
    ColorPalette.PIXEL_BLUE -> PaletteSwatchPixelBlue
    ColorPalette.EMERALD -> PaletteSwatchEmerald
    ColorPalette.TERRACOTTA -> PaletteSwatchTerracotta
    ColorPalette.INDIGO -> PaletteSwatchIndigo
}

// 1. Pixel Blue (Default)
val PixelBlueLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF4F607A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCE7FA),
    onSecondaryContainer = Color(0xFF0B3068),
    tertiary = Color(0xFF146C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EED0),
    onTertiaryContainer = Color(0xFF072711),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF3F6FC),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF3F6FC),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E9F5),
    onSurfaceVariant = Color(0xFF5E6470),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF2FA),
    surfaceContainer = Color(0xFFE7EEF8),
    surfaceContainerHigh = Color(0xFFE1E9F5),
    surfaceContainerHighest = Color(0xFFD9E2F0)
)

val PixelBlueDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFBAC7E0),
    onSecondary = Color(0xFF243146),
    secondaryContainer = Color(0xFF2A3B54),
    onSecondaryContainer = Color(0xFFD3E3FD),
    tertiary = Color(0xFF6DD58C),
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF16462A),
    onTertiaryContainer = Color(0xFFC4EED0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7D1D17),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C22),
    surfaceContainer = Color(0xFF1D2026),
    surfaceContainerHigh = Color(0xFF282B32),
    surfaceContainerHighest = Color(0xFF33363E)
)

// 2. Emerald
val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF89F8C7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF4C6358),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFF3E6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC2E8FB),
    onTertiaryContainer = Color(0xFF001F29),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF2F8F5),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF2F8F5),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFBFC9C2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFECF4EF),
    surfaceContainer = Color(0xFFE6EFEA),
    surfaceContainerHigh = Color(0xFFDFE9E3),
    surfaceContainerHighest = Color(0xFFD8E3DD)
)

val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF6CDBAC),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005138),
    onPrimaryContainer = Color(0xFF89F8C7),
    secondary = Color(0xFFB3CCBF),
    onSecondary = Color(0xFF1E352B),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFA6CCDF),
    onTertiary = Color(0xFF093543),
    tertiaryContainer = Color(0xFF254B5B),
    onTertiaryContainer = Color(0xFFC2E8FB),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7D1D17),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDFE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDFE4DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFBFC9C2),
    outline = Color(0xFF89938C),
    outlineVariant = Color(0xFF404943),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633)
)

// 3. Terracotta
val TerracottaLightColorScheme = lightColorScheme(
    primary = Color(0xFF9C413B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775653),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1513),
    tertiary = Color(0xFF715B2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDDFA6),
    onTertiaryContainer = Color(0xFF261900),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF5F4),
    onBackground = Color(0xFF231918),
    surface = Color(0xFFFBF5F4),
    onSurface = Color(0xFF231918),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371),
    outlineVariant = Color(0xFFD8C2BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9EFEF),
    surfaceContainer = Color(0xFFF4E8E7),
    surfaceContainerHigh = Color(0xFFEEE1DF),
    surfaceContainerHighest = Color(0xFFE7D9D7)
)

val TerracottaDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF5F1412),
    primaryContainer = Color(0xFF7D2A25),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFDFC38C),
    onTertiary = Color(0xFF3F2E04),
    tertiaryContainer = Color(0xFF574419),
    onTertiaryContainer = Color(0xFFFDDFA6),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7D1D17),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DFDD),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DFDD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A),
    outlineVariant = Color(0xFF534341),
    surfaceContainerLowest = Color(0xFF140C0B),
    surfaceContainerLow = Color(0xFF231918),
    surfaceContainer = Color(0xFF271D1C),
    surfaceContainerHigh = Color(0xFF322826),
    surfaceContainerHighest = Color(0xFF3D3231)
)

// 4. Indigo
val IndigoLightColorScheme = lightColorScheme(
    primary = Color(0xFF4C56AF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF000767),
    secondary = Color(0xFF5C5D72),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E0F9),
    onSecondaryContainer = Color(0xFF191A2C),
    tertiary = Color(0xFF78536B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8EE),
    onTertiaryContainer = Color(0xFF2E1126),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF6F6FC),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFF6F6FC),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F0FA),
    surfaceContainer = Color(0xFFEBEAF5),
    surfaceContainerHigh = Color(0xFFE5E3F0),
    surfaceContainerHighest = Color(0xFFDEDCEB)
)

val IndigoDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBEC2FF),
    onPrimary = Color(0xFF1A237E),
    primaryContainer = Color(0xFF333D95),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFC5C4DD),
    onSecondary = Color(0xFF2E2F42),
    secondaryContainer = Color(0xFF444559),
    onSecondaryContainer = Color(0xFFE1E0F9),
    tertiary = Color(0xFFE8B9D5),
    onTertiary = Color(0xFF46263C),
    tertiaryContainer = Color(0xFF5F3C53),
    onTertiaryContainer = Color(0xFFFFD8EE),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF7D1D17),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121319),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121319),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFF0D0E14),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF292930),
    surfaceContainerHighest = Color(0xFF34343B)
)

fun colorSchemeForPalette(palette: ColorPalette, darkTheme: Boolean): ColorScheme = when (palette) {
    ColorPalette.PIXEL_BLUE -> if (darkTheme) PixelBlueDarkColorScheme else PixelBlueLightColorScheme
    ColorPalette.EMERALD -> if (darkTheme) EmeraldDarkColorScheme else EmeraldLightColorScheme
    ColorPalette.TERRACOTTA -> if (darkTheme) TerracottaDarkColorScheme else TerracottaLightColorScheme
    ColorPalette.INDIGO -> if (darkTheme) IndigoDarkColorScheme else IndigoLightColorScheme
}

fun semanticColorsFor(
    palette: ColorPalette,
    darkTheme: Boolean,
    colorScheme: ColorScheme,
    dynamicColorsActive: Boolean = false
): WalletSemanticColors {
    val base = if (darkTheme) DarkWalletSemanticColors else LightWalletSemanticColors
    val heroStart = if (darkTheme) colorScheme.primaryContainer else colorScheme.primary
    val heroEnd = if (dynamicColorsActive) {
        heroStart
    } else {
        when (palette) {
            ColorPalette.PIXEL_BLUE -> if (darkTheme) Color(0xFF0B57D0) else Color(0xFF1A73E8)
            ColorPalette.EMERALD -> if (darkTheme) Color(0xFF006C4C) else Color(0xFF00875F)
            ColorPalette.TERRACOTTA -> if (darkTheme) Color(0xFF9C413B) else Color(0xFFB54E47)
            ColorPalette.INDIGO -> if (darkTheme) Color(0xFF4C56AF) else Color(0xFF5E68C6)
        }
    }
    return base.copy(
        heroStart = heroStart,
        heroEnd = heroEnd,
        heroOnSurface = PureWhite,
        heroOnSurfaceMuted = Color(0xD9FFFFFF)
    )
}
