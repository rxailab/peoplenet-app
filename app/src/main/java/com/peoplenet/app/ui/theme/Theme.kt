package com.peoplenet.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    secondary = PurpleLight,
    tertiary = PinkAccent,
    background = BgGradientStart,
    surface = CardBg,
    onPrimary = CardBg,
    onSecondary = CardBg,
    onBackground = DarkText,
    onSurface = DarkText,
    outline = CardBorder,
    surfaceVariant = TagBg
)

@Composable
fun PeopleNetTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgGradientStart.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
