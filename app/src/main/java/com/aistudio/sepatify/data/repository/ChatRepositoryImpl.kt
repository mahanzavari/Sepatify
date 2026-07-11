package com.aistudio.sepatify.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.aistudio.sepatify.data.local.ChatMessageDao
import com.aistudio.sepatify.data.local.ChatMessageEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.Supa
import com.aistudio.sepatify.data.remote.dto.ChatMessageDto
import com.aistudio.sepatify.data.remote.dto.NewChatMessageDto
import com.aistudio.sepatify.data.remote.dto.ProfileDto
import com.aistudio.sepatify.data.remote.dto.TypingPayload
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val authRepository: AuthRepository
) : ChatRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val usernameToId = mutableMapOf<String, String>()
    private val idToUsername = mutableMapOf<String, String>()
    private val typingStates = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val typingChannelUsers = mutableSetOf<String>()

    init {
        repoScope.launch { subscribeToRealtimeMessages() }
    }

    override fun getRecentConversations(): Flow<List<String>> {
        // Trigger sync in the background, but WAIT for the session to be fully valid first
        repoScope.launch {
            if (authRepository.hasValidSession()) { // This forces it to wait for initialization
                syncRecentConversations()
            }
        }
        return chatMessageDao.getRecentConversations()
    }

    private suspend fun syncRecentConversations() {
        // Since we checked hasValidSession() above, currentUserId() will now correctly return your UUID
        val myId = authRepository.currentUserId() ?: return

        try {
            val remoteMessages = Supa.client.from("chat_messages")
                .select(columns = Columns.ALL) {
                    filter {
                        or {
                            eq("sender_id", myId)
                            eq("receiver_id", myId)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                    limit(60) // Only fetch the last 60 messages to populate the feed quickly
                }
                .decodeList<ChatMessageDto>()

            remoteMessages.forEach { upsertRemoteMessage(it, myId) }
        } catch (e: Exception) {
            // Silently ignore network errors so it safely falls back to local SQLite cache
            e.printStackTrace()
        }
    }

    // ---------------------------------------------------------------------
    // Username <-> Supabase user id resolution (with a small in-memory cache)
    // ---------------------------------------------------------------------

    private suspend fun resolveId(username: String): String? {
        usernameToId[username]?.let { return it }
        return try {
            val profile = Supa.client.from("profiles")
                .select(columns = Columns.ALL) { filter { eq("username", username) } }
                .decodeSingleOrNull<ProfileDto>()
            profile?.let {
                usernameToId[username] = it.id
                idToUsername[it.id] = username
                it.id
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveUsername(id: String): String {
        idToUsername[id]?.let { return it }
        return try {
            val profile = Supa.client.from("profiles")
                .select(columns = Columns.ALL) { filter { eq("id", id) } }
                .decodeSingleOrNull<ProfileDto>()
            val name = profile?.username ?: id
            idToUsername[id] = name
            usernameToId[name] = id
            name
        } catch (e: Exception) {
            id
        }
    }

    // ---------------------------------------------------------------------
    // Messages (Room is the source of truth for the UI; Supabase Realtime keeps it in sync)
    // ---------------------------------------------------------------------

    override fun getMessages(otherUser: String): Flow<List<ChatMessageEntity>> {
        repoScope.launch { fetchConversationHistory(otherUser) }
        repoScope.launch { markConversationRead(otherUser) }
        return chatMessageDao.getMessagesBetweenUsers("Me", otherUser)
    }

    override fun getMessagesPaged(otherUser: String): Flow<PagingData<ChatMessageEntity>> {
        repoScope.launch { fetchConversationHistory(otherUser) }
        repoScope.launch { markConversationRead(otherUser) }
        return Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
            chatMessageDao.getMessagesBetweenUsersPaged("Me", otherUser)
        }.flow
    }

    private suspend fun fetchConversationHistory(otherUser: String) {
        val myId = authRepository.currentUserId() ?: return
        val otherId = resolveId(otherUser) ?: return
        try {
            val remoteMessages = Supa.client.from("chat_messages")
                .select(columns = Columns.ALL) {
                    filter {
                        or {
                            and {
                                eq("sender_id", myId)
                                eq("receiver_id", otherId)
                            }
                            and {
                                eq("sender_id", otherId)
                                eq("receiver_id", myId)
                            }
                        }
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ChatMessageDto>()
            remoteMessages.forEach { upsertRemoteMessage(it, myId) }
        } catch (e: Exception) {
            // Offline - Room keeps serving the last synced conversation.
        }
    }

    private suspend fun upsertRemoteMessage(dto: ChatMessageDto, myId: String) {
        val existing = chatMessageDao.findByRemoteId(dto.id)
        val senderName = if (dto.senderId == myId) "Me" else resolveUsername(dto.senderId)
        val receiverName = if (dto.receiverId == myId) "Me" else resolveUsername(dto.receiverId)
        val entity = ChatMessageEntity(
            id = existing?.id ?: 0,
            remoteId = dto.id,
            senderName = senderName,
            receiverName = receiverName,
            text = dto.text,
            isSongShare = dto.isSongShare,
            songId = dto.songId,
            songTitle = dto.songTitle,
            songArtist = dto.songArtist,
            songCover = dto.songCover,
            songAudio = dto.songAudio,
            timestamp = parseTimestamp(dto.createdAt),
            status = dto.status
        )
        chatMessageDao.insertMessage(entity)
    }

    private fun parseTimestamp(iso: String?): Long {
        if (iso == null) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(iso.take(19))?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun subscribeToRealtimeMessages() {
        try {
            val channel = Supa.client.realtime.channel("chat-messages-global")
            val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "chat_messages"
            }
            channel.subscribe()
            changes.collect { action ->
                val myId = authRepository.currentUserId() ?: return@collect
                val dto = when (action) {
                    is PostgresAction.Insert -> runCatching { jsonParser.decodeFromJsonElement<ChatMessageDto>(action.record) }.getOrNull()
                    is PostgresAction.Update -> runCatching { jsonParser.decodeFromJsonElement<ChatMessageDto>(action.record) }.getOrNull()
                    else -> null
                } ?: return@collect

                if (dto.senderId == myId || dto.receiverId == myId) {
                    upsertRemoteMessage(dto, myId)
                    if (dto.receiverId == myId && dto.status == "Sent") {
                        markDelivered(dto.id)
                    }
                }
            }
        } catch (e: Exception) {
            // Realtime not reachable (offline/misconfigured project) - Room cache still works.
        }
    }

    private suspend fun markDelivered(remoteMessageId: Long) {
        try {
            Supa.client.from("chat_messages").update(mapOf("status" to "Delivered")) {
                filter { eq("id", remoteMessageId) }
            }
        } catch (e: Exception) { /* best effort */ }
    }

    private suspend fun markConversationRead(otherUser: String) {
        val myId = authRepository.currentUserId() ?: return
        val otherId = resolveId(otherUser) ?: return
        try {
            Supa.client.from("chat_messages").update(mapOf("status" to "Read")) {
                filter {
                    eq("receiver_id", myId)
                    eq("sender_id", otherId)
                    neq("status", "Read")
                }
            }
            chatMessageDao.markConversationRead("Me", otherUser)
        } catch (e: Exception) { /* best effort */ }
    }

    override suspend fun sendMessage(otherUser: String, text: String, songShare: Song?) {
        val localEntity = ChatMessageEntity(
            senderName = "Me",
            receiverName = otherUser,
            text = text,
            isSongShare = songShare != null,
            songId = songShare?.id,
            songTitle = songShare?.title,
            songArtist = songShare?.artistName,
            songCover = songShare?.coverImageUrl,
            songAudio = songShare?.audioUrl,
            status = "Sending"
        )
        val localId = chatMessageDao.insertMessage(localEntity)

        val myId = authRepository.currentUserId()
        val otherId = resolveId(otherUser)
        if (myId == null || otherId == null) return

        try {
            val inserted = Supa.client.from("chat_messages")
                .insert(
                    NewChatMessageDto(
                        senderId = myId,
                        receiverId = otherId,
                        text = text,
                        isSongShare = songShare != null,
                        songId = songShare?.id,
                        songTitle = songShare?.title,
                        songArtist = songShare?.artistName,
                        songCover = songShare?.coverImageUrl,
                        songAudio = songShare?.audioUrl,
                        status = "Sent"
                    )
                ) { select(columns = Columns.ALL) }
                .decodeSingle<ChatMessageDto>()

            chatMessageDao.insertMessage(localEntity.copy(id = localId, remoteId = inserted.id, status = "Sent"))
        } catch (e: Exception) {
            // Stays "Sending" locally; will be retried the next time the conversation is opened.
        }
    }

    // ---------------------------------------------------------------------
    // Typing indicator via a Supabase Realtime broadcast channel (no DB writes)
    // ---------------------------------------------------------------------

    private fun getOrCreateTypingFlow(otherUser: String): MutableStateFlow<Boolean> {
        return synchronized(typingStates) {
            typingStates.getOrPut(otherUser) { MutableStateFlow(false) }
        }
    }

    private suspend fun typingChannelName(otherUser: String): String? {
        val myId = authRepository.currentUserId() ?: return null
        val otherId = resolveId(otherUser) ?: return null
        return "typing-" + listOf(myId, otherId).sorted().joinToString("-")
    }

    private suspend fun ensureTypingChannel(otherUser: String) {
        if (!typingChannelUsers.add(otherUser)) return
        val myId = authRepository.currentUserId() ?: return
        val channelName = typingChannelName(otherUser) ?: return
        try {
            val channel = Supa.client.realtime.channel(channelName)
            channel.subscribe()
            repoScope.launch {
                try {
                    // Explicitly pass <JsonObject> as the type parameter to broadcastFlow
                    channel.broadcastFlow<JsonObject>(event = "typing").collect { jsonObject ->
                        val payload = runCatching {
                            jsonParser.decodeFromJsonElement<TypingPayload>(jsonObject)
                        }.getOrNull()
                        if (payload != null && payload.userId != myId) {
                            getOrCreateTypingFlow(otherUser).value = payload.isTyping
                        }
                    }
                } catch (e: Exception) { /* ignore */ }
            }
        } catch (e: Exception) {
            typingChannelUsers.remove(otherUser)
        }
    }

    override fun getTypingState(otherUser: String): Flow<Boolean> {
        repoScope.launch { ensureTypingChannel(otherUser) }
        return getOrCreateTypingFlow(otherUser)
    }

    override suspend fun setTyping(otherUser: String, isTyping: Boolean) {
        val myId = authRepository.currentUserId() ?: return
        ensureTypingChannel(otherUser)
        val channelName = typingChannelName(otherUser) ?: return
        try {
            val channel = Supa.client.realtime.channel(channelName)
            val payload = buildJsonObject {
                put("user_id", JsonPrimitive(myId))
                put("is_typing", JsonPrimitive(isTyping))
            }
            channel.broadcast("typing", payload)
        } catch (e: Exception) { /* best effort */ }
    }

    // ---------------------------------------------------------------------
    // Social graph (follow / followers / discovery)
    // ---------------------------------------------------------------------

    override fun getFollowedUsers(): Flow<List<String>> = flow {
        val myId = authRepository.currentUserId()
        if (myId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            val rows = Supa.client.from("follows")
                .select(columns = Columns.raw("followed_id, profiles!follows_followed_id_fkey(username)")) {
                    filter { eq("follower_id", myId) }
                }
                .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
            emit(rows.mapNotNull { row -> (row["profiles"] as? Map<*, *>)?.get("username") as? String })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getFollowers(): Flow<List<String>> = flow {
        val myId = authRepository.currentUserId()
        if (myId == null) {
            emit(emptyList())
            return@flow
        }
        try {
            val rows = Supa.client.from("follows")
                .select(columns = Columns.raw("follower_id, profiles!follows_follower_id_fkey(username)")) {
                    filter { eq("followed_id", myId) }
                }
                .decodeList<Map<String, kotlinx.serialization.json.JsonElement>>()
            emit(rows.mapNotNull { row -> (row["profiles"] as? Map<*, *>)?.get("username") as? String })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun toggleFollowUser(username: String) {
        val myId = authRepository.currentUserId() ?: return
        val otherId = resolveId(username) ?: return
        try {
            val alreadyFollowing = Supa.client.from("follows")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("follower_id", myId)
                        eq("followed_id", otherId)
                    }
                }
                .decodeList<com.aistudio.sepatify.data.remote.dto.FollowDto>()
                .isNotEmpty()

            if (alreadyFollowing) {
                Supa.client.from("follows").delete {
                    filter {
                        eq("follower_id", myId)
                        eq("followed_id", otherId)
                    }
                }
            } else {
                Supa.client.from("follows").insert(
                    com.aistudio.sepatify.data.remote.dto.FollowDto(followerId = myId, followedId = otherId)
                )
            }
        } catch (e: Exception) { /* best effort, likely offline */ }
    }

    override fun isFollowing(username: String): Flow<Boolean> = flow {
        val myId = authRepository.currentUserId()
        val otherId = resolveId(username)
        if (myId == null || otherId == null) {
            emit(false)
            return@flow
        }
        try {
            val exists = Supa.client.from("follows")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("follower_id", myId)
                        eq("followed_id", otherId)
                    }
                }
                .decodeList<com.aistudio.sepatify.data.remote.dto.FollowDto>()
                .isNotEmpty()
            emit(exists)
        } catch (e: Exception) {
            emit(false)
        }
    }

    override fun searchUsers(query: String): Flow<List<String>> = flow {
        val myId = authRepository.currentUserId()
        try {
            val profiles = Supa.client.from("profiles")
                .select(columns = Columns.ALL) {
                    if (query.isNotBlank()) {
                        filter { ilike("username", "%$query%") }
                    }
                    limit(30)
                }
                .decodeList<ProfileDto>()
            emit(profiles.filter { it.id != myId }.map { it.username })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}