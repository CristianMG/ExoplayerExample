package com.cristianmg.newplayerivoox.player.engine.exoplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import com.cristianmg.newplayerivoox.R
import com.cristianmg.newplayerivoox.player.Track
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.queue.TracksQueueEngine
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber


/**
 * Implementation of exoplayer engine @see https://exoplayer.dev/hello-world.html
 * @property context Context context service
 * @property player SimpleExoPlayer instance of exoplayer service
 * @property coroutineIoScope CoroutineScope scope to execute coroutines
 * @constructor
 */
class ExoplayerEngine(
    private val context: Context,
    override var callback: EngineCallback?
) : EnginePlayer, Player.EventListener, PlayerNotificationManager.NotificationListener,
    TracksQueueEngine {

    private val player: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(this@ExoplayerEngine)
            }
    }

    private val descriptionAdapter = DescriptionAdapter(context)

    private val playerNotificationManager: PlayerNotificationManager by lazy {
        createNotificationChannel()

        PlayerNotificationManager(
            context,
            CHANNEL_ID,
            NOTIFICATION_ID,
            descriptionAdapter,
            this
        )
    }

    private var concatenatedSource =
        ConcatenatingMediaSource()

    private val coroutineIoScope = CoroutineScope(Dispatchers.IO + Job())

    override val currentTrack: Track?
        get() = player.currentTag as? Track

    override fun initPlayer() {
        playerNotificationManager.setPlayer(player)
        player.prepare(concatenatedSource)
        player.setHandleAudioBecomingNoisy(true)
        player.setHandleWakeLock(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, NOTIFICATION_CHANEL_NAME, importance)
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(0L)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    override suspend fun addToQueue(
        track: Track,
        playWhenReady: Boolean,
        clearOldPlayList: Boolean
    ) = addToQueue(listOf(track), playWhenReady, clearOldPlayList)

    override suspend fun addToQueue(
        tracks: List<Track>,
        playWhenReady: Boolean,
        clearOldPlayList: Boolean
    ) {
        if (clearOldPlayList) {
            concatenatedSource.clear()
        }
        player.playWhenReady = playWhenReady
        addItems(*tracks.toTypedArray())
    }


    private fun addItems(vararg tracks: Track) {
        tracks.forEach {
            concatenatedSource.addMediaSource(getDataSourceFromTrack(it))
        }
    }

    /**
     * Prepare uri and data source to be able to execute
     * @param track Track
     * @return MediaSource
     */
    private fun getDataSourceFromTrack(track: Track): ProgressiveMediaSource {
        // Produces DataSource instances through which media data is loaded.
        val httpDataSourceFactory = DefaultHttpDataSourceFactory(
            Util.getUserAgent(context, context.applicationInfo.name),
            null /* listener */,
            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
            true /* allowCrossProtocolRedirects */
        )
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            null,
            httpDataSourceFactory
        )

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .setTag(track)
            .createMediaSource(track.getUri())

        /***
         * Check conditions to allow user listen audios
         */
        mediaSource.addEventListener(Handler(), object : MediaSourceEventListener {
            override fun onLoadStarted(
                windowIndex: Int,
                mediaPeriodId: MediaSource.MediaPeriodId?,
                loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
                mediaLoadData: MediaSourceEventListener.MediaLoadData?
            ) {
                /***
                 * Check preconditions to playback audio
                 */
                try {
                    coroutineIoScope.launch {
                        val hasError = callback?.shouldStartPlayback(track)
                        hasError?.let {
                            callback?.preconditionsPlaybackFailed(it)
                        }
                    }

                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        })

        return mediaSource
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        Timber.d("onTimelineChanged")
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.d("onLoadingChanged:$isLoading")
        callback?.onLoadingChange(isLoading)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Timber.e(error, "onPlayerError")
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        callback?.onNotificationChanged(notificationId, notification, ongoing)
    }

    override suspend fun isPlaying(): Boolean = player.isPlaying
    override fun release() {
        playerNotificationManager.setPlayer(null)
        player.release()
    }

    override suspend fun next() = player.next()

    override suspend fun hasNext(): Boolean = player.hasNext()

    override suspend fun clear() = concatenatedSource.clear()


    companion object {
        private const val CHANNEL_ID = "1001"
        private const val NOTIFICATION_CHANEL_NAME = "Notificaciones"
        const val NOTIFICATION_ID = R.id.notificationId
    }


}