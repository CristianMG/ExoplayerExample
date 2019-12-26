package com.cristianmg.newplayerivoox.player.engine

import com.cristianmg.newplayerivoox.player.Track

interface EnginePlayer {
    val currentTrack: Track?
    var callback:EngineCallback?

    fun initPlayer()
    fun release()
}