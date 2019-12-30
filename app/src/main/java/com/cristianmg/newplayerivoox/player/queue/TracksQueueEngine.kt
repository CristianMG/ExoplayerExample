package com.cristianmg.newplayerivoox.player.queue

import androidx.annotation.MainThread
import com.cristianmg.newplayerivoox.player.Track

interface TracksQueueEngine {


    suspend fun play(track: Track)

    /***
     * Add to queue one element
     * @param track Track the track to add queue
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    @MainThread
    suspend fun addToQueue(
        track: Track,
        clearOldPlayList: Boolean = false
    )

    /***
     * Add multiple elements to queue
     * @param track Track the track to add queue
     * @param clearOldPlayList Boolean the old playlist should be cleared
     */
    @MainThread
    suspend fun addToQueue(
        tracks: List<Track>,
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

    @MainThread
    suspend fun isEmpty(): Boolean
}