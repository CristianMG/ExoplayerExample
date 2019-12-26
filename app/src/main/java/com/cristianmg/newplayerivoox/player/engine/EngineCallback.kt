package com.cristianmg.newplayerivoox.player.engine

import android.app.Notification
import com.cristianmg.newplayerivoox.player.Track
import java.lang.Exception


/**
 * To inform who component use engine that differents events
 */
interface EngineCallback {
    fun onLoadingChange(boolean: Boolean)
    fun onPlayTrack(track: Track)
    fun onFinishPlay(track: Track)
    fun onNotificationChanged(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    )

    fun preconditionsPlaybackFailed(error: Exception)
    suspend fun shouldStartPlayback(currentTrack: Track?): Exception?
}