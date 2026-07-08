package com.aistudio.sepatify.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.PlaylistViewModel

@Composable
fun PlaylistsScreen(
    playlistViewModel: PlaylistViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    locString: (Int) -> String
) {
    val playlists by playlistViewModel.userPlaylists.collectAsState()
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistsFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(playlists) {
        if (playlistsFirstLoad) playlistsFirstLoad = false
    }

    var newPlaylistTitle by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("User") }

    val context = LocalContext.current
    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }

    LaunchedEffect(selectedPlaylist) {
        hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    val sepatifyColors = MaterialTheme.sepatifyColors
    val sepatifyShapes = MaterialTheme.sepatifyShapes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (selectedPlaylist == null) {
            // Main lists overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = locString(R.string.playlists_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Button(
                    onClick = { showCreateDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create playlist")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = locString(R.string.create_playlist), style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two column playlist grids
            // "Playlists shall be displayed in a two-column LazyVerticalGrid"
            val totalPlaylists = listOf(
                PlaylistEntity(-3, locString(R.string.quick_liked), "Your liked tracks", false, "Liked"),
                PlaylistEntity(-1, locString(R.string.international_music_category), "Seeded playlist tracks", false, "International"),
                PlaylistEntity(-2, locString(R.string.local_music_category), "Traditional local tracks", false, "Local")
            ) + playlists

            val isDark = isSystemInDarkTheme()

            if (playlistsFirstLoad) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(6) { PlaylistCardSkeleton() }
                }
            } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(totalPlaylists) { plt ->
                    val index = totalPlaylists.indexOf(plt)
                    val (backgroundColor, textColor, iconColor) = when (index % 4) {
                        0 -> if (isDark) {
                            Triple(sepatifyColors.playlistAccentBlue, sepatifyColors.playlistAccentBlueLight, sepatifyColors.playlistAccentBlueLight.copy(alpha = 0.7f))
                        } else {
                            Triple(sepatifyColors.playlistAccentBlueLight, Color(0xFF001D35), Color(0xFF90CAF9))
                        }
                        1 -> if (isDark) {
                            Triple(sepatifyColors.playlistAccentPurple, sepatifyColors.playlistAccentPurpleLight, sepatifyColors.playlistAccentPurpleLight)
                        } else {
                            Triple(sepatifyColors.playlistAccentPurpleLight, Color(0xFF21005D), Color(0xFFD0BCFF))
                        }
                        2 -> if (isDark) {
                            Triple(sepatifyColors.playlistAccentRose, sepatifyColors.playlistAccentRoseLight, sepatifyColors.playlistAccentRoseLight)
                        } else {
                            Triple(sepatifyColors.playlistAccentRoseLight, Color(0xFF410002), Color(0xFFFFB4AB))
                        }
                        else -> if (isDark) {
                            Triple(sepatifyColors.playlistAccentMint, sepatifyColors.playlistAccentMintLight, sepatifyColors.playlistAccentMintLight)
                        } else {
                            Triple(sepatifyColors.playlistAccentMintLight, Color(0xFF003916), Color(0xFFAFE3C0))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(134.dp)
                            .clickable { selectedPlaylist = plt },
                        shape = sepatifyShapes.card,
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
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(iconColor.copy(alpha = 0.35f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = plt.category.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = plt.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = plt.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.75f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            } // end else (not first load)
        } else {
            // Detailed playlist View
            val plist = selectedPlaylist!!

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = plist.title, style = MaterialTheme.typography.titleLarge)
                    Text(text = plist.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }

                // Show delete button only if user created it!
                if (plist.isUserCreated) {
                    IconButton(onClick = {
                        playlistViewModel.deletePlaylist(plist.id)
                        selectedPlaylist = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (plist.category == "Local" && !hasPermission) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Storage Permission",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Storage Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sepatify needs permission to access your device storage so we can read and play your local audio files.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { launcher.launch(permission) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Grant Permission")
                    }
                }
            } else {
                val pagedSongs = remember(plist.id, plist.category) {
                    playlistViewModel.getSongsForPlaylistPaged(plist.id, plist.category)
                }.collectAsLazyPagingItems()

                if (pagedSongs.itemCount == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(text = locString(R.string.no_results), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            Text(text = "Add a few tracks to this playlist and they will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(pagedSongs.itemCount, key = { index -> pagedSongs[index]?.id ?: index }) { index ->
                            val s = pagedSongs[index] ?: return@items
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSongSelect(s, pagedSongs.itemSnapshotList.items) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val art = rememberSongArt(s)
                                AsyncImage(
                                    model = art,
                                    contentDescription = s.title,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = s.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                    Text(text = s.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }

                                if (plist.isUserCreated) {
                                    IconButton(onClick = {
                                        playlistViewModel.removeSongFromPlaylist(plist.id, s.id)
                                    }) {
                                        Icon(Icons.Default.RemoveCircle, contentDescription = "Remove song", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CREATE NEW PLAYLIST DIALOG
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(locString(R.string.create_playlist)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newPlaylistTitle,
                            onValueChange = { newPlaylistTitle = it },
                            label = { Text(locString(R.string.playlist_name_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text(locString(R.string.playlist_desc_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistTitle.isNotBlank()) {
                                playlistViewModel.createNewPlaylist(newPlaylistTitle, newPlaylistDesc, "User")
                                newPlaylistTitle = ""
                                newPlaylistDesc = ""
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
