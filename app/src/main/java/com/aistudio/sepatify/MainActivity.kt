package com.aistudio.sepatify

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.sepatify.data.local.PlaylistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.network.NetworkMonitor
import com.aistudio.sepatify.ui.components.EqualizerDialog
import com.aistudio.sepatify.ui.components.ShareBottomSheet
import com.aistudio.sepatify.ui.theme.SepatifyTheme
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.screens.*
import com.aistudio.sepatify.ui.viewmodel.*
import org.koin.androidx.compose.koinViewModel
import org.koin.android.ext.android.inject
import java.util.Locale
import kotlinx.coroutines.launch

private const val TAB_HOME = "home"
private const val TAB_SEARCH = "search"
private const val TAB_PLAYLISTS = "playlists"
private const val TAB_DOWNLOADS = "downloads"
private const val TAB_CHAT = "chat"
private const val TAB_PROFILE = "profile"
private const val TAB_LIKED = "liked"
private const val TAB_RECENT = "recent"
private const val TAB_FOLLOWED = "followed"

private data class BottomNavItem(
    val tabKey: String,
    val titleResId: Int,
    val icon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val networkMonitor: NetworkMonitor by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            // Main ViewModels from Koin
            val mainViewModel: MainViewModel = koinViewModel()
            val authViewModel: AuthViewModel = koinViewModel()
            val homeViewModel: HomeViewModel = koinViewModel()
            val searchViewModel: SearchViewModel = koinViewModel()
            val downloadViewModel: DownloadViewModel = koinViewModel()
            val playlistViewModel: PlaylistViewModel = koinViewModel()
            val chatViewModel: ChatViewModel = koinViewModel()
            val sharedAudioViewModel: SharedAudioViewModel = koinViewModel()

            // Observe settings
            val currentLanguage by mainViewModel.currentLanguage.collectAsState()
            val currentTheme by mainViewModel.currentTheme.collectAsState()
            val currentFontSizeScale by mainViewModel.fontSizeScale.collectAsState()
            val isPremium by mainViewModel.isPremium.collectAsState()
            val userEmail by mainViewModel.userEmail.collectAsState()
            val isConnected by networkMonitor.isConnected.collectAsState(initial = true)

            // Configuration for live locale/RTL translations
            val context = LocalContext.current
            val locale = Locale(currentLanguage)
            Locale.setDefault(locale)
            val config = Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)
            val layoutDirection = if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

            // Find original ActivityResultRegistryOwner from original context
            var activityOwner: androidx.activity.result.ActivityResultRegistryOwner =
                context as androidx.activity.result.ActivityResultRegistryOwner
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is androidx.activity.result.ActivityResultRegistryOwner) {
                    activityOwner = currentContext
                    break
                }
                currentContext = currentContext.baseContext
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides activityOwner
            ) {
                SepatifyTheme(themeMode = currentTheme, fontSizeScale = currentFontSizeScale) {
                    val locString: (Int) -> String = { resId ->
                        localizedContext.resources.getString(resId)
                    }

                    val isCheckingSession by authViewModel.isCheckingSession.collectAsState()
                    val dimens = MaterialTheme.sepatifyDimens

                    // Wait for session verification
                    if (isCheckingSession) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.sizeEmptyStateCircle)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Loading...",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(dimens.sizeEmptyStateIcon)
                                )
                            }
                        }
                    } else if (userEmail.isNullOrBlank()) {
                        LoginScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { _, _ -> },
                            locString = locString
                        )
                    } else {
                        AppMainHub(
                            mainViewModel = mainViewModel,
                            homeViewModel = homeViewModel,
                            searchViewModel = searchViewModel,
                            downloadViewModel = downloadViewModel,
                            playlistViewModel = playlistViewModel,
                            chatViewModel = chatViewModel,
                            sharedAudioViewModel = sharedAudioViewModel,
                            isPremium = isPremium,
                            isConnected = isConnected,
                            locString = locString
                        )
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun AppMainHub(
    mainViewModel: MainViewModel,
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    downloadViewModel: DownloadViewModel,
    playlistViewModel: PlaylistViewModel,
    chatViewModel: ChatViewModel,
    sharedAudioViewModel: SharedAudioViewModel,
    isPremium: Boolean,
    isConnected: Boolean,
    locString: (Int) -> String
) {
    var activeTab by remember { mutableStateOf(TAB_HOME) }
    var showNowPlayingOverlay by remember { mutableStateOf(false) }
    var showMiniPlayer by remember { mutableStateOf(false) }
    var itemToShare by remember { mutableStateOf<Any?>(null) }
    var viewedUser by remember { mutableStateOf<String?>(null) }
    var viewedUserPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    val currentSong by sharedAudioViewModel.currentSong.collectAsState()
    val isPlaying by sharedAudioViewModel.isPlaying.collectAsState()
    val progress by sharedAudioViewModel.progress.collectAsState()
    val duration by sharedAudioViewModel.duration.collectAsState()

    val displayName by mainViewModel.userDisplayName.collectAsState()
    val avatarUrl by mainViewModel.userAvatar.collectAsState()

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) {
            showMiniPlayer = true
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes
    val colors = MaterialTheme.sepatifyColors

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                chatViewModel.trackPresence()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                chatViewModel.untrackPresence()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            chatViewModel.untrackPresence()
        }
    }

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var activeChatUser by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) return@remember c
            c = c.baseContext
        }
        null
    }

    val bottomNavItems = listOf(
        BottomNavItem(TAB_HOME, R.string.nav_home, Icons.Default.Home, "nav_home"),
        BottomNavItem(TAB_SEARCH, R.string.nav_search, Icons.Default.Search, "nav_search"),
        BottomNavItem(TAB_PLAYLISTS, R.string.nav_playlists, Icons.Default.LibraryMusic, "nav_playlists"),
        BottomNavItem(TAB_DOWNLOADS, R.string.nav_downloads, Icons.Default.Download, "nav_downloads"),
        BottomNavItem(TAB_CHAT, R.string.nav_chat, Icons.Default.Chat, "nav_chat"),
        BottomNavItem(TAB_PROFILE, R.string.nav_profile, Icons.Default.Person, "nav_profile")
    )

    val snackbarHostState = remember { SnackbarHostState() }
    var wasDisconnected by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            wasDisconnected = true
            snackbarHostState.showSnackbar(
                message = locString(R.string.network_lost),
                duration = SnackbarDuration.Indefinite
            )
        } else if (wasDisconnected) {
            wasDisconnected = false
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = locString(R.string.network_restored),
                duration = SnackbarDuration.Short
            )
        }
    }

    androidx.activity.compose.BackHandler(
        enabled = showNowPlayingOverlay || viewedUserPlaylist != null || viewedUser != null || activeTab != TAB_HOME || isPlaying || (activeTab == TAB_CHAT && activeChatUser != null)
    ) {
        if (showNowPlayingOverlay) {
            showNowPlayingOverlay = false
        } else if (viewedUserPlaylist != null) {
            viewedUserPlaylist = null
        } else if (viewedUser != null) {
            viewedUser = null
        } else if (activeTab == TAB_CHAT && activeChatUser != null) {
            activeChatUser = null
        } else if (activeTab != TAB_HOME) {
            activeTab = TAB_HOME
        } else if (isPlaying) {
            activity?.moveTaskToBack(true)
        }
    }

    if (showPrivacyDialog) {
        PrivacyAndSocialDialog(onDismiss = { showPrivacyDialog = false }, locString = locString)
    }

    if (showNotificationDialog) {
        NotificationDialog(onDismiss = { showNotificationDialog = false }, locString = locString)
    }

    if (showEqualizerDialog) {
        EqualizerDialog(viewModel = sharedAudioViewModel, onDismiss = { showEqualizerDialog = false })
    }

    itemToShare?.let { shareTarget ->
        ShareBottomSheet(
            song = shareTarget as? Song,
            playlist = shareTarget as? PlaylistEntity,
            chatViewModel = chatViewModel,
            onDismiss = { itemToShare = null },
            locString = locString
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(dimens.widthDrawer),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(modifier = Modifier.height(dimens.spaceNormal))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceTwelve),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(dimens.sizeIconExtraLarge)
                    )
                    Text(
                        text = locString(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = dimens.spaceEight),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimens.alphaShimmerHighlight)
                )

                Text(
                    text = locString(R.string.drawer_account_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceEight)
                )

                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dimens.sizeAvatarNormal)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = locString(R.string.user_avatar_content_description),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    val initial = if (displayName.isNotEmpty()) displayName.take(1).uppercase() else "G"
                                    Text(
                                        text = initial,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = displayName.ifEmpty { locString(R.string.drawer_guest_user) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = locString(R.string.drawer_view_profile),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    selected = activeTab == TAB_PROFILE,
                    onClick = {
                        scope.launch { drawerState.close() }
                        activeTab = TAB_PROFILE
                    },
                    modifier = Modifier.padding(horizontal = dimens.spaceTwelve),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = dimens.alphaMuted)
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = dimens.spaceTwelve),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = dimens.alphaShimmerHighlight)
                )

                Text(
                    text = locString(R.string.drawer_settings_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceEight)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_account_details)) },
                    selected = activeTab == TAB_PROFILE,
                    onClick = {
                        scope.launch { drawerState.close() }
                        activeTab = TAB_PROFILE
                    },
                    modifier = Modifier.padding(horizontal = dimens.spaceTwelve)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_privacy_social)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showPrivacyDialog = true
                    },
                    modifier = Modifier.padding(horizontal = dimens.spaceTwelve)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_notifications)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNotificationDialog = true
                    },
                    modifier = Modifier.padding(horizontal = dimens.spaceTwelve)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_equalizer_audio)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showEqualizerDialog = true
                    },
                    modifier = Modifier.padding(horizontal = dimens.spaceTwelve)
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    if (!showNowPlayingOverlay) {
                        CommonTopBar(
                            title = locString(R.string.app_name),
                            avatarUrl = avatarUrl,
                            displayName = displayName,
                            isPremium = isPremium,
                            onSettingsClick = { scope.launch { drawerState.open() } },
                            onAvatarClick = { scope.launch { drawerState.open() } },
                            onNotificationsClick = { showNotificationDialog = true }
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .padding(bottom = dimens.heightBottomNavBar + if (currentSong != null) dimens.marginKeyboardMiniplayer else dimens.zero)
                            .fillMaxWidth()
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    ) { data ->
                        Snackbar(
                            modifier = Modifier.padding(12.dp).widthIn(max = 240.dp),
                            shape = CircleShape, 
                            containerColor = MaterialTheme.sepatifyColors.playlistAccentMintLight,
                            contentColor = MaterialTheme.sepatifyColors.playlistTextMintDark
                        ) {
                            Text(
                                text = data.visuals.message,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                bottomBar = {}
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = dimens.zero,
                            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                        )
                ) {
                    AnimatedContent(
                        targetState = activeTab,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val initialIndex = bottomNavItems.indexOfFirst { it.tabKey == initialState }.takeIf { it >= 0 } ?: 0
                            val targetIndex = bottomNavItems.indexOfFirst { it.tabKey == targetState }.takeIf { it >= 0 } ?: 0
                            val direction = if (targetIndex >= initialIndex) 1 else -1

                            (slideInHorizontally(
                                initialOffsetX = { direction * it / 4 },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(animationSpec = tween(180)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        targetOffsetX = { -direction * it / 4 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    ) + fadeOut(animationSpec = tween(120))
                                )
                        },
                        label = "tabContentAnimation"
                    ) { tab ->
                        when (tab) {
                            TAB_HOME -> HomeScreen(
                                homeViewModel = homeViewModel,
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                onQuickActionClick = { action ->
                                    when (action) {
                                        "liked" -> activeTab = TAB_LIKED
                                        "recent" -> activeTab = TAB_RECENT
                                        "playlists" -> activeTab = TAB_PLAYLISTS
                                        "artists" -> activeTab = TAB_FOLLOWED
                                    }
                                },
                                locString = locString
                            )

                            TAB_SEARCH -> SearchScreen(
                                searchViewModel = searchViewModel,
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                locString = locString
                            )

                            TAB_PLAYLISTS -> PlaylistsScreen(
                                playlistViewModel = playlistViewModel,
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                onShareClick = { itemToShare = it },
                                locString = locString
                            )

                            TAB_LIKED -> LikedSongsScreen(
                                playlistViewModel = playlistViewModel,
                                sharedAudioViewModel = sharedAudioViewModel,
                                onBackClick = { activeTab = TAB_HOME },
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                locString = locString
                            )

                            TAB_RECENT -> RecentlyPlayedScreen(
                                playlistViewModel = playlistViewModel,
                                sharedAudioViewModel = sharedAudioViewModel,
                                onBackClick = { activeTab = TAB_HOME },
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                locString = locString
                            )

                            TAB_FOLLOWED -> FollowedUsersScreen(
                                chatViewModel = chatViewModel,
                                locString = locString,
                                onUserClick = { user -> viewedUser = user }
                            )

                            TAB_DOWNLOADS -> DownloadsScreen(
                                downloadViewModel = downloadViewModel,
                                isPremium = isPremium,
                                onSongSelect = { song, queue ->
                                    sharedAudioViewModel.playSong(song, queue)
                                },
                                locString = locString
                            )

                            TAB_CHAT -> ChatsScreen(
                                chatViewModel = chatViewModel,
                                activeChatUser = activeChatUser,
                                onActiveChatUserChange = { activeChatUser = it },
                                onViewUserProfile = { viewedUser = it },
                                onPlaySharedSong = { song ->
                                    sharedAudioViewModel.playSong(song)
                                },
                                locString = locString,
                                isMiniPlayerVisible = currentSong != null
                            )

                            TAB_PROFILE -> ProfileScreen(
                                mainViewModel = mainViewModel,
                                locString = locString
                            )
                        }
                    }

                    if (!showNowPlayingOverlay) {
                        val isKeyboardVisible = WindowInsets.isImeVisible
                        val showNavigationBar = !isKeyboardVisible && !(activeTab == TAB_CHAT && activeChatUser != null)
                        
                        if (showNavigationBar || (currentSong != null && showMiniPlayer)) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background.copy(alpha = dimens.alphaSemiMuted),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .navigationBarsPadding()
                            ) {
                                Spacer(modifier = Modifier.height(dimens.spaceHuge))
                                
                                if (currentSong != null) {
                                    MiniPlayer(
                                        currentSong = currentSong!!,
                                        isPlaying = isPlaying,
                                        progress = progress,
                                        duration = duration,
                                        onPlayPauseClick = { sharedAudioViewModel.togglePlayPause() },
                                        onPlayerBarClick = { showNowPlayingOverlay = true },
                                        coverModifier = Modifier
                                    )
                                }
                                
                                if (showNavigationBar) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(dimens.heightBottomNavBar)
                                            .padding(horizontal = dimens.spaceEight),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        bottomNavItems.forEach { item ->
                                            val selected = (activeTab == item.tabKey)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null,
                                                        onClick = { activeTab = item.tabKey }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(dimens.widthBottomNavTab)
                                                        .height(dimens.heightBottomNavTabContainer)
                                                        .clip(shapes.button)
                                                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight) else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = locString(item.titleResId),
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = dimens.alphaStandard)
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
            }
        }

        AnimatedVisibility(
            visible = viewedUser != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            if (viewedUser != null) {
                UserProfileScreen(
                    username = viewedUser!!,
                    chatViewModel = chatViewModel,
                    onBack = { viewedUser = null },
                    onChatClick = {
                        activeChatUser = viewedUser
                        activeTab = TAB_CHAT
                        viewedUser = null
                    },
                    onPlaylistClick = { viewedUserPlaylist = it },
                    onPlaySong = { song -> sharedAudioViewModel.playSong(song) }
                )
            }
        }

        AnimatedVisibility(
            visible = viewedUserPlaylist != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            if (viewedUserPlaylist != null) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    PlaylistDetailView(
                        playlist = viewedUserPlaylist!!,
                        hasPermission = true,
                        onBack = { viewedUserPlaylist = null },
                        onShare = { itemToShare = viewedUserPlaylist },
                        onDelete = { }, // Public playlist, deletion disabled by UI mapping
                        onRemoveSong = { }, // Public playlist, removal disabled by UI mapping
                        onSongSelect = { song, queue -> sharedAudioViewModel.playSong(song, queue) },
                        requestPermission = { },
                        playlistViewModel = playlistViewModel,
                        locString = locString
                    )
                }
            }
        }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showNowPlayingOverlay,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 350)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                NowPlayingScreen(
                    sharedAudioViewModel = sharedAudioViewModel,
                    downloadViewModel = downloadViewModel,
                    isPremium = isPremium,
                    onBackClick = { showNowPlayingOverlay = false },
                    onShareClick = { itemToShare = currentSong },
                    locString = locString,
                    coverModifier = Modifier
                )
            }
        }
    }
}

