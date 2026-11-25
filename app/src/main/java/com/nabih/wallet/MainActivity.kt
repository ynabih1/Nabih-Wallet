package com.nabih.wallet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nabih.wallet.data.preferences.ThemeMode
import com.nabih.wallet.notification.NotificationHelper
import com.nabih.wallet.ui.NabihApp
import com.nabih.wallet.ui.WalletViewModel
import com.nabih.wallet.ui.theme.NabihWalletTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val prefs = uiState.preferences
            val systemDark = isSystemInDarkTheme()
            val isDark = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
            }

            NabihWalletTheme(
                themeMode = prefs.themeMode,
                dynamicColorsEnabled = prefs.dynamicColorsEnabled,
                colorPalette = prefs.colorPalette
            ) {
                NabihApp(viewModel = viewModel)
            }
        }
    }
}
