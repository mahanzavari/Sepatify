package com.aistudio.sepatify.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp

enum class PlaylistsViewState { MAIN, FOLDER_DETAIL, CREATE_PLAYLIST, CREATE_FOLDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    playlistViewModel: PlaylistViewModel,
    onSongSelect: (Song, List<Song>) -> Unit,
    onShareClick: (Any) -> Unit = {},
    locString: (Int) -> String
) {
    val playlists by playlistViewModel.userPlaylists.collectAsState()
    
    var activeView by remember { mutableStateOf(PlaylistsViewState.MAIN) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var activeFolder by remember { mutableStateOf<String?>(null) }
    
    var showAddBottomSheet by remember { mutableStateOf(false) }
    var playlistsFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(playlists) {
        if (playlistsFirstLoad) playlistsFirstLoad = false
    }

    // Permissions
    val context = androidx.compose.ui.platform.LocalContext.current
    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }

    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes
    val colors = MaterialTheme.sepatifyColors

    // Back Handler
    androidx.activity.compose.BackHandler(enabled = selectedPlaylist != null || activeView != PlaylistsViewState.MAIN) {
        if (selectedPlaylist != null) {
            selectedPlaylist = null
        } else {
            activeView = PlaylistsViewState.MAIN
            activeFolder = null
        }
    }

    if (showAddBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        val coroutineScope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showAddBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(bottom = dimens.spaceLarge)) {
                Text(
                    text = "Create New",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceEight),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ListItem(
                    headlineContent = { Text("Add Playlist", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Create a new playlist with songs") },
                    leadingContent = {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showAddBottomSheet = false
                            activeView = PlaylistsViewState.CREATE_PLAYLIST
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Add Folder", fontWeight = FontWeight.Bold) },
                    supportingContent = { Text("Group existing playlists together") },
                    leadingContent = {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            showAddBottomSheet = false
                            activeView = PlaylistsViewState.CREATE_FOLDER
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(dimens.spaceNormal)
    ) {
        AnimatedContent(
            targetState = selectedPlaylist != null || activeView != PlaylistsViewState.MAIN,
            transitionSpec = {
                (slideInHorizontally(initialOffsetX = { if (targetState) it / 4 else -it / 4 }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { if (targetState) -it / 4 else it / 4 }) + fadeOut())
            },
            label = ""
        ) { isDetailMode ->
            if (!isDetailMode) {
                // MAIN GRID
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceNormal),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = locString(R.string.playlists_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showAddBottomSheet = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    val totalPlaylists = listOf(
                        PlaylistEntity(-3, locString(R.string.quick_liked), "Your liked tracks", false, "Liked"),
                        PlaylistEntity(-1, locString(R.string.international_music_category), "Seeded playlist tracks", false, "International"),
                        PlaylistEntity(-2, locString(R.string.local_music_category), "Traditional local tracks", false, "Local")
                    ) + playlists

                    val standardPlaylists = totalPlaylists.filter { !it.category.startsWith("folder:") }
                    val folderNames = totalPlaylists.filter { it.category.startsWith("folder:") }
                        .map { it.category.removePrefix("folder:") }.distinct()

                    val gridItems = standardPlaylists + folderNames

                    if (playlistsFirstLoad) {
                        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve), verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
                            items(6) { PlaylistCardSkeleton() }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                            verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                            contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll)
                        ) {
                            items(gridItems.size) { index ->
                                val item = gridItems[index]
                                if (item is String) {
                                    // Render Folder Card
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(dimens.heightPlaylistCard).clickable {
                                            activeFolder = item
                                            activeView = PlaylistsViewState.FOLDER_DETAIL
                                        },
                                        shape = shapes.card,
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(dimens.spaceNormal),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(modifier = Modifier.clip(shapes.chip).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)).padding(horizontal = dimens.spaceTen, vertical = dimens.spaceFive)) {
                                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(dimens.sizeIconSmall))
                                            }
                                            Spacer(modifier = Modifier.height(dimens.spaceTen))
                                            Column {
                                                Text(text = item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text(text = "Playlist Folder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f))
                                            }
                                        }
                                    }
                                } else if (item is PlaylistEntity) {
                                    val (backgroundColor, textColor, _) = getCardColors(index, isSystemInDarkTheme(), colors)
                                    Card(
                                        modifier = Modifier.fillMaxWidth().height(dimens.heightPlaylistCard).clickable { selectedPlaylist = item },
                                        shape = shapes.card,
                                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize().padding(dimens.spaceNormal), verticalArrangement = Arrangement.SpaceBetween) {
                                            Box(modifier = Modifier.clip(shapes.chip).background(textColor.copy(alpha = 0.24f)).padding(horizontal = dimens.spaceTen, vertical = dimens.spaceFive)) {
                                                when (item.category) {
                                                    "Liked" -> Icon(Icons.Default.Favorite, contentDescription = null, tint = textColor, modifier = Modifier.size(dimens.sizeIconSmall))
                                                    "Local" -> Icon(Icons.Default.Folder, contentDescription = null, tint = textColor, modifier = Modifier.size(dimens.sizeIconSmall))
                                                    "International" -> Icon(Icons.Default.Language, contentDescription = null, tint = textColor, modifier = Modifier.size(dimens.sizeIconSmall))
                                                    else -> Text(item.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Column {
                                                Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1)
                                                Text(text = item.description, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.75f), maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Secondary Views
                when {
                    selectedPlaylist != null -> {
                        PlaylistDetailView(
                            playlist = selectedPlaylist!!,
                            hasPermission = hasPermission,
                            onBack = { selectedPlaylist = null },
                            onShare = { onShareClick(selectedPlaylist!!) },
                            onDelete = {
                                playlistViewModel.deletePlaylist(selectedPlaylist!!.id)
                                selectedPlaylist = null
                            },
                            onRemoveSong = { sid -> playlistViewModel.removeSongFromPlaylist(selectedPlaylist!!.id, sid) },
                            onSongSelect = onSongSelect,
                            requestPermission = { launcher.launch(permission) },
                            playlistViewModel = playlistViewModel,
                            locString = locString
                        )
                    }
                    activeView == PlaylistsViewState.FOLDER_DETAIL && activeFolder != null -> {
                        FolderDetailView(
                            folderName = activeFolder!!,
                            playlists = playlists.filter { it.category == "folder:$activeFolder" },
                            onBack = { activeView = PlaylistsViewState.MAIN; activeFolder = null },
                            onPlaylistSelect = { selectedPlaylist = it }
                        )
                    }
                    activeView == PlaylistsViewState.CREATE_PLAYLIST -> {
                        CreatePlaylistView(
                            playlistViewModel = playlistViewModel,
                            onBack = { activeView = PlaylistsViewState.MAIN }
                        )
                    }
                    activeView == PlaylistsViewState.CREATE_FOLDER -> {
                        CreateFolderView(
                            playlistViewModel = playlistViewModel,
                            availablePlaylists = playlists.filter { it.isUserCreated && !it.category.startsWith("folder:") },
                            onBack = { activeView = PlaylistsViewState.MAIN }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderDetailView(
    folderName: String,
    playlists: List<PlaylistEntity>,
    onBack: () -> Unit,
    onPlaylistSelect: (PlaylistEntity) -> Unit
) {
    val dimens = MaterialTheme.sepatifyDimens
    val colors = MaterialTheme.sepatifyColors
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceNormal), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(modifier = Modifier.width(dimens.spaceEight))
            Column {
                Text(text = folderName, style = MaterialTheme.typography.titleLarge)
                Text(text = "Folder · ${playlists.size} Playlists", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
            contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll)
        ) {
            items(playlists.size) { index ->
                val item = playlists[index]
                val (backgroundColor, textColor, _) = getCardColors(index, isSystemInDarkTheme(), colors)
                Card(
                    modifier = Modifier.fillMaxWidth().height(dimens.heightPlaylistCard).clickable { onPlaylistSelect(item) },
                    shape = MaterialTheme.sepatifyShapes.card,
                    colors = CardDefaults.cardColors(containerColor = backgroundColor)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(dimens.spaceNormal), verticalArrangement = Arrangement.SpaceBetween) {
                        Box(modifier = Modifier.clip(MaterialTheme.sepatifyShapes.chip).background(textColor.copy(alpha = 0.24f)).padding(horizontal = dimens.spaceTen, vertical = dimens.spaceFive)) {
                            Text("USER", style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = item.description, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.75f), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlaylistView(
    playlistViewModel: PlaylistViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var tabIndex by remember { mutableStateOf(0) }
    val selectedSongs = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val likedSongs by playlistViewModel.getSongsForPlaylist(-3L, "Liked").collectAsState(initial = emptyList())
    val recentSongs by playlistViewModel.getRecentlyPlayedSongs().collectAsState(initial = emptyList())
    val dimens = MaterialTheme.sepatifyDimens

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceNormal), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("New Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedSongs.isNotEmpty()) {
                        isCreating = true
                        playlistViewModel.createNewPlaylistWithSongs(title, desc, selectedSongs) { success, errorMsg ->
                            isCreating = false
                            if (success) {
                                onBack()
                            } else {
                                android.widget.Toast.makeText(context, errorMsg ?: "Failed to create playlist", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                enabled = title.isNotBlank() && selectedSongs.isNotEmpty() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        }

        // Form Inputs
        Column {
            OutlinedTextField(
                value = title, 
                onValueChange = { title = it }, 
                label = { Text("Playlist Name") }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.sepatifyShapes.small,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(dimens.spaceEight))
            OutlinedTextField(
                value = desc, 
                onValueChange = { desc = it }, 
                label = { Text("Description (Optional)") }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.sepatifyShapes.small,
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(dimens.spaceLarge))
        
        // Tab Layout for selections
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = tabIndex == 0, 
                onClick = { tabIndex = 0 }, 
                text = { Text("Liked Songs", fontWeight = if (tabIndex==0) FontWeight.Bold else FontWeight.Normal) }
            )
            Tab(
                selected = tabIndex == 1, 
                onClick = { tabIndex = 1 }, 
                text = { Text("Recently Played", fontWeight = if (tabIndex==1) FontWeight.Bold else FontWeight.Normal) }
            )
        }

        val list = (if (tabIndex == 0) likedSongs else recentSongs).filter { !it.id.startsWith("local_") && !it.audioUrl.startsWith("file://") && !it.audioUrl.startsWith("/") }
        
        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No cloud songs found in this category.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(top = dimens.spaceNormal, bottom = dimens.spaceBottomOverScroll)) {
                items(list) { song ->
                    val isSelected = selectedSongs.contains(song.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (isSelected) selectedSongs.remove(song.id) else selectedSongs.add(song.id) }
                            .padding(vertical = dimens.spaceEight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(dimens.spaceTwelve))
                        AsyncImage(
                            model = song.coverImageUrl, 
                            contentDescription = null, 
                            modifier = Modifier.size(50.dp).clip(MaterialTheme.sepatifyShapes.small)
                        )
                        Spacer(modifier = Modifier.width(dimens.spaceTwelve))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(song.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFolderView(
    playlistViewModel: PlaylistViewModel,
    availablePlaylists: List<PlaylistEntity>,
    onBack: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    val selectedPlaylists = remember { mutableStateListOf<Long>() }
    var isGrouping by remember { mutableStateOf(false) }
    val dimens = MaterialTheme.sepatifyDimens

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = dimens.spaceNormal), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("New Folder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    if (folderName.isNotBlank() && selectedPlaylists.isNotEmpty()) {
                        isGrouping = true
                        playlistViewModel.groupPlaylistsIntoFolder(folderName, selectedPlaylists) {
                            isGrouping = false
                            onBack()
                        }
                    }
                },
                enabled = folderName.isNotBlank() && selectedPlaylists.isNotEmpty() && !isGrouping
            ) {
                if (isGrouping) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Group")
                }
            }
        }

        Column {
            OutlinedTextField(
                value = folderName, 
                onValueChange = { folderName = it }, 
                label = { Text("Folder Name") }, 
                modifier = Modifier.fillMaxWidth(), 
                shape = MaterialTheme.sepatifyShapes.small,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(dimens.spaceLarge))
            Text("Select Playlists to Group", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(dimens.spaceEight))
        }

        if (availablePlaylists.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No user playlists available to group.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll)) {
                items(availablePlaylists) { pl ->
                    val isSelected = selectedPlaylists.contains(pl.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (isSelected) selectedPlaylists.remove(pl.id) else selectedPlaylists.add(pl.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = null)
                        Spacer(modifier = Modifier.width(dimens.spaceTwelve))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            if (pl.description.isNotBlank()) {
                                Text(pl.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-component wrapper for details to keep code clean
@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    hasPermission: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRemoveSong: (String) -> Unit,
    onSongSelect: (Song, List<Song>) -> Unit,
    requestPermission: () -> Unit,
    playlistViewModel: PlaylistViewModel,
    locString: (Int) -> String
) {
    val dimens = MaterialTheme.sepatifyDimens
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Spacer(modifier = Modifier.width(dimens.spaceEight))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = playlist.title, style = MaterialTheme.typography.titleLarge)
                Text(text = playlist.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary) }
            if (playlist.isUserCreated) {
                IconButton(onClick = { showDeleteConfirm = true }) { 
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) 
                }
             }
         }
 
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Playlist", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete '${playlist.title}'? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDelete()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(locString(R.string.cd_delete), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(locString(R.string.cancel))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(dimens.spaceNormal))

        if (playlist.category == "Local" && !hasPermission) {
            Column(modifier = Modifier.fillMaxWidth().padding(dimens.spaceHuge), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceNormal)) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(dimens.spaceTera), tint = MaterialTheme.colorScheme.primary)
                Text(locString(R.string.permission_storage_required), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Button(onClick = requestPermission) { Text(locString(R.string.permission_grant_btn)) }
            }
        } else {
            val pagedSongs = remember(playlist.id, playlist.category) { playlistViewModel.getSongsForPlaylistPaged(playlist.id, playlist.category) }.collectAsLazyPagingItems()
            when (pagedSongs.loadState.refresh) {
                is androidx.paging.LoadState.Loading -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll)) {
                        items(6) { SongRowSkeleton() }
                    }
                }
                else -> {
                    if (pagedSongs.itemCount == 0) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = dimens.spaceHuge), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(dimens.spaceTera), tint = MaterialTheme.colorScheme.primary)
                                Text(locString(R.string.no_results), style = MaterialTheme.typography.titleMedium)
                                Text(locString(R.string.playlist_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dimens.spaceBottomOverScroll)) {
                            items(pagedSongs.itemCount) { index ->
                                val s = pagedSongs[index] ?: return@items
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { onSongSelect(s, pagedSongs.itemSnapshotList.items.filterNotNull()) }.padding(vertical = dimens.spaceSix),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(model = s.coverImageUrl, contentDescription = s.title, modifier = Modifier.size(dimens.sizeSongThumbnailMedium).clip(MaterialTheme.shapes.extraSmall))
                                    Spacer(modifier = Modifier.width(dimens.spaceTwelve))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                                        Text(s.artistName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                    }
                                    if (playlist.isUserCreated) {
                                        IconButton(onClick = { onRemoveSong(s.id); pagedSongs.refresh() }) {
                                            Icon(Icons.Default.RemoveCircle, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Utility to generate cycle-colors for cards to make the grid beautiful
fun getCardColors(index: Int, isDark: Boolean, colors: com.aistudio.sepatify.ui.theme.SepatifyColors): Triple<Color, Color, Color> {
    return when (index % 4) {
        0 -> if (isDark) Triple(colors.playlistAccentBlue, colors.playlistAccentBlueLight, colors.playlistAccentBlueLight.copy(alpha = 0.7f))
        else Triple(colors.playlistAccentBlueLight, colors.playlistTextBlueDark, colors.playlistAccentBlueLight)
        1 -> if (isDark) Triple(colors.playlistAccentPurple, colors.playlistAccentPurpleLight, colors.playlistAccentPurpleLight)
        else Triple(colors.playlistAccentPurpleLight, colors.playlistTextPurpleDark, colors.playlistAccentPurpleLight)
        2 -> if (isDark) Triple(colors.playlistAccentRose, colors.playlistAccentRoseLight, colors.playlistAccentRoseLight)
        else Triple(colors.playlistAccentRoseLight, colors.playlistTextRoseDark, colors.playlistAccentRoseLight)
        else -> if (isDark) Triple(colors.playlistAccentMint, colors.playlistAccentMintLight, colors.playlistAccentMintLight)
        else Triple(colors.playlistAccentMintLight, colors.playlistTextMintDark, colors.playlistAccentMintLight)
    }
}