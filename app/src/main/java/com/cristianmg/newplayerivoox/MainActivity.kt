package com.cristianmg.newplayerivoox

import android.net.Uri
import android.os.Bundle
import com.cristianmg.newplayerivoox.player.Track
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseBindingActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btRandom.setOnClickListener {
            mService?.play(object: Track{
                override fun getName(): String = "example track"
                override fun getUri(): Uri = Uri.parse("https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_5MG.mp3")
            })
        }
    }

}
