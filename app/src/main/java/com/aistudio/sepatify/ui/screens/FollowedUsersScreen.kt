package com.aistudio.sepatify.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
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
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.remote.dto.ProfileDto
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    chatViewModel: ChatViewModel,
    activeProfileUsername: String?,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit,
    locString: (Int) -> String
) {
    LaunchedEffect(activeProfileUsername) {
        chatViewModel.viewProfile(activeProfileUsername)
    }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val followers by chatViewModel.networkFollowers.collectAsState()
    val following by chatViewModel.networkFollowing.collectAsState()
    val onlineUsers by chatViewModel.onlineUsers.collectAsState()
    val isRefreshing by chatViewModel.isNetworkRefreshing.collectAsState()

    val dimens = MaterialTheme.sepatifyDimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spaceNormal),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(dimens.spaceEight))
            Text(
                text = if (activeProfileUsername == null) "My Connections" else "$activeProfileUsername's Connections",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }) {
                Text("Followers (${followers.size})", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
            }
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }) {
                Text("Following (${following.size})", modifier = Modifier.padding(16.dp), fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal)
            }
        }

        val displayList = if (selectedTabIndex == 0) followers else following

        // Refresh network data when switching tabs or viewing a new profile
        LaunchedEffect(selectedTabIndex, activeProfileUsername) {
            chatViewModel.refreshNetwork()
        }

        Box(modifier = Modifier.weight(1f)) {
            if (displayList.isEmpty() && !isRefreshing) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    title = "No Connections Found",
                    subtitle = "This list is currently empty.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = dimens.spaceNormal, end = dimens.spaceNormal, top = dimens.spaceNormal, bottom = dimens.spaceBottomOverScroll),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceTen),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayList) { profile ->
                        val isFollowingFlow = chatViewModel.isFollowing(profile.username).collectAsState(initial = false)
                        val isOnline = onlineUsers.contains(profile.username)
                        UserProfileRow(
                            profile = profile,
                            isFollowing = isFollowingFlow.value,
                            isOnline = isOnline,
                            onUserClick = { onUserClick(profile.username) },
                            onFollowToggle = { chatViewModel.toggleFollow(profile.username) },
                            locString = locString
                        )
                    }
                }
            }

            // Elegant, universally compatible loading feedback
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
fun PublicProfileScreen(
    chatViewModel: ChatViewModel,
    activeProfileUsername: String?,
    onBackClick: () -> Unit,
    onFollowersClick: (String?) -> Unit,
    onFollowingClick: (String?) -> Unit,
    onChatClick: (String) -> Unit,
    locString: (Int) -> String
) {
    LaunchedEffect(activeProfileUsername) {
        chatViewModel.viewProfile(activeProfileUsername)
    }

    val profile by chatViewModel.viewedProfile.collectAsState()
    val stats by chatViewModel.viewedProfileStats.collectAsState()
    val onlineUsers by chatViewModel.onlineUsers.collectAsState()

    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(modifier = Modifier.fillMaxWidth().padding(dimens.spaceNormal), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
        }

        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return
        }

        val p = profile!!
        val isOnline = onlineUsers.contains(p.username)
        val isFollowingFlow = chatViewModel.isFollowing(p.username).collectAsState(initial = false)

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Avatar
            Box(
                modifier = Modifier
                    .size(dimens.sizeAvatarFrame)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!p.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = p.avatarUrl,
                        contentDescription = p.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(dimens.sizeLogoIcon))
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceNormal))

            Text(text = p.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = "@${p.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(dimens.spaceFour))

            Text(
                text = if (isOnline) locString(R.string.status_online) else locString(R.string.status_offline),
                style = MaterialTheme.typography.labelMedium,
                color = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray
            )

            Spacer(modifier = Modifier.height(dimens.spaceLarge))

            // Action Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimens.spaceNormal)) {
                Button(
                    onClick = { chatViewModel.toggleFollow(p.username) },
                    modifier = Modifier.weight(1f),
                    shape = shapes.button,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowingFlow.value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        contentColor = if (isFollowingFlow.value) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(if (isFollowingFlow.value) locString(R.string.following) else locString(R.string.follow))
                }

                IconButton(
                    onClick = { onChatClick(p.username) },
                    modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, shapes.button)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Message", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceLarge))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    modifier = Modifier.clickable { onFollowersClick(activeProfileUsername) }.padding(dimens.spaceEight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stats.first.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Followers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }

                Column(
                    modifier = Modifier.clickable { onFollowingClick(activeProfileUsername) }.padding(dimens.spaceEight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stats.second.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = "Following", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun UserProfileRow(
    profile: ProfileDto,
    isFollowing: Boolean,
    isOnline: Boolean,
    onUserClick: () -> Unit,
    onFollowToggle: () -> Unit,
    locString: (Int) -> String
) {
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onUserClick() }
                .padding(dimens.spaceNormal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)) {
                Box(
                    modifier = Modifier
                        .size(dimens.sizeAvatarLarge)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarUrl.isNullOrEmpty()) {
                        AsyncImage(model = profile.avatarUrl, contentDescription = profile.displayName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(text = profile.displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Column {
                    Text(text = profile.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(text = "@${profile.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (isOnline) {
                        Text(text = locString(R.string.status_online), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Button(
                onClick = onFollowToggle,
                shape = shapes.chip,
                contentPadding = PaddingValues(horizontal = dimens.spaceTwelve, vertical = dimens.zero),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isFollowing) locString(R.string.unfollow) else locString(R.string.follow),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFollowing) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}