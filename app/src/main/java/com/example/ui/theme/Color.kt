package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Claude-inspired Warm Cream & Terracotta Palette
val WarmBeigeBackground = Color(0xFFF7F6F0)
val WarmBackground = WarmBeigeBackground
val WarmOffWhiteSurface = Color(0xFFFFFFF8)
val WarmCardSurface = Color(0xFFEDECE4)
val WarmCardSurfaceHover = Color(0xFFE5E4DC)
val WarmSurfaceVariant = Color(0xFFEBE8DF)

val BurntOrangePrimary = Color(0xFFD96B43)
val BurntOrangeLight = Color(0xFFFBF0EA)
val BurntOrangeDark = Color(0xFFA84725)

val DarkOliveCard = Color(0xFF2C2F24)
val DarkOliveCardSecondary = Color(0xFF383C2F)

val HeroCardBrush = Brush.linearGradient(
    colors = listOf(DarkOliveCard, DarkOliveCardSecondary)
)
val TerracottaGradientBrush = Brush.linearGradient(
    colors = listOf(BurntOrangePrimary, BurntOrangeDark)
)

val TextPrimaryDark = Color(0xFF2B2922)
val TextSecondaryBrown = Color(0xFF6E695D)
val TextMutedBrown = Color(0xFF9E988A)
val TextOnDarkSecondary = Color(0xFFB5B7AB)
val TextOnDarkMuted = Color(0xFF8F9185)

val MutedIncomeGreen = Color(0xFF3B7A57)
val MutedIncomeGreenLight = Color(0xFFEDF5F0)

val MutedExpenseTerracotta = Color(0xFFC7513B)
val MutedExpenseLight = Color(0xFFFDF1EE)

val WarmAmberWarning = Color(0xFFD97706)
val WarmAmberWarningLight = Color(0xFFFEF3C7)

val DividerColor = Color(0xFFEFECE6)
val BorderSubtle = Color(0xFFEBE8E1)
val BorderCard = Color(0xFFEBE8E1)
val ShadowAmbient = Color(0x08000000)

