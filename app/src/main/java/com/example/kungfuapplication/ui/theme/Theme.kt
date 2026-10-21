package com.example.kungfuapplication.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = KungFuRed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = KungFuRedContainer,
    onPrimaryContainer = KungFuOnRedContainer,
    secondary = KungFuGold,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = KungFuGoldContainer,
    onSecondaryContainer = KungFuOnGoldContainer,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
)

private val DarkColorScheme = darkColorScheme(
    primary = KungFuRedLight,
    onPrimary = KungFuOnRedContainer,
    primaryContainer = KungFuRedDark,
    onPrimaryContainer = KungFuRedContainer,
    secondary = KungFuGoldLight,
    onSecondary = KungFuOnGoldContainer,
    secondaryContainer = KungFuGold,
    onSecondaryContainer = KungFuGoldContainer,
)

@Composable
fun KungFuApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Se usan los colores de la escuela por defecto; el color dinámico (Android 12+) es opcional.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
