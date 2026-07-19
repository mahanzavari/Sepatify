package com.aistudio.sepatify.ui.viewmodel

import com.aistudio.sepatify.data.model.Song

sealed interface AuthEvent {
    data class Login(val email: String, val pass: String) : AuthEvent
    data class Register(val email: String, val name: String, val pass: String) : AuthEvent
    object Logout : AuthEvent
    object ResetState : AuthEvent
}

sealed interface MainEvent {
    data class UpdateTheme(val theme: String) : MainEvent
    data class UpdateLanguage(val lang: String) : MainEvent
    data class UpdateFontSize(val scale: Float) : MainEvent
    data class SetPremium(val isPremium: Boolean, val syncRemote: Boolean = true) : MainEvent
    data class UpdateDisplayName(val name: String) : MainEvent
    data class UpdateAvatar(val avatar: String) : MainEvent
    object Logout : MainEvent
}

sealed interface HomeEvent {
    object Refresh : HomeEvent
}

sealed interface SearchEvent {
    data class UpdateQuery(val query: String) : SearchEvent
    data class SetFilter(val filter: String) : SearchEvent
    data class DeleteHistoryItem(val query: String) : SearchEvent
    object ClearHistory : SearchEvent
}

sealed interface DownloadEvent {
    data class UpdateSort(val sortBy: String) : DownloadEvent
    data class InitiateDownload(val song: Song) : DownloadEvent
    data class RemoveDownload(val songId: String) : DownloadEvent
}

sealed interface PlaylistEvent {
    data class CreatePlaylist(val title: String, val desc: String, val isPrivate: Boolean, val songIds: List<String>, val onResult: (Boolean, String?) -> Unit) : PlaylistEvent
    data class GroupPlaylists(val folderName: String, val playlistIds: List<Long>, val onComplete: () -> Unit) : PlaylistEvent
    data class DeletePlaylist(val playlistId: Long) : PlaylistEvent
    data class RemoveSong(val playlistId: Long, val songId: String) : PlaylistEvent
    data class RemoveRecentSong(val songId: String) : PlaylistEvent
}

sealed interface ChatEvent {
    data class LoadUserDetails(val username: String) : ChatEvent
    object TrackPresence : ChatEvent
    object UntrackPresence : ChatEvent
    data class ToggleFollow(val username: String) : ChatEvent
    data class SetTyping(val otherUser: String, val isTyping: Boolean) : ChatEvent
    data class SendMessage(val otherUser: String, val text: String, val songShare: Song? = null) : ChatEvent
    data class UpdateSearchQuery(val query: String) : ChatEvent
}

sealed interface AudioEvent {
    data class PlaySong(val song: Song, val queue: List<Song> = emptyList()) : AudioEvent
    object TogglePlayPause : AudioEvent
    object StopPlayback : AudioEvent
    object PlayNext : AudioEvent
    object PlayPrevious : AudioEvent
    data class SeekTo(val position: Long) : AudioEvent
    object ToggleShuffle : AudioEvent
    object ToggleRepeat : AudioEvent
    data class ToggleLikeSong(val song: Song) : AudioEvent
    data class SetPlaybackSpeed(val speed: Float) : AudioEvent
    data class SetEqualizerEnabled(val enabled: Boolean) : AudioEvent
    data class SetEqualizerBandLevel(val bandIndex: Int, val levelMilliBels: Int) : AudioEvent
    data class SetBassBoostStrength(val strength: Int) : AudioEvent
    data class SetVirtualizerStrength(val strength: Int) : AudioEvent
    data class SetReverbPreset(val presetIndex: Int) : AudioEvent
    data class SetCrossfadeEnabled(val enabled: Boolean) : AudioEvent
    data class SetCrossfadeDuration(val seconds: Int) : AudioEvent
    data class SetSleepTimer(val minutes: Int?) : AudioEvent
}
