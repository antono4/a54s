package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OppoGreenPrimary,
    primaryContainer = OppoGreenDark,
    secondary = OppoGreenSecondary,
    secondaryContainer = DarkBentoCardBg,
    tertiary = Color(0xFFFFD54F),
    background = DarkBentoBg,
    surface = DarkBentoSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DarkBentoBg,
    onBackground = DarkBentoText,
    onSurface = DarkBentoText,
    outline = DarkBentoBorder,
    outlineVariant = DarkBentoBorder,
    error = Color(0xFFFF5252),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = OppoGreenPrimary,
    primaryContainer = SageCardBg,
    secondary = OppoGreenSecondary,
    secondaryContainer = SageCardBg,
    tertiary = Color(0xFFFFB13B),
    background = SageBackground,
    surface = SageSurface,
    onPrimary = Color.White,
    onSecondary = DeepText,
    onTertiary = DeepText,
    onBackground = DeepText,
    onSurface = DeepText,
    outline = SageBorder,
    outlineVariant = SageBorder,
    error = Color(0xFFFF5252),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Disable dynamicColor to enforce our beautiful cohesive custom Oppo theme!
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
