package com.aistudio.sepatify.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY displayName ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE isFollowed = 1 ORDER BY displayName ASC")
    fun getFollowedArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :id")
    fun getArtistById(id: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE username = :username")
    fun getArtistByUsername(username: String): Flow<ArtistEntity?>

    @Query("SELECT * FROM artists WHERE displayName LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%'")
    fun searchArtists(query: String): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: ArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Query("UPDATE artists SET isFollowed = :followed WHERE id = :artistId")
    suspend fun updateFollowStatus(artistId: String, followed: Boolean)

    @Query("DELETE FROM artists WHERE id = :id")
    suspend fun deleteArtist(id: String)

    @Query("DELETE FROM artists")
    suspend fun clearAll()
}