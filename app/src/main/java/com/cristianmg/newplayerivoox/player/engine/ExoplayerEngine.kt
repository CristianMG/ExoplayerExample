package com.cristianmg.newplayerivoox.player.engine

import android.content.Context
import com.cristianmg.newplayerivoox.player.Track
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*
import timber.log.Timber
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource.HttpDataSourceException


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
) : EnginePlayer, Player.EventListener {


    private val player: SimpleExoPlayer by lazy {
        val player = SimpleExoPlayer.Builder(context)
            .build()
        player.addListener(this)
        return@lazy player
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override var currentTrack: Track? = null


    override fun play(track: Track) {
        Timber.d("Executing play about ${track.getName()} with URI ${track.getUri()}")
        currentTrack = track
        coroutineScope.launch {
            player.playWhenReady = true
            player.prepare(getDataSourceFromTRack(track))
        }
    }

    /**
     * Prepare uri and data source to be able to execute
     * @param track Track
     * @return MediaSource
     */
    suspend fun getDataSourceFromTRack(track: Track): MediaSource {

        // Produces DataSource instances through which media data is loaded.
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, "ivoox")
        )

        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(track.getUri())

    }


    override fun onLoadingChanged(isLoading: Boolean) {
        Timber.d("onLoadingChanged:$isLoading")
        callback?.onLoadingChange(isLoading)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Timber.e(error, "onPlayerError")

        if (error.type == ExoPlaybackException.TYPE_SOURCE) {
            val cause = error.sourceException
            if (cause is HttpDataSourceException) {
                val requestDataSpec = cause.dataSpec
                // It's possible to find out more about the error both by casting and by
                // querying the cause.
                if (cause is HttpDataSource.InvalidResponseCodeException) {
                    // Cast to InvalidResponseCodeException and retrieve the response code,
                    // message and headers.
                } else {
                    // Try calling httpError.getCause() to retrieve the underlying cause,
                    // although note that it may be null.
                }
            }
        }
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

    override fun isPlaying(): Boolean = player.isPlaying
    override fun release() =
        player.release()

}