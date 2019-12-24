package com.cristianmg.newplayerivoox.player


import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.cristianmg.newplayerivoox.player.engine.EngineCallback
import com.cristianmg.newplayerivoox.player.engine.EnginePlayer
import com.cristianmg.newplayerivoox.player.engine.ExoplayerEngine


class PlayerService : Service(), EngineCallback {

    private var enginePlayer: EnginePlayer = loadEnginePlayer()

    fun play(track: Track) {
        enginePlayer.play(track)
    }

    /**
     * The current track finish to play
     * @param track Track
     */
    override fun OnFinishPlay(track: Track) {

    }

    /**
     * The track is starting to play
     * @param track Track
     */
    override fun OnPlayTrack(track: Track) {

    }

    /**
     * The engine inform that the loading was changed
     * @param boolean Boolean
     */
    override fun onLoadingChange(boolean: Boolean) {

    }


    /**
     * The engine is always the same
     * @return EnginePlayer
     */
    private fun loadEnginePlayer(): EnginePlayer {
        return ExoplayerEngine(this,this)
    }






    /**
     * Binder options service
     * **/
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder =
        binder

}