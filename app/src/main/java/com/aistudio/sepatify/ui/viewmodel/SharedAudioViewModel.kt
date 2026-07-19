package com.aistudio.sepatify.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.sepatify.data.model.Song
import com.aistudio.sepatify.data.repository.DownloadRepository
import com.aistudio.sepatify.data.repository.SongRepository
import com.aistudio.sepatify.player.AudioPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class SharedAudioViewModel(
    private val songRepository: SongRepository,
    private val downloadRepository: DownloadRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = audioPlayerManager.currentSong
    val isPlaying: StateFlow<Boolean> = audioPlayerManager.isPlaying
    val playlist: StateFlow<List<Song>> = audioPlayerManager.playlist
    val progress: StateFlow<Long> = audioPlayerManager.progress
    val duration: StateFlow<Long> = audioPlayerManager.duration
    val isShuffle: StateFlow<Boolean> = audioPlayerManager.isShuffle
    val isRepeat: StateFlow<Boolean> = audioPlayerManager.isRepeat
    val playbackSpeed: StateFlow<Float> = audioPlayerManager.playbackSpeed

    val eqEnabled: StateFlow<Boolean> = audioPlayerManager.eqEnabled
    val eqBandLevels: StateFlow<Map<Int, Int>> = audioPlayerManager.eqBandLevels
    val eqFrequencies: StateFlow<List<Int>> = audioPlayerManager.eqFrequencies
    val eqBandRange: StateFlow<Pair<Int, Int>> = audioPlayerManager.eqBandRange
    val bassBoostStrength: StateFlow<Int> = audioPlayerManager.bassBoostStrength
    val virtualizerStrength: StateFlow<Int> = audioPlayerManager.virtualizerStrength
    val reverbPreset: StateFlow<Int> = audioPlayerManager.reverbPreset
    val crossfadeEnabled: StateFlow<Boolean> = audioPlayerManager.crossfadeEnabled
    val crossfadeDurationSec: StateFlow<Int> = audioPlayerManager.crossfadeDurationSec
    val fftBands: StateFlow<FloatArray> = audioPlayerManager.fftBands
    val isBassDetected: StateFlow<Boolean> = audioPlayerManager.isBassDetected

    private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
    val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()

    private val _visualizerHeights = MutableStateFlow(List(64) { 15 })
    val visualizerHeights: StateFlow<List<Int>> = _visualizerHeights.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var visualizerJob: Job? = null

    init {
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
        viewModelScope.launch(Dispatchers.IO) {
            val downloadedList = downloadRepository.getDownloadedSongs().first()
            val downloadedMap = downloadedList.associateBy { it.id }

            fun resolveInstant(s: Song): Song {
                if (s.id.startsWith("local_") || s.audioUrl.startsWith("/") || s.audioUrl.startsWith("file://")) {
                    return s
                }
                val downloadedEntity = downloadedMap[s.id]
                if (downloadedEntity != null && File(downloadedEntity.localFilePath).exists()) {
                    return s.copy(audioUrl = "file://${downloadedEntity.localFilePath}")
                }
                return s
            }

            val resolvedSong = resolveInstant(song)
            val resolvedQueue = queue.map { resolveInstant(it) }

            songRepository.addRecentSong(resolvedSong)
            
            withContext(Dispatchers.Main) {
                audioPlayerManager.playSong(resolvedSong, resolvedQueue)
            }
        }
    }

    fun togglePlayPause() {
        audioPlayerManager.togglePlayPause()
    }

    fun stopPlayback() {
        audioPlayerManager.stopPlayback()
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
                    audioPlayerManager.togglePlayPause()
                }
                _sleepTimerMinutes.value = null
            }
        }
    }

    private fun startVisualizerLoop() {
        visualizerJob?.cancel()
        visualizerJob = viewModelScope.launch {
            while (true) {
                if (isPlaying.value) {
                    _visualizerHeights.value = List(64) { Random.nextInt(5, 45) }
                } else {
                    _visualizerHeights.value = List(64) { 5 }
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

    // === MVI central event handler ===
    fun onEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.PlaySong               -> playSong(event.song, event.queue)
            AudioEvent.TogglePlayPause           -> togglePlayPause()
            AudioEvent.StopPlayback              -> stopPlayback()
            AudioEvent.PlayNext                  -> playNext()
            AudioEvent.PlayPrevious              -> playPrevious()
            is AudioEvent.SeekTo                 -> seekTo(event.position)
            AudioEvent.ToggleShuffle             -> toggleShuffle()
            AudioEvent.ToggleRepeat              -> toggleRepeat()
            is AudioEvent.ToggleLikeSong         -> toggleLikeSong(event.song)
            is AudioEvent.SetPlaybackSpeed       -> setPlaybackSpeed(event.speed)
            is AudioEvent.SetEqualizerEnabled    -> setEqualizerEnabled(event.enabled)
            is AudioEvent.SetEqualizerBandLevel  -> setEqualizerBandLevel(event.bandIndex, event.levelMilliBels)
            is AudioEvent.SetBassBoostStrength   -> setBassBoostStrength(event.strength)
            is AudioEvent.SetVirtualizerStrength -> setVirtualizerStrength(event.strength)
            is AudioEvent.SetReverbPreset        -> setReverbPreset(event.presetIndex)
            is AudioEvent.SetCrossfadeEnabled    -> setCrossfadeEnabled(event.enabled)
            is AudioEvent.SetCrossfadeDuration   -> setCrossfadeDuration(event.seconds)
            is AudioEvent.SetSleepTimer          -> setSleepTimer(event.minutes)
        }
    }
}