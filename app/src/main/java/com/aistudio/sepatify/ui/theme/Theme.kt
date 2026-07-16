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
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val surfaceHighlight: Color,
    
    // Dynamic Bento Home Accents
    val bentoLikedBg: Color,
    val bentoLikedTxt: Color,
    val bentoLikedIconBg: Color,
    
    val bentoRecentBg: Color,
    val bentoRecentTxt: Color,
    val bentoRecentIconBg: Color,
    
    val bentoPlaylistsBg: Color,
    val bentoPlaylistsTxt: Color,
    val bentoPlaylistsIconBg: Color,
    
    val bentoArtistsBg: Color,
    val bentoArtistsTxt: Color,
    val bentoArtistsIconBg: Color,
    
    // Playlists Hub Accents
    val playlistAccentBlue: Color,
    val playlistAccentPurple: Color,
    val playlistAccentRose: Color,
    val playlistAccentMint: Color,
    val playlistAccentBlueLight: Color,
    val playlistAccentPurpleLight: Color,
    val playlistAccentRoseLight: Color,
    val playlistAccentMintLight: Color,
    val playlistTextBlueDark: Color,
    val playlistTextPurpleDark: Color,
    val playlistTextRoseDark: Color,
    val playlistTextMintDark: Color,

    // Dynamic Genre Cards
    val genrePop: Color,
    val genreIndie: Color,
    val genreRock: Color,
    val genreRandB: Color,
    val genrePodcasts: Color,
    val genreMadeForYou: Color,
    val genreCharts: Color,
    val genreNewReleases: Color,

    // Dynamic Premium Badges and Accents
    val premiumGoldAccent: Color,
    val premiumGoldTextDark: Color,
    val premiumGoldLight: Color,
    val premiumGoldDark: Color,
    val premiumGoldFrameBg: Color,

    // Player Components Fallbacks
    val playerBgFallback: Color,
    val playerDefaultDominant: Color,
    val playerSpindleHole: Color,
    
    val neutralGrey: Color
)

val LightSepatifyColors = SepatifyColors(
    surfaceElevated = SurfaceElevated,
    surfaceMuted = SurfaceMuted,
    surfaceHighlight = SurfaceHighlight,

    bentoLikedBg = Color(0xFFD0E4FF),
    bentoLikedTxt = Color(0xFF001D35),
    bentoLikedIconBg = Color(0xFFAAC7FF),

    bentoRecentBg = Color(0xFFFFFFFF),
    bentoRecentTxt = Color(0xFF1C1B1F),
    bentoRecentIconBg = Color(0xFFF3EDF7),

    bentoPlaylistsBg = Color(0xFFFFDAD6),
    bentoPlaylistsTxt = Color(0xFF410002),
    bentoPlaylistsIconBg = Color(0xFFFFB4AB),

    bentoArtistsBg = Color(0xFFF3E8FF),
    bentoArtistsTxt = Color(0xFF21005D),
    bentoArtistsIconBg = Color(0xFFEADDFF),

    playlistAccentBlue = PlaylistAccentBlue,
    playlistAccentPurple = PlaylistAccentPurple,
    playlistAccentRose = PlaylistAccentRose,
    playlistAccentMint = PlaylistAccentMint,
    playlistAccentBlueLight = PlaylistAccentBlueLight,
    playlistAccentPurpleLight = PlaylistAccentPurpleLight,
    playlistAccentRoseLight = PlaylistAccentRoseLight,
    playlistAccentMintLight = PlaylistAccentMintLight,
    playlistTextBlueDark = Color(0xFF001D35),
    playlistTextPurpleDark = Color(0xFF21005D),
    playlistTextRoseDark = Color(0xFF410002),
    playlistTextMintDark = Color(0xFF003916),

    genrePop = Color(0xFF27856A),
    genreIndie = Color(0xFF477C2B),
    genreRock = Color(0xFFE8115B),
    genreRandB = Color(0xFFD84080),
    genrePodcasts = Color(0xFF2296F3),
    genreMadeForYou = Color(0xFF1E3264),
    genreCharts = Color(0xFF8D67AB),
    genreNewReleases = Color(0xFFE1306C),

    premiumGoldAccent = Color(0xFFFFD54F),
    premiumGoldTextDark = Color(0xFF7A4F00),
    premiumGoldLight = Color(0xFFFDE16D),
    premiumGoldDark = Color(0xFFD4AC0D),
    premiumGoldFrameBg = Color(0xFFFFF3C4),

    playerBgFallback = Color(0xFFF5FAF6),
    playerDefaultDominant = Color(0xFF1E3524),
    playerSpindleHole = Color(0xFF1A1A1A),

    neutralGrey = Color(0x1B79747E)
)

