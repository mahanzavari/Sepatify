package com.aistudio.sepatify.data.repository

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.model.Artist
import com.aistudio.sepatify.domain.model.ArtistProfile
import kotlinx.coroutines.flow.Flow

interface ArtistRepository {
    // Get all artists
    fun getAllArtists(): Flow<List<Artist>>
    
    // Get followed artists
    fun getFollowedArtists(): Flow<List<Artist>>
    
    // Get artist by ID
    fun getArtistById(id: String): Flow<Artist?>
    
    // Get artist profile with songs
    suspend fun getArtistProfile(artistId: String): ArtistProfile?
    
    // Search artists
    fun searchArtists(query: String): Flow<List<Artist>>
    
    // Follow/unfollow artist (reuses follows table)
    suspend fun toggleFollowArtist(artistId: String)
    
    // Check if following artist
    fun isFollowingArtist(artistId: String): Flow<Boolean>
    
    // Get artist's songs
    fun getArtistSongs(artistId: String): Flow<List<Song>>
    
    // Refresh artists from remote
    suspend fun refreshArtists()
}