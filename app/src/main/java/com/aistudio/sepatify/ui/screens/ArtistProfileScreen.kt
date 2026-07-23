package com.aistudio.sepatify.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.viewmodel.ArtistEvent
import com.aistudio.sepatify.ui.viewmodel.ArtistUiState
import com.aistudio.sepatify.ui.viewmodel.ArtistViewModel

@Composable
fun ArtistProfileScreen(
    artistId: String,
    artistViewModel: ArtistViewModel,
    onBack: () -> Unit,
    onSongSelect: (Song, List<Song>) -> Unit
) {
    val uiState by artistViewModel.uiState.collectAsState()

    LaunchedEffect(artistId) {
        artistViewModel.onEvent(ArtistEvent.LoadProfile(artistId))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E0E))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Artist", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
            }
        }

        when (val state = uiState) {
            ArtistUiState.Loading -> {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(6) { SongRowSkeleton() }
                }
            }
            is ArtistUiState.Error -> {
                EmptyStateView(
                    icon = Icons.Default.PlayArrow,
                    title = "Couldn't load this artist",
                    subtitle = state.message,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
            is ArtistUiState.Success -> {
                val artist = state.profile.artist
                val songs = state.profile.songs

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2020))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = artist.avatarUrl,
                                            contentDescription = artist.displayName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Color(0xFF1DB954), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = artist.displayName,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (artist.verified) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color(0xFF1DB954), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Text("@${artist.username}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "${artist.monthlyListeners} monthly listeners",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF1DB954)
                                        )
                                    }
                                }

                                if (!artist.bio.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(artist.bio, style = MaterialTheme.typography.bodyMedium, color = Color.LightGray)
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = { artistViewModel.onEvent(ArtistEvent.ToggleFollow(artist.id)) },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (artist.isFollowed) Color.Transparent else Color(0xFF1DB954)),
                                    shape = RoundedCornerShape(12.dp),
                                    border = if (artist.isFollowed) BorderStroke(1.dp, Color(0xFF1DB954)) else null
                                ) {
                                    Text(
                                        if (artist.isFollowed) "Following" else "Follow",
                                        color = if (artist.isFollowed) Color(0xFF1DB954) else Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Popular", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${songs.size} songs", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    }

                    if (songs.isEmpty()) {
                        item {
                            Text("No songs from this artist yet.", color = Color.Gray, modifier = Modifier.padding(16.dp))
                        }
                    } else {
                        items(songs, key = { it.id }) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongSelect(song, songs) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = song.coverImageUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = song.title, style = MaterialTheme.typography.bodyLarge, color = Color.White, maxLines = 1)
                                    Text(text = song.artistName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF1DB954))
                            }
                        }
                    }
                }
            }
        }
    }
}