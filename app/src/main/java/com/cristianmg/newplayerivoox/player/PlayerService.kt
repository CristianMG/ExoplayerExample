package com.cristianmg.newplayerivoox.player

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.engine.EnginePlayerError
import com.cristianmg.newplayerivoox.player.engine.exoplayer.ExoplayerEngine
import com.cristianmg.newplayerivoox.player.queue.TracksQueue
import com.cristianmg.newplayerivoox.player.queue.TracksQueueEngine
import com.google.android.exoplayer2.ui.PlayerView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class PlayerService : Service(), EngineCallback {

    private lateinit var enginePlayer: EnginePlayer
    private lateinit var queueEngine: TracksQueueEngine
    var callbackService: ServiceCallback? = null
    private val engineScope = CoroutineScope(Dispatchers.Main)
    private val background = CoroutineScope(Dispatchers.IO + Job())

    val queue: TracksQueue by lazy {
        TracksQueue(queueEngine)
    }

    override fun onCreate() {
        super.onCreate()
        loadEngines()
        engineScope.launch { enginePlayer.initPlayer() }
    }

    fun setView(pvExoplayer: PlayerView) {
        enginePlayer.setView(pvExoplayer)
    }


    /**
     * The engine inform that the loading was changed
     * @param boolean Boolean
     */
    override fun onLoadingChange(boolean: Boolean) {
        Timber.d("onLoadingChange isLoading: $boolean")

    }

    /**
     * The engine is always the same
     */
    private fun loadEngines() {
        val engine = ExoplayerEngine(this, this)
        enginePlayer = engine
        queueEngine = engine
    }

    override fun onNotificationChanged(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        engineScope.launch {
            val isQueueEmpty = queueEngine.isEmpty()
            Timber.d("onNotificationChanged notificationId$notificationId , ongoing $ongoing, isQueueEmpty: $isQueueEmpty")
            if (ongoing) {
                startForeground(notificationId, notification)
            } else {
                stopForeground(isQueueEmpty)
            }
        }
    }

    override suspend fun checkPreconditions(currentTrack: Track?): EnginePlayerError? {
        return null
    }

    /**
     * Why we have to do if preconditions were failed
     * @param error Exception
     */
    override fun preconditionsPlaybackFailed(error: EnginePlayerError) {
        callbackService?.preconditionsPlaybackFailed(error)
        engineScope.launch {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engineScope.launch {
            enginePlayer.release()
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

    interface ServiceCallback {
        fun preconditionsPlaybackFailed(illegalStateException: EnginePlayerError)
    }

}