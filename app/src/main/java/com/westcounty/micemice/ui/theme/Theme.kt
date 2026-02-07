package com.westcounty.micemice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LabTeal,
    secondary = LabAmber,
    tertiary = LabRose,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = LabBlue,
    secondary = LabTeal,
    tertiary = LabRose,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
)

@Composable
fun MicemiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
