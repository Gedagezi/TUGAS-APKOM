package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SegmaLightColorScheme = lightColorScheme(
    primary = SegmaNavy,
    onPrimary = SegmaSurface,
    primaryContainer = SegmaLime,
    onPrimaryContainer = SegmaNavy,
    secondary = SegmaLime,
    onSecondary = SegmaNavy,
    secondaryContainer = SegmaLimeSoft,
    onSecondaryContainer = SegmaNavy,
    tertiary = SegmaSuccessGreen,
    onTertiary = SegmaSurface,
    background = SegmaBackground,
    onBackground = SegmaTextPrimary,
    surface = SegmaSurface,
    onSurface = SegmaTextPrimary,
    surfaceVariant = SegmaSurfaceAlt,
    onSurfaceVariant = SegmaTextSecondary,
    outline = SegmaBorder,
    outlineVariant = SegmaBorderLight,
    error = SegmaDangerRed,
    onError = SegmaSurface
)

val SegmaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SegmaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SegmaLightColorScheme,
        typography = SegmaTypography,
        shapes = SegmaShapes,
        content = content
    )
}
