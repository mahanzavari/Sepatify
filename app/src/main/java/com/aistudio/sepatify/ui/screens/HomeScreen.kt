package com.aistudio.sepatify.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.viewmodel.HomeUiState
import com.aistudio.sepatify.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    onQuickActionClick: (String) -> Unit, // "liked", "recent", "playlists", "artists"
    locString: (Int) -> String
) {
    val uiState by homeViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        when (val state = uiState) {
            HomeUiState.Loading -> {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Carousel skeleton
                    ShimmerItem(height = 180.dp, cornerRadius = 24.dp)

                    // Bento quick-action skeletons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShimmerItem(modifier = Modifier.weight(1f), height = 138.dp, cornerRadius = 24.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShimmerItem(height = 64.dp, cornerRadius = 24.dp)
                            ShimmerItem(height = 64.dp, cornerRadius = 24.dp)
                        }
                    }
                    ShimmerItem(height = 68.dp, cornerRadius = 24.dp)

                    // Horizontal song section skeletons (Most Popular, New Releases, etc.)
                    repeat(3) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ShimmerItem(
                                modifier = Modifier.fillMaxWidth(0.4f),
                                height = 16.dp,
                                cornerRadius = 4.dp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(4) {
                                    SongCardSkeleton()
                                }
                            }
                        }
                    }
                }
            }
            is HomeUiState.Success -> {
                // 1. CAROUSEL (Hero Bento Card)
                // "The home screen shall display a top carousel of new albums, daily recommendations, and trending songs"
                val carouselSongs = (state.trending.take(3) + state.recommendations.take(3)).distinct()
                val pagerState = rememberPagerState(pageCount = { carouselSongs.size })

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(24.dp)) // heavily rounded bento corners
                ) {
                    HorizontalPager(state = pagerState) { page ->
                        val song = carouselSongs[page]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onSongSelect(song, carouselSongs) }
                        ) {
                            val art = rememberSongArt(song)
                            AsyncImage(
                                model = art,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            // Ambient overlay brush
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(100.dp), // Premium capsule badge
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Text(
                                        text = locString(R.string.daily_recommendations).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = song.artistName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // 2. QUICK ACTIONS (Bento Grid Style)
                // "Four quick-action buttons shall be present: Liked Songs, Recently Played, My Playlists, Top Artists"
                val isDark = isSystemInDarkTheme()

                val likedBg = if (isDark) Color(0xFF003354) else Color(0xFFD0E4FF)
                val likedTxt = if (isDark) Color(0xFFD0E4FF) else Color(0xFF001D35)
                val likedIconBg = if (isDark) Color(0xFF001D35) else Color(0xFFAAC7FF)

                val recentBg = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
                val recentTxt = if (isDark) Color(0xFFEADDFF) else Color(0xFF1C1B1F)
                val recentIconBg = if (isDark) Color(0xFF21005D) else Color(0xFFF3EDF7)

                val playlistsBg = if (isDark) Color(0xFF3B080B) else Color(0xFFFFDAD6)
                val playlistsTxt = if (isDark) Color(0xFFFFDAD6) else Color(0xFF410002)
                val playlistsIconBg = if (isDark) Color(0xFF680003) else Color(0xFFFFB4AB)

                val artistsBg = if (isDark) Color(0xFF2D164B) else Color(0xFFF3E8FF)
                val artistsTxt = if (isDark) Color(0xFFEADDFF) else Color(0xFF21005D)
                val artistsIconBg = if (isDark) Color(0xFF21005D) else Color(0xFFEADDFF)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left element: Liked Songs Medium Tall Bento Card (col-span-1, row-span-2)
                    BentoMediumCard(
                        title = locString(R.string.quick_liked),
                        subtitle = "Offline tracks",
                        icon = Icons.Default.Favorite,
                        backgroundColor = likedBg,
                        textColor = likedTxt,
                        iconContainerColor = likedIconBg,
                        modifier = Modifier.weight(1f),
                        onClick = { onQuickActionClick("liked") }
                    )

                    // Right elements: Stack of two Small Bento Cards
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoSmallCard(
                            title = locString(R.string.quick_recent),
                            icon = Icons.Default.History,
                            backgroundColor = recentBg,
                            textColor = recentTxt,
                            iconContainerColor = recentIconBg,
                            hasBorder = !isDark,
                            onClick = { onQuickActionClick("recent") }
                        )
                        BentoSmallCard(
                            title = locString(R.string.quick_playlists),
                            icon = Icons.Default.LibraryMusic,
                            backgroundColor = playlistsBg,
                            textColor = playlistsTxt,
                            iconContainerColor = playlistsIconBg,
                            onClick = { onQuickActionClick("playlists") }
                        )
                    }
                }

                // Full width wide Bento card (col-span-2, row-span-1)
                BentoWideCard(
                    title = locString(R.string.quick_artists),
                    subtitle = "Social Hub & Core Community",
                    icon = Icons.Default.People,
                    backgroundColor = artistsBg,
                    textColor = artistsTxt,
                    iconContainerColor = artistsIconBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    onClick = { onQuickActionClick("artists") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. HORIZONTAL SECTIONS
                // "Horizontal LazyRow sections shall display: Most Popular, New Releases, Global Playlists, Local Playlists"
                HorizontalSongSection(
                    title = locString(R.string.most_popular),
                    songs = state.mostPopular,
                    onSongSelect = { onSongSelect(it, state.mostPopular) }
                )

                HorizontalSongSection(
                    title = locString(R.string.new_releases),
                    songs = state.newReleases,
                    onSongSelect = { onSongSelect(it, state.newReleases) }
                )

                HorizontalSongSection(
                    title = locString(R.string.global_playlists),
                    songs = state.globalPlaylistSongs,
                    onSongSelect = { onSongSelect(it, state.globalPlaylistSongs) }
                )

                HorizontalSongSection(
                    title = locString(R.string.local_playlists),
                    songs = state.localPlaylistSongs,
                    onSongSelect = { onSongSelect(it, state.localPlaylistSongs) }
                )
            }
        }
    }
}

@Composable
fun BentoMediumCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    iconContainerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(138.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun BentoSmallCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    iconContainerColor: Color,
    hasBorder: Boolean = false,
    onClick: () -> Unit
) {
    val borderModifier = if (hasBorder) {
        Modifier.border(width = 1.dp, color = Color(0x1B79747E), shape = RoundedCornerShape(24.dp))
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .then(borderModifier)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BentoWideCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    iconContainerColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }
    }
}

@Composable
fun HorizontalSongSection(
    title: String,
    songs: List<Song>,
    onSongSelect: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        if (songs.isEmpty()) {
            // Shimmer placeholders while this section's data is still arriving
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) { SongCardSkeleton() }
            }
        } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clickable { onSongSelect(song) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val art = rememberSongArt(song)
                    AsyncImage(
                        model = art,
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = song.artistName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }
        }
        } // end else
    }
}
