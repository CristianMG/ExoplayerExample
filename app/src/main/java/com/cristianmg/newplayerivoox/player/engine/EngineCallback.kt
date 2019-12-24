package com.cristianmg.newplayerivoox.player.engine

import android.app.Notification
import com.cristianmg.newplayerivoox.player.Track


/**
 * To inform who component use engine that differents events
 */
interface EngineCallback {
    fun onLoadingChange(boolean: Boolean)
    fun OnPlayTrack(track: Track)
    fun OnFinishPlay(track: Track)
    fun onNotificationChanged(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    )
}