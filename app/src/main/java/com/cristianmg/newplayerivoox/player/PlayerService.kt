package com.cristianmg.newplayerivoox.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cristianmg.newplayerivoox.MainActivity
import com.cristianmg.newplayerivoox.R
import java.util.*


class PlayerService : Service() {


    // Binder given to clients
    private val binder = LocalBinder()

    // Random number generator
    private val mGenerator = Random()

    /** method for clients  */
    val randomNumber: Int
        get() = mGenerator.nextInt(100)


    private val pendingIntent: PendingIntent
        get() {

            val notificationIntent = Intent(this, MainActivity::class.java)
            return PendingIntent.getActivity(this, 2, notificationIntent, 0)
        }


    override fun onCreate() {
        super.onCreate()

        val mNotificationManager: NotificationManager? =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Example")
            .setContentText("Example")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val notificationChannel =
                NotificationChannel(CHANNEL_ID, "settings_notification", importance)
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = longArrayOf(0L)
            builder.setChannelId(CHANNEL_ID)
            mNotificationManager?.createNotificationChannel(notificationChannel)
        }

        val mediaActionReceiver = PendingIntent.getBroadcast(this, 1,  Intent("com.cristianmg.newplayerivoox.player.MediaActionReceiver"), PendingIntent.FLAG_UPDATE_CURRENT)
        builder.addAction(android.R.drawable.ic_media_pause,"Pause", mediaActionReceiver)

        startForeground(R.id.notificationId,builder.build())
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissNotification()
    }


    fun dismissNotification() {
        val ns = Context.NOTIFICATION_SERVICE
        val mNotificationManager = getSystemService(ns) as NotificationManager
        mNotificationManager.cancel(R.id.notificationId)
        stopForeground(true)
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    companion object {
        private val CHANNEL_ID = "1001"

    }
}