@Composable
fun PrivacyAndSocialDialog(onDismiss: () -> Unit, locString: (Int) -> String) {
    var shareHistory by remember { mutableStateOf(true) }
    var privateSession by remember { mutableStateOf(false) }
    var profileVisibility by remember { mutableStateOf(true) }
    val dimens = MaterialTheme.sepatifyDimens

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                locString(R.string.privacy_social_settings_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceEight),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceNormal)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.privacy_share_activity),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.privacy_share_activity_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = shareHistory, onCheckedChange = { shareHistory = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.privacy_private_session),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.privacy_private_session_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = privateSession, onCheckedChange = { privateSession = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.privacy_profile_search),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.privacy_profile_search_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = profileVisibility, onCheckedChange = { profileVisibility = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(locString(R.string.save_changes))
            }
        }
    )
}

@Composable
fun NotificationDialog(onDismiss: () -> Unit, locString: (Int) -> String) {
    var newMusic by remember { mutableStateOf(true) }
    var socialAlerts by remember { mutableStateOf(true) }
    var systemUpdates by remember { mutableStateOf(false) }
    val dimens = MaterialTheme.sepatifyDimens

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(locString(R.string.notification_settings_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceEight),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceNormal)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.notification_new_music),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.notification_new_music_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = newMusic, onCheckedChange = { newMusic = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.notification_social),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.notification_social_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = socialAlerts, onCheckedChange = { socialAlerts = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceEight)) {
                        Text(
                            locString(R.string.notification_promo),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            locString(R.string.notification_promo_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = systemUpdates, onCheckedChange = { systemUpdates = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(locString(R.string.save_changes))
            }
        }
    )
}