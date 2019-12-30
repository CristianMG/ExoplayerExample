package com.cristianmg.newplayerivoox.player.engine.exoplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import com.cristianmg.newplayerivoox.R
import com.cristianmg.newplayerivoox.player.Track
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.queue.TracksQueueEngine
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit
import kotlin.math.pow


/**
 * Implementation of ExoPlayer engine @see https://exoplayer.dev/hello-world.html
 * @property context Context context service
 * @property player SimpleExoPlayer instance of exoplayer service
 * @property background CoroutineScope scope to execute coroutines
 * @constructor
 */
class ExoplayerEngine(
    private val context: Context,
    override var callback: EngineCallback?
) : EnginePlayer, Player.EventListener, PlayerNotificationManager.NotificationListener,
    TracksQueueEngine {

    private val descriptionAdapter = DescriptionAdapter(context)

    private val playerNotificationManager: PlayerNotificationManager by lazy {
        createNotificationChannel()

        PlayerNotificationManager(
            context,
            CHANNEL_ID,
            NOTIFICATION_ID,
            descriptionAdapter,
            this
        ).apply {
            setColorized(true)

        }
    }

    private val player: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(context)
            .build()
            .apply {
                addListener(this@ExoplayerEngine)
            }
    }

    private var playlist =
        mutableListOf<MediaSource>()

    private val background = CoroutineScope(Dispatchers.IO + Job())
    private val adsLoaders = mutableListOf<AdsLoader>()
    private var view: PlayerView? = null

    private val dataSourceFactory: OkHttpDataSourceFactory by lazy {
        val okHttpClient: OkHttpClient = OkHttpClient.Builder()
            .readTimeout(
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS.toLong(),
                TimeUnit.MILLISECONDS
            )
            .connectTimeout(
                DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS.toLong(),
                TimeUnit.MILLISECONDS
            )
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        OkHttpDataSourceFactory(
            okHttpClient,
            Util.getUserAgent(context, context.applicationInfo.name)
        )
    }

    override val currentTrack: Track?
        get() = player.currentTag as? Track

    override suspend fun initPlayer() {
        playerNotificationManager.setPlayer(player)
        player.setHandleAudioBecomingNoisy(true)
        player.setHandleWakeLock(true)

        if (ExoPlayerCache.simpleCache(context).cacheSpace > 6.0.pow(6.0)) {
            Timber.d("The cache is too long starting to evict")
            background.launch { ExoPlayerCache.simpleCache(context).release() }
        }
    }

    override fun setView(pvExoplayer: PlayerView) {
        this.view = pvExoplayer
        pvExoplayer.player = player
    }


    override suspend fun getPlaybackPosition(): Long {
        return 0L
    }


    override suspend fun play() {
        playlist.getOrNull(0)?.let {
            val track = it.getTagAsTrack()
            if (track?.prerollPlayed() == true) {

            }
        }
    }


    override suspend fun addToQueue(
        track: Track,
        clearOldPlayList: Boolean
    ) = addToQueue(listOf(track), clearOldPlayList)

    override suspend fun addToQueue(
        tracks: List<Track>,
        clearOldPlayList: Boolean
    ) {
        if (clearOldPlayList) {
            playlist.clear()
        }
        addItems(*tracks.toTypedArray())
    }

    private fun addItems(vararg tracks: Track) {
        tracks.forEach {
            playlist.add(getDataSourceFromTrack(it))
        }
    }

    /**
     * Prepare uri and data source to be able to execute
     * @param track Track
     * @return MediaSource
     */
    private fun getDataSourceFromTrack(track: Track): MediaSource {
        // Produces DataSource instances through which media data is loaded
        val mediaSource =
            ProgressiveMediaSource.Factory(/*CacheDataSourceFactory(ExoPlayerCache.simpleCache(context), */
                dataSourceFactory/*)*/
            )
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
                    background.launch {
                        val hasError = callback?.checkPreconditions(track)
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
    override suspend fun next() = player.next()
    override suspend fun hasNext(): Boolean = player.hasNext()
    override suspend fun clear() = playlist.clear()
    override suspend fun isEmpty(): Boolean = playlist.isEmpty()

    override suspend fun release() {
        playerNotificationManager.setPlayer(null)
        player.release()
        adsLoaders.forEach {
            it.release()
        }
        adsLoaders.clear()
    }

    object ExoPlayerCache {
        private var cache: SimpleCache? = null
        fun simpleCache(context: Context): SimpleCache {
            if (cache == null) {

                val directory = File(context.cacheDir, "media")
                if (!directory.exists())
                    directory.mkdir()

                cache = SimpleCache(
                    directory,
                    NoOpCacheEvictor(),
                    ExoDatabaseProvider(context)
                )

                Timber.d("Initializing cache exoplayer")
            }
            return cache!!
        }
    }

    companion object {
        private const val CHANNEL_ID = "1001"
        private const val NOTIFICATION_CHANEL_NAME = "Notificaciones"
        const val NOTIFICATION_ID = R.id.notificationId
    }

}