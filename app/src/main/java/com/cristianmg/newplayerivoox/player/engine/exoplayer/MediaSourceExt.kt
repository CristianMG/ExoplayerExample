package com.cristianmg.newplayerivoox.player.engine.exoplayer

import com.cristianmg.newplayerivoox.player.Track
import com.google.android.exoplayer2.source.MediaSource

fun MediaSource.getTagAsTrack(): Track?{
    return tag as? Track
}