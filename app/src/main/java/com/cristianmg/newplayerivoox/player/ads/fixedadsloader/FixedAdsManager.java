package com.cristianmg.newplayerivoox.player.ads.fixedadsloader;

import java.util.List;

public interface FixedAdsManager {
    List<Float> getAdCuePoints();

    void adListener(AdEventListener adEventListener);
    void removeListener(AdEventListener adEventListener);

    void release();
}
