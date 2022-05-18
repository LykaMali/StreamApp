package com.mali.streamapp.networking

import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.RemoteMessage.Notification
import com.google.gson.Gson
import com.mali.streamapp.models.CallModel
import com.mali.streamapp.models.Customer
import com.mali.streamapp.models.PushNotification
import com.mali.streamapp.userprefs.UserPreferences
import com.mali.streamapp.utils.log_e
import com.onesignal.*
import java.lang.Exception

open class APIManager {
    // TODO: 11.05.2022 Instance
    fun instance(context: Context): APIManager {
        this.context = context
        return this
    }

    // TODO: 11.05.2022 References
    private var context: Context? = null
    private val customersReference = Firebase.firestore.collection("customers")
    private val callReference = Firebase.firestore.collection("call")

    // TODO: 11.05.2022 Functions 
    fun createUser(customer: Customer): MutableLiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        if (context != null) {
            customersReference
                .add(customer)
                .addOnCompleteListener { completeTask ->
                    liveData.postValue(completeTask.isSuccessful)
                }
        }
        return liveData
    }

    fun getCustomer(): LiveData<Customer> {
        val liveData = MutableLiveData<Customer>()
        if (context != null) {
            val preferences = UserPreferences(context!!)
            customersReference
                .whereEqualTo("uid", preferences.userUID)
                .addSnapshotListener { snapshots, error ->
                    if (snapshots?.documents.isNullOrEmpty().not() && error == null) {
                        val gson = Gson()
                        val json = gson.toJson(snapshots?.documents?.first()?.data)
                        val customer: Customer = gson.fromJson(json, Customer::class.java)
                        customer.documentID = snapshots?.documents?.first()?.id
                        liveData.postValue(customer)
                    } else {
                        preferences.fcmToken = null
                        preferences.hasUserLoggedIn = false
                        preferences.userUID = null
                    }
                }
        }

        return liveData
    }

    fun getDestinationCustomer(id: String?): LiveData<Customer> {
        val liveData = MutableLiveData<Customer>()
        if (!id.isNullOrEmpty()) {
            customersReference
                .whereEqualTo("uid", id)
                .get()
                .addOnCompleteListener { task ->
                    val snapshots = task.result
                    val error = task.exception
                    if (snapshots != null && error == null) {
                        val gson = Gson()
                        if (!snapshots.isEmpty) {
                            val json = gson.toJson(snapshots.documents.first().data)
                            val customer: Customer = gson.fromJson(json, Customer::class.java)
                            customer.documentID = snapshots.documents.first().id
                            liveData.postValue(customer)
                        }
                    }
                }
        }
        return liveData
    }

    fun updateCustomerFCM(documentID: String, fcmToken: String): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        customersReference
            .document(documentID)
            .update("fcmToken", fcmToken)
            .addOnCompleteListener { task ->
                liveData.postValue(task.isSuccessful)
            }
        return liveData
    }

    fun updateCustomerStatus(status: Int, customer: Customer): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        customersReference
            .document(customer.documentID!!)
            .update("status", status)
            .addOnCompleteListener { task ->
                liveData.postValue(task.isSuccessful)
            }
        return liveData
    }

    fun getCustomers(): LiveData<List<Customer>> {
        val liveData = MutableLiveData<List<Customer>>()
        customersReference
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot?.documents != null && !snapshot.documents.isEmpty()) {
                    var datas: MutableList<Customer> = mutableListOf()
                    val gson = Gson()

                    snapshot.documents.forEach { documentSnapshot ->
                        val json = gson.toJson(documentSnapshot.data)
                        val customer: Customer = gson.fromJson(json, Customer::class.java)
                        customer.documentID = documentSnapshot.id
                        datas.add(customer)
                    }

                    datas = datas.filter { it.uid != UserPreferences(context!!).userUID  }.toMutableList()
                    liveData.postValue(datas)
                }
            }
        return liveData
    }

    fun callUserPhone(callModel: CallModel): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        callReference
            .add(callModel)
            .addOnCompleteListener {
                liveData.postValue(it.isSuccessful)
            }
        return liveData
    }

    fun awakeForAnyCall(): LiveData<CallModel> {
        val liveData = MutableLiveData<CallModel>()
        val preferences = UserPreferences(context!!)
        callReference
            .addSnapshotListener { snapShot, error ->
                if (error == null && snapShot?.documents.isNullOrEmpty().not()) {
                    var datas: MutableList<CallModel> = mutableListOf()
                    val gson = Gson()
                    snapShot?.forEach { dataSnapshot ->
                        val json = gson.toJson(dataSnapshot.data)
                        val callModel: CallModel = gson.fromJson(json, CallModel::class.java)
                        callModel.id = dataSnapshot.id
                        if (callModel.used != null && !callModel.used!!) {
                            datas.add(callModel)
                        }
                    }
                    var isBreak = false
                    datas.forEach { callModel ->
                        if (callModel.callee == preferences.userUID || callModel.caller == preferences.userUID && !isBreak) {
                            //desc: this user callee
                            liveData.postValue(callModel)
                            isBreak = true
                        }
                    }
                }
            }
        return liveData
    }

    fun awakeForCurrentCall(callModel: CallModel): LiveData<CallModel> {
        val liveData = MutableLiveData<CallModel>()
        callReference
            .document(callModel.id!!)
            .addSnapshotListener { snapshot, error ->
                val gson = Gson()
                val json = gson.toJson(snapshot?.data)
                if (json.isNullOrEmpty().not()) {
                    val cm: CallModel = gson.fromJson(json, CallModel::class.java)
                    cm.id = snapshot?.id
                    liveData.postValue(cm)
                }
            }
        return liveData
    }

    fun getCallRoom(id: String): LiveData<CallModel> {
        val liveData = MutableLiveData<CallModel>()
        callReference
            .document(id)
            .get()
            .addOnCompleteListener { task ->
                val snapshot = task.result
                val error = task.exception
                val gson = Gson()
                if (snapshot != null && error == null) {
                    val json = gson.toJson(snapshot.data)
                    val cm: CallModel = gson.fromJson(json, CallModel::class.java)
                    cm.id = snapshot.id
                    liveData.postValue(cm)
                }
            }
        return liveData
    }

    fun denyCall(callModel: CallModel): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        callReference
            .document(callModel.id!!)
            .update("denied", true)
            .addOnCompleteListener {
                liveData.postValue(it.isSuccessful)
                setRoomUsed(callModel)
            }
        return liveData
    }

    fun approveCall(callModel: CallModel): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        callReference
            .document(callModel.id!!)
            .update("approved", true)
            .addOnCompleteListener {
                liveData.postValue(it.isSuccessful)
                setRoomUsed(callModel)
            }
        return liveData
    }

    fun setRoomUsed(callModel: CallModel): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        callReference
            .document(callModel.id!!)
            .update("used", true)
            .addOnCompleteListener {
                liveData.postValue(it.isSuccessful)
            }
        return liveData
    }

    fun removeCallModel(callModel: CallModel): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        callReference
            .document(callModel.id!!)
            .delete()
            .addOnCompleteListener {
                liveData.postValue(it.isSuccessful)
            }
        return liveData
    }

    // TODO: 12.05.2022 Notification Send
    fun sendNotification(
        destinationFcmToken: String?,
        subTitle: String,
        message: String,
        handler: OneSignal.PostNotificationResponseHandler,
        additionalData: Any? = null,
        isCallNotification: Boolean = false
    ) {
        if (destinationFcmToken != null) {
            val subtitleJson = mapOf<String, Any>(
                Pair("en", subTitle),
            )
            val contentJson = mapOf<String, Any>(
                Pair("en", message)
            )
            val json = mutableMapOf(
                Pair("subtitle", subtitleJson),
                Pair("contents", contentJson),
                Pair("include_player_ids", listOf(destinationFcmToken)),
                Pair("data", null),
                Pair("buttons", null)
            )
            if (additionalData != null) {
                val newData = additionalData as? String
                val data = mapOf<String, Any>(
                    Pair("id", newData.toString())
                )
                if (!data.values.isNullOrEmpty()) {
                    json["data"] = data
                }
            }

            if (isCallNotification) {
                val approveButton = mapOf(
                    Pair("id", "approveButton"),
                    Pair("text", "Yanıtla"),
                    Pair("icon", null)
                )
                val cancelButton = mapOf(
                    Pair("id", "cancelButton"),
                    Pair("text", "Geri çevir"),
                    Pair("icon", null)
                )

                json["buttons"] = listOf(
                    approveButton, cancelButton
                )
            }

            val newJSON = Gson().toJson(json)
            log_e("newjson = $newJSON")
            OneSignal.postNotification(newJSON, handler)
        }
    }

    suspend fun sendFirebaseNotification(notification: PushNotification): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        try {
            val response = RetrofitInstance.api.postNotification(notification)

            if (response.isSuccessful) {
                val data = Gson().toJson(response)
                log_e("RESPONSE: $data")
            } else {
                log_e("ERROR: ${response.errorBody().toString()}")
            }
        } catch (e: Exception) {
            log_e("an error occured = ${e.localizedMessage}")
            e.printStackTrace()
        }
        return liveData
    }

    // TODO: 14.05.2022 For Token
    open fun setNewToken(token: String, context: Context): LiveData<Boolean> {
        val liveData = MutableLiveData<Boolean>()
        customersReference
            .whereEqualTo("uid", UserPreferences(context = context).userUID)
            .get()
            .addOnCompleteListener { task ->
                val snapshot = task.result
                val exception = task.exception

                if (snapshot != null && exception == null && snapshot.documents.isNotEmpty()) {

                    updateCustomerFCM(snapshot.documents.first().id, token)
                    liveData.postValue(true)
                }
            }
        return liveData
    }
} 
