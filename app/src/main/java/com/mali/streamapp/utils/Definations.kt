package com.mali.streamapp.utils

import android.media.MediaPlayer
import android.provider.MediaStore

object Definations {
    // TODO: 12.05.2022 OneSignal Variables
    var ONE_SIGNAL_APP_ID = "2ee6c7cb-0724-4f1f-8120-c9ce5e22d194"

    // TODO: 12.05.2022 Agora Variables
    const val APP_ID = "dd17e8fba7454302bbdfe88803a0c9bc"
    const val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    const val PERMISSION_REQ_ID_CAMERA = PERMISSION_REQ_ID_RECORD_AUDIO + 1
    var token = "006dd17e8fba7454302bbdfe88803a0c9bcIACtEsR6nfHWfpuw6nivG86QZtkWOfjlj3V3pb1d+uNKcPJA/HsAAAAAIgA4smrcwu2AYgQAAQDC7YBiAgDC7YBiAwDC7YBiBADC7YBi"

    // TODO: 14.05.2022 Firebase messaging
    const val BASE_URL = "https://fcm.googleapis.com/"
    const val SERVER_KEY = "AAAA3zPqsTw:APA91bGc4yujO_CHYFnYSJYrQiqVd_nbEPqmCn5xANHJh0KeIICE6ozBjqwolK4xM4kmW-2hrFNikEDPrkh5KCln8gfYKv0HR931qsW0LCf71W3RAF5RxaFXGeuXbuPye3H5uo0daVot"
    const val CONTENT_TYPE = "application/json"

    // TODO: 12.05.2022 Common Variables
    var isAdded = false

    // TODO: 12.05.2022 Reachability & Utils
    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    // TODO: 16.05.2022 Call Actions
    const val INCOMING_CALL_CHANNEL_ID = "incoming_call"
    const val INCOMING_CALL_NOTIFICATION_ID = 1

    const val ACTION_ACCEPT_CALL = 101
    const val ACTION_DENY_CALL = 102

    var mediaPlayer: MediaPlayer? = null
}