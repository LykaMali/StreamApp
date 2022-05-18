package com.mali.streamapp.models

import java.io.Serializable

data class Customer(
    var uid: String?,
    var name: String?,
    var surname: String?,
    var fcmToken: String? = "",
    var documentID: String? = ""
): Serializable
