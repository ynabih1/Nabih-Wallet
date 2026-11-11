package com.example

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NabihApp
import com.example.ui.WalletViewModel
import com.example.ui.theme.NabihWalletTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(Color.BLACK),
      navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
    )
    setContent {
      val viewModel: WalletViewModel = viewModel()
      val language by viewModel.language.collectAsStateWithLifecycle()

      LaunchedEffect(language) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
      }

      NabihWalletTheme(darkTheme = true) {
        NabihApp(viewModel = viewModel)
      }
    }
  }
}
