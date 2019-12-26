package com.cristianmg.newplayerivoox

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.cristianmg.newplayerivoox.player.PlayerService
import com.cristianmg.newplayerivoox.player.Track
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.lang.Exception

class MainActivity : BaseBindingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Timber.plant(Timber.DebugTree())


        btRandom.setOnClickListener {

            mService?.queue?.addToQueue(
                listOf(
                    getTrack(
                        "1",
                        "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                    ),
                    getTrack(
                        "2",
                        "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                    ),
                    getTrack(
                        "3",
                        "https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"
                    )
                    //getTrack("4","https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3"),
                    //getTrack("5","https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3")
                )
            )

        }
    }

    override fun onServiceBinder() {
        mService?.callbackService = object : PlayerService.ServiceCallback {
            override fun preconditionsPlaybackFailed(illegalStateException: Exception) {
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

            override fun getContentText(): String? = "Ejemplo contenido"

            override fun loadLargeIcon(): Bitmap =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.spotify)?.toBitmap()!!

            override fun getName(): String = name
            override fun getUri(): Uri =
                Uri.parse(url)
        }
    }

}
