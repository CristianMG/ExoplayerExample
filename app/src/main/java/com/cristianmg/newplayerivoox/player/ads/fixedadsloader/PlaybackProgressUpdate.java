package com.cristianmg.newplayerivoox.player.ads.fixedadsloader;

import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

public class PlaybackProgressUpdate {

    public static final PlaybackProgressUpdate VIDEO_TIME_NOT_READY = new PlaybackProgressUpdate(-1L, -1L);

    private float currentTime;
    private float duration;

    public PlaybackProgressUpdate(float currentTime, float duration) {
        this.currentTime = currentTime;
        this.duration = duration;
    }
}
