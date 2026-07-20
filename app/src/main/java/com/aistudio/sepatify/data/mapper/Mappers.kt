package com.aistudio.sepatify.data.mapper

import com.aistudio.sepatify.data.local.*
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.dto.*
import com.aistudio.sepatify.domain.model.*

// ── Song mappers ──────────────────────────────────────────────

fun SongDto.toDomain() = Song(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    category = category
)

fun SongDto.toCacheEntity() = SongCacheEntity(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    category = category
)

fun SongCacheEntity.toDomain() = Song(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    category = category
)

fun LikedSongEntity.toDomain() = Song(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl
)

fun RecentlyPlayedEntity.toDomain() = Song(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl
)

fun DownloadedSongEntity.toDomain() = DownloadedSong(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl,
    localFilePath = localFilePath,
    timestamp = timestamp
)

fun Song.toLikedEntity() = LikedSongEntity(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl
)

fun Song.toRecentEntity() = RecentlyPlayedEntity(
    id = id,
    title = title,
    artistName = artistName,
    coverImageUrl = coverImageUrl,
    audioUrl = audioUrl
)

// ── Playlist mappers ──────────────────────────────────────────

fun PlaylistEntity.toDomain() = Playlist(
    id = id,
    title = title,
    description = description,
    isUserCreated = isUserCreated,
    category = category,
    isPrivate = isPrivate
)

fun PlaylistDto.toDomain() = Playlist(
    id = id,
    title = title,
    description = description,
    isUserCreated = ownerId != null,
    category = category,
    isPrivate = isPrivate
)

// ── ChatMessage mappers ───────────────────────────────────────

fun ChatMessageEntity.toDomain() = ChatMessage(
    id = id,
    remoteId = remoteId,
    senderName = senderName,
    receiverName = receiverName,
    text = text,
    isSongShare = isSongShare,
    songId = songId,
    songTitle = songTitle,
    songArtist = songArtist,
    songCover = songCover,
    songAudio = songAudio,
    timestamp = timestamp,
    status = status
)

// ── Profile mappers ───────────────────────────────────────────

fun ProfileDto.toDomain() = UserProfile(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    isPremium = isPremium,
    createdAt = createdAt
)