package com.aistudio.sepatify.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.aistudio.sepatify.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

class AudioPlayerManager(private val context: Context) {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Smart caching (FR): streamed audio is cached on disk so seeking/replaying doesn't re-download.
    private val downloadCache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(300L * 1024 * 1024) // 300 MB
        val databaseProvider = StandaloneDatabaseProvider(context)
        SimpleCache(cacheDir, evictor, databaseProvider)
    }

    private val cacheDataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory()))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        // Audio Focus (FR): ExoPlayer will automatically pause/duck when it loses audio focus
        // (e.g. phone calls, other apps' voice notes) and resume once focus is regained.
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .build()
    private var mediaSession: MediaSession? = null

    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

    // Players state flows
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0L)
    val progress: StateFlow<Long> = _progress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playlist = MutableStateFlow<List<Song>>(emptyList())
    val playlist: StateFlow<List<Song>> = _playlist.asStateFlow()

    // FX State flows
    private val _eqEnabled = MutableStateFlow(false)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled.asStateFlow()

    private val _eqBandLevels = MutableStateFlow<Map<Int, Int>>(emptyMap()) // Band index -> level (dB or milliBels)
    val eqBandLevels: StateFlow<Map<Int, Int>> = _eqBandLevels.asStateFlow()

    private val _eqFrequencies = MutableStateFlow<List<Int>>(emptyList()) // List of center freqs in Hz
    val eqFrequencies: StateFlow<List<Int>> = _eqFrequencies.asStateFlow()

    private val _eqBandRange = MutableStateFlow(Pair(-1500, 1500)) // milliBels (usually -15dB to +15dB)
    val eqBandRange: StateFlow<Pair<Int, Int>> = _eqBandRange.asStateFlow()

    private val _bassBoostStrength = MutableStateFlow(0) // 0 to 1000
    val bassBoostStrength: StateFlow<Int> = _bassBoostStrength.asStateFlow()

    private val _virtualizerStrength = MutableStateFlow(0) // 0 to 1000
    val virtualizerStrength: StateFlow<Int> = _virtualizerStrength.asStateFlow()

    private val _reverbPreset = MutableStateFlow(0) // 0: None, 1: SmallRoom, 2: MediumRoom, 3: LargeRoom, 4: MediumHall, 5: LargeHall, 6: Plate
    val reverbPreset: StateFlow<Int> = _reverbPreset.asStateFlow()

    private val _crossfadeEnabled = MutableStateFlow(true)
    val crossfadeEnabled: StateFlow<Boolean> = _crossfadeEnabled.asStateFlow()

    private val _crossfadeDurationSec = MutableStateFlow(4) // 4 seconds
    val crossfadeDurationSec: StateFlow<Int> = _crossfadeDurationSec.asStateFlow()

    private var progressJob: Job? = null
    private var crossfadeJob: Job? = null
    private val handler = Handler(Looper.getMainLooper())

    init {
        // Build the MediaSession
        try {
            mediaSession = MediaSession.Builder(context, exoPlayer).build()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId
                val matchedSong = _playlist.value.find { it.id == songId }
                _currentSong.value = matchedSong
                _progress.value = 0L
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)

                // Start crossfade in (fade up from 0 to 1) when item transitions
                if (_crossfadeEnabled.value) {
                    triggerCrossfadeIn()
                } else {
                    exoPlayer.volume = 1.0f
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                _playbackSpeed.value = playbackParameters.speed
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffle.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _isRepeat.value = (repeatMode != Player.REPEAT_MODE_OFF)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _progress.value = newPosition.positionMs
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)
            }
        })

        // Listen for audio session id to initialize AudioEffects
        exoPlayer.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioSessionIdChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                initializeAudioEffects(audioSessionId)
            }
        })
    }

    fun getMediaSession(): MediaSession? = mediaSession

    fun setPlaylist(songs: List<Song>) {
        _playlist.value = songs
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.audioUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artistName)
                        .setArtworkUri(Uri.parse(song.coverImageUrl))
                        .build()
                )
                .build()
        }
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()
    }

    fun playSong(song: Song, customQueue: List<Song> = emptyList()) {
        if (customQueue.isNotEmpty() && customQueue != _playlist.value) {
            setPlaylist(customQueue)
        } else if (_playlist.value.isEmpty()) {
            setPlaylist(listOf(song))
        }

        val idx = _playlist.value.indexOfFirst { it.id == song.id }
        if (idx != -1) {
            exoPlayer.seekTo(idx, 0L)
            exoPlayer.playWhenReady = true
            exoPlayer.play()
            _currentSong.value = song
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.mediaItemCount > 0) {
                exoPlayer.play()
            } else if (_playlist.value.isNotEmpty()) {
                playSong(_playlist.value.first())
            }
        }
    }

    fun playNext() {
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        } else if (_isRepeat.value && exoPlayer.mediaItemCount > 0) {
            exoPlayer.seekTo(0, 0L)
            exoPlayer.play()
        }
    }

    fun playPrevious() {
        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else {
            exoPlayer.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _progress.value = positionMs
    }

    fun toggleShuffle() {
        val nextMode = !_isShuffle.value
        _isShuffle.value = nextMode
        exoPlayer.shuffleModeEnabled = nextMode
    }

    fun toggleRepeat() {
        val nextRepeat = !_isRepeat.value
        _isRepeat.value = nextRepeat
        exoPlayer.repeatMode = if (nextRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer.playbackParameters = PlaybackParameters(speed)
    }

    // --- Audio Effects (EQ, Bass Boost, Virtualizer, Reverb) ---

    private fun initializeAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        applicationScope.launch(Dispatchers.Default) {
            try {
                // Initialize Equalizer
                val eq = Equalizer(0, audioSessionId)
                val bandsCount = eq.numberOfBands.toInt()
                val freqs = mutableListOf<Int>()
                val initialLevels = mutableMapOf<Int, Int>()

                for (i in 0 until bandsCount) {
                    freqs.add(eq.getCenterFreq(i.toShort()) / 1000) // Convert milliHertz to Hertz
                    initialLevels[i] = eq.getBandLevel(i.toShort()).toInt()
                }

                val range = eq.getBandLevelRange()
                val minLevel = range[0].toInt()
                val maxLevel = range[1].toInt()

                _eqFrequencies.value = freqs
                _eqBandRange.value = Pair(minLevel, maxLevel)
                _eqBandLevels.value = initialLevels

                eq.enabled = _eqEnabled.value
                equalizer = eq

                // Initialize BassBoost
                val bb = BassBoost(0, audioSessionId)
                bb.enabled = _eqEnabled.value
                bb.setStrength(_bassBoostStrength.value.toShort())
                bassBoost = bb

                // Initialize Virtualizer
                val virt = Virtualizer(0, audioSessionId)
                virt.enabled = _eqEnabled.value
                virt.setStrength(_virtualizerStrength.value.toShort())
                virtualizer = virt

                // Initialize PresetReverb
                val reverb = PresetReverb(0, audioSessionId)
                reverb.enabled = _eqEnabled.value
                applyReverbPresetDirectly(reverb, _reverbPreset.value)
                presetReverb = reverb

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            presetReverb?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEqualizerBandLevel(bandIndex: Int, levelMilliBels: Int) {
        val currentLevels = _eqBandLevels.value.toMutableMap()
        currentLevels[bandIndex] = levelMilliBels
        _eqBandLevels.value = currentLevels

        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMilliBels.toShort())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBassBoostStrength(strength: Int) {
        _bassBoostStrength.value = strength
        try {
            bassBoost?.setStrength(strength.toShort())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        _virtualizerStrength.value = strength
        try {
            virtualizer?.setStrength(strength.toShort())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setReverbPreset(presetIndex: Int) {
        _reverbPreset.value = presetIndex
        presetReverb?.let {
            applyReverbPresetDirectly(it, presetIndex)
        }
    }

    private fun applyReverbPresetDirectly(reverb: PresetReverb, presetIndex: Int) {
        try {
            reverb.preset = when (presetIndex) {
                1 -> PresetReverb.PRESET_SMALLROOM
                2 -> PresetReverb.PRESET_MEDIUMROOM
                3 -> PresetReverb.PRESET_LARGEROOM
                4 -> PresetReverb.PRESET_MEDIUMHALL
                5 -> PresetReverb.PRESET_LARGEHALL
                6 -> PresetReverb.PRESET_PLATE
                else -> PresetReverb.PRESET_NONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Crossfade / Gapless Simulation ---

    fun setCrossfadeEnabled(enabled: Boolean) {
        _crossfadeEnabled.value = enabled
        if (!enabled) {
            exoPlayer.volume = 1.0f
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        _crossfadeDurationSec.value = seconds.coerceIn(1, 10)
    }

    private fun triggerCrossfadeIn() {
        crossfadeJob?.cancel()
        crossfadeJob = applicationScope.launch {
            val durationMs = _crossfadeDurationSec.value * 1000L
            val steps = 20
            val interval = durationMs / steps
            for (i in 0..steps) {
                if (!isActive) break
                val volume = i.toFloat() / steps
                exoPlayer.volume = volume
                delay(interval)
            }
            exoPlayer.volume = 1.0f
        }
    }

    private fun checkAndTriggerCrossfadeOut(currentPos: Long, totalDuration: Long) {
        if (totalDuration <= 0) return
        val crossfadeDurationMs = _crossfadeDurationSec.value * 1000L
        val remainingMs = totalDuration - currentPos

        if (remainingMs in 1..crossfadeDurationMs) {
            // Smoothly decrease volume towards the end
            val ratio = remainingMs.toFloat() / crossfadeDurationMs
            exoPlayer.volume = ratio.coerceIn(0.0f, 1.0f)
        }
    }

    // --- Tracker Loops ---

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = applicationScope.launch {
            while (isActive) {
                val currentPos = exoPlayer.currentPosition
                val totalDur = exoPlayer.duration.coerceAtLeast(0L)
                _progress.value = currentPos
                _duration.value = totalDur

                if (_crossfadeEnabled.value) {
                    checkAndTriggerCrossfadeOut(currentPos, totalDur)
                }

                delay(200)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        applicationScope.cancel()
        try {
            mediaSession?.release()
            exoPlayer.release()
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            downloadCache.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
