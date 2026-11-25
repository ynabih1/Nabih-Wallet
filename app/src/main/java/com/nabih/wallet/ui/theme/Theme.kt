package com.nabih.wallet.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.nabih.wallet.data.preferences.ColorPalette
import com.nabih.wallet.data.preferences.ThemeMode

@Composable
fun NabihWalletTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColorsEnabled: Boolean = false,
    colorPalette: ColorPalette = ColorPalette.PIXEL_BLUE,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val dynamicActive = dynamicColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = if (dynamicActive) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorSchemeForPalette(colorPalette, darkTheme)
    }

    val semanticColors = semanticColorsFor(
        palette = colorPalette,
        darkTheme = darkTheme,
        colorScheme = colorScheme,
        dynamicColorsActive = dynamicActive
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalWalletSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NabihTypography,
            content = content
        )
    }
}
