package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldLight,
    onPrimary = VaultNavyDark,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = VaultNavyDark,
    tertiary = TransferBlue,
    background = VaultNavyDark,
    onBackground = Color(0xFFF1F5F9),
    surface = VaultNavySurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = VaultNavyCardDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = VaultBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = GoldAccentDark,
    onSecondary = Color.White,
    tertiary = TransferBlue,
    background = VaultNavyLight,
    onBackground = Color(0xFF0F172A),
    surface = VaultNavySurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = VaultNavyCardLight,
    onSurfaceVariant = Color(0xFF475569),
    outline = VaultBorderLight
)

@Composable
fun MoneyVaultTheme(
    themeMode: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
