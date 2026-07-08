package com.aistudio.sepatify.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CosmicDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSecondary,
    onSurfaceVariant = DarkOnSurface,
    error = Color(0xFFE53935),
    onError = Color(0xFFFFFFFF)
)

private val ElegantLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSecondary,
    onSurfaceVariant = LightOnSecondary,
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF)
)

private fun TextStyle.scale(factor: Float): TextStyle {
    // 1. ADD THIS SAFEGUARD: Restrict the scale factor between 0.8x and 2.0x
    val safeFactor = factor.coerceIn(0.8f, 2.0f) 

    if (this.fontSize.isSp) {
        // 2. USE safeFactor INSTEAD OF factor
        val newSize = this.fontSize.value * safeFactor
        val newLineHeight = if (this.lineHeight.isSp) this.lineHeight.value * safeFactor else this.lineHeight.value
        return this.copy(
            fontSize = newSize.sp,
            lineHeight = if (this.lineHeight.isSp) newLineHeight.sp else this.lineHeight
        )
    }
    return this
}

data class SepatifyColors(
    val surfaceElevated: Color = SurfaceElevated,
    val surfaceMuted: Color = SurfaceMuted,
    val surfaceHighlight: Color = SurfaceHighlight,
    val playlistAccentBlue: Color = PlaylistAccentBlue,
    val playlistAccentPurple: Color = PlaylistAccentPurple,
    val playlistAccentRose: Color = PlaylistAccentRose,
    val playlistAccentMint: Color = PlaylistAccentMint,
    val playlistAccentBlueLight: Color = PlaylistAccentBlueLight,
    val playlistAccentPurpleLight: Color = PlaylistAccentPurpleLight,
    val playlistAccentRoseLight: Color = PlaylistAccentRoseLight,
    val playlistAccentMintLight: Color = PlaylistAccentMintLight,
)

data class SepatifyShapes(
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(999.dp),
    val dialog: RoundedCornerShape = RoundedCornerShape(20.dp),
    val button: RoundedCornerShape = RoundedCornerShape(16.dp),
)

val LocalSepatifyColors = staticCompositionLocalOf { SepatifyColors() }
val LocalSepatifyShapes = staticCompositionLocalOf { SepatifyShapes() }

val MaterialTheme.sepatifyColors: SepatifyColors
    @Composable get() = LocalSepatifyColors.current

val MaterialTheme.sepatifyShapes: SepatifyShapes
    @Composable get() = LocalSepatifyShapes.current

@Composable
fun SepatifyTheme(
    themeMode: String = "system",
    fontSizeScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) CosmicDarkColorScheme else ElegantLightColorScheme
    val sepatifyColors = SepatifyColors()
    val sepatifyShapes = SepatifyShapes()

    val baseTypography = com.aistudio.sepatify.ui.theme.Typography
    val scaledTypography = androidx.compose.material3.Typography(
        displayLarge = baseTypography.displayLarge.scale(fontSizeScale),
        displayMedium = baseTypography.displayMedium.scale(fontSizeScale),
        displaySmall = baseTypography.displaySmall.scale(fontSizeScale),
        headlineLarge = baseTypography.headlineLarge.scale(fontSizeScale),
        headlineMedium = baseTypography.headlineMedium.scale(fontSizeScale),
        headlineSmall = baseTypography.headlineSmall.scale(fontSizeScale),
        titleLarge = baseTypography.titleLarge.scale(fontSizeScale),
        titleMedium = baseTypography.titleMedium.scale(fontSizeScale),
        titleSmall = baseTypography.titleSmall.scale(fontSizeScale),
        bodyLarge = baseTypography.bodyLarge.scale(fontSizeScale),
        bodyMedium = baseTypography.bodyMedium.scale(fontSizeScale),
        bodySmall = baseTypography.bodySmall.scale(fontSizeScale),
        labelLarge = baseTypography.labelLarge.scale(fontSizeScale),
        labelMedium = baseTypography.labelMedium.scale(fontSizeScale),
        labelSmall = baseTypography.labelSmall.scale(fontSizeScale)
    )

    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSepatifyColors provides sepatifyColors,
        LocalSepatifyShapes provides sepatifyShapes,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            shapes = shapes,
            content = content
        )
    }
}
