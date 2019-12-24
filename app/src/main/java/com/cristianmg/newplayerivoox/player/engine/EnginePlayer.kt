package com.cristianmg.newplayerivoox.player.engine

import com.cristianmg.newplayerivoox.player.Track

interface EnginePlayer {
    val currentTrack: Track?
    var callback:EngineCallback?

    fun initPlayer()
    fun play(track: Track)
    fun play(trackList:List<Track>)

    fun release()
    fun isPlaying(): Boolean
}