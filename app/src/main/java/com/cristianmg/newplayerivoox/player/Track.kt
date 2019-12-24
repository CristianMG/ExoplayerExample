package com.cristianmg.newplayerivoox.player

import android.graphics.Bitmap
import android.net.Uri

interface Track {
    fun getName(): String
    fun getUri(): Uri
    fun getContentText(): String?
    fun loadLargeIcon(): Bitmap
}