package com.playdice.pickone.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.playdice.pickone.model.AppSettings
import com.playdice.pickone.model.ThemeMode

// ShadeCraft's neutral canvas. Neumorphic depth depends on the component and
// its parent sharing this exact base color.
val LightNeuBackground = Color(0xFFECF0F3)
val DarkNeuBackground = Color(0xFF303234)

data class NeuColors(
    val surface: Color,
    val highlight: Color,
    val shadow: Color,
)

val LocalNeuColors = staticCompositionLocalOf {
    NeuColors(
        surface = LightNeuBackground,
        highlight = Color.White,
        shadow = Color(0xFFD9D9D9),
    )
}

@Composable
fun PickOneTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = Color(settings.themeColor)
    val scheme = if (dark) darkScheme(accent) else lightScheme(accent)
    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgbCompat()
            window.navigationBarColor = scheme.background.toArgbCompat()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val neuColors = if (dark) {
        NeuColors(
            surface = DarkNeuBackground,
            highlight = Color(0xFF45484B),
            shadow = Color(0xFF111111),
        )
    } else {
        NeuColors(
            surface = LightNeuBackground,
            highlight = Color(0xFFFFFFFF),
            shadow = Color(0xFFD9D9D9),
        )
    }
    CompositionLocalProvider(LocalNeuColors provides neuColors) {
        MaterialTheme(colorScheme = scheme, typography = PickOneTypography, content = content)
    }
}

private fun lightScheme(accent: Color): ColorScheme = lightColorScheme(
    primary = accent,
    onPrimary = if (accent.luminance() > .5f) Color(0xFF1B1B22) else Color.White,
    primaryContainer = accent.copy(alpha = .16f).compositeOn(LightNeuBackground),
    onPrimaryContainer = Color(0xFF25263A),
    secondary = Color(0xFF596178),
    background = LightNeuBackground,
    onBackground = Color(0xFF252832),
    surface = LightNeuBackground,
    onSurface = Color(0xFF252832),
    surfaceVariant = Color(0xFFE3E7F0),
    onSurfaceVariant = Color(0xFF626878),
    outline = Color(0xFFB8BECC),
    error = Color(0xFFBA1A1A),
)

private fun darkScheme(accent: Color): ColorScheme = darkColorScheme(
    primary = accent.lightenForDark(),
    onPrimary = Color(0xFF171722),
    primaryContainer = accent.copy(alpha = .30f).compositeOn(DarkNeuBackground),
    onPrimaryContainer = Color(0xFFE8E7FF),
    secondary = Color(0xFFBFC5D8),
    background = DarkNeuBackground,
    onBackground = Color(0xFFE8EAF0),
    surface = DarkNeuBackground,
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF2A2F3A),
    onSurfaceVariant = Color(0xFFB7BDCB),
    outline = Color(0xFF545B6B),
    error = Color(0xFFFFB4AB),
)

private fun Color.lightenForDark() = Color(
    red = (red + .22f).coerceAtMost(1f),
    green = (green + .22f).coerceAtMost(1f),
    blue = (blue + .22f).coerceAtMost(1f),
    alpha = alpha,
)

private fun Color.compositeOn(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1f - a),
        green = green * a + background.green * (1f - a),
        blue = blue * a + background.blue * (1f - a),
        alpha = 1f,
    )
}

private fun Color.toArgbCompat(): Int = toArgb()
