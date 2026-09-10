package com.senademirci.futbolyoklama.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    primaryContainer = PitchGreenLight,
    onPrimaryContainer = PitchGreenDark,
    secondary = PitchGreenDark,
    onSecondary = Color.White,
    error = StatusAbsent,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = PitchGreenLight,
    onPrimary = PitchGreenDark,
    primaryContainer = PitchGreenDark,
    onPrimaryContainer = PitchGreenLight,
    secondary = PitchGreenLight,
    onSecondary = PitchGreenDark,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0000),
)

@Composable
fun FutbolYoklamaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
