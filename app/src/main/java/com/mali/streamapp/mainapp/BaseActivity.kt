package com.mali.streamapp.mainapp

import android.app.PendingIntent
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PersistableBundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleOwner
import com.mali.streamapp.R
import com.mali.streamapp.activities.MainActivity
import com.mali.streamapp.models.CallModel
import com.mali.streamapp.models.Customer
import com.mali.streamapp.networking.APIManager
import com.mali.streamapp.userprefs.UserPreferences
import com.onesignal.OneSignal
import java.util.*
import com.mali.streamapp.utils.*
import com.onesignal.OSFocusHandler
import java.lang.Exception
import kotlin.math.log
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.messaging.FirebaseMessaging
import com.mali.streamapp.activities.TOPIC


abstract class BaseActivity: AppCompatActivity() {

    private lateinit var customer: Customer
    private var callTimer: CountDownTimer? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var lastAlert: AlertDialog
    private lateinit var preferences: UserPreferences
    private var listener: CallListener? = null
    private var lastCallModel: CallModel? = null
    private var isNewRoom = false
    private var isCallFromLocal = false
    private var isTimeOut = false


    open fun callListenerReceiver(
        listener: CallListener?,
        lifecycleOwner: LifecycleOwner,
    ) {

        this.listener = listener
        preferences = UserPreferences(this)
        runOnUiThread {
            APIManager().instance(this)
                .awakeForAnyCall()
                .observe(lifecycleOwner, { callModel ->
                    val isCaller = callModel.caller == preferences.userUID
                    val isCallee = !isCaller
                    isCallFromLocal = isCaller
                    if (isCaller && !isCallee && !isNewRoom) {
                        listener?.onLocalCallSended(callModel)
                        playCallerSong()
                        lastCallModel = callModel
                        isNewRoom = true
                        awakeOnlyCallModel(callModel, lifecycleOwner)
                    } else if (!isCaller && isCallee && !isNewRoom) {
                        listener?.onRemoteCallReceived(callModel)
                        playCalleeSong()
                        lastCallModel = callModel
                        isNewRoom = true
                        awakeOnlyCallModel(callModel, lifecycleOwner)
                    }
                })
        }
        showWhenLockedAndTurnScreenOn()
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        val app = App().instance()!!
        app.create()
        app.currentActivity = this
    }

