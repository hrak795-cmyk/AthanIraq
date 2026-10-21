package com.athaniqraq.app.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DeepGreen = Color(0xFF0D5C3D)
val Gold = Color(0xFFD4AF37)
val Cream = Color(0xFFF5F1E8)

@Composable
fun AthanTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(primary = Color(0xFF62C99A), secondary = Gold)
    else lightColorScheme(primary = DeepGreen, secondary = Gold, background = Cream, surface = Cream)
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
