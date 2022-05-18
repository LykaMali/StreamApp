package com.mali.streamapp.utils;

import android.app.Notification
import android.app.PendingIntent;
import android.content.ContentResolver
import android.content.Context
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mali.streamapp.R;
import com.mali.streamapp.activities.MainActivity;
import com.mali.streamapp.networking.APIManager
import android.graphics.BitmapFactory
import android.media.*
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat.BADGE_ICON_SMALL
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat

import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import android.media.AudioManager.STREAM_NOTIFICATION
import com.mali.streamapp.userprefs.UserPreferences
import com.mali.streamapp.utils.Definations.ACTION_ACCEPT_CALL
import com.mali.streamapp.utils.Definations.INCOMING_CALL_CHANNEL_ID
import com.mali.streamapp.utils.Definations.INCOMING_CALL_NOTIFICATION_ID
import android.os.PowerManager
import android.os.Build
import android.view.WindowManager

import android.view.Window
import android.view.WindowManager.LayoutParams.*
import com.mali.streamapp.mainapp.App
import kotlin.math.log

open class FirebaseInstance: FirebaseMessagingService() {

    // TODO: 16.05.2022 Constructors
    // TODO: 16.05.2022 variables
    var isPlaying = false

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (token.isNotEmpty()) {
            APIManager()
                .instance(baseContext)
                .setNewToken(token, baseContext)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        wakeScreenLock()
        val body = message.data["body"]
        if (body.isNullOrEmpty().not()) {
            log_e("message body occured = $body")
            createCustomNotification(body!!)
        } else {
            createCustomNotification("Bir arama aldınız.")
        }
    }

    private fun createCustomNotification(message: String) {
        val notificationManager = NotificationManagerCompat.from(this)
        val channel = NotificationChannelCompat.Builder(INCOMING_CALL_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName("Incoming Calls")
            .setDescription("Incoming audio and video call alerts")
            .build()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL.toString()
        }

        val pendingActivityIntent = PendingIntent.getActivity(applicationContext,
            ACTION_ACCEPT_CALL,
            intent,
                PendingIntent.FLAG_MUTABLE
            )

        notificationManager.createNotificationChannel(channel)
        val notificationBuilder = NotificationCompat.Builder(this, INCOMING_CALL_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setBadgeIconType(BADGE_ICON_SMALL)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Stream App")
            .setContentText(message)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round))
            .setOngoing(true)
            .setAutoCancel(true)
            .addAction(action(notificationManager))
            .setFullScreenIntent(pendingActivityIntent, true)
            //.setSound(Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/ring_ring.mp3"))
            //.setContentIntent(here to pending intent)
        val notification = notificationBuilder.build()
        notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)

        currentRingtone()
    }

    private fun action(notificationManager: NotificationManagerCompat): NotificationCompat.Action {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = ACTION_ACCEPT_CALL.toString()
        }
        val acceptCallIntent = PendingIntent.getActivity(applicationContext,
            ACTION_ACCEPT_CALL,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(applicationContext, R.drawable.btn_video),
            "Göster",
            acceptCallIntent,
        ).build()
    }

    private fun currentRingtone() {
        try {
            if (!isPlaying)
                Definations.mediaPlayer = MediaPlayer.create(this, R.raw.ring_ring)
                Definations.mediaPlayer?.start()
                isPlaying = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun wakeScreenLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn =
            if (Build.VERSION.SDK_INT >= 20) pm.isInteractive else pm.isScreenOn // check if screen is on

        if (!isScreenOn) {
            val wl =
                pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "myApp:notificationLock")
            wl.acquire(3000) //set your time in milliseconds
        }
    }
}
