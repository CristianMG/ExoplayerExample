package com.cristianmg.newplayerivoox.player.engine

import android.app.Notification
import android.net.Uri
import com.cristianmg.newplayerivoox.player.Track


/**
 * To inform who component use engine that differents events
 */
interface EngineCallback {
    fun onLoadingChange(boolean: Boolean)
    fun onNotificationChanged(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    )

    fun preconditionsPlaybackFailed(error: EnginePlayerError)

    /**
     * This function avoid to check if any track is valid to playing
     * @param currentTrack Track? track that will be palyed
     * @return Exception? error if there is
     */
    suspend fun checkPreconditions(currentTrack: Track?): EnginePlayerError?
}