package com.mali.streamapp.models

import com.google.firebase.Timestamp
import java.io.Serializable

data class CallModel(
    var id: String? = "",
    var caller: String?,
    var callee: String?,
    var used: Boolean? = false,
    var denied: Boolean? = false,
    var approved: Boolean? = false
    //var created_at: Timestamp = Timestamp.now()
): Serializable
