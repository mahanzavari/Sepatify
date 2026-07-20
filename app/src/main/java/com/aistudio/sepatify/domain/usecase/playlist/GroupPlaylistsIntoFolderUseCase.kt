package com.aistudio.sepatify.domain.usecase.playlist

import com.aistudio.sepatify.domain.repository.SongRepository

class GroupPlaylistsIntoFolderUseCase(
    private val songRepository: SongRepository
) {
    suspend operator fun invoke(folderName: String, playlistIds: List<Long>) {
        val category = "folder:$folderName"
        playlistIds.forEach { pid ->
            songRepository.updatePlaylistCategory(pid, category)
        }
    }
}
