package com.pixelos.clicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pixelos.clicker.ui.screens.ClickerScreen
import com.pixelos.clicker.ui.theme.PixelClickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelClickerTheme {
                ClickerScreen()
            }
        }
    }
}