    open fun showCallerAlert(
        title: String,
        message: String,
        cancellCallback: () -> Unit = {},
        approveCallback: () -> Unit = {},
        noCallback: () -> Unit = {},
    ) {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle(title)
        //set message for alert dialog
        builder.setMessage(message)
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        //performing positive action
        builder.setPositiveButton("İptal et") {dialogInterface, which ->
            cancellCallback.invoke()
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        lastAlert = alertDialog
        if (!alertDialog.isShowing) alertDialog.show()
    }

    open fun showCalleeAlert(
        title: String,
        message: String,
        cancelCallback: () -> Unit = {},
        approveCallback: () -> Unit = {},
        noCallback: () -> Unit = {},
    ) {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle(title)
        //set message for alert dialog
        builder.setMessage(message)
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        //performing positive action
        builder.setPositiveButton("Kabul et") {dialogInterface, which ->
            approveCallback.invoke()
        }
        //performing negative action
        builder.setNegativeButton("Geri çevir") {dialogInterface, which ->
            noCallback.invoke()
        }
        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        lastAlert = alertDialog
        alertDialog.show()
    }

    private fun awakeOnlyCallModel(
        callModel: CallModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        APIManager()
            .instance(this)
            .awakeForCurrentCall(callModel)
            .observe(lifecycleOwner, { newCallModel ->
                check(newCallModel)
            })
    }

    private fun check(callModel: CallModel) {
        if (lastCallModel?.id == callModel.id && !isTimeOut) {
            if (callModel.denied != null && callModel.denied!!) {
                callTimer?.cancel()
                mediaPlayer?.stop()
                lastAlert.hide()
                lastCallModel = null
                //isNewRoom = false
                mediaPlayer = null
                callTimer = null
                listener?.onRemoteUserDenied(callModel)
            }

            if (callModel.approved != null && callModel.approved!!) {
                callTimer?.cancel()
                mediaPlayer?.stop()
                lastAlert.hide()
                lastCallModel = null
                //isNewRoom = false
                mediaPlayer = null
                callTimer = null
                listener?.onRemoteUserApproveCall(callModel)
            }
        }

        if (callModel.used != null && callModel.used!!) {
            isNewRoom = false
        }
    }

    private fun playCalleeSong() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.ring_ring)
        this.mediaPlayer = mediaPlayer
        if (!mediaPlayer.isPlaying) mediaPlayer.start()
        val timer = object: CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                runOnUiThread {
                    if (lastCallModel != null) {
                        APIManager()
                            .instance(this@BaseActivity)
                            .denyCall(lastCallModel!!)
                            .observe(this@BaseActivity, {
                                mediaPlayer.stop()
                                if (lastAlert != null) lastAlert.hide()
                                if (listener != null && lastCallModel != null) listener?.onCallTimeOut(lastCallModel!!)
                            })
                    }
                }
            }
        }
        timer.start()
        callTimer = timer
    }

    private fun playCallerSong() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.arasanaringingtone)
        this.mediaPlayer = mediaPlayer
        if (!mediaPlayer.isPlaying) mediaPlayer.start()
        val timer = object: CountDownTimer(29000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                runOnUiThread {
                    if (lastCallModel != null) {
                        isTimeOut = true
                        APIManager()
                            .instance(this@BaseActivity)
                            .denyCall(lastCallModel!!)
                            .observe(this@BaseActivity, {
                                mediaPlayer.stop()
                                if (lastCallModel != null) listener?.onCallTimeOut(lastCallModel!!)
                                lastAlert.hide()
                                isTimeOut = false
                            })
                    }
                }
            }
        }
        timer.start()
        callTimer = timer
    }

    // TODO: 12.05.2022 OneSignal Notification
    open fun setupNoficationState() {
        /*  val i = Intent()
        i.action = Settings.ACTION_ACCESSIBILITY_SETTINGS
        i.addCategory(Intent.CATEGORY_DEFAULT)
        i.data = Uri.parse("package:" + baseContext.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        baseContext.startActivity(i)

         */


        if (UserPreferences(this).hasUserLoggedIn!!)
        // Enable verbose OneSignal logging to debug issues if needed.
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)

        // OneSignal Initialization
        OneSignal.initWithContext(baseContext)
        OneSignal.setAppId(Definations.ONE_SIGNAL_APP_ID)
        OneSignal.promptLocation()
        OneSignal.promptForPushNotifications()
        val userID = UserPreferences(this).userUID
        if (!userID.isNullOrEmpty()) OneSignal.setExternalUserId(userID)
        // Returns an `OSDeviceState` object with the current immediate device state info
        // Returns an `OSDeviceState` object with the current immediate device state info
        val device = OneSignal.getDeviceState()
        log_e("device state = ${device?.pushToken}")
        if (device != null) {
            //Get the OneSignal Push Player Id
            val userId = device.userId

            //Get device's push token identifier

            //Get device's push token identifier
            val getPushToken = device.userId

            //Get whether notifications are enabled on the device at the app level

            //Get whether notifications are enabled on the device at the app level
            val areNotificationsEnabled = device.areNotificationsEnabled()

            //The device's push subscription status

            //The device's push subscription status
            val isSubscribed = device.isSubscribed

            //Returns value of pushDisabled method

            //Returns value of pushDisabled method
            val isPushDisabled = device.isPushDisabled

            UserPreferences(this).fcmToken = userId

            //set user
            APIManager()
                .instance(this)
                .getCustomer()
                .observe(this, {

                    /*
                    if (it.documentID != null && userId != null)
                        APIManager()
                            .instance(this)
                            .updateCustomerFCM(it.documentID!!, userId)
                            .observe(this, {})
                     */

                })
        }

        // TODO: 13.05.2022 Notification Handler
        OneSignal.setNotificationWillShowInForegroundHandler {
            log_e("app background show notification!!") 
        }

        OneSignal.setNotificationOpenedHandler {
            try {
                val notificationData = it.notification.additionalData
                val id = notificationData.getString("id")

                APIManager()
                    .instance(this)
                    .getCallRoom(id)
                    .observe(this, { callModel ->
                        log_e("last call model = $callModel")
                        lastCallModel = callModel
                        val actions = it.notification.actionButtons
                        if (actions.isNullOrEmpty().not()) {
                            val approveAction = actions.first { data -> data.id == "approveButton" }
                            val cancelAction = actions.first { data -> data.id == "cancelButton" }

                            log_e("action detail = ${it.action.actionId}")
                            when (it.action.actionId) {
                                approveAction.id -> {
                                    APIManager()
                                        .instance(this)
                                        .approveCall(callModel)
                                        .observe(this, {

                                        })
                                }

                                cancelAction.id -> {
                                    APIManager()
                                        .instance(this)
                                        .denyCall(callModel)
                                        .observe(this, {

                                        })
                                }
                            }
                        }
                    })
            } catch (e: Exception) {
                e.printStackTrace()
                log_e("an error occured = ${e.localizedMessage}")
            }
        }

        // TODO: 14.05.2022 Firebase Notification
        NotificationManagerCompat.from(this)
            .cancelAll()

        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
        FirebaseMessaging
            .getInstance()
            .token
            .addOnCompleteListener { task ->
                val token = task.result
                val error = task.exception
                if (token != null && error == null) {
                    APIManager()
                        .instance(baseContext)
                        .setNewToken(token, baseContext)
                } else {
                    log_e("an token error occured = ${error?.localizedMessage}")
                }
            }
    }

    // TODO: 17.05.2022 Settings
    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }
}