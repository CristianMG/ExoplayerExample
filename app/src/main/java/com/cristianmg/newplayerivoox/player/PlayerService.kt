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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import java.lang.IllegalStateException

class PlayerService : Service(), EngineCallback {

    private lateinit var enginePlayer: EnginePlayer
    private lateinit var queueEngine: TracksQueueEngine
    var callbackService: ServiceCallback? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)

    val queue: TracksQueue by lazy {
        TracksQueue(queueEngine)
    }

    override fun onCreate() {
        super.onCreate()
        loadEngines()
        enginePlayer.initPlayer()
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
        Timber.d("onNotificationChanged notificationId$notificationId , ongoing $ongoing")
        mainScope.launch {
            if (ongoing) {
                startForeground(notificationId, notification)
            } else {
                stopForeground(!queueEngine.hasNext())
            }
        }
    }

    override suspend fun checkPreconditions(currentTrack: Track?): EnginePlayerError? {
/*        if (currentTrack?.getName() == "3")
            return IllegalStateException("3 is not permitted as a title")*/
        return null
    }

    /**
     * Why we have to do if preconditions were failed
     * @param error Exception
     */
    override fun preconditionsPlaybackFailed(error: EnginePlayerError) {
        callbackService?.preconditionsPlaybackFailed(error)
        mainScope.launch {

            /**
             * We can skip or do another things with the player when the preconditions are failed
             * **/
          /*  if (error is IllegalStateException) {
                if (queueEngine.hasNext()) {
                    queueEngine.next()
                } else {
                    queueEngine.clear()
                }
            }*/
        }
    }


    interface ServiceCallback {
        fun preconditionsPlaybackFailed(illegalStateException: EnginePlayerError)
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