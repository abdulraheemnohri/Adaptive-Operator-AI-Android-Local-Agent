package com.adaptiveoperator.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.adaptiveoperator.ai.presentation.navigation.AppNavHost
import com.adaptiveoperator.ai.presentation.theme.AdaptiveOperatorTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host. All real screens (Home, Chat, Operator, Model Manager, Skills,
 * Memory, Security Center, Settings) are Compose destinations inside AppNavHost.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AdaptiveOperatorTheme(darkTheme = true) {
                AppNavHost()
            }
        }
    }
}
