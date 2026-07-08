package com.aistudio.sepatify.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.aistudio.sepatify.R
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.ui.viewmodel.ChatViewModel

@Composable
fun ChatsScreen(
    chatViewModel: ChatViewModel,
    activeChatUser: String?,
    onActiveChatUserChange: (String?) -> Unit,
    onPlaySharedSong: (Song) -> Unit,
    locString: (Int) -> String
) {
    val searchUsersQuery by chatViewModel.searchQuery.collectAsState()
    val matchingUsers by chatViewModel.filteredUsers.collectAsState()
    val followedUsers by chatViewModel.followedUsers.collectAsState()
    var chatsFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(followedUsers) {
        if (chatsFirstLoad) chatsFirstLoad = false
    }

    var chatInputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (activeChatUser == null) {
            // General Chats / Contacts Hub
            Text(
                text = locString(R.string.chat_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // User queries search
            OutlinedTextField(
                value = searchUsersQuery,
                onValueChange = { chatViewModel.updateSearchQuery(it) },
                placeholder = { Text(locString(R.string.search_users_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Lookup") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (searchUsersQuery.isNotEmpty()) {
                // Search Lookup list Results
                Text(
                    text = locString(R.string.matching_users_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(matchingUsers) { usr ->
                        val isFollowingFlow = chatViewModel.isFollowing(usr).collectAsState(initial = false)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onActiveChatUserChange(usr) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = usr.take(1).uppercase(), color = MaterialTheme.colorScheme.primary)
                                }
                                Text(text = usr, style = MaterialTheme.typography.bodyLarge)
                            }

                            Button(
                                onClick = { chatViewModel.toggleFollow(usr) },
                                shape = RoundedCornerShape(16.dp),
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
                // Default Chat feed for followed users
                Text(
                    text = locString(R.string.conversations_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                if (chatsFirstLoad) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(6) { ChatRowSkeleton() }
                    }
                } else if (followedUsers.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = locString(R.string.empty_conversations_title),
                        subtitle = locString(R.string.empty_conversations_message),
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        items(followedUsers) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onActiveChatUserChange(user) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = user.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = user, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = locString(R.string.chat_thread_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.Chat, contentDescription = "Chat icon", tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        } else {
            // DIRECT DISCUSSION THREAD VIEW
            val user = activeChatUser!!
            val pagedMessages = chatViewModel.getMessagesPaged(user).collectAsLazyPagingItems()
            val otherIsTyping = remember(user) { chatViewModel.getTypingState(user) }.collectAsState(initial = false)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onActiveChatUserChange(null) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(text = user, style = MaterialTheme.typography.titleMedium)
                    if (otherIsTyping.value) {
                        Text(text = locString(R.string.typing), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(text = locString(R.string.chat_active_status), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message Bubble list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    if (pagedMessages.itemCount == 0) {
                        EmptyStateView(
                            icon = Icons.Default.ChatBubbleOutline,
                            title = "Say Hello!",
                            subtitle = "Start a real-time conversation or share your favorite songs directly with your contacts.",
                            modifier = Modifier.fillMaxWidth().height(220.dp)
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
                            shape = if (isMe) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp) else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.isSongShare) {
                                    // Custom Shared song card!
                                    // "FR-96: Users shall be able to share a song in chat; the song shall appear as a custom mini-card that triggers playback on tap"
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
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
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AsyncImage(
                                                model = msg.songCover,
                                                contentDescription = msg.songTitle,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = msg.songTitle ?: "", style = MaterialTheme.typography.labelMedium)
                                                Text(text = msg.songArtist ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            }
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Tape",
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

                        // Tick marks alignment
                        // "FR-94: Messages shall display status indicators: Sending (clock), Delivered (single check), Read (double check)"
                        if (isMe) {
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val statusText = when (msg.status) {
                                    "Sending" -> locString(R.string.message_sending)
                                    "Delivered" -> locString(R.string.message_delivered)
                                    else -> locString(R.string.message_read)
                                }
                                val iconVector = when (msg.status) {
                                    "Sending" -> Icons.Default.Schedule
                                    "Delivered" -> Icons.Default.Check
                                    else -> Icons.Default.DoneAll
                                }
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = statusText,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message entry footer row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatInputText,
                    onValueChange = { chatInputText = it },
                    placeholder = { Text(locString(R.string.message_hint)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (chatInputText.isNotBlank()) {
                            chatViewModel.sendMessage(user, chatInputText, null)
                            chatInputText = ""
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .size(46.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
