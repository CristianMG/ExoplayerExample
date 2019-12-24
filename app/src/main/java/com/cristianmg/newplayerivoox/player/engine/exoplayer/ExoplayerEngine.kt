package com.cristianmg.newplayerivoox.player.engine.exoplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.cristianmg.newplayerivoox.R
import com.cristianmg.newplayerivoox.player.Track
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.queue.TracksQueueEngine
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import timber.log.Timber
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.source.ConcatenatingMediaSource

/**
 * Implementation of exoplayer engine @see https://exoplayer.dev/hello-world.html
 * @property context Context context service
 * @property player SimpleExoPlayer instance of exoplayer service
 * @property coroutineScope CoroutineScope scope to execute coroutines
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

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    override val currentTrack: Track?
        get() = player.currentTag as? Track

    override fun initPlayer() {
        playerNotificationManager.setPlayer(player)
        player.prepare(concatenatedSource)
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

    override fun addToQueue(
        track: Track,
        playWhenReady: Boolean,
        clearOldPlayList: Boolean
    ) = addToQueue(listOf(track),playWhenReady,clearOldPlayList)

    override fun addToQueue(
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
    private fun getDataSourceFromTrack(track: Track): MediaSource {
        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, "ivoox")
        )
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .setTag(track)
            .createMediaSource(track.getUri())
    }


    override fun onPositionDiscontinuity(reason: Int) {

    }


    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.d("onLoadingChanged:$isLoading")
        callback?.onLoadingChange(isLoading)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Timber.e(error, "onPlayerError")
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> Timber.d(Player.STATE_IDLE.toString())
            Player.STATE_BUFFERING -> Timber.d(Player.STATE_BUFFERING.toString())
            Player.STATE_READY -> {
                Timber.d(Player.STATE_READY.toString())
                currentTrack?.let {
                    callback?.OnPlayTrack(it)
                }
            }
            Player.STATE_ENDED -> {
                Timber.d(Player.STATE_ENDED.toString())
                currentTrack?.let {
                    callback?.OnFinishPlay(it)
                }
            }
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        callback?.onNotificationChanged(notificationId, notification, ongoing)
    }

    override fun isPlaying(): Boolean = player.isPlaying
    override fun release() {
        playerNotificationManager.setPlayer(null)
        player.release()
    }


    companion object {
        private val CHANNEL_ID = "1001"
        private val NOTIFICATION_CHANEL_NAME = "Notificaciones"
        const val NOTIFICATION_ID = R.id.notificationId
    }


}