package com.cristianmg.newplayerivoox.player.queue

import androidx.annotation.MainThread
import com.cristianmg.newplayerivoox.player.Track

interface TracksQueueEngine {

    /***
     * Add to queue one element
     * @param track Track the track to add queue
     * @param playWhenReady Boolean Player should be execute when ready
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    @MainThread
    suspend fun addToQueue(
        track: Track,
        playWhenReady: Boolean = true,
        clearOldPlayList: Boolean = false
    )


    /***
     * Add multiple elements to queue
     * @param track Track the track to add queue
     * @param playWhenReady Boolean Player should be execute when ready
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    @MainThread
    suspend fun addToQueue(
        tracks: List<Track>,
        playWhenReady: Boolean = true,
        clearOldPlayList: Boolean = false
    )

    @MainThread
    suspend fun isPlaying(): Boolean

    @MainThread
    suspend fun hasNext(): Boolean

    @MainThread
    suspend fun next()

    @MainThread
    suspend fun clear()
}