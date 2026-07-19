package com.aistudio.sepatify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.DownloadedSongEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.viewmodel.DownloadEvent
import com.aistudio.sepatify.ui.viewmodel.DownloadViewModel
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    downloadViewModel: DownloadViewModel,
    isPremium: Boolean,
    onSongSelect: (Song, List<Song>) -> Unit,
    locString: (Int) -> String
) {
    val downloadedSongs by downloadViewModel.downloadedSongs.collectAsState()
    val activeDownloads by downloadViewModel.activeDownloads.collectAsState()
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(downloadedSongs) {
        if (isFirstLoad) isFirstLoad = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Text(
            text = locString(R.string.downloads_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isFirstLoad) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(6) { SongRowSkeleton() }
            }
        } else if (downloadedSongs.isEmpty() && activeDownloads.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.CloudDownload,
                title = locString(R.string.downloads_empty_title),
                subtitle = locString(R.string.downloads_empty_subtitle),
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            var expanded by remember { mutableStateOf(false) }
            val sortType by downloadViewModel.sortType.collectAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tracks_count, downloadedSongs.size),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                Box {
                    TextButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.cd_sort))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = stringResource(R.string.sort))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text(locString(R.string.sort_by_date)) },
                            onClick = { downloadViewModel.onEvent(DownloadEvent.UpdateSort("date")); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(locString(R.string.sort_by_title)) },
                            onClick = { downloadViewModel.onEvent(DownloadEvent.UpdateSort("title")); expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text(locString(R.string.sort_by_artist)) },
                            onClick = { downloadViewModel.onEvent(DownloadEvent.UpdateSort("artist")); expanded = false }
                        )
                    }
                }
            }

            Text(
                text = locString(R.string.swipe_to_delete_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 200.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                items(activeDownloads.toList()) { (songId, progress) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = locString(R.string.downloading), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                items(
                    items = downloadedSongs,
                    key = { it.id }
                ) { entity ->
                    val domainSong = Song(
                        id = entity.id,
                        title = entity.title,
                        artistName = entity.artistName,
                        coverImageUrl = entity.coverImageUrl,
                        audioUrl = entity.audioUrl
                    )

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                downloadViewModel.onEvent(DownloadEvent.RemoveDownload(entity.id))
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.cd_delete),
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val domainQueue = downloadedSongs.map { s ->
                                        Song(s.id, s.title, s.artistName, s.coverImageUrl, s.audioUrl)
                                    }
                                    onSongSelect(domainSong, domainQueue)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = entity.coverImageUrl,
                                    contentDescription = entity.title,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = entity.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(text = entity.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.cd_downloaded),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}