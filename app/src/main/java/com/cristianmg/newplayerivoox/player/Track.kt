package com.cristianmg.newplayerivoox.player

import android.net.Uri

interface Track {
    fun getName(): String
    fun getUri(): Uri
}