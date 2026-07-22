package com.aistudio.sepatify.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.theme.sepatifyColors
import com.aistudio.sepatify.ui.theme.sepatifyDimens
import com.aistudio.sepatify.ui.theme.sepatifyShapes
import com.aistudio.sepatify.ui.viewmodel.ChatEvent
import com.aistudio.sepatify.ui.viewmodel.ChatViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ChatsScreen(
    chatViewModel: ChatViewModel,
    activeChatUser: String?,
    onActiveChatUserChange: (String?) -> Unit,
    onViewUserProfile: (String) -> Unit,
    onPlaySharedSong: (Song) -> Unit,
    locString: (Int) -> String,
    isMiniPlayerVisible: Boolean
) {
    val searchUsersQuery by chatViewModel.searchQuery.collectAsState()
    val matchingUsers by chatViewModel.filteredUsers.collectAsState()
    val recentConversations by chatViewModel.recentConversations.collectAsState()
    val onlineUsers by chatViewModel.onlineUsers.collectAsState()
    var chatsFirstLoad by remember { mutableStateOf(true) }
    
    LaunchedEffect(recentConversations) {
        if (chatsFirstLoad) chatsFirstLoad = false
    }

    var chatInputText by remember { mutableStateOf("") }
    val dimens = MaterialTheme.sepatifyDimens
    val shapes = MaterialTheme.sepatifyShapes

    val isKeyboardVisible = WindowInsets.isImeVisible
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val bottomPadding = if (isKeyboardVisible) {
        dimens.spaceNormal
    } else {
        var padding = dimens.spaceNormal + navBarPadding
        if (isMiniPlayerVisible) padding += dimens.marginKeyboardMiniplayer
        if (activeChatUser == null) padding += dimens.heightBottomNavBar
        padding
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = dimens.spaceNormal, end = dimens.spaceNormal, top = dimens.spaceNormal)
            .imePadding()
            .padding(bottom = bottomPadding)
    ) {
        if (activeChatUser == null) {
            // --- CONVERSATION LIST & SEARCH ---
            Text(
                text = locString(R.string.chat_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(dimens.spaceTwelve))

            OutlinedTextField(
                value = searchUsersQuery,
                onValueChange = { chatViewModel.onEvent(ChatEvent.UpdateSearchQuery(it)) },
                placeholder = { Text(locString(R.string.search_users_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = locString(R.string.chat_lookup_desc)) },
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.card
            )

            Spacer(modifier = Modifier.height(dimens.spaceNormal))

            if (searchUsersQuery.isNotEmpty()) {
                Text(
                    text = locString(R.string.matching_users_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceEight),
                    modifier = Modifier.fillMaxWidth().weight(dimens.aspectRatioSquare)
                ) {
                    items(matchingUsers) { usr ->
                        val isFollowingFlow = chatViewModel.isFollowing(usr).collectAsState(initial = false)
                        val profile by chatViewModel.getProfile(usr).collectAsState(initial = null)
                        val displayName = profile?.displayName ?: usr

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewUserProfile(usr) }
                                .padding(vertical = dimens.spaceEight),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceTen)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(dimens.sizeAvatarNormal)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = profile!!.avatarUrl,
                                            contentDescription = displayName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(text = displayName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Text(text = displayName, style = MaterialTheme.typography.bodyLarge)
                            }

                            Button(
                                onClick = { chatViewModel.onEvent(ChatEvent.ToggleFollow(usr)) },
                                shape = shapes.button,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingFlow.value) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = if (isFollowingFlow.value) locString(R.string.following) else locString(R.string.follow),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = locString(R.string.conversations_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = dimens.alphaSemiMuted)
                )

                if (chatsFirstLoad) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(dimens.aspectRatioSquare),
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceFour)
                    ) {
                        items(6) { ChatRowSkeleton() }
                    }
                } else if (recentConversations.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = locString(R.string.empty_conversations_title),
                        subtitle = "Search users above to start a live conversation.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(dimens.spaceTen),
                        modifier = Modifier.fillMaxWidth().weight(dimens.aspectRatioSquare)
                    ) {
                        items(recentConversations) { user ->
                            val profile by chatViewModel.getProfile(user).collectAsState(initial = null)
                            val displayName = profile?.displayName ?: user

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onActiveChatUserChange(user) }
                                    .padding(vertical = dimens.spaceEight),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwelve)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(dimens.sizeAvatarLarge)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight))
                                        .clickable { onViewUserProfile(user) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile?.avatarUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = profile!!.avatarUrl,
                                            contentDescription = displayName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(text = displayName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Column(modifier = Modifier.weight(dimens.aspectRatioSquare)) {
                                    Text(text = displayName, style = MaterialTheme.typography.bodyLarge)

                                    val isOnline = onlineUsers.contains(user)
                                    Text(
                                        text = if (isOnline) locString(R.string.status_online) else locString(R.string.status_offline),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = dimens.alphaMuted)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Chat, 
                                    contentDescription = locString(R.string.chat_icon_desc), 
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaOverlayAmbient)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // --- ACTIVE CHAT VIEW ---
            val user = activeChatUser
            val pagedMessages = chatViewModel.getMessagesPaged(user).collectAsLazyPagingItems()
            val otherIsTyping = remember(user) { chatViewModel.getTypingState(user) }.collectAsState(initial = false)

            val profile by chatViewModel.getProfile(user).collectAsState(initial = null)
            val displayName = profile?.displayName ?: user

            var isTypingSent by remember(user) { mutableStateOf(false) }
            val listState = rememberLazyListState()

            LaunchedEffect(pagedMessages.itemCount, isKeyboardVisible) {
                if (pagedMessages.itemCount > 0) {
                    listState.animateScrollToItem(pagedMessages.itemCount - 1)
                }
            }

            LaunchedEffect(chatInputText, user) {
                if (chatInputText.isNotEmpty()) {
                    if (!isTypingSent) {
                        chatViewModel.onEvent(ChatEvent.SetTyping(user, true))
                        isTypingSent = true
                    }
                    kotlinx.coroutines.delay(2500)
                    chatViewModel.onEvent(ChatEvent.SetTyping(user, false))
                    isTypingSent = false
                } else {
                    if (isTypingSent) {
                        chatViewModel.onEvent(ChatEvent.SetTyping(user, false))
                        isTypingSent = false
                    }
                }
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, user) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                        chatViewModel.onEvent(ChatEvent.SetTyping(user, false))
                        isTypingSent = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                    chatViewModel.onEvent(ChatEvent.SetTyping(user, false))
                }
            }

            // Top Header active chat
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onActiveChatUserChange(null) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = locString(R.string.back_desc))
                }
                Spacer(modifier = Modifier.width(dimens.spaceEight))

                // Wrapping Avatar and Title in a clickable Row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onViewUserProfile(user) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(dimens.sizeAvatarNormal)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaShimmerHighlight)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profile?.avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = profile!!.avatarUrl,
                                contentDescription = displayName,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(dimens.spaceTwelve))

                    Column {
                        Text(text = displayName, style = MaterialTheme.typography.titleMedium)
                        if (otherIsTyping.value) {
                            Text(text = locString(R.string.typing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else {
                            val isOnline = onlineUsers.contains(user)
                            Text(
                                text = if (isOnline) locString(R.string.status_online) else locString(R.string.status_offline),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceTwelve))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(dimens.aspectRatioSquare),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceTwelve),
                contentPadding = PaddingValues(vertical = dimens.spaceEight)
            ) {
                item {
                    if (pagedMessages.itemCount == 0) {
                        EmptyStateView(
                            icon = Icons.Default.ChatBubbleOutline,
                            title = locString(R.string.chat_say_hello_title),
                            subtitle = locString(R.string.chat_say_hello_desc),
                            modifier = Modifier.fillMaxWidth().height(dimens.heightEmptyChatVisual)
                        )
                    }
                }

                items(pagedMessages.itemCount, key = pagedMessages.itemKey { it.id }) { index ->
                    val msg = pagedMessages[index] ?: return@items
                    val isMe = msg.senderName == "Me"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = if (isMe) shapes.chatBubbleMe else shapes.chatBubbleOther,
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.widthIn(max = dimens.maxWidthChatBubble)
                        ) {
                            Column(modifier = Modifier.padding(dimens.spaceTwelve)) {
                                if (msg.isSongShare) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                        shape = MaterialTheme.shapes.extraSmall,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = dimens.spaceEight)
                                            .clickable {
                                                onPlaySharedSong(
                                                    Song(
                                                        id = msg.songId ?: "",
                                                        title = msg.songTitle ?: "",
                                                        artistName = msg.songArtist ?: "",
                                                        coverImageUrl = msg.songCover ?: "",
                                                        audioUrl = msg.songAudio ?: ""
                                                    )
                                                )
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(dimens.spaceEight),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(dimens.spaceEight)
                                        ) {
                                            AsyncImage(
                                                model = msg.songCover,
                                                contentDescription = msg.songTitle,
                                                modifier = Modifier
                                                    .size(dimens.sizeAvatarNormal)
                                                    .clip(MaterialTheme.shapes.extraSmall)
                                            )
                                            Column(modifier = Modifier.weight(dimens.aspectRatioSquare)) {
                                                Text(text = msg.songTitle ?: "", style = MaterialTheme.typography.labelMedium)
                                                Text(text = msg.songArtist ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = msg.songTitle,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = msg.text,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        if (isMe) {
                            Row(
                                modifier = Modifier.padding(top = dimens.spaceTwo),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(dimens.spaceTwo)
                            ) {
                                val statusText = when (msg.status) {
                                    "Sending" -> locString(R.string.message_sending)
                                    "Sent" -> locString(R.string.message_sent)
                                    "Delivered" -> locString(R.string.message_delivered)
                                    else -> locString(R.string.message_read)
                                }
                                val iconVector = when (msg.status) {
                                    "Sending" -> Icons.Default.Schedule
                                    "Sent" -> Icons.Default.Check
                                    else -> Icons.Default.DoneAll
                                }
                                
                                val targetColor = if (msg.status == "Read") {
                                    MaterialTheme.colorScheme.primary.copy(alpha = dimens.alphaStandard)
                                } else {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = dimens.alphaMuted)
                                }
                                
                                val animatedColor by animateColorAsState(
                                    targetValue = targetColor,
                                    animationSpec = tween(350),
                                    label = "statusColor"
                                )

                                AnimatedContent(
                                    targetState = iconVector,
                                    transitionSpec = {
                                        (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(250))) togetherWith
                                        (scaleOut(tween(200)) + fadeOut(tween(200)))
                                    },
                                    label = "statusIcon"
                                ) { vector ->
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = statusText,
                                        tint = animatedColor,
                                        modifier = Modifier.size(dimens.spaceTwelve)
                                    )
                                }
                                
                                AnimatedContent(
                                    targetState = statusText,
                                    transitionSpec = {
                                        fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                                    },
                                    label = "statusText"
                                ) { text ->
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = animatedColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(dimens.spaceTen))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceEight)
            ) {
                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text(locString(R.string.message_hint)) },
                    modifier = Modifier.weight(dimens.aspectRatioSquare),
                    shape = shapes.card
                )
                IconButton(
                    onClick = {
                        if (chatInputText.isNotBlank()) {
                            chatViewModel.onEvent(ChatEvent.SendMessage(user, chatInputText, null))
                            chatInputText = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(dimens.sizeAvatarLarge)
                ) {
                    Icon(Icons.Default.Send, contentDescription = locString(R.string.send_desc), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}