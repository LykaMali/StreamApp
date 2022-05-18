package com.mali.streamapp.networking

import com.mali.streamapp.models.PushNotification
import com.mali.streamapp.utils.Definations
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface NotificationAPI {


    @Headers("Authorization: key=${Definations.SERVER_KEY}", "Content-Type:${Definations.CONTENT_TYPE}")
    @POST("fcm/send")
    suspend fun postNotification(
        @Body notification: PushNotification
    ): Response<ResponseBody>
}