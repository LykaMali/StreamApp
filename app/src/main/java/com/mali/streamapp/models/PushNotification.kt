package com.mali.streamapp.models

import android.app.Notification

data class PushNotification(
    val data: NotificationData,
    val to: String
)