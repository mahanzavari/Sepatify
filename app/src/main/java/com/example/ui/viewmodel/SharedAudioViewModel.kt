package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Song
import com.example.data.repository.DownloadRepository
import com.example.data.repository.SongRepository
import com.example.player.AudioPlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class SharedAudioViewModel(
    private val songRepository: SongRepository,
    private val downloadRepository: DownloadRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    // Delegate playback flows directly to AudioPlayerManager
    val currentSong: StateFlow<Song?> = audioPlayerManager.currentSong
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val playlist: StateFlow<List<Song>> = audioPlayerManager.playlist
    val progress: StateFlow<Long> = audioPlayerManager.progress
    val duration: StateFlow<Long> = audioPlayerManager.duration
    val isShuffle: StateFlow<Boolean> = audioPlayerManager.isShuffle
    val isRepeat: StateFlow<Boolean> = audioPlayerManager.isRepeat
    val playbackSpeed: StateFlow<Float> = audioPlayerManager.playbackSpeed

    // Delegate Equalizer & Audio Effect flows directly to AudioPlayerManager
    val eqEnabled: StateFlow<Boolean> = audioPlayerManager.eqEnabled
    val eqBandLevels: StateFlow<Map<Int, Int>> = audioPlayerManager.eqBandLevels
    val eqFrequencies: StateFlow<List<Int>> = audioPlayerManager.eqFrequencies
    val eqBandRange: StateFlow<Pair<Int, Int>> = audioPlayerManager.eqBandRange
    val bassBoostStrength: StateFlow<Int> = audioPlayerManager.bassBoostStrength
    val virtualizerStrength: StateFlow<Int> = audioPlayerManager.virtualizerStrength
    val reverbPreset: StateFlow<Int> = audioPlayerManager.reverbPreset
    val crossfadeEnabled: StateFlow<Boolean> = audioPlayerManager.crossfadeEnabled
    val crossfadeDurationSec: StateFlow<Int> = audioPlayerManager.crossfadeDurationSec

    // Keeps local sleep timer flow
    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    // Keeps beautiful visualizer heights flow
    private val _visualizerHeights = MutableStateFlow(List(12) { 15 })
    val visualizerHeights: StateFlow<List<Int>> = _visualizerHeights.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var visualizerJob: Job? = null

    init {
        // Load initial songs into play queue
        viewModelScope.launch {
            songRepository.getAllSongs().collectLatest { songs ->
                if (audioPlayerManager.playlist.value.isEmpty()) {
                    audioPlayerManager.setPlaylist(songs)
                }
            }
        }

        startVisualizerLoop()
    }

    fun playSong(song: Song, queue: List<Song> = emptyList()) {
        // Smart routing (FR): if this track was downloaded, stream it from disk instead of the network.
        viewModelScope.launch {
            val resolvedSong = resolveForPlayback(song)
            val resolvedQueue = queue.map { resolveForPlayback(it) }
            songRepository.addRecentSong(resolvedSong)
            audioPlayerManager.playSong(resolvedSong, resolvedQueue)
        }
    }

    private suspend fun resolveForPlayback(song: Song): Song {
        val localPath = downloadRepository.getLocalPlaybackUri(song) ?: return song
        return song.copy(audioUrl = "file://$localPath")
    }

    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
    }

    fun playNext() {
        audioPlayerManager.playNext()
    }

    fun playPrevious() {
        audioPlayerManager.playPrevious()
    }

    fun seekTo(position: Long) {
        audioPlayerManager.seekTo(position)
    }

    fun toggleShuffle() {
        audioPlayerManager.toggleShuffle()
    }

    fun toggleRepeat() {
        audioPlayerManager.toggleRepeat()
    }

    fun isSongLiked(songId: String): Flow<Boolean> {
        return songRepository.isSongLiked(songId)
    }

    fun toggleLikeSong(song: Song) {
        viewModelScope.launch {
            songRepository.toggleLikeSong(song)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioPlayerManager.setPlaybackSpeed(speed)
    }

    // --- Audio Effects Controller API ---

    fun setEqualizerEnabled(enabled: Boolean) {
        audioPlayerManager.setEqualizerEnabled(enabled)
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        audioPlayerManager.setEqualizerBandLevel(bandIndex, levelMilliBels)
    }

    fun setBassBoostStrength(strength: Int) {
        audioPlayerManager.setBassBoostStrength(strength)
    }

    fun setVirtualizerStrength(strength: Int) {
        audioPlayerManager.setVirtualizerStrength(strength)
    }

    fun setReverbPreset(presetIndex: Int) {
        audioPlayerManager.setReverbPreset(presetIndex)
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        audioPlayerManager.setCrossfadeEnabled(enabled)
    }

    fun setCrossfadeDuration(seconds: Int) {
        audioPlayerManager.setCrossfadeDuration(seconds)
    }

    // --- Sleep Timer Loop ---

    fun setSleepTimer(minutes: Int?) {
        _sleepTimerMinutes.value = minutes
        sleepTimerJob?.cancel()
        if (minutes != null) {
            sleepTimerJob = viewModelScope.launch {
                var remainingSeconds = minutes * 60
                while (remainingSeconds > 0) {
                    delay(1000)
                    remainingSeconds--
                    _sleepTimerMinutes.value = (remainingSeconds / 60) + if (remainingSeconds % 60 > 0) 1 else 0
                }
                if (isPlaying.value) {
                    audioPlayerManager.togglePlayPause() // Pause playback
                }
                _sleepTimerMinutes.value = null
            }
        }
    }

    // --- Visualizer Loop ---

    private fun startVisualizerLoop() {
        visualizerJob?.cancel()
        visualizerJob = viewModelScope.launch {
            while (true) {
                if (isPlaying.value) {
                    _visualizerHeights.value = List(12) { Random.nextInt(5, 45) }
                } else {
                    _visualizerHeights.value = List(12) { 5 }
                }
                delay(120)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        visualizerJob?.cancel()
    }
}
