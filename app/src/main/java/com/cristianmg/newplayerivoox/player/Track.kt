package com.cristianmg.newplayerivoox.player

import android.graphics.Bitmap
import android.net.Uri

interface Track {


    fun getName(): String
    fun getUri(): Uri
    fun getContentText(): String?
    fun loadLargeIcon(): Bitmap

    /**
     * Get all duration of track in miliseconds
     * @return Long
     */
    fun getDuration(): Long

    fun getAdvertiseInterval(): Long
    fun getIdentifier(): String

    fun prerollPlayed(): Boolean
    fun postrollPlayerd(): Boolean
}