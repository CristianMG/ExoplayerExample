package com.cristianmg.newplayerivoox.player.ads

import android.net.Uri
import android.os.Looper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ads.AdPlaybackState
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.*


class FixedAdsLoader(
    private val tag: Any,
    private val preroll: FixedAd? = null,
    private val midroll: FixedAd? = null,
    private val postroll: FixedAd? = null,
    private val loader: (suspend (tag: Any, ad: FixedAd) -> Uri?)
) : AdsLoader, Player.EventListener {

    private var player: Player? = null
    private var supportedMimeTypes: List<String> = mutableListOf()
    private var eventListener: AdsLoader.EventListener? = null
    private var adPlaybackState: AdPlaybackState? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO + Job())

    override fun start(
        eventListener: AdsLoader.EventListener,
        adViewProvider: AdsLoader.AdViewProvider?
    ) {
        Assertions.checkState(
            player != null, "Set player using adsLoader.setPlayer before preparing the player."
        )

        this.eventListener = eventListener
        player?.addListener(this)


        adPlaybackState = AdPlaybackState(*getAdGroupTimesUs().toLongArray())
        updateAdPlaybackState()

        if (preroll != null) {
            loadAd(preroll) {
                adPlaybackState = adPlaybackState?.withAdCount(0, 1)
                    ?.withAdUri(0, 0, it)

                updateAdPlaybackState()
            }
        }
    }

    private fun loadAd(fixedAd: FixedAd, closure: (uri: Uri) -> Unit) {
        backgroundScope.launch {
            loader(tag, fixedAd)?.let {
                closure(it)
            } ?: kotlin.run {
                //TODO ad loaded fail
            }
        }
    }

    private fun getAdGroupTimesUs(): List<Long> =
        mutableListOf<Long>().apply {
            preroll?.let {
                add(0)
            }
            midroll?.let {
                add(midroll.progressToFetch ?: 0L * C.MICROS_PER_SECOND)
            }
            postroll?.let {
                add(C.TIME_END_OF_SOURCE)
            }
        }.toList()

    override fun stop() {
        if (player == null)
            return
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        Timber.d("isPlaying -> ${player?.isPlaying} isPlayingAd -> ${player?.isPlayingAd} /// ${player?.currentAdGroupIndex} /// ${player?.currentAdIndexInAdGroup}")
    }

    override fun setPlayer(player: Player?) {
        Assertions.checkState(Looper.getMainLooper() == Looper.myLooper())
        Assertions.checkState(
            player == null || player.applicationLooper == Looper.getMainLooper()
        )
        this.player = player
    }

    override fun handlePrepareError(
        adGroupIndex: Int,
        adIndexInAdGroup: Int,
        exception: IOException?
    ) {
        Timber.e("AdgroupIndex adGroupIndex:$adGroupIndex adIndexInAdGroup:$adIndexInAdGroup exception: $exception")
        if (player == null)
            return

        val uri =
            adPlaybackState?.adGroups?.getOrNull(adGroupIndex)?.uris?.getOrNull(adIndexInAdGroup)
        try {
            handleAdPrepareError(adGroupIndex, adIndexInAdGroup, exception)
        } catch (e: Exception) {
            maybeNotifyInternalError(uri, "handlePrepareError", e)
        }

    }

    private fun maybeNotifyInternalError(uri: Uri?, name: String, cause: Exception) {
        val message = "Internal error in $name"
        Timber.e(cause, message)
        adPlaybackState?.let { aps ->

            // We can't recover from an unexpected error in general, so skip all remaining ads.
            for (i in 0 until aps.adGroupCount) {
                adPlaybackState = aps.withSkippedAdGroup(i)
            }
            updateAdPlaybackState()
            uri?.let {
                eventListener?.onAdLoadError(
                    AdsMediaSource.AdLoadException.createForUnexpected(
                        RuntimeException(
                            message,
                            cause
                        )
                    ),
                    DataSpec(uri)
                )
            }
        }
    }

    private fun handleAdPrepareError(
        adGroupIndex: Int,
        adIndexInAdGroup: Int,
        exception: IOException?
    ) {

    }

    private fun updateAdPlaybackState() {
        eventListener?.onAdPlaybackState(adPlaybackState)
    }

    override fun setSupportedContentTypes(vararg contentTypes: Int) {
        val supportedMimeTypes = mutableListOf<String>()
        for (contentType in contentTypes) {
            when (contentType) {
                C.TYPE_DASH -> supportedMimeTypes.add(MimeTypes.APPLICATION_MPD)
                C.TYPE_HLS -> supportedMimeTypes.add(MimeTypes.APPLICATION_M3U8)
                C.TYPE_OTHER -> supportedMimeTypes.addAll(
                    listOf(
                        MimeTypes.VIDEO_MP4,
                        MimeTypes.VIDEO_WEBM,
                        MimeTypes.VIDEO_H263,
                        MimeTypes.AUDIO_MP4,
                        MimeTypes.AUDIO_MPEG
                    )
                )
                C.TYPE_SS -> {
                    // IMA does not support Smooth Streaming ad media.
                }
            }
        }
        this.supportedMimeTypes = supportedMimeTypes.toList()
    }


    override fun release() {
        player?.removeListener(this)
        player = null
        eventListener = null
        adPlaybackState = null
    }


    /**
     *
     * @property uuid UUID uuid to identifier the ad
     * @property adPosition AdPosition the position ad that indicate where the ad should be loaded
     * @property progressToFetch Int? time to show a midroll audio
     * @constructor
     */
    data class FixedAd(
        val uuid: UUID = UUID.randomUUID(),
        val adPosition: AdPosition,
        val progressToFetch: Long? = null,
        var isPlayed: Boolean = false
    )


    enum class AdPosition {
        PRE_ROLL(), MID_ROLL(), POST_ROLL()
    }

}



