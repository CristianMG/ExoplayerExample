/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cristianmg.newplayerivoox.player.ads.fixedadsloader;

import android.content.Context;
import android.net.Uri;
import android.os.Looper;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.cristianmg.newplayerivoox.player.ads.fixedadsloader.error.AdsLoadException;
import com.cristianmg.newplayerivoox.player.ads.fixedadsloader.error.AdsLoadGroupException;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ads.AdPlaybackState;
import com.google.android.exoplayer2.source.ads.AdPlaybackState.AdState;
import com.google.android.exoplayer2.source.ads.AdsLoader;
import com.google.android.exoplayer2.source.ads.AdsMediaSource.AdLoadException;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link AdsLoader} using the IMA SDK. All methods must be called on the main thread.
 *
 * <p>The player instance that will play the loaded ads must be set before playback using {@link
 * #setPlayer(Player)}. If the ads loader is no longer required, it must be released by calling
 * {@link #release()}.
 */
public final class ImaAdsLoader
        implements Player.EventListener,
        AdsLoader, AdEventListener {

    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.ima");
    }


    private static final boolean DEBUG = false;
    private static final String TAG = "ImaAdsLoader";


    private static final long IMA_DURATION_UNSET = -1L;

    /**
     * Threshold before the end of content at which IMA is notified that content is complete if the
     * player buffers, in milliseconds.
     */
    private static final long END_OF_CONTENT_POSITION_THRESHOLD_MS = 5000;

    /**
     * The maximum duration before an ad break that IMA may start preloading the next ad.
     */
    private static final long MAXIMUM_PRELOAD_DURATION_MS = 8000;

    private final Timeline.Period period;

    private boolean wasSetPlayerCalled;
    @Nullable
    private Player nextPlayer;
    private Object pendingAdRequestContext;
    private List<String> supportedMimeTypes;
    @Nullable
    private EventListener eventListener;
    @Nullable
    private Player player;
    private int lastVolumePercentage;

    private boolean initializedAdsManager;
    private Pair<AdLoadException, Uri> pendingAdLoadError;
    private Timeline timeline;
    private long contentDurationMs;
    private AdPlaybackState adPlaybackState;

    private PlaybackProgressUpdate lastAdProgress;
    private PlaybackProgressUpdate lastContentProgress;
    private AdEventListener adEventListener;
    // Fields tracking IMA's state.

    /**
     * The expected ad group index that IMA should load next.
     */
    private int expectedAdGroupIndex;
    /**
     * The index of the current ad group that IMA is loading.
     */
    private int adGroupIndex;

    /**
     * Whether {@link com.google.ads.interactivemedia.v3.api.AdsLoader#contentComplete()} has been
     * called since starting ad playback.
     */
    private boolean sentContentComplete;

    // Fields tracking the player/loader state.

    /**
     * Whether the player is playing an ad.
     */
    private boolean playingAd;
    /**
     * If the player is playing an ad, stores the ad index in its ad group. {@link C#INDEX_UNSET}
     * otherwise.
     */
    private int playingAdIndexInAdGroup;
    /**
     * Whether there's a pending ad preparation error which IMA needs to be notified of when it
     * transitions from playing content to playing the ad.
     */
    private boolean shouldNotifyAdPrepareError;

    /**
     * Stores the pending content position when a seek operation was intercepted to play an ad.
     */
    private long pendingContentPositionMs;


    private FixedAdsManager adsManager;

    /**
     * Creates a new IMA ads loader.
     *
     * @param context    The context.
     * @param adsManager The adsManager to handle points position and load ad
     */
    private ImaAdsLoader(
            Context context,
            @Nullable FixedAdsManager adsManager) {
        Assertions.checkArgument(adsManager != null);
        this.lastAdProgress = null;
        this.lastContentProgress = null;
        this.adEventListener = null;
        this.adsManager = adsManager;
        period = new Timeline.Period();
        pendingContentPositionMs = C.TIME_UNSET;
        adGroupIndex = C.INDEX_UNSET;
        contentDurationMs = C.TIME_UNSET;
        timeline = Timeline.EMPTY;
    }

    /**
     * Requests ads, if they have not already been requested. Must be called on the main thread.
     *
     * <p>Ads will be requested automatically when the player is prepared if this method has not been
     * called, so it is only necessary to call this method if you want to request ads before preparing
     * the player.
     */
    public void requestAds() {
        if (adPlaybackState != null || pendingAdRequestContext != null) {
            // Ads have already been requested.
            return;
        }
        //TODO request ad
    }

    // AdsLoader implementation.
    @Override
    public void setPlayer(@Nullable Player player) {
        Assertions.checkState(Looper.getMainLooper() == Looper.myLooper());
        Assertions.checkState(
                player == null || player.getApplicationLooper() == Looper.getMainLooper());
        nextPlayer = player;
        wasSetPlayerCalled = true;
    }

    @Override
    public void setSupportedContentTypes(@C.ContentType int... contentTypes) {
        List<String> supportedMimeTypes = new ArrayList<>();
        for (@C.ContentType int contentType : contentTypes) {
            if (contentType == C.TYPE_DASH) {
                supportedMimeTypes.add(MimeTypes.APPLICATION_MPD);
            } else if (contentType == C.TYPE_HLS) {
                supportedMimeTypes.add(MimeTypes.APPLICATION_M3U8);
            } else if (contentType == C.TYPE_OTHER) {
                supportedMimeTypes.addAll(
                        Arrays.asList(
                                MimeTypes.VIDEO_MP4,
                                MimeTypes.VIDEO_WEBM,
                                MimeTypes.VIDEO_H263,
                                MimeTypes.AUDIO_MP4,
                                MimeTypes.AUDIO_MPEG));
            } else if (contentType == C.TYPE_SS) {
                // IMA does not support Smooth Streaming ad media.
            }
        }
        this.supportedMimeTypes = Collections.unmodifiableList(supportedMimeTypes);
    }


    @Override
    public void start(EventListener eventListener, AdViewProvider adViewProvider) {
        Assertions.checkState(
                wasSetPlayerCalled, "Set player using adsLoader.setPlayer before preparing the player.");
        player = nextPlayer;
        if (player == null) {
            return;
        }
        this.eventListener = eventListener;
        lastVolumePercentage = 0;
        // lastAdProgress = null;
        //lastContentProgress = null;

        player.addListener(this);
        maybeNotifyPendingAdLoadError();
        if (adPlaybackState != null) {
            // Pass the ad playback state to the player, and resume ads if necessary.
            eventListener.onAdPlaybackState(adPlaybackState);
        } else if (adsManager != null) {
            adPlaybackState = new AdPlaybackState(getAdGroupTimesUs(adsManager.getAdCuePoints()));
            updateAdPlaybackState();
        } else {
            // Ads haven't loaded yet, so request them.
            requestAds();
        }
    }

    @Override
    public void stop() {
        if (player == null) {
            return;
        }
        if (adsManager != null) {
            adPlaybackState =
                    adPlaybackState.withAdResumePositionUs(
                            playingAd ? C.msToUs(player.getCurrentPosition()) : 0);
        }
        lastVolumePercentage = getVolume();
        lastAdProgress = getAdProgress();
        lastContentProgress = getContentProgress();
        player.removeListener(this);
        player = null;
        eventListener = null;
    }

    @Override
    public void release() {
        pendingAdRequestContext = null;
        if (adsManager != null) {
            if (adEventListener != null) {
                adsManager.removeListener(adEventListener);
            }
            adsManager.release();
            adsManager = null;
        }
        pendingAdLoadError = null;
        adPlaybackState = AdPlaybackState.NONE;
        updateAdPlaybackState();
    }

    @Override
    public void handlePrepareError(int adGroupIndex, int adIndexInAdGroup, IOException exception) {
        if (player == null) {
            return;
        }
        try {
            handleAdPrepareError(adGroupIndex, adIndexInAdGroup, exception);
        } catch (Exception e) {
            maybeNotifyInternalError("handlePrepareError", e);
        }
    }


    @Override
    public void onAdLoaded(Integer adGroupIndex, Integer adPosition, Integer allAdsCount, Uri uri) {

        this.adGroupIndex = adGroupIndex;
        int adCount = allAdsCount;

        if (DEBUG) {
            Log.d(TAG, "Loaded ad " + adPosition + " of " + adCount + " in group " + adGroupIndex);
        }
        int oldAdCount = adPlaybackState.adGroups[adGroupIndex].count;
        if (adCount != oldAdCount) {
            if (oldAdCount == C.LENGTH_UNSET) {
                adPlaybackState = adPlaybackState.withAdCount(adGroupIndex, adCount);
                updateAdPlaybackState();
            } else {
                // IMA sometimes unexpectedly decreases the ad count in an ad group.
                Log.w(TAG, "Unexpected ad count in LOADED, " + adCount + ", expected " + oldAdCount);
            }
        }
        if (adGroupIndex != expectedAdGroupIndex) {
            Log.w(
                    TAG,
                    "Expected ad group index "
                            + expectedAdGroupIndex
                            + ", actual ad group index "
                            + adGroupIndex);
            expectedAdGroupIndex = adGroupIndex;
        }

        loadAd(uri);
    }

    @Override
    public void onAdError(AdsLoadException adErrorException, Uri uri) {
        if (DEBUG) {
            Log.d(TAG, "onAdError", adErrorException);
        }
        if (adsManager == null) {
            // No ads were loaded, so allow playback to start without any ads.
            pendingAdRequestContext = null;
            adPlaybackState = new AdPlaybackState();
            updateAdPlaybackState();
        } else if (isAdGroupLoadError(adErrorException)) {
            try {
                handleAdGroupLoadError(adErrorException);
            } catch (Exception e) {
                maybeNotifyInternalError("onAdError", e);
            }
        }
        if (pendingAdLoadError == null) {
            pendingAdLoadError = new Pair<>(AdLoadException.createForAllAds(adErrorException), uri);
        }
        maybeNotifyPendingAdLoadError();
    }

    private boolean isAdGroupLoadError(AdsLoadException adErrorException) {
        return adErrorException instanceof AdsLoadGroupException;
    }

    // ContentProgressProvider implementation.

    private PlaybackProgressUpdate getContentProgress() {
        if (player == null) {
            return lastContentProgress;
        }
        boolean hasContentDuration = contentDurationMs != C.TIME_UNSET;
        long contentPositionMs;
        if (pendingContentPositionMs != C.TIME_UNSET) {
            contentPositionMs = pendingContentPositionMs;
            expectedAdGroupIndex =
                    adPlaybackState.getAdGroupIndexForPositionUs(C.msToUs(contentPositionMs));
        } else if (!playingAd && hasContentDuration) {
            contentPositionMs = player.getCurrentPosition();
            // Update the expected ad group index for the current content position. The update is delayed
            // until MAXIMUM_PRELOAD_DURATION_MS before the ad so that an ad group load error delivered
            // just after an ad group isn't incorrectly attributed to the next ad group.
            int nextAdGroupIndex =
                    adPlaybackState.getAdGroupIndexAfterPositionUs(
                            C.msToUs(contentPositionMs), C.msToUs(contentDurationMs));
            if (nextAdGroupIndex != expectedAdGroupIndex && nextAdGroupIndex != C.INDEX_UNSET) {
                long nextAdGroupTimeMs = C.usToMs(adPlaybackState.adGroupTimesUs[nextAdGroupIndex]);
                if (nextAdGroupTimeMs == C.TIME_END_OF_SOURCE) {
                    nextAdGroupTimeMs = contentDurationMs;
                }
                if (nextAdGroupTimeMs - contentPositionMs < MAXIMUM_PRELOAD_DURATION_MS) {
                    expectedAdGroupIndex = nextAdGroupIndex;
                }
            }
        } else {
            return PlaybackProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        long contentDurationMs = hasContentDuration ? this.contentDurationMs : IMA_DURATION_UNSET;
        return new PlaybackProgressUpdate(contentPositionMs, contentDurationMs);
    }

    // VideoAdPlayer implementation.


    private PlaybackProgressUpdate getAdProgress() {
        if (player == null) {
            return lastAdProgress;
        } else if (playingAd) {
            long adDuration = player.getDuration();
            return adDuration == C.TIME_UNSET ? PlaybackProgressUpdate.VIDEO_TIME_NOT_READY
                    : new PlaybackProgressUpdate(player.getCurrentPosition(), adDuration);
        } else {
            return PlaybackProgressUpdate.VIDEO_TIME_NOT_READY;
        }
    }

    private int getVolume() {
        if (player == null) {
            return lastVolumePercentage;
        }

        Player.AudioComponent audioComponent = player.getAudioComponent();
        if (audioComponent != null) {
            return (int) (audioComponent.getVolume() * 100);
        }

        // Check for a selected track using an audio renderer.
        TrackSelectionArray trackSelections = player.getCurrentTrackSelections();
        for (int i = 0; i < player.getRendererCount() && i < trackSelections.length; i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_AUDIO && trackSelections.get(i) != null) {
                return 100;
            }
        }
        return 0;
    }


    private void loadAd(Uri uri) {
        try {
            if (DEBUG) {
                Log.d(TAG, "loadAd in ad group " + adGroupIndex);
            }

            if (adGroupIndex == C.INDEX_UNSET) {
                Log.w(
                        TAG,
                        "Unexpected loadAd without LOADED event; assuming ad group index is actually "
                                + expectedAdGroupIndex);
                adGroupIndex = expectedAdGroupIndex;
            }
            int adIndexInAdGroup = getAdIndexInAdGroupToLoad(adGroupIndex);
            if (adIndexInAdGroup == C.INDEX_UNSET) {
                Log.w(TAG, "Unexpected loadAd in an ad group with no remaining unavailable ads");
                return;
            }
            adPlaybackState =
                    adPlaybackState.withAdUri(adGroupIndex, adIndexInAdGroup, uri);
            updateAdPlaybackState();
        } catch (Exception e) {
            maybeNotifyInternalError("loadAd", e);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Player.TimelineChangeReason int reason) {
        if (timeline.isEmpty()) {
            // The player is being reset or contains no media.
            return;
        }
        Assertions.checkArgument(timeline.getPeriodCount() == 1);
        this.timeline = timeline;
        long contentDurationUs = timeline.getPeriod(0, period).durationUs;
        contentDurationMs = C.usToMs(contentDurationUs);
        if (contentDurationUs != C.TIME_UNSET) {
            adPlaybackState = adPlaybackState.withContentDurationUs(contentDurationUs);
        }
        if (!initializedAdsManager && adsManager != null) {
            initializedAdsManager = true;
            initializeAdsManager();
        }
        onPositionDiscontinuity(Player.DISCONTINUITY_REASON_INTERNAL);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, @Player.State int playbackState) {
        if (adsManager == null) {
            return;
        }

        if (playbackState == Player.STATE_BUFFERING
                && playWhenReady) {
            checkForContentComplete();
        } else if (playbackState == Player.STATE_ENDED) {
            if (DEBUG) {
                Log.d(TAG, "VideoAdPlayerCallback.onEnded in onPlayerStateChanged");
            }
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        //TODO inform error
    }

    @Override
    public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
        if (adsManager == null) {
            return;
        }
        Assertions.checkArgument(player != null, "The player cannot be null");

        if (!playingAd && !player.isPlayingAd()) {
            checkForContentComplete();
            if (sentContentComplete) {
                for (int i = 0; i < adPlaybackState.adGroupCount; i++) {
                    if (adPlaybackState.adGroupTimesUs[i] != C.TIME_END_OF_SOURCE) {
                        adPlaybackState = adPlaybackState.withSkippedAdGroup(i);
                    }
                }
                updateAdPlaybackState();
            } else if (!timeline.isEmpty()) {
                long positionMs = player.getCurrentPosition();
                timeline.getPeriod(0, period);
                int newAdGroupIndex = period.getAdGroupIndexForPositionUs(C.msToUs(positionMs));
                if (newAdGroupIndex != C.INDEX_UNSET) {
                    pendingContentPositionMs = positionMs;
                    if (newAdGroupIndex != adGroupIndex) {
                        shouldNotifyAdPrepareError = false;
                    }
                }
            }
        }
    }

    // Internal methods.

    private void initializeAdsManager() {
        // Skip ads based on the start position as required.
        Assertions.checkArgument(player != null, "Player cannot be null");

        long[] adGroupTimesUs = getAdGroupTimesUs(adsManager.getAdCuePoints());
        long contentPositionMs = player.getContentPosition();
        int adGroupIndexForPosition =
                adPlaybackState.getAdGroupIndexForPositionUs(C.msToUs(contentPositionMs));
        if (adGroupIndexForPosition > 0 && adGroupIndexForPosition != C.INDEX_UNSET) {
            // Skip any ad groups before the one at or immediately before the playback position.
            for (int i = 0; i < adGroupIndexForPosition; i++) {
                adPlaybackState = adPlaybackState.withSkippedAdGroup(i);
            }
            // Play ads after the midpoint between the ad to play and the one before it, to avoid issues
            // with rounding one of the two ad times.
            long adGroupForPositionTimeUs = adGroupTimesUs[adGroupIndexForPosition];
            long adGroupBeforeTimeUs = adGroupTimesUs[adGroupIndexForPosition - 1];
            double midpointTimeUs = (adGroupForPositionTimeUs + adGroupBeforeTimeUs) / 2d;
        }

        if (adGroupIndexForPosition != C.INDEX_UNSET && hasMidrollAdGroups(adGroupTimesUs)) {
            // Provide the player's initial position to trigger loading and playing the ad.
            pendingContentPositionMs = contentPositionMs;
        }

        updateAdPlaybackState();
        if (DEBUG) {
            Log.d(TAG, "Initialized ads loader");
        }
    }


    private void handleAdGroupLoadError(AdsLoadException error) {
        int adGroupIndex =
                this.adGroupIndex == C.INDEX_UNSET ? expectedAdGroupIndex : this.adGroupIndex;
        if (adGroupIndex == C.INDEX_UNSET) {
            // Drop the error, as we don't know which ad group it relates to.
            return;
        }
        AdPlaybackState.AdGroup adGroup = adPlaybackState.adGroups[adGroupIndex];
        if (adGroup.count == C.LENGTH_UNSET) {
            adPlaybackState =
                    adPlaybackState.withAdCount(adGroupIndex, Math.max(1, adGroup.states.length));
            adGroup = adPlaybackState.adGroups[adGroupIndex];
        }
        for (int i = 0; i < adGroup.count; i++) {
            if (adGroup.states[i] == AdPlaybackState.AD_STATE_UNAVAILABLE) {
                if (DEBUG) {
                    Log.d(TAG, "Removing ad " + i + " in ad group " + adGroupIndex);
                }
                adPlaybackState = adPlaybackState.withAdLoadError(adGroupIndex, i);
            }
        }
        updateAdPlaybackState();
        if (pendingAdLoadError == null) {
            pendingAdLoadError = new Pair<>(AdLoadException.createForAdGroup(error, adGroupIndex), Uri.EMPTY);
        }
        pendingContentPositionMs = C.TIME_UNSET;
    }

    private void handleAdPrepareError(int adGroupIndex, int adIndexInAdGroup, Exception exception) {
        if (DEBUG) {
            Log.d(
                    TAG, "Prepare error for ad " + adIndexInAdGroup + " in group " + adGroupIndex, exception);
        }
        if (adsManager == null) {
            Log.w(TAG, "Ignoring ad prepare error after release");
            return;
        }

        adPlaybackState = adPlaybackState.withAdLoadError(adGroupIndex, adIndexInAdGroup);
        updateAdPlaybackState();
    }

    private void checkForContentComplete() {
        if (contentDurationMs != C.TIME_UNSET && pendingContentPositionMs == C.TIME_UNSET
                && player.getContentPosition() + END_OF_CONTENT_POSITION_THRESHOLD_MS >= contentDurationMs
                && !sentContentComplete) {
            //TODO inform content complete
            if (DEBUG) {
                Log.d(TAG, "adsLoader.contentComplete");
            }
            sentContentComplete = true;
            // After sending content complete IMA will not poll the content position, so set the expected
            // ad group index.
            expectedAdGroupIndex =
                    adPlaybackState.getAdGroupIndexForPositionUs(C.msToUs(contentDurationMs));
        }
    }

    private void updateAdPlaybackState() {
        // Ignore updates while detached. When a player is attached it will receive the latest state.
        if (eventListener != null) {
            eventListener.onAdPlaybackState(adPlaybackState);
        }
    }

    /**
     * Returns the next ad index in the specified ad group to load, or {@link C#INDEX_UNSET} if all
     * ads in the ad group have loaded.
     */
    private int getAdIndexInAdGroupToLoad(int adGroupIndex) {
        @AdState int[] states = adPlaybackState.adGroups[adGroupIndex].states;
        int adIndexInAdGroup = 0;
        // IMA loads ads in order.
        while (adIndexInAdGroup < states.length
                && states[adIndexInAdGroup] != AdPlaybackState.AD_STATE_UNAVAILABLE) {
            adIndexInAdGroup++;
        }
        return adIndexInAdGroup == states.length ? C.INDEX_UNSET : adIndexInAdGroup;
    }

    private void maybeNotifyPendingAdLoadError() {
        if (pendingAdLoadError != null && eventListener != null) {
            eventListener.onAdLoadError(pendingAdLoadError.first, new DataSpec(pendingAdLoadError.second));
            pendingAdLoadError = null;
        }
    }

    private void maybeNotifyInternalError(String name, Exception cause) {
        String message = "Internal error in " + name;
        Log.e(TAG, message, cause);
        // We can't recover from an unexpected error in general, so skip all remaining ads.
        if (adPlaybackState == null) {
            adPlaybackState = AdPlaybackState.NONE;
        } else {
            for (int i = 0; i < adPlaybackState.adGroupCount; i++) {
                adPlaybackState = adPlaybackState.withSkippedAdGroup(i);
            }
        }
        updateAdPlaybackState();
        if (eventListener != null) {
            // Internal error I don't have any general uri like ImaSdk
            eventListener.onAdLoadError(
                    AdLoadException.createForUnexpected(new RuntimeException(message, cause)),
                    new DataSpec(Uri.EMPTY));
        }
    }

    private static long[] getAdGroupTimesUs(List<Float> cuePoints) {
        if (cuePoints.isEmpty()) {
            // If no cue points are specified, there is a preroll ad.
            return new long[]{0};
        }

        int count = cuePoints.size();
        long[] adGroupTimesUs = new long[count];
        int adGroupIndex = 0;
        for (int i = 0; i < count; i++) {
            double cuePoint = cuePoints.get(i);
            if (cuePoint == -1.0) {
                adGroupTimesUs[count - 1] = C.TIME_END_OF_SOURCE;
            } else {
                adGroupTimesUs[adGroupIndex++] = (long) (C.MICROS_PER_SECOND * cuePoint);
            }
        }
        // Cue points may be out of order, so sort them.
        Arrays.sort(adGroupTimesUs, 0, adGroupIndex);
        return adGroupTimesUs;
    }


    private static boolean hasMidrollAdGroups(long[] adGroupTimesUs) {
        int count = adGroupTimesUs.length;
        if (count == 1) {
            return adGroupTimesUs[0] != 0 && adGroupTimesUs[0] != C.TIME_END_OF_SOURCE;
        } else if (count == 2) {
            return adGroupTimesUs[0] != 0 || adGroupTimesUs[1] != C.TIME_END_OF_SOURCE;
        } else {
            // There's at least one midroll ad group, as adGroupTimesUs is never empty.
            return true;
        }
    }

}
