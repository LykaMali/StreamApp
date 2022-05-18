package com.mali.streamapp.mainapp

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.auth.User
import com.google.firebase.messaging.FirebaseMessaging
import com.mali.streamapp.models.Customer
import com.mali.streamapp.networking.APIManager
import com.mali.streamapp.userprefs.UserPreferences
import com.mali.streamapp.utils.Definations
import com.mali.streamapp.utils.log_e

class App: Application() {

    // TODO: 11.05.2022 instance
    open fun instance() = appInstance

    // TODO: 11.05.2022 Variables
    private var appInstance: App? = null
    private lateinit var userPreferences: UserPreferences
    private lateinit var apiManager: APIManager

    var isInCall = false
    var isRinging = false
    var isIncomingCall = false
    var willShowCallHistory = false
    var isCheckingNotReadNotifications = false

    var currentActivity: BaseActivity? = null

    // TODO: 11.05.2022 Functions
    fun create() {
        appInstance = this

        //desc: firebase config
        FirebaseApp.initializeApp(baseContext)

        //desc: check user
        apiManager = APIManager().instance(this)
    }

    fun context(): Context = baseContext

    // TODO: 11.05.2022 Observers


    // TODO: 11.05.2022 App Actions
    fun checkUser(lifecycleOwner: LifecycleOwner, context: Context) {
        userPreferences = UserPreferences(context)
        val loggedIn = userPreferences.hasUserLoggedIn
        val randomString = Definations.getRandomString(15)
        if (!loggedIn!!) {
            val customer = Customer(
                uid = randomString,
                name = "User-${Definations.getRandomString(3)}",
                surname = ""
            )
            Log.e("App", "create user $customer")
            apiManager.createUser(customer).observe(lifecycleOwner, {
                userPreferences.hasUserLoggedIn = it
                userPreferences.userUID = randomString

                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnCompleteListener { task ->
                        val token = task.result
                        val error = task.exception
                        if (token != null && error == null) {
                            log_e("an new token occured = $token")
                            APIManager()
                                .instance(context)
                                .setNewToken(token, context)
                        } else {
                            log_e("an token error occured = ${error?.localizedMessage}")
                        }
                    }
            })
        }
    }
}