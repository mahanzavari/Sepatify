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
import androidx.compose.ui.unit.Dp
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
    val safeFactor = factor.coerceIn(0.8f, 2.0f) 

    if (this.fontSize.isSp) {
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
    
    // Bento & Playlist Accents (Dark Mode)
    val playlistAccentBlue: Color = PlaylistAccentBlue,
    val playlistAccentPurple: Color = PlaylistAccentPurple,
    val playlistAccentRose: Color = PlaylistAccentRose,
    val playlistAccentMint: Color = PlaylistAccentMint,
    
    // Bento & Playlist Accents (Light Mode)
    val playlistAccentBlueLight: Color = PlaylistAccentBlueLight,
    val playlistAccentPurpleLight: Color = PlaylistAccentPurpleLight,
    val playlistAccentRoseLight: Color = PlaylistAccentRoseLight,
    val playlistAccentMintLight: Color = PlaylistAccentMintLight,
    
    // Light Mode Contrast Texts
    val playlistTextBlueDark: Color = Color(0xFF001D35),
    val playlistTextPurpleDark: Color = Color(0xFF21005D),
    val playlistTextRoseDark: Color = Color(0xFF410002),
    val playlistTextMintDark: Color = Color(0xFF003916),

    // Search Browse Genre Cards Colors
    val genrePop: Color = Color(0xFF27856A),
    val genreIndie: Color = Color(0xFF477C2B),
    val genreRock: Color = Color(0xFFE8115B),
    val genreRandB: Color = Color(0xFFD84080),
    val genrePodcasts: Color = Color(0xFF2296F3),
    val genreMadeForYou: Color = Color(0xFF1E3264),
    val genreCharts: Color = Color(0xFF8D67AB),
    val genreNewReleases: Color = Color(0xFFE1306C),

    // Premium UI Elements Gold Colors
    val premiumGoldAccent: Color = Color(0xFFFFD54F),
    val premiumGoldTextDark: Color = Color(0xFF7A4F00),
    val premiumGoldLight: Color = Color(0xFFFDE16D),
    val premiumGoldDark: Color = Color(0xFFD4AC0D),
    
    // General overlay tokens
    val neutralGrey: Color = Color(0x1B79747E)
)

data class SepatifyDimens(
    val zero: Dp = 0.dp,
    val borderHalf: Dp = 0.5.dp,
    val borderThin: Dp = 1.dp,
    val borderMedium: Dp = 1.5.dp,
    val borderThick: Dp = 2.dp,
    val borderHeavy: Dp = 3.dp,
    
    val spaceTwo: Dp = 2.dp,
    val spaceThree: Dp = 3.dp,
    val spaceFour: Dp = 4.dp,
    val spaceFive: Dp = 5.dp,
    val spaceSix: Dp = 6.dp,
    val spaceEight: Dp = 8.dp,
    val spaceTen: Dp = 10.dp,
    val spaceTwelve: Dp = 12.dp,
    val spaceFourteen: Dp = 14.dp,
    val spaceNormal: Dp = 16.dp,
    val spaceTwenty: Dp = 20.dp,
    val spaceLarge: Dp = 24.dp,
    val spaceExtraLarge: Dp = 28.dp,
    val spaceHuge: Dp = 32.dp,
    val spaceMega: Dp = 48.dp,
    val spaceGiga: Dp = 54.dp,
    val spaceTera: Dp = 64.dp,
    val spacePeta: Dp = 68.dp,
    val spaceExa: Dp = 80.dp,
    val spaceZetta: Dp = 100.dp,
    val spaceYotta: Dp = 110.dp,
    
    val sizeAvatarSmall: Dp = 36.dp,
    val sizeAvatarNormal: Dp = 40.dp,
    val sizeAvatarMedium: Dp = 44.dp,
    val sizeAvatarLarge: Dp = 48.dp,
    val sizeAvatarHuge: Dp = 108.dp,
    val sizeAvatarFrame: Dp = 116.dp,
    
    val heightBentoSmall: Dp = 64.dp,
    val heightBentoWide: Dp = 68.dp,
    val heightBentoMedium: Dp = 138.dp,
    val heightPlaylistCard: Dp = 134.dp,
    
    val sizeScrollbarThumb: Dp = 6.dp,
    val sizeScrollbarTrack: Dp = 24.dp,
    
    val spaceBottomOverScroll: Dp = 200.dp
)

data class SepatifyShapes(
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(999.dp),
    val dialog: RoundedCornerShape = RoundedCornerShape(20.dp),
    val button: RoundedCornerShape = RoundedCornerShape(16.dp),
    val chatBubbleMe: RoundedCornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp),
    val chatBubbleOther: RoundedCornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp)
)

val LocalSepatifyColors = staticCompositionLocalOf { SepatifyColors() }
val LocalSepatifyDimens = staticCompositionLocalOf { SepatifyDimens() }
val LocalSepatifyShapes = staticCompositionLocalOf { SepatifyShapes() }

val MaterialTheme.sepatifyColors: SepatifyColors
    @Composable get() = LocalSepatifyColors.current

val MaterialTheme.sepatifyDimens: SepatifyDimens
    @Composable get() = LocalSepatifyDimens.current

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
    val sepatifyDimens = SepatifyDimens()
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
        LocalSepatifyDimens provides sepatifyDimens,
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