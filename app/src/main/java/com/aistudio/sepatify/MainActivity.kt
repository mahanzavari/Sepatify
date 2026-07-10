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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aistudio.sepatify.ui.components.EqualizerDialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import coil.compose.AsyncImage
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.SepatifyTheme
import com.aistudio.sepatify.ui.screens.*
import com.aistudio.sepatify.ui.viewmodel.*
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

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

            // Configuration for live locale/RTL translations
            val context = LocalContext.current
            val locale = Locale(currentLanguage)
            Locale.setDefault(locale)
            val config = Configuration(LocalConfiguration.current).apply {
                setLocale(locale)
            }
            val localizedContext = context.createConfigurationContext(config)
            val layoutDirection = if (currentLanguage == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

            // Find original ActivityResultRegistryOwner from original context because localizedContext does not implement it
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

                    // Wait for the session verification to complete before rendering logic
                    if (isCheckingSession) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Loading...",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    } else if (userEmail.isNullOrBlank()) {
                        // User not logged in, show Auth Screen backed by Supabase
                        LoginScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { email, name ->
                                // Auth success callback is monitored inside LoginScreen
                            },
                            locString = locString
                        )
                    } else {
                        // App Main Hub with navigation
                        AppMainHub(
                            mainViewModel = mainViewModel,
                            homeViewModel = homeViewModel,
                            searchViewModel = searchViewModel,
                            downloadViewModel = downloadViewModel,
                            playlistViewModel = playlistViewModel,
                            chatViewModel = chatViewModel,
                            sharedAudioViewModel = sharedAudioViewModel,
                            isPremium = isPremium,
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
    locString: (Int) -> String
) {
    var activeTab by remember { mutableStateOf(TAB_HOME) }
    var showNowPlayingOverlay by remember { mutableStateOf(false) }
    var showMiniPlayer by remember { mutableStateOf(false) }

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

    androidx.activity.compose.BackHandler(
        enabled = showNowPlayingOverlay || activeTab != TAB_HOME || isPlaying || (activeTab == TAB_CHAT && activeChatUser != null)
    ) {
        if (showNowPlayingOverlay) {
            showNowPlayingOverlay = false
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // App Logo or Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = locString(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // ACCOUNT SECTION WITH PROFILE PIC
                Text(
                    text = locString(R.string.drawer_account_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
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
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )

                // SETTINGS SECTION
                Text(
                    text = locString(R.string.drawer_settings_section),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_account_details)) },
                    selected = activeTab == TAB_PROFILE,
                    onClick = {
                        scope.launch { drawerState.close() }
                        activeTab = TAB_PROFILE
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_privacy_social)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showPrivacyDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_notifications)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showNotificationDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
                    label = { Text(locString(R.string.drawer_equalizer_audio)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        showEqualizerDialog = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
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
                bottomBar = {} // Leave empty so Scaffold doesn't push content up
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = paddingValues.calculateTopPadding(),
                            bottom = 0.dp, // Allows content to flow underneath the transparent nav bar
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
                                onUserClick = { user ->
                                    activeChatUser = user
                                    activeTab = TAB_CHAT
                                }
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
                                onPlaySharedSong = { song ->
                                    sharedAudioViewModel.playSong(song)
                                },
                                locString = locString
                            )

                            TAB_PROFILE -> ProfileScreen(
                                mainViewModel = mainViewModel,
                                locString = locString
                            )
                        }
                    }

                    // Mini-player floats purely over the content.
                    // Gradient overlay combining MiniPlayer and Custom NavBar
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
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                                    .navigationBarsPadding()
                                    .imePadding()
                            ) {
                                Spacer(modifier = Modifier.height(32.dp)) // Extra space for smooth fade
                                
                                if (currentSong != null && showMiniPlayer) {
                                    MiniPlayer(
                                        currentSong = currentSong!!,
                                        isPlaying = isPlaying,
                                        progress = progress,
                                        duration = duration,
                                        onPlayPauseClick = { sharedAudioViewModel.togglePlayPause() },
                                        onPlayerBarClick = { showNowPlayingOverlay = true },
                                        onDismiss = {
                                            sharedAudioViewModel.stopPlayback()
                                            showMiniPlayer = false
                                        },
                                        coverModifier = Modifier
                                    )
                                }
                                
                                if (showNavigationBar) {
                                    // Custom Navbar to prevent 6-item M3 clipping
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(72.dp)
                                            .padding(horizontal = 8.dp),
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
                                                        .width(48.dp) // Safely fits inside screen bounds
                                                        .height(32.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = item.icon,
                                                        contentDescription = locString(item.titleResId),
                                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
            } // Closes Scaffold
        } // Closes Box
    } // Closes ModalNavigationDrawer

    // Shared element transition: album cover scales from mini-player up to full-screen player
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        // Full-screen NowPlaying overlay (target state)
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
                    locString = locString,
                    coverModifier = Modifier.sharedElement(
                        state = rememberSharedContentState(key = "album_cover"),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        boundsTransform = { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        }
                    )
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(locString(R.string.notification_settings_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
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
