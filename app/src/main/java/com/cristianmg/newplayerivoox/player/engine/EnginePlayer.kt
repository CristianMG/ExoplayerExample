package com.cristianmg.newplayerivoox.player.engine

import androidx.annotation.MainThread
import com.cristianmg.newplayerivoox.player.Track
import com.google.android.exoplayer2.ui.PlayerView

interface EnginePlayer {
    val currentTrack: Track?
    var callback: EngineCallback?

    @MainThread
    suspend fun initPlayer()

    @MainThread
    suspend fun release()

    @MainThread
    suspend fun getPlaybackPosition():Long

    fun setView(pvExoplayer: PlayerView)
}