package com.adaptiveoperator.ai.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = AccentOperator,
    onPrimary = SurfaceBlack,
    secondary = AccentOperatorDim,
    background = SurfaceBlack,
    surface = SurfaceElevated,
    surfaceVariant = SurfaceCard,
    outline = OutlineSubtle,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = StatusError
)

@Composable
fun AdaptiveOperatorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Dark-first per Section 48. A light scheme can be added later, but the whole
    // visual language (status colors, floating bubble, live traces) is designed dark.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
