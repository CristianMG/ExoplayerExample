package com.cristianmg.newplayerivoox

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.cristianmg.newplayerivoox.player.PlayerService
import com.cristianmg.newplayerivoox.player.Track
import com.cristianmg.newplayerivoox.player.engine.EnginePlayerError
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : BaseBindingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())

        btRandom.setOnClickListener {
            mService?.queue?.play(
                getTrack(
                    "1",
                    "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                )
            )
            /*      mService?.queue?.addToQueue(
                      listOf(
                          getTrack(
                              "1",
                              "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                          ),
                          getTrack(
                              "2",
                              "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
                          ),
                          getTrack(
                              "3",
                              "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                          )
                          //getTrack("4","https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"),
                          //getTrack("5","https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3")
                    */  //)
            // )
        }
    }

    override fun onServiceBinder() {
        mService?.setView(pvExoplayer)
        mService?.callbackService = object : PlayerService.ServiceCallback {

            override fun preconditionsPlaybackFailed(illegalStateException: EnginePlayerError) {
                Snackbar.make(
                    rlContent,
                    "Error recibido $illegalStateException",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun getTrack(name: String, url: String): Track {

        return object : Track {
            override fun prerollPlayed(): Boolean = true

            override fun postrollPlayerd(): Boolean = true
            override fun getIdentifier(): String = UUID.randomUUID().toString()
            override fun getDuration(): Long = TimeUnit.MINUTES.toMillis(3)
            override fun getAdvertiseInterval(): Long = TimeUnit.SECONDS.toMillis(30)
            override fun getContentText(): String? = "Ejemplo contenido"

            override fun loadLargeIcon(): Bitmap =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.spotify)?.toBitmap()!!

            override fun getName(): String = name
            override fun getUri(): Uri =
                Uri.parse(url)
        }
    }

}
