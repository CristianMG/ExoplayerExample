package com.cristianmg.newplayerivoox

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.cristianmg.newplayerivoox.player.Track
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseBindingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btRandom.setOnClickListener {
            mService?.play(object : Track {

                override fun getContentText(): String? = "Ejemplo contenido"

                override fun loadLargeIcon(): Bitmap =
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.spotify)?.toBitmap()!!

                override fun getName(): String = "example track"
                override fun getUri(): Uri =
                    Uri.parse("https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3")
            })
        }
    }

}
