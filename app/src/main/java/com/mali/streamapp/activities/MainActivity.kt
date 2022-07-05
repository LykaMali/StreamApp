package com.mali.streamapp.activities

import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.mali.streamapp.utils.Definations
import com.mali.streamapp.R
import com.mali.streamapp.databinding.ActivityMainBinding
import com.mali.streamapp.mainapp.App
import com.mali.streamapp.mainapp.BaseActivity
import com.mali.streamapp.models.CallModel
import com.mali.streamapp.models.Customer
import com.mali.streamapp.models.NotificationData
import com.mali.streamapp.models.PushNotification
import com.mali.streamapp.networking.APIManager
import com.mali.streamapp.userprefs.UserPreferences
import com.mali.streamapp.utils.CallListener
import com.mali.streamapp.utils.Definations.ACTION_ACCEPT_CALL
import com.mali.streamapp.utils.Definations.ACTION_DENY_CALL
import com.mali.streamapp.utils.log_e
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

const val TOPIC = "/topics/myTopic"

class MainActivity : BaseActivity(),
    CallListener,
    OneSignal.PostNotificationResponseHandler {

    private lateinit var binding: ActivityMainBinding
    var userRole = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = App()
        app.create()
        app.checkUser(this, baseContext)
        requestPermission()
        callListenerReceiver(this, this)
        log_e("user found ${UserPreferences(this).userUID}")
        //desc: setup notification
        setupNoficationState()
        handleIntent(intent)


        val id = UserPreferences(this).userUID
        if (id != null)
            binding.textViewUserUid.text = id
    }


    fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY
        ),
            Definations.PERMISSION_REQ_ID_RECORD_AUDIO
        ) 
    }

    fun onSubmit(view: View) {
        if (validateArea()) {
            val channelName = findViewById<EditText>(R.id.channel)
            val userRadioButton = findViewById<RadioGroup>(R.id.radioGroup)
            val checked = userRadioButton.checkedRadioButtonId
            val audienceButton = findViewById<RadioButton>(R.id.audienceButton)
            audienceButton.isChecked = false
            userRole = if (checked == audienceButton.id) 0 else 1
            val intent = Intent(this, VideoActivity::class.java)
            intent.putExtra("ChannelName", channelName.text.toString())
            intent.putExtra("UserRole", userRole)
            startActivity(intent)
        } else {
            Toast.makeText(this, "Please enter a valid room name!", Toast.LENGTH_LONG).show()
        }
    }

    fun onAnyCallClicked(view: View) {
        val apiManager = APIManager()
            .instance(this)

        apiManager
            .getCustomer()
            .observe(this, { customer ->
            if (customer != null) {

                apiManager
                    .getCustomers()
                    .observe(this, {
                    if (it.isNullOrEmpty().not()) {
                        var randomCustomer = it.random()
                        if (randomCustomer.uid == customer.uid)
                            randomCustomer = it.random()

                        callForeground(randomCustomer)
                    }
                })
            }
        })
    }

    fun callForeground(customer: Customer) {
        val apiManager = APIManager()
        apiManager.instance(this)
        val userPreferences = UserPreferences(this)
        val callModel = CallModel(
            callee = customer.uid,
            caller = userPreferences.userUID
        )
        apiManager.callUserPhone(callModel)
    }

    fun validateArea(): Boolean {
        val roomName = binding.channel.text
        return roomName.isNullOrEmpty().not()
    }

    override fun onLocalCallSended(callModel: CallModel) {
        runOnUiThread {
            log_e("local call sended the user = ${callModel.callee}")
            showCallerAlert(
                "Merhaba",
                "Bir arama yönlendirdiniz ${callModel.callee}",
                cancellCallback = {
                    APIManager()
                        .instance(this)
                        .denyCall(callModel)
                        .observe(this, {

                        })
                },
                approveCallback = { },
                noCallback = {
                    APIManager()
                        .instance(this)
                        .denyCall(callModel)
                        .observe(this, {

                        })
                }
            )

            // TODO: 13.05.2022 show user
            APIManager()
                .instance(this)
                .getDestinationCustomer(callModel.callee)
                .observe(this, { destinationUser ->
                    PushNotification(
                        data = NotificationData(
                            callModel.id.toString(),
                            "${callModel.caller} tarafından bir arama aldınız. Aramayı görüntülemek için tıklayınız."
                        ),
                        to = destinationUser.fcmToken.toString()
                    ).also {
                        sendNotification(it)
                    }
                })

        }
    }

    override fun onRemoteCallReceived(callModel: CallModel) {
        runOnUiThread {
            log_e("remote call received by user = ${callModel.caller}")
             showCalleeAlert(
                "Merhaba",
                "${callModel.caller} Tarafından bir arama aldınız. Kabul etmek ister misiniz?",
                cancelCallback = {
                     APIManager()
                        .instance(this)
                        .denyCall(callModel)
                        .observe(this, {

                        })
                },
                approveCallback = {
                    APIManager()
                        .instance(this)
                        .approveCall(callModel)
                        .observe(this, {

                        })
                },
                noCallback = {
                    APIManager()
                        .instance(this)
                        .denyCall(callModel)
                        .observe(this, {

                        })
                }
            )
        }
    }

    override fun onRemoteUserDenied(callModel: CallModel) {
        runOnUiThread {
            Toast.makeText(this, "Arama kullanıcı tarafından reddedildi!!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRemoteUserApproveCall(callModel: CallModel) {
        runOnUiThread {
            val intent = Intent(this, VideoActivity::class.java)
            intent.putExtra("ChannelName", getString(R.string.agora_channel_name))
            intent.putExtra("UserRole", 1)
            intent.putExtra("CallerId", callModel.caller)
            intent.putExtra("CalleeId", callModel.callee)
            intent.putExtra("CallId", callModel.id)
            log_e("user approve the call = ${Definations.isAdded}")
            if (!Definations.isAdded) {
                Definations.isAdded = true
                startActivity(intent)
            }
        }
    }

    override fun onCallTimeOut(callModel: CallModel) {
        runOnUiThread {
            Toast.makeText(this, "Arama zaman aşımına uğradı!!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSuccess(success: JSONObject?) {
        log_e("success post notification = $success")
    }

    override fun onFailure(error: JSONObject?) {
        log_e("an post notification error = $error")
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        APIManager()
            .instance(this@MainActivity)
            .sendFirebaseNotification(notification)
    }

    // TODO: 14.05.2022 Handle Intent
    private fun handleIntent(intent: Intent?) {
        val action = intent?.action
        log_e("new intent = $action")
        if (action != null && action != Intent.ACTION_MAIN) {
            when (action.toInt()) {
                ACTION_ACCEPT_CALL -> {
                    if (Definations.mediaPlayer != null) {
                        Definations.mediaPlayer!!.stop()
                        Definations.mediaPlayer = null

                        log_e("pending call intent clicked!!")
                    }
                }
                ACTION_DENY_CALL -> {

                }
            }
        }
    }
}