package com.aistudio.sepatify.data.repository

import com.aistudio.sepatify.data.local.ArtistDao
import com.aistudio.sepatify.data.local.ArtistEntity
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.remote.Supa
import com.aistudio.sepatify.data.remote.dto.ArtistDto
import com.aistudio.sepatify.data.remote.dto.ArtistWithSongsDto
import com.aistudio.sepatify.domain.model.Artist
import com.aistudio.sepatify.domain.model.ArtistProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ArtistRepositoryImpl(
    private val artistDao: ArtistDao,
    private val authRepository: AuthRepository
) : ArtistRepository {

    override fun getAllArtists(): Flow<List<Artist>> {
        return artistDao.getAllArtists().map { list -> list.map { it.toDomain() } }
    }

    override fun getFollowedArtists(): Flow<List<Artist>> {
        return artistDao.getFollowedArtists().map { list -> list.map { it.toDomain() } }
    }

    override fun getArtistById(id: String): Flow<Artist?> {
        return artistDao.getArtistById(id).map { it?.toDomain() }
    }

    override suspend fun getArtistProfile(artistId: String): ArtistProfile? {
        val myId = authRepository.currentUserId()
        
        return try {
            // Fetch artist with songs using join
            val artistWithSongs = Supa.client.from("artists")
                .select(columns = Columns.raw("*, songs(*)")) {
                    filter { eq("id", artistId) }
                }
                .decodeSingleOrNull<ArtistWithSongsDto>()
            
            if (artistWithSongs == null) return null
            
            // Check if followed
            var isFollowed = false
            var followersCount = 0
            
            if (myId != null) {
                // Check follow status
                val followRows = Supa.client.from("follows")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("follower_id", myId)
                            eq("followed_id", artistId)
                        }
                    }
                    .decodeList<com.aistudio.sepatify.data.remote.dto.FollowDto>()
                isFollowed = followRows.isNotEmpty()
                
                // Count followers
                val followers = Supa.client.from("follows")
                    .select(columns = Columns.ALL) {
                        filter { eq("followed_id", artistId) }
                    }
                    .decodeList<com.aistudio.sepatify.data.remote.dto.FollowDto>()
                followersCount = followers.size
            }
            
            // Convert songs
            val songs = artistWithSongs.songs?.map { songDto ->
                Song(
                    id = songDto.id,
                    title = songDto.title,
                    artistName = songDto.artistName,
                    coverImageUrl = songDto.coverImageUrl,
                    audioUrl = songDto.audioUrl,
                    category = songDto.category
                )
            } ?: emptyList()
            
            ArtistProfile(
                artist = artistWithSongs.toDomain().copy(isFollowed = isFollowed),
                songs = songs,
                followersCount = followersCount,
                isFollowed = isFollowed
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun searchArtists(query: String): Flow<List<Artist>> {
        return artistDao.searchArtists(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun toggleFollowArtist(artistId: String) {
        val myId = authRepository.currentUserId() ?: return
        
        try {
            val alreadyFollowing = Supa.client.from("follows")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("follower_id", myId)
                        eq("followed_id", artistId)
                    }
                }
                .decodeList<com.aistudio.sepatify.data.remote.dto.FollowDto>()
                .isNotEmpty()

            if (alreadyFollowing) {
                Supa.client.from("follows").delete {
                    filter {
                        eq("follower_id", myId)
                        eq("followed_id", artistId)
                    }
                }
                artistDao.updateFollowStatus(artistId, false)
            } else {
                Supa.client.from("follows").insert(
                    com.aistudio.sepatify.data.remote.dto.FollowDto(
                        followerId = myId, 
                        followedId = artistId
                    )
                )
                artistDao.updateFollowStatus(artistId, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun isFollowingArtist(artistId: String): Flow<Boolean> {
        return artistDao.getArtistById(artistId).map { it?.isFollowed ?: false }
    }

    override fun getArtistSongs(artistId: String): Flow<List<Song>> {
        // This is fetched from remote; local cache could be added
        return kotlinx.coroutines.flow.flow {
            try {
                val songs = Supa.client.from("songs")
                    .select(columns = Columns.ALL) {
                        filter { eq("artist_id", artistId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<com.aistudio.sepatify.data.remote.dto.SongDto>()
                    .map { dto ->
                        Song(
                            id = dto.id,
                            title = dto.title,
                            artistName = dto.artistName,
                            coverImageUrl = dto.coverImageUrl,
                            audioUrl = dto.audioUrl,
                            category = dto.category
                        )
                    }
                emit(songs)
            } catch (e: Exception) {
                emit(emptyList())
            }
        }
    }

    override suspend fun refreshArtists() {
        try {
            val remoteArtists = Supa.client.from("artists")
                .select(columns = Columns.ALL)
                .decodeList<ArtistDto>()
            
            // Get current follow status before clearing
            val followedIds = artistDao.getFollowedArtists().first().map { it.id }.toSet()
            
            val entities = remoteArtists.map { dto ->
                ArtistEntity(
                    id = dto.id,
                    username = dto.username,
                    displayName = dto.displayName,
                    avatarUrl = dto.avatarUrl,
                    bio = dto.bio,
                    verified = dto.verified,
                    monthlyListeners = dto.monthlyListeners,
                    isFollowed = dto.id in followedIds
                )
            }
            
            artistDao.insertArtists(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// Extension functions
private fun ArtistEntity.toDomain() = Artist(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    verified = verified,
    monthlyListeners = monthlyListeners,
    isFollowed = isFollowed
)

private fun ArtistDto.toDomain() = Artist(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    verified = verified,
    monthlyListeners = monthlyListeners
)

private fun ArtistWithSongsDto.toDomain() = Artist(
    id = id,
    username = username,
    displayName = displayName,
    avatarUrl = avatarUrl,
    bio = bio,
    verified = verified,
    monthlyListeners = monthlyListeners
)