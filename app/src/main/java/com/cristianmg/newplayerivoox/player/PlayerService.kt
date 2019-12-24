package com.cristianmg.newplayerivoox.player


import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.engine.exoplayer.ExoplayerEngine
import com.cristianmg.newplayerivoox.player.queue.TracksQueueEngine
import timber.log.Timber


class PlayerService : Service(), EngineCallback {

    private lateinit var enginePlayer: EnginePlayer
    private lateinit var queue: TracksQueueEngine

    override fun onCreate() {
        super.onCreate()
        enginePlayer.initPlayer()
    }


    /**
     * The current track finish to play
     * @param track Track
     */
    override fun OnFinishPlay(track: Track) {

    }

    /**
     * The track is starting to play
     * @param track Track
     */
    override fun OnPlayTrack(track: Track) {

    }

    /**
     * The engine inform that the loading was changed
     * @param boolean Boolean
     */
    override fun onLoadingChange(boolean: Boolean) {

    }

    /**
     * The engine is always the same
     */
    private fun loadEngines() {
        val engine = ExoplayerEngine(this, this)
        enginePlayer = engine
        queue = engine
    }

    override fun onNotificationChanged(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        Timber.d("onNotificationChanged notificationId$notificationId , ongoing $ongoing")
        if (ongoing) {
            startForeground(notificationId, notification)
        } else {
            stopForeground(false)
        }
    }

    /**
     * Binder options service
     * **/
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder =
        binder

}