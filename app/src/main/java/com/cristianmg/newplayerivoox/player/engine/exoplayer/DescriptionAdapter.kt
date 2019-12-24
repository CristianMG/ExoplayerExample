package com.cristianmg.newplayerivoox.player.engine.exoplayer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.cristianmg.newplayerivoox.MainActivity
import com.cristianmg.newplayerivoox.R
import com.cristianmg.newplayerivoox.player.Track
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

class DescriptionAdapter(val context: Context) :
    PlayerNotificationManager.MediaDescriptionAdapter {

    private val pendingIntent: PendingIntent
        get() {
            val notificationIntent = Intent(context, MainActivity::class.java)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            return PendingIntent.getActivity(context, 345, notificationIntent, 0)
        }

    private val lruCache: LruCache<String, Bitmap> = LruCache(6)

    fun currentTrack(player: Player): Track? {
        return (player.currentTag as? Track)
    }


    override fun getCurrentContentTitle(player: Player): String =
        currentTrack(player)?.getName() ?: ""


    override fun getCurrentContentText(player: Player): String? =
        currentTrack(player)?.getContentText() ?: ""


    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        return currentTrack(player)?.let {
            lruCache.get(it.getUri().toString()) ?: it.loadLargeIcon()
        } ?: getPlaceHolderBitmap()
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? =
        pendingIntent

    /**
     * Try to get placeholder of cache but it does not find the bitmap is loaded from resources
     * @return Bitmap
     */
    fun getPlaceHolderBitmap(): Bitmap {
        return lruCache.get(KEY_LRU_BITMAP) ?: ContextCompat.getDrawable(
            context,
            R.drawable.exo_icon_circular_play
        )?.toBitmap()!!
            .apply {
                lruCache.put(KEY_LRU_BITMAP, this)
            }
    }

    companion object {
        const val KEY_LRU_BITMAP = "KEY_LRU_BITMAP"
    }
}