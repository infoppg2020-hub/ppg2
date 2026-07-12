package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val BentoColorScheme = darkColorScheme(
    primary = BentoGreen,
    onPrimary = Color.Black,
    secondary = BentoBlue,
    onSecondary = Color.White,
    background = BentoBackground,
    onBackground = Color.White,
    surface = BentoSurface,
    onSurface = Color.White,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = Color(0xFFA1A1AA), // Zinc 400
    outline = BentoBorder,
    error = BentoRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark bento theme by default
    dynamicColor: Boolean = false, // Disable dynamic colors so our Bento branding remains intact
    content: @Composable () -> Unit,
) {
    val colorScheme = BentoColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
