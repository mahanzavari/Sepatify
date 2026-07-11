package com.aistudio.sepatify.data.local

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteSearchById(id: Int)

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}

@Dao
interface LikedSongDao {
    @Query("SELECT * FROM liked_songs ORDER BY timestamp DESC")
    fun getLikedSongs(): Flow<List<LikedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLikedSong(song: LikedSongEntity)

    @Query("DELETE FROM liked_songs WHERE id = :id")
    suspend fun deleteLikedSongById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :id)")
    fun isLikedFlow(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM liked_songs WHERE id = :id)")
    suspend fun isLiked(id: String): Boolean

    @Query("DELETE FROM liked_songs")
    suspend fun clearAllLikedSongs()
}

@Dao
interface RecentlyPlayedDao {
    @Query("SELECT * FROM recently_played ORDER BY timestamp DESC LIMIT 30")
    fun getRecentSongs(): Flow<List<RecentlyPlayedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSong(song: RecentlyPlayedEntity)

    @Query("DELETE FROM recently_played")
    suspend fun clearAllRecent()
}

@Dao
interface DownloadedSongDao {
    @Query("SELECT * FROM downloaded_songs ORDER BY timestamp DESC")
    fun getDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedSong(song: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE id = :id")
    suspend fun deleteDownloadedSongById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM downloaded_songs WHERE id = :id)")
    fun isDownloadedFlow(id: String): Flow<Boolean>
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists")
    fun getPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE category = :category")
    fun getPlaylistsByCategory(category: String): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongCrossRef(playlistId: Long, songId: String)

    // Inner join querying for songs associated with playlist ID
    @Query("""
        SELECT liked_songs.* FROM liked_songs
        INNER JOIN playlist_song_cross_ref ON liked_songs.id = playlist_song_cross_ref.songId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
    """)
    fun getLikedSongsForPlaylist(playlistId: Long): Flow<List<LikedSongEntity>>

    // Or downloaded songs
    @Query("""
        SELECT downloaded_songs.* FROM downloaded_songs
        INNER JOIN playlist_song_cross_ref ON downloaded_songs.id = playlist_song_cross_ref.songId
        WHERE playlist_song_cross_ref.playlistId = :playlistId
    """)
    fun getDownloadedSongsForPlaylist(playlistId: Long): Flow<List<DownloadedSongEntity>>
}

@Dao
interface ChatMessageDao {
    @Query("""
        SELECT * FROM chat_messages
        WHERE (senderName = :user1 AND receiverName = :user2)
           OR (senderName = :user2 AND receiverName = :user1)
        ORDER BY timestamp ASC
    """)
    fun getMessagesBetweenUsers(user1: String, user2: String): Flow<List<ChatMessageEntity>>

    // Paging 3 source for the chat history screen (FR: chat history must use Paging 3).
    @Query("""
        SELECT * FROM chat_messages
        WHERE (senderName = :user1 AND receiverName = :user2)
           OR (senderName = :user2 AND receiverName = :user1)
        ORDER BY timestamp ASC
    """)
    fun getMessagesBetweenUsersPaged(user1: String, user2: String): PagingSource<Int, ChatMessageEntity>


    @Query("""
        SELECT CASE WHEN senderName = 'Me' THEN receiverName ELSE senderName END 
        FROM chat_messages
        WHERE senderName = 'Me' OR receiverName = 'Me'
        GROUP BY CASE WHEN senderName = 'Me' THEN receiverName ELSE senderName END
        ORDER BY MAX(timestamp) DESC
    """)
    fun getRecentConversations(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("UPDATE chat_messages SET status = :status WHERE remoteId = :remoteId")
    suspend fun updateMessageStatusByRemoteId(remoteId: Long, status: String)

    @Query("SELECT * FROM chat_messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun findByRemoteId(remoteId: Long): ChatMessageEntity?

    @Query("""
        UPDATE chat_messages SET status = 'Read'
        WHERE receiverName = :me AND senderName = :otherUser AND status != 'Read'
    """)
    suspend fun markConversationRead(me: String, otherUser: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface SongCacheDao {
    @Query("SELECT * FROM song_cache ORDER BY title ASC")
    fun getAll(): Flow<List<SongCacheEntity>>

    @Query("SELECT * FROM song_cache WHERE category = :category ORDER BY title ASC")
    fun getByCategory(category: String): Flow<List<SongCacheEntity>>

    @Query("SELECT * FROM song_cache WHERE title LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<SongCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongCacheEntity>)

    @Query("DELETE FROM song_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM song_cache")
    suspend fun count(): Int
}
