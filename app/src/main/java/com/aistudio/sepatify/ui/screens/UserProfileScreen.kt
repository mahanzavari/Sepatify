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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.ui.viewmodel.ChatViewModel

@Composable
fun UserProfileScreen(
    username: String,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onPlaylistClick: (PlaylistEntity) -> Unit
) {
    val profile by chatViewModel.getProfile(username).collectAsState(initial = null)
    val isFollowing by chatViewModel.isFollowing(username).collectAsState(initial = false)
    val details by chatViewModel.viewedUserDetails.collectAsState()

    LaunchedEffect(username) {
        chatViewModel.loadUserDetails(username)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E0E)) // Deep pitch black from Figma design
            .statusBarsPadding()
    ) {
        // 1. Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Profile", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
            IconButton(onClick = { }) {
                Icon(Icons.Default.MoreHoriz, contentDescription = "More", tint = Color.White)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            // 2. Profile Header (Bento Style Card)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2020)) // Dark slate container
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = profile?.avatarUrl,
                                    contentDescription = username,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Color(0xFF1DB954), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954))
                                        .border(2.dp, Color(0xFF1F2020), CircleShape)
                                )
                            }
                            Column {
                                Text(
                                    text = profile?.displayName ?: username,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("@$username", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Mock "Currently Listening" pill
                                Row(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1DB954).copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Equalizer, contentDescription = null, tint = Color(0xFF1DB954), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Solar Drift", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1DB954))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { chatViewModel.toggleFollow(username) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (isFollowing) Color.Transparent else Color(0xFF1DB954)),
                                shape = RoundedCornerShape(12.dp),
                                border = if (isFollowing) BorderStroke(1.dp, Color(0xFF1DB954)) else null
                            ) {
                                Text(if (isFollowing) "Following" else "Follow", color = if (isFollowing) Color(0xFF1DB954) else Color.Black, fontWeight = FontWeight.Bold)
                            }
                            IconButton(
                                onClick = onChatClick,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1DB954))
                            ) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Chat", tint = Color.Black)
                            }
                            IconButton(
                                onClick = { },
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2B2B))
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. Social Stats Row
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2020))) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        StatItem(details?.followersCount.toString(), "Followers")
                        Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        StatItem(details?.followingCount.toString(), "Following")
                        Box(modifier = Modifier.height(30.dp).width(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                        StatItem(details?.playlists?.size.toString(), "Playlists")
                    }
                }
            }

            // 4. Tabs
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F2020)).padding(4.dp)) {
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF1DB954)).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Public Playlists", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Recently Played", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Public Playlists Title
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Public Playlists", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("See all >", style = MaterialTheme.typography.labelLarge, color = Color(0xFF1DB954))
                }
            }

            // 6. Playlists Grid
            val playlists = details?.playlists ?: emptyList()
            if (playlists.isEmpty()) {
                item { Text("No public playlists available.", color = Color.Gray, modifier = Modifier.padding(16.dp)) }
            } else {
                items(playlists.chunked(2)) { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        for (playlist in rowItems) {
                            val gradientColors = remember(playlist.id) {
                                val base = when (playlist.id % 4) {
                                    0L -> Color(0xFF4A148C) // Deep Purple
                                    1L -> Color(0xFF1B5E20) // Dark Emerald
                                    2L -> Color(0xFFB71C1C) // Dark Red/Rose
                                    else -> Color(0xFF01579B) // Deep Blue
                                }
                                listOf(base, base.copy(alpha = 0.5f))
                            }

                            Card(
                                modifier = Modifier.weight(1f).aspectRatio(0.85f).clickable { onPlaylistClick(playlist) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(gradientColors)).padding(16.dp)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(playlist.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                        Text(playlist.title, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${(playlist.id * 3 % 20) + 5} tracks", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                                    }
                                    Box(modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).clip(CircleShape).background(Color(0xFF1DB954)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}