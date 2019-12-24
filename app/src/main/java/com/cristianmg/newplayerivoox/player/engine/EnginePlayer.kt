package com.cristianmg.newplayerivoox.player.engine

import com.cristianmg.newplayerivoox.player.Track

interface EnginePlayer {
    var currentTrack: Track?
    var callback:EngineCallback?

    fun play(track: Track)
    fun release()
    fun isPlaying(): Boolean
}