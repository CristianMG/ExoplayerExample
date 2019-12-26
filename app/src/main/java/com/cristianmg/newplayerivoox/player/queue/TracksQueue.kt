package com.cristianmg.newplayerivoox.player.queue

import com.cristianmg.newplayerivoox.player.Track


class TracksQueue(val queueEngine: TracksQueueEngine) {


    fun addToQueue(track: Track) {
        queueEngine.addToQueue(track, clearOldPlayList = true)
    }

    fun addToQueue(track: List<Track>) {
        queueEngine.addToQueue(track, clearOldPlayList = true)
    }

}