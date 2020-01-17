package com.cristianmg.newplayerivoox.player.ads

import android.net.Uri
import android.os.Looper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_AD_INSERTION
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ads.AdPlaybackState
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.source.ads.AdsMediaSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.*


/**
 * I have some doubts about the FixedAdsLoader implementation.

The most important.

1) Why adsPlayback has ads groups?
I can understant that this can be a require to implementation the ImaExtension.
To ilustrate better, I have multiple ads, this ads are distributed across the timeline of my track.
It would be correct make a ad group for each ad? or
It would be better split my ads in three groups? for example, preroll,midroll,postroll.

What is the properly option?

I was watching the ImaAdsLoader, in this loader the ads are distributed across the track int the "cuepoints".


 * @property tag Any
 * @property ads List<FixedAd>
 * @property player Player?
 * @property supportedMimeTypes List<String>
 * @property eventListener EventListener?
 * @property adPlaybackState AdPlaybackState?
 * @property backgroundScope CoroutineScope
 * @property currentAdGroupIndex Int
 * @property currentAdIndexInAdGroup Int
 * @property isPlayingAd Boolean
 * @constructor
 */

class FixedAdsLoader(
    private val tag: Any,
    private val ads: List<FixedAd> = mutableListOf()
) : AdsLoader, Player.EventListener {

    private var player: Player? = null
    private var supportedMimeTypes: List<String> = mutableListOf()
    private var eventListener: AdsLoader.EventListener? = null
    private var adPlaybackState: AdPlaybackState? = null
    private val backgroundScope = CoroutineScope(Dispatchers.IO + Job())
    private var wasPlayingAd: Boolean = false
    private var timeline: Timeline? = null
    private var contentDurationMs: Long = C.TIME_UNSET
    private val period: Timeline.Period = Timeline.Period()
    private var skippedBeforePlayback = false

    /**
     * Whether [com.google.ads.interactivemedia.v3.api.AdsLoader.contentComplete] has been
     * called since starting ad playback.
     */
    private var sentContentComplete: Boolean = false

    /** Stores the pending content position when a seek operation was intercepted to play an ad.  */
    private var pendingContentPositionMs: Long = 0
    /** Whether [.getContentProgress] has sent [.pendingContentPositionMs] to IMA.  */
    private var sentPendingContentPositionMs: Boolean = false
    /**
     * Current index ad group
     */
    private val currentAdGroupIndex: Int
        get() = player?.currentAdGroupIndex ?: C.INDEX_UNSET


    /**
     * Current index ad group if it is sound now
     */
    private val currentAdIndexInAdGroup: Int
        get() = player?.currentAdIndexInAdGroup ?: C.INDEX_UNSET

    /**
     * Is currently player playing ad
     */
    private val isPlayingAd: Boolean
        get() = player?.isPlayingAd ?: false

    override fun start(
        eventListener: AdsLoader.EventListener,
        adViewProvider: AdsLoader.AdViewProvider?
    ) {
        Assertions.checkState(
            player != null,
            "Set player using adsLoader.setPlayer before preparing the player."
        )
        this.eventListener = eventListener
        player?.addListener(this)

        adPlaybackState = AdPlaybackState(*getAdGroupTimesUs().toLongArray())
        updateAdPlaybackState()

        /**
         * Added all ads in each position
         */
        ads.forEachIndexed { index, ads ->
            adPlaybackState = adPlaybackState?.withAdCount(index, 1)
            updateAdPlaybackState()
            loadAd(ads) {
                adPlaybackState = adPlaybackState?.withAdUri(index, 0, it)
                updateAdPlaybackState()
            }
        }
    }

    /**
     * This function load ad and call closure with the Uri response
     * @param fixedAd FixedAd
     * @param closure (uri: Uri) -> Unit
     */
    private fun loadAd(fixedAd: FixedAd, closure: (uri: Uri) -> Unit) {
        backgroundScope.launch {
            try {
                loader(tag, fixedAd)?.let {
                    closure(it)
                } ?: kotlin.run {
                    handleAdGroupLoadError(
                        IllegalStateException("Ads loader error"),
                        fixedAd.progressToFetch.toInt()
                    )
                }
            } catch (e: java.lang.Exception) {
                Timber.e(e, "The ad cannot be loaded")
                handleAdGroupLoadError(
                    IllegalStateException("Ads loader error"),
                    fixedAd.progressToFetch.toInt()
                )
            }
        }
    }


    override fun onPositionDiscontinuity(reason: Int) {
        Timber.d("onPositionDiscontinuity reason:$reason")
        if (!isPlayingAd && !wasPlayingAd) {
            checkForContentComplete()
            if (sentContentComplete) {
                Timber.d("Marking ad as listen")
                for (i in 0 until (adPlaybackState?.adGroupCount ?: 0)) {
                    if (adPlaybackState?.adGroupTimesUs?.getOrNull(i) != C.TIME_END_OF_SOURCE) {
                        adPlaybackState = adPlaybackState?.withSkippedAdGroup(i)
                    }
                }
                updateAdPlaybackState()
            }
        } else if (timeline?.isEmpty != true) {
            val positionMs = player!!.currentPosition
            timeline?.getPeriod(0, period)
            val newAdGroupIndex = period.getAdGroupIndexForPositionUs(C.msToUs(positionMs))
            if (newAdGroupIndex != C.INDEX_UNSET) {
                sentPendingContentPositionMs = false
                pendingContentPositionMs = positionMs
            }
        }


        wasPlayingAd = isPlayingAd
    }


    private fun checkForContentComplete() {
        if (contentDurationMs != C.TIME_UNSET && pendingContentPositionMs == C.TIME_UNSET && !sentContentComplete
        ) {
            sentContentComplete = true
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.d("onPlayerStateChanged playWhenReady:$playWhenReady playbackState:$playbackState")
    }

    /**
     * Create de points group with the ads inject in constructor
     * @return List<Long> list with group of ads
     */
    private fun getAdGroupTimesUs(): List<Long> =
        ads.map { it.progressToFetch }


    override fun onTimelineChanged(timeline: Timeline, @Player.TimelineChangeReason reason: Int) {
        if (timeline.isEmpty) {
            // The player is being reset or contains no media.
            return
        }
        Assertions.checkArgument(timeline.periodCount == 1)
        this.timeline = timeline
        val contentDurationUs = timeline.getPeriod(0, period).durationUs
        contentDurationMs = C.usToMs(contentDurationUs)
        if (contentDurationUs != C.TIME_UNSET) {
            adPlaybackState = adPlaybackState?.withContentDurationUs(contentDurationUs)
        }

        if (!skippedBeforePlayback) {
            skippedBeforePlayback = true
            skippedBeforePlayback()
        }
        onPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL)
    }



    private fun skippedBeforePlayback(){
        // Skip ads based on the start position as required.
        val adGroupTimesUs = adPlaybackState?.adGroupTimesUs
        val contentPositionMs = player!!.contentPosition
        val adGroupIndexForPosition =
            adPlaybackState?.getAdGroupIndexForPositionUs(C.msToUs(contentPositionMs))
        if (adGroupIndexForPosition!=null && adGroupTimesUs!=null &&
            adGroupIndexForPosition > 0 && adGroupIndexForPosition != C.INDEX_UNSET) {
            // Skip any ad groups before the one at or immediately before the playback position.
            for (i in 0 until adGroupIndexForPosition) {
                adPlaybackState = adPlaybackState?.withSkippedAdGroup(i)
            }
            // Play ads after the midpoint between the ad to play and the one before it, to avoid issues
            // with rounding one of the two ad times.
            val adGroupForPositionTimeUs = adGroupTimesUs[adGroupIndexForPosition]
            val adGroupBeforeTimeUs = adGroupTimesUs[adGroupIndexForPosition - 1]
            val midpointTimeUs = (adGroupForPositionTimeUs + adGroupBeforeTimeUs) / 2.0
        }
    }
    override fun handlePrepareError(
        adGroupIndex: Int,
        adIndexInAdGroup: Int,
        exception: IOException?
    ) {
        Timber.e(
            exception,
            "handlePrepareError adGroupIndex:$adGroupIndex adIndexInAdGroup:$adIndexInAdGroup"
        )
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
        if (adPlaybackState == null)
            adPlaybackState = AdPlaybackState.NONE

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
        Timber.e(
            exception,
            "handleAdPrepareError: adGroupIndex -> $adGroupIndex,adIndexInAdGroup -> $adIndexInAdGroup"
        )

        adPlaybackState = adPlaybackState?.withAdLoadError(adGroupIndex, adIndexInAdGroup)
        updateAdPlaybackState()
    }

    private fun handleAdGroupLoadError(error: java.lang.RuntimeException, adGroupIndex: Int) {
        adPlaybackState?.adGroups?.getOrNull(adGroupIndex)?.let { adGroup ->
            for (i in 0 until adGroup.count) {
                Timber.d("Marking ad as error loaded groupIndex: $adGroupIndex indexAd:$i")
                adPlaybackState = adPlaybackState?.withAdLoadError(adGroupIndex, i)
            }
        }
        updateAdPlaybackState()
    }

    private fun updateAdPlaybackState() {
        eventListener?.onAdPlaybackState(adPlaybackState)
    }

    override fun stop() {
        if (player == null)
            return
    }

    override fun release() {
        player?.removeListener(this)
        player = null
        eventListener = null
        adPlaybackState = null
    }

    /**
     * Load ad with delay to simulate the petition to server
     * @param tag Any
     * @param ad FixedAd
     * @return Uri?
     */
    private suspend fun loader(tag: Any, ad: FixedAd): Uri? {
        Timber.d("Start to load ad tag:$tag ad$ad")
        Thread.sleep(3000)
        Timber.d("Finish to load ad tag:$tag ad$ad")
        return Uri.parse("https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3")
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

    override fun setPlayer(player: Player?) {
        Assertions.checkState(Looper.getMainLooper() == Looper.myLooper())
        Assertions.checkState(
            player == null || player.applicationLooper == Looper.getMainLooper()
        )
        this.player = player
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
        val progressToFetch: Long,
        var isPlayed: Boolean = false
    )


    enum class AdPosition {
        PRE_ROLL, MID_ROLL, POST_ROLL
    }

}



