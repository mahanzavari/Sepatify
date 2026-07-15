package com.aistudio.sepatify.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.PlaylistViewModel

@Composable
fun PlaylistsScreen(
    playlistViewModel: PlaylistViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    onShareClick: (Any) -> Unit = {},
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
    val dimens = MaterialTheme.sepatifyDimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(dimens.spaceNormal)
    ) {
        AnimatedContent(
            targetState = selectedPlaylist != null,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(
                        initialOffsetX = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(180)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(animationSpec = tween(120))
                        )
                } else {
                    (slideInHorizontally(
                        initialOffsetX = { -it / 4 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(180)))
                        .togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeOut(animationSpec = tween(120))
                        )
                }
            },
            label = ""
        ) { isDetail ->
            if (!isDetail) {
                Spacer(modifier = Modifier.height(dimens.spaceNormal))

                val totalPlaylists = listOf(
                    PlaylistEntity(-3, locString(R.string.quick_liked), "Your liked tracks", false, "Liked"),
                    PlaylistEntity(-1, locString(R.string.international_music_category), "Seeded playlist tracks", false, "International"),
                    PlaylistEntity(-2, locString(R.string.local_music_category), "Traditional local tracks", false, "Local")
                ) + playlists

                val isDark = isSystemInDarkTheme()

                if (playlistsFirstLoad) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) { PlaylistCardSkeleton() }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(totalPlaylists) { plt ->
                            val index = totalPlaylists.indexOf(plt)
                            val (backgroundColor, textColor, _) = when (index % 4) {
                                0 -> if (isDark) {
                                    Triple(sepatifyColors.playlistAccentBlue, sepatifyColors.playlistAccentBlueLight, sepatifyColors.playlistAccentBlueLight.copy(alpha = 0.7f))
                                } else {
                                    Triple(sepatifyColors.playlistAccentBlueLight, sepatifyColors.playlistTextBlueDark, sepatifyColors.playlistAccentBlueLight)
                                }
                                1 -> if (isDark) {
                                    Triple(sepatifyColors.playlistAccentPurple, sepatifyColors.playlistAccentPurpleLight, sepatifyColors.playlistAccentPurpleLight)
                                } else {
                                    Triple(sepatifyColors.playlistAccentPurpleLight, sepatifyColors.playlistTextPurpleDark, sepatifyColors.playlistAccentPurpleLight)
                                }
                                2 -> if (isDark) {
                                    Triple(sepatifyColors.playlistAccentRose, sepatifyColors.playlistAccentRoseLight, sepatifyColors.playlistAccentRoseLight)
                                } else {
                                    Triple(sepatifyColors.playlistAccentRoseLight, sepatifyColors.playlistTextRoseDark, sepatifyColors.playlistAccentRoseLight)
                                }
                                else -> if (isDark) {
                                    Triple(sepatifyColors.playlistAccentMint, sepatifyColors.playlistAccentMintLight, sepatifyColors.playlistAccentMintLight)
                                } else {
                                    Triple(sepatifyColors.playlistAccentMintLight, sepatifyColors.playlistTextMintDark, sepatifyColors.playlistAccentMintLight)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(dimens.heightPlaylistCard)
                                    .clickable { selectedPlaylist = plt },
                                shape = sepatifyShapes.card,
                                colors = CardDefaults.cardColors(containerColor = backgroundColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(dimens.spaceNormal),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(sepatifyShapes.chip)
                                            .background(textColor.copy(alpha = 0.24f))
                                            .padding(horizontal = dimens.spaceTen, vertical = dimens.spaceFive)
                                    ) {
                                        when (plt.category) {
                                            "Liked" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = plt.title,
                                                    tint = textColor,
                                                    modifier = Modifier.size(dimens.sizeIconSmall)
                                                )
                                            }
                                            "Local" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = plt.title,
                                                    tint = textColor,
                                                    modifier = Modifier.size(dimens.sizeIconSmall)
                                                )
                                            }
                                            "International" -> {
                                                Icon(
                                                    imageVector = Icons.Default.Language,
                                                    contentDescription = plt.title,
                                                    tint = textColor,
                                                    modifier = Modifier.size(dimens.sizeIconSmall)
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = plt.category.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = textColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(dimens.spaceTen))
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
                }
            } else {
                val plist = selectedPlaylist ?: return@AnimatedContent

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedPlaylist = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = locString(R.string.back_desc))
                        }
                        Spacer(modifier = Modifier.width(dimens.spaceEight))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = plist.title, style = MaterialTheme.typography.titleLarge)
                            Text(text = plist.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }

                        IconButton(onClick = { onShareClick(plist) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Playlist",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (plist.isUserCreated) {
                            IconButton(onClick = {
                                playlistViewModel.deletePlaylist(plist.id)
                                selectedPlaylist = null
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = locString(R.string.remove_from_playlist), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(dimens.spaceNormal))

                    if (plist.category == "Local" && !hasPermission) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimens.spaceNormal, vertical = dimens.spaceHuge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimens.spaceNormal)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = locString(R.string.permission_storage_required),
                                modifier = Modifier.size(dimens.spaceTera),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = locString(R.string.permission_storage_required),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = locString(R.string.permission_storage_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { launcher.launch(permission) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(locString(R.string.permission_grant_btn))
                            }
                        }
                    } else {
                        val pagedSongs = remember(plist.id, plist.category) {
                            playlistViewModel.getSongsForPlaylistPaged(plist.id, plist.category)
                        }.collectAsLazyPagingItems()

                        if (pagedSongs.itemCount == 0) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(vertical = dimens.spaceHuge),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
                                    Icon(Icons.Default.QueueMusic, contentDescription = locString(R.string.no_results), modifier = Modifier.size(dimens.spaceTera), tint = MaterialTheme.colorScheme.primary)
                                    Text(text = locString(R.string.no_results), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                    Text(text = locString(R.string.playlist_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            val listState = rememberLazyListState()

                            Box(modifier = Modifier.fillMaxSize()) {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll),
                                    verticalArrangement = Arrangement.spacedBy(dimens.spaceTen),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(pagedSongs.itemCount, key = { index -> pagedSongs[index]?.id ?: index }) { index ->
                                        val s = pagedSongs[index] ?: return@items
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSongSelect(s, pagedSongs.itemSnapshotList.items) }
                                                .padding(vertical = dimens.spaceSix)
                                                .padding(end = dimens.spaceLarge),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                                        ) {
                                            val art = rememberSongArt(s)
                                            AsyncImage(
                                                model = art,
                                                contentDescription = s.title,
                                                modifier = Modifier
                                                    .size(dimens.sizeSongThumbnailMedium)
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = s.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                                Text(text = s.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                            }

                                            Box {
                                                var moreMenuExpanded by remember { mutableStateOf(false) }
                                                IconButton(onClick = { moreMenuExpanded = true }) {
                                                    Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "More Options",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = moreMenuExpanded,
                                                    onDismissRequest = { moreMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(locString(R.string.share)) },
                                                        onClick = {
                                                            moreMenuExpanded = false
                                                            onShareClick(s)
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Add to queue") },
                                                        onClick = {
                                                            moreMenuExpanded = false
                                                            android.widget.Toast.makeText(context, "Added to queue (Mock)", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Add to playlist") },
                                                        onClick = {
                                                            moreMenuExpanded = false
                                                            android.widget.Toast.makeText(context, "Added to playlist (Mock)", android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                                                    )
                                                }
                                            }

                                            if (plist.isUserCreated) {
                                                IconButton(onClick = {
                                                    playlistViewModel.removeSongFromPlaylist(plist.id, s.id)
                                                }) {
                                                    Icon(Icons.Default.RemoveCircle, contentDescription = locString(R.string.remove_from_playlist), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                                }
                                            }
                                        }
                                    }
                                }

                                val coroutineScope = rememberCoroutineScope()
                                val totalItemsCount = listState.layoutInfo.totalItemsCount
                                if (totalItemsCount > 0) {
                                    val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                                    val firstVisibleItemIndex = listState.firstVisibleItemIndex

                                    val thumbHeightRatio = (visibleItemsCount.toFloat() / totalItemsCount.toFloat()).coerceIn(0.1f, 1f)

                                    if (thumbHeightRatio < 1f) {
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .width(dimens.sizeScrollbarTrack)
                                        ) {
                                            val trackHeightPx = constraints.maxHeight.toFloat()
                                            val thumbHeightPx = trackHeightPx * thumbHeightRatio
                                            val scrollableTrackPx = trackHeightPx - thumbHeightPx

                                            val thumbOffsetYPx = if (totalItemsCount - visibleItemsCount > 0) {
                                                (firstVisibleItemIndex.toFloat() / (totalItemsCount - visibleItemsCount)) * scrollableTrackPx
                                            } else 0f

                                            var accumulatedDrag by remember { mutableFloatStateOf(0f) }

                                            Box(
                                                modifier = Modifier
                                                    .offset { androidx.compose.ui.unit.IntOffset(0, thumbOffsetYPx.toInt()) }
                                                    .height(with(androidx.compose.ui.platform.LocalDensity.current) { thumbHeightPx.toDp() })
                                                    .width(dimens.sizeScrollbarThumb)
                                                    .clip(RoundedCornerShape(dimens.spaceThree))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                                    .align(Alignment.TopEnd)
                                                    .pointerInput(Unit) {
                                                        detectVerticalDragGestures(
                                                            onDragStart = { accumulatedDrag = thumbOffsetYPx },
                                                            onVerticalDrag = { change, dragAmount ->
                                                                change.consume()
                                                                accumulatedDrag += dragAmount
                                                                if (scrollableTrackPx > 0) {
                                                                    val dragProportion = (accumulatedDrag / scrollableTrackPx).coerceIn(0f, 1f)
                                                                    val targetIndex = (dragProportion * (totalItemsCount - visibleItemsCount)).toInt()
                                                                    coroutineScope.launch {
                                                                        listState.scrollToItem(targetIndex)
                                                                    }
                                                                }
                                                            }
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text(locString(R.string.create_playlist)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
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
                        Text(locString(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text(locString(R.string.cancel))
                    }
                }
            )
        }
    }
}