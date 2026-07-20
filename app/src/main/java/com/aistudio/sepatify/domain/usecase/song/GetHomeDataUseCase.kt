package com.aistudio.sepatify.domain.usecase.song

import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class HomeData(
    val trending: List<Song>,
    val recommendations: List<Song>,
    val newReleases: List<Song>,
    val mostPopular: List<Song>,
    val globalPlaylistSongs: List<Song>,
    val localPlaylistSongs: List<Song>,
    val exclusiveSongs: List<Song>
)

class GetHomeDataUseCase(
    private val songRepository: SongRepository
) {
    @Suppress("UNCHECKED_CAST")
    operator fun invoke(): Flow<HomeData> = combine(
        songRepository.getTrendingSongs(),
        songRepository.getDailyRecommendations(),
        songRepository.getNewReleases(),
        songRepository.getMostPopular(),
        songRepository.getGlobalPlaylists(),
        songRepository.getLocalPlaylists(),
        songRepository.getExclusiveSongs()
    ) { values ->
        HomeData(
            trending = values[0] as List<Song>,
            recommendations = values[1] as List<Song>,
            newReleases = values[2] as List<Song>,
            mostPopular = values[3] as List<Song>,
            globalPlaylistSongs = values[4] as List<Song>,
            localPlaylistSongs = values[5] as List<Song>,
            exclusiveSongs = values[6] as List<Song>
        )
    }
}
