package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = SlateCard,
    onPrimaryContainer = GoldLight,
    secondary = GoldLight,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = CreamText,
    surface = DarkSurface,
    onSurface = CreamText,
    surfaceVariant = SlateSurface,
    onSurfaceVariant = MutedText
)

private val LightColorScheme = DarkColorScheme


@Composable
fun SoyAgaciTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
