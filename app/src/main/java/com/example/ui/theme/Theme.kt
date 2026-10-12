package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AtsStatusColors(
    val ok: Color,
    val warn: Color,
    val bad: Color,
    val card: Color,
    val line: Color,
    val muted: Color
)

val LocalAtsColors = staticCompositionLocalOf {
    AtsStatusColors(
        ok = LightOk,
        warn = LightWarn,
        bad = LightBad,
        card = LightCard,
        line = LightLine,
        muted = LightMuted
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = Color(0xFF124B3B),
    onPrimaryContainer = DarkText,
    secondary = DarkAccent,
    onSecondary = DarkOnAccent,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkText,
    tertiary = DarkWarn,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkCard,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkLine,
    error = DarkBad,
    onError = DarkOnAccent
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = Color(0xFFD6EFE7),
    onPrimaryContainer = Color(0xFF073B2C),
    secondary = LightAccent,
    onSecondary = LightOnAccent,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightText,
    tertiary = LightWarn,
    background = LightBg,
    onBackground = LightText,
    surface = LightCard,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightMuted,
    outline = LightLine,
    error = LightBad,
    onError = LightOnAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val atsColors = if (darkTheme) {
        AtsStatusColors(
            ok = DarkOk,
            warn = DarkWarn,
            bad = DarkBad,
            card = DarkCard,
            line = DarkLine,
            muted = DarkMuted
        )
    } else {
        AtsStatusColors(
            ok = LightOk,
            warn = LightWarn,
            bad = LightBad,
            card = LightCard,
            line = LightLine,
            muted = LightMuted
        )
    }

    CompositionLocalProvider(LocalAtsColors provides atsColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
