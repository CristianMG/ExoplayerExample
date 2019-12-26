package com.cristianmg.newplayerivoox.player.queue

import com.cristianmg.newplayerivoox.player.Track

interface TracksQueueEngine {

    /***
     * Add to queue one element
     * @param track Track the track to add queue
     * @param playWhenReady Boolean Player should be execute when ready
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    fun addToQueue(track: Track, playWhenReady: Boolean = true, clearOldPlayList: Boolean = false)


    /***
     * Add multiple elements to queue
     * @param track Track the track to add queue
     * @param playWhenReady Boolean Player should be execute when ready
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    fun addToQueue(tracks: List<Track>, playWhenReady: Boolean = true, clearOldPlayList: Boolean = false)

    fun isPlaying(): Boolean
    fun hasNext(): Boolean
    fun next()
    fun clear()
}