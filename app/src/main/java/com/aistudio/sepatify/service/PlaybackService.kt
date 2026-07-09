package com.aistudio.sepatify.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.aistudio.sepatify.player.AudioPlayerManager
import org.koin.android.ext.android.inject

class PlaybackService : MediaSessionService() {

    // Retrieve the shared singleton AudioPlayerManager
    private val audioPlayerManager: AudioPlayerManager by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        // Building the MediaSession with 'this' (the Service context) is REQUIRED 
        // for Android to recognize it as a proper foreground media service and 
        // elevate it to the System Status Bar / Media Carousel.
        mediaSession = MediaSession.Builder(this, audioPlayerManager.exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}