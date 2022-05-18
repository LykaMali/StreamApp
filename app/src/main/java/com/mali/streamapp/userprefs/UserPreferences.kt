package com.mali.streamapp.userprefs

import android.content.Context

class UserPreferences(
    context: Context
):Preferences(context, " com.malirita.streamapp.data.local.preferences.userpreferences") {

    // desc: user logged in
    var hasUserLoggedIn by booleanPref("hasUserLoggedIn", false)

    // desc: user
    var userUID by stringPref("userUID", null)

    // desc: fcm
    var fcmToken by stringPref("fcmToken", "")
}