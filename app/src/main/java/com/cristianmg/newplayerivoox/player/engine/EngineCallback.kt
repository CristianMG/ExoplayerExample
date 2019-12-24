package com.cristianmg.newplayerivoox.player.engine

import com.cristianmg.newplayerivoox.player.Track


/**
 * To inform who component use engine that differents events
 */
interface EngineCallback {
    fun onLoadingChange(boolean: Boolean)
    fun OnPlayTrack(track: Track)
    fun OnFinishPlay(track: Track)
}