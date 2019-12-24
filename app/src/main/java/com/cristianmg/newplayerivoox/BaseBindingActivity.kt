package com.cristianmg.newplayerivoox

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cristianmg.newplayerivoox.player.PlayerService

abstract class BaseBindingActivity : AppCompatActivity() {

    var mService: PlayerService? = null
    private var mBound: Boolean = false
    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                mService?.stopForeground(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Intent(this, PlayerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as PlayerService.LocalBinder
            mService = binder.getService()
            mBound = true
            Log.d("MainActivity", "ServiceConnected")
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            Log.d("MainActivity", "ServiceDisconnected")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
        unbindService(connection)
        mBound = false
        mService = null
        unregisterReceiver(receiver)
    }
}