val DarkSepatifyColors = SepatifyColors(
    surfaceElevated = SurfaceElevated,
    surfaceMuted = SurfaceMuted,
    surfaceHighlight = SurfaceHighlight,

    bentoLikedBg = Color(0xFF003354),
    bentoLikedTxt = Color(0xFFD0E4FF),
    bentoLikedIconBg = Color(0xFF001D35),

    bentoRecentBg = Color(0xFF1E1E1E),
    bentoRecentTxt = Color(0xFFEADDFF),
    bentoRecentIconBg = Color(0xFF21005D),

    bentoPlaylistsBg = Color(0xFF3B080B),
    bentoPlaylistsTxt = Color(0xFFFFDAD6),
    bentoPlaylistsIconBg = Color(0xFF680003),

    bentoArtistsBg = Color(0xFF2D164B),
    bentoArtistsTxt = Color(0xFFEADDFF),
    bentoArtistsIconBg = Color(0xFF21005D),

    playlistAccentBlue = PlaylistAccentBlue,
    playlistAccentPurple = PlaylistAccentPurple,
    playlistAccentRose = PlaylistAccentRose,
    playlistAccentMint = PlaylistAccentMint,
    playlistAccentBlueLight = PlaylistAccentBlueLight,
    playlistAccentPurpleLight = PlaylistAccentPurpleLight,
    playlistAccentRoseLight = PlaylistAccentRoseLight,
    playlistAccentMintLight = PlaylistAccentMintLight,
    playlistTextBlueDark = Color(0xFF001D35),
    playlistTextPurpleDark = Color(0xFF21005D),
    playlistTextRoseDark = Color(0xFF410002),
    playlistTextMintDark = Color(0xFF003916),

    genrePop = Color(0xFF27856A),
    genreIndie = Color(0xFF477C2B),
    genreRock = Color(0xFFE8115B),
    genreRandB = Color(0xFFD84080),
    genrePodcasts = Color(0xFF2296F3),
    genreMadeForYou = Color(0xFF1E3264),
    genreCharts = Color(0xFF8D67AB),
    genreNewReleases = Color(0xFFE1306C),

    premiumGoldAccent = Color(0xFFFFD54F),
    premiumGoldTextDark = Color(0xFF7A4F00),
    premiumGoldLight = Color(0xFFFDE16D),
    premiumGoldDark = Color(0xFFD4AC0D),
    premiumGoldFrameBg = Color(0xFF121212),

    playerBgFallback = Color(0xFF121212),
    playerDefaultDominant = Color(0xFF1E3524),
    playerSpindleHole = Color(0xFF1A1A1A),

    neutralGrey = Color(0x1B79747E)
)

