/*
package com.cristianmg.newplayerivoox.player.ads

import android.net.Uri
import com.cristianmg.newplayerivoox.player.Track
import com.cristianmg.newplayerivoox.player.ads.model.VastBannerManifest
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher
import com.google.android.exoplayer2.upstream.*
import java.io.IOException
import com.google.android.exoplayer2.upstream.Loader.createRetryAction
import com.google.android.exoplayer2.upstream.Loader.DONT_RETRY_FATAL
import com.google.android.exoplayer2.upstream.Loader.LoadErrorAction
import java.util.*


class IvooxAdsMediaSource(
    private val trackRelated: Track,
    private val manifestDataSourceFactory: DataSource.Factory,
    private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
) : BaseMediaSource(),
    Loader.Callback<ParsingLoadable<VastBannerManifest>> {
    override fun createPeriod(
        id: MediaSource.MediaPeriodId?,
        allocator: Allocator?,
        startPositionUs: Long
    ): MediaPeriod {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private var mediaTransferListener: TransferListener? = null
    private val vastBannerLoader: Loader by lazy {
        Loader("Loader:vastBannerLoader")
    }
    private val manifestDataSource: DataSource by lazy { manifestDataSourceFactory.createDataSource() }

    private val manifestEventDispatcher: EventDispatcher = createEventDispatcher(null)
    private var manifestUri: Uri? = null
    private var manifest: VastBannerManifest? = null
    private var manifestLoadStartTimestamp: Long = 0

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        this.mediaTransferListener = mediaTransferListener
        startLoadingVastBanner()
    }

    private fun startLoadingVastBanner() {
        val loadable = ParsingLoadable(
            manifestDataSource,
            manifestUri, C.DATA_TYPE_MANIFEST, IvooxAdsManifestParser()
        )

        val elapsedRealtimeMs = vastBannerLoader.startLoading(
            loadable, this, loadErrorHandlingPolicy.getMinimumLoadableRetryCount(loadable.type)
        )
        manifestEventDispatcher.loadStarted(loadable.dataSpec, loadable.type, elapsedRealtimeMs)
    }


    override fun onChildSourceInfoRefreshed(
        id: MediaSource.MediaPeriodId,
        mediaSource: MediaSource,
        timeline: Timeline
    ) {
        val newTimeline = SinglePeriodTimeline(
            C.TIME_UNSET,
            isSeekable =
            true,
            isDynamic =
            false,
            isLive =
            true,
            manifest =
            manifest,
            tag
        )

        refreshSourceInfo(newTimeline)
    }


    override fun maybeThrowSourceInfoRefreshError() {
        vastBannerLoader.maybeThrowError()
    }

    override fun releaseSourceInternal() {
        vastBannerLoader.release()
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        // mediaPeriod.
    }

    override fun onLoadCompleted(
        loadable: ParsingLoadable<VastBannerManifest>,
        elapsedRealtimeMs: Long,
        loadDurationMs: Long
    ) {
        manifestEventDispatcher.loadCompleted(
            loadable.dataSpec,
            loadable.uri,
            loadable.responseHeaders,
            loadable.type,
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded()
        )

        manifest = loadable.result
        manifestLoadStartTimestamp = elapsedRealtimeMs - loadDurationMs

        manifest?.let { mf ->
            val mediaSource = ProgressiveMediaSource.Factory(manifestDataSourceFactory)
                .setTag(trackRelated)
                .createMediaSource(mf.uri)
            prepareSource()
            prepareChildSource(MediaSource.MediaPeriodId(UUID.randomUUID()), mediaSource)
        }

    }


    override fun onLoadCanceled(
        loadable: ParsingLoadable<VastBannerManifest>,
        elapsedRealtimeMs: Long,
        loadDurationMs: Long,
        released: Boolean
    ) {
        manifestEventDispatcher.loadCanceled(
            loadable.dataSpec,
            loadable.uri,
            loadable.responseHeaders,
            loadable.type,
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded()
        );
    }

    override fun onLoadError(
        loadable: ParsingLoadable<VastBannerManifest>,
        elapsedRealtimeMs: Long,
        loadDurationMs: Long,
        error: IOException?,
        errorCount: Int
    ): LoadErrorAction {
        val retryDelayMs = loadErrorHandlingPolicy.getRetryDelayMsFor(
            C.DATA_TYPE_MANIFEST, loadDurationMs, error, errorCount
        )

        val loadErrorAction = if (retryDelayMs == C.TIME_UNSET)
            DONT_RETRY_FATAL
        else
            createRetryAction(resetErrorCount = false, retryDelayMs)
        manifestEventDispatcher.loadError(
            loadable.dataSpec,
            loadable.uri,
            loadable.responseHeaders,
            loadable.type,
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded(),
            error,
            !loadErrorAction.isRetry
        )
        return loadErrorAction
    }


}
*/
