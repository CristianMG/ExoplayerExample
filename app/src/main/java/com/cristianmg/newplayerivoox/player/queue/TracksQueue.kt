package com.cristianmg.newplayerivoox.player.queue

import com.cristianmg.newplayerivoox.player.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TracksQueue(private val queueEngine: TracksQueueEngine) {

    private val mainScope = CoroutineScope(Dispatchers.Main)

    fun play(track: Track) {
        mainScope.launch {
            queueEngine.play(track)
        }
    }

    fun addToQueue(track: Track) {
        mainScope.launch {
            queueEngine.addToQueue(track, clearOldPlayList = true)
        }
    }

    fun addToQueue(track: List<Track>) {
        mainScope.launch {
            queueEngine.addToQueue(track, clearOldPlayList = true)
        }
    }

}