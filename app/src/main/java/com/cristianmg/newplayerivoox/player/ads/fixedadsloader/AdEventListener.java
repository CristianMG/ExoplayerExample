package com.cristianmg.newplayerivoox.player.ads.fixedadsloader;

import android.net.Uri;

import com.cristianmg.newplayerivoox.player.ads.fixedadsloader.error.AdsLoadException;

interface AdEventListener {

    void onAdLoaded(Integer adGroupIndex, Integer adPosition, Integer allAdsCount, Uri uri);

    void onAdError(AdsLoadException adErrorException, Uri uri);
}