data class SepatifyDimens(
    val zero: Dp = 0.dp,
    val borderHalf: Dp = 0.5.dp,
    val borderThin: Dp = 1.dp,
    val borderMedium: Dp = 1.5.dp,
    val borderThick: Dp = 2.dp,
    val borderHeavy: Dp = 3.dp,
    val borderGiga: Dp = 1.5.dp,
    
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
    val spaceGiant: Dp = 64.dp,
    val spaceTera: Dp = 64.dp,
    val spacePeta: Dp = 68.dp,
    val spaceExa: Dp = 80.dp,
    val spaceZetta: Dp = 100.dp,
    val spaceYotta: Dp = 110.dp,
    
    val sizeIconTiny: Dp = 11.dp,
    val sizeIconSmall: Dp = 16.dp,
    val sizeIconNormal: Dp = 18.dp,
    val sizeIconLarge: Dp = 24.dp,
    val sizeIconExtraLarge: Dp = 32.dp,
    
    val sizeAvatarSmall: Dp = 36.dp,
    val sizeAvatarNormal: Dp = 40.dp,
    val sizeAvatarMedium: Dp = 44.dp,
    val sizeAvatarLarge: Dp = 48.dp,
    val sizeAvatarBig: Dp = 108.dp,
    val sizeAvatarHuge: Dp = 108.dp,
    val sizeAvatarFrame: Dp = 116.dp,
    
    val sizeSongThumbnailSmall: Dp = 40.dp,
    val sizeSongThumbnailMedium: Dp = 50.dp,
    val sizeSongThumbnailNormal: Dp = 54.dp,
    val sizeSongThumbnailLarge: Dp = 110.dp,
    
    val heightBentoSmall: Dp = 64.dp,
    val heightBentoWide: Dp = 68.dp,
    val heightBentoMedium: Dp = 138.dp,
    val heightPlaylistCard: Dp = 134.dp,
    val heightGenreCard: Dp = 100.dp,
    
    val sizeScrollbarThumb: Dp = 6.dp,
    val sizeScrollbarTrack: Dp = 24.dp,
    
    val spaceBottomOverScroll: Dp = 200.dp,

    val sizeLogo: Dp = 100.dp,
    val sizeLogoIcon: Dp = 60.dp,
    val heightStandardButton: Dp = 56.dp,
    val sizeRoundButtonCorner: Dp = 28.dp,
    val sizeEmptyStateCircle: Dp = 80.dp,
    val sizeEmptyStateIcon: Dp = 40.dp,
    val sizeCropBox: Dp = 200.dp,
    val heightCarousel: Dp = 180.dp,
    val sizeBentoIconContainer: Dp = 42.dp,
    val sizeVinylDiskNormal: Dp = 220.dp,
    val sizeVinylDiskActive: Dp = 230.dp,
    val sizeVinylDiskShadow: Dp = 10.dp,
    val sizeVinylDiskRimShine: Dp = 4.dp,
    val heightSliderTrackDefault: Dp = 4.dp,
    val heightSliderTrackExpanded: Dp = 6.dp,
    val heightSliderTrackWrapper: Dp = 24.dp,
    val sizeSliderThumb: Dp = 12.dp,
    val sizeIconPlayPauseCircle: Dp = 28.dp,
    val sizeIconControl: Dp = 26.dp,
    val sizeIconSkip: Dp = 36.dp,
    val sizePlayPauseContainer: Dp = 68.dp,
    val sizeIconPlayPause: Dp = 40.dp,
    val sizeIconSpeed: Dp = 18.dp,
    val heightVisualizerContainer: Dp = 60.dp,
    val visualizerMinHeight: Dp = 2.dp,
    val lyricsPaddingVertical: Dp = 120.dp,
    val maxWidthChatBubble: Dp = 280.dp,
    val heightEmptyChatVisual: Dp = 220.dp,
    val widthDrawer: Dp = 300.dp,
    val marginKeyboardMiniplayer: Dp = 90.dp,
    val heightBottomNavBar: Dp = 72.dp,
    val widthBottomNavTab: Dp = 48.dp,
    val heightBottomNavTabContainer: Dp = 32.dp,
    val heightMaxLazyColumn: Dp = 300.dp,

    // === SEMANTIC STATS & CONFIG CONSTANTS ===
    val alphaDisabled: Float = 0.38f,
    val alphaMuted: Float = 0.5f,
    val alphaSemiMuted: Float = 0.6f,
    val alphaStandard: Float = 0.7f,
    val alphaHighlighted: Float = 0.75f,
    val alphaGrooves: Float = 0.04f,
    val alphaRimShine: Float = 0.08f,
    val alphaGlassReflection: Float = 0.42f,
    val alphaShimmerBase: Float = 0.08f,
    val alphaShimmerHighlight: Float = 0.20f,
    val alphaOverlayAmbient: Float = 0.8f,
    val aspectRatioSquare: Float = 1.0f,
    val scaleSticker: Float = 0.42f,
    val scaleSpindleHole: Float = 0.04f,
    val rotationGroovesZ: Float = 25f
)

data class SepatifyShapes(
    val card: RoundedCornerShape = RoundedCornerShape(24.dp),
    val chip: RoundedCornerShape = RoundedCornerShape(999.dp),
    val dialog: RoundedCornerShape = RoundedCornerShape(20.dp),
    val button: RoundedCornerShape = RoundedCornerShape(16.dp),
    val small: RoundedCornerShape = RoundedCornerShape(12.dp),
    val chatBubbleMe: RoundedCornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 0.dp, bottomStart = 16.dp),
    val chatBubbleOther: RoundedCornerShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 0.dp)
)

val LocalSepatifyColors = staticCompositionLocalOf { LightSepatifyColors }
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
    val sepatifyColors = if (darkTheme) DarkSepatifyColors else LightSepatifyColors
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