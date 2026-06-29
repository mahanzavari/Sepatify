package com.example

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.EqualizerDialog
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.Song
import com.example.ui.theme.SepatifyTheme
import com.example.ui.screens.*
import com.example.ui.viewmodel.*
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

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
            var activityOwner: androidx.activity.result.ActivityResultRegistryOwner = context as androidx.activity.result.ActivityResultRegistryOwner
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

                    if (userEmail == null) {
                        // User not logged in, show Auth Screen
                        LoginScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { email, name ->
                                // Navigation triggers login callback
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var activeTab by remember { mutableStateOf("home") } // "home", "search", "playlists", "downloads", "chat", "profile", "liked", "recent", "followed"
    var showNowPlayingOverlay by remember { mutableStateOf(false) }

    val currentSong by sharedAudioViewModel.currentSong.collectAsState()
    val isPlaying by sharedAudioViewModel.isPlaying.collectAsState()
    val progress by sharedAudioViewModel.progress.collectAsState()
    val duration by sharedAudioViewModel.duration.collectAsState()

    val displayName by mainViewModel.userDisplayName.collectAsState()
    val avatarUrl by mainViewModel.userAvatar.collectAsState()

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

    androidx.activity.compose.BackHandler(
        enabled = showNowPlayingOverlay || activeTab != "home" || isPlaying || (activeTab == "chat" && activeChatUser != null)
    ) {
        if (showNowPlayingOverlay) {
            showNowPlayingOverlay = false
        } else if (activeTab == "chat" && activeChatUser != null) {
            activeChatUser = null
        } else if (activeTab != "home") {
            activeTab = "home"
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
                        text = "Sepatify",
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
                                        contentDescription = "User Avatar",
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
                                    text = "View & Edit Profile",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    selected = activeTab == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        activeTab = "profile"
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
                    selected = activeTab == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        activeTab = "profile"
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
            bottomBar = {
                if (!showNowPlayingOverlay) {
                    val isKeyboardVisible = WindowInsets.isImeVisible
                    val showNavigationBar = !isKeyboardVisible && !(activeTab == "chat" && activeChatUser != null)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.75f)
                                    )
                                )
                            )
                            .navigationBarsPadding()
                            .imePadding()
                    ) {
                        if (currentSong != null || showNavigationBar) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.08f),
                                thickness = 0.5.dp
                            )
                        }

                        // Floating mini player inside App bottom Scaffold context (FR-66)
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
                            // Material 3 bottom Navigation Bar (NFR navigation rules)
                            val navBarItemColors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            NavigationBar(
                                modifier = Modifier,
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp
                            ) {
                            NavigationBarItem(
                                selected = (activeTab == "home"),
                                onClick = { activeTab = "home" },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_home),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_home")
                            )
                            NavigationBarItem(
                                selected = (activeTab == "search"),
                                onClick = { activeTab = "search" },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_search),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_search")
                            )
                            NavigationBarItem(
                                selected = (activeTab == "playlists"),
                                onClick = { activeTab = "playlists" },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Playlists") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_playlists),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_playlists")
                            )
                            NavigationBarItem(
                                selected = (activeTab == "downloads"),
                                onClick = { activeTab = "downloads" },
                                icon = { Icon(Icons.Default.Download, contentDescription = "Downloads") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_downloads),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_downloads")
                            )
                            NavigationBarItem(
                                selected = (activeTab == "chat"),
                                onClick = { activeTab = "chat" },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Social") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_chat),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_chat")
                            )
                            NavigationBarItem(
                                selected = (activeTab == "profile"),
                                onClick = { activeTab = "profile" },
                                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                colors = navBarItemColors,
                                label = {
                                    Text(
                                        text = locString(R.string.nav_profile),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.testTag("nav_profile")
                            )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding(), // Prevents bottom bar & player from blocking list/button views
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                )
        ) {
            when (activeTab) {
                "home" -> HomeScreen(
                    homeViewModel = homeViewModel,
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    onQuickActionClick = { action ->
                        when (action) {
                            "liked" -> activeTab = "liked"
                            "recent" -> activeTab = "recent"
                            "playlists" -> activeTab = "playlists"
                            "artists" -> activeTab = "followed"
                        }
                    },
                    locString = locString
                )
                "search" -> SearchScreen(
                    searchViewModel = searchViewModel,
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    locString = locString
                )
                "playlists" -> PlaylistsScreen(
                    playlistViewModel = playlistViewModel,
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    locString = locString
                )
                "liked" -> LikedSongsScreen(
                    playlistViewModel = playlistViewModel,
                    sharedAudioViewModel = sharedAudioViewModel,
                    onBackClick = { activeTab = "home" },
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    locString = locString
                )
                "recent" -> RecentlyPlayedScreen(
                    playlistViewModel = playlistViewModel,
                    sharedAudioViewModel = sharedAudioViewModel,
                    onBackClick = { activeTab = "home" },
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    locString = locString
                )
                "followed" -> FollowedUsersScreen(
                    chatViewModel = chatViewModel,
                    locString = locString,
                    onUserClick = { user ->
                        activeChatUser = user
                        activeTab = "chat"
                    }
                )
                "downloads" -> DownloadsScreen(
                    downloadViewModel = downloadViewModel,
                    isPremium = isPremium,
                    onSongSelect = { song, queue ->
                        sharedAudioViewModel.playSong(song, queue)
                    },
                    locString = locString
                )
                "chat" -> ChatsScreen(
                    chatViewModel = chatViewModel,
                    activeChatUser = activeChatUser,
                    onActiveChatUserChange = { activeChatUser = it },
                    onPlaySharedSong = { song ->
                        sharedAudioViewModel.playSong(song)
                    },
                    locString = locString
                )
                "profile" -> ProfileScreen(
                    mainViewModel = mainViewModel,
                    locString = locString
                )
            }
        }
    } // Closes Scaffold
    } // Closes Box
    } // Closes ModalNavigationDrawer

    // FULL SCREEN OVERLAY NOW PLAYING DETAIL LAYER
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
                coverModifier = Modifier
            )
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
        title = { Text(locString(R.string.privacy_social_settings_title), style = MaterialTheme.typography.titleLarge) },
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
                        Text(locString(R.string.privacy_share_activity), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.privacy_share_activity_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = shareHistory, onCheckedChange = { shareHistory = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(locString(R.string.privacy_private_session), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.privacy_private_session_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = privateSession, onCheckedChange = { privateSession = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(locString(R.string.privacy_profile_search), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.privacy_profile_search_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = { Text("Notification Settings", style = MaterialTheme.typography.titleLarge) },
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
                        Text(locString(R.string.notification_new_music), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.notification_new_music_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = newMusic, onCheckedChange = { newMusic = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(locString(R.string.notification_social), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.notification_social_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = socialAlerts, onCheckedChange = { socialAlerts = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(locString(R.string.notification_promo), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(locString(R.string.notification_promo_desc), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
