package com.cristianmg.newplayerivoox.player.engine

import androidx.annotation.MainThread
import com.cristianmg.newplayerivoox.player.Track

interface EnginePlayer {
    val currentTrack: Track?
    var callback: EngineCallback?

    @MainThread
    suspend fun initPlayer()

    @MainThread
    suspend fun release()
}