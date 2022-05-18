package com.mali.streamapp.activities

import android.graphics.PorterDuff
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import com.mali.streamapp.utils.FileUtil
import com.mali.streamapp.R
import com.mali.streamapp.databinding.VideoActivityBinding
import com.mali.streamapp.mainapp.BaseActivity
import com.mali.streamapp.networking.APIManager
import com.mali.streamapp.utils.Definations
import com.mali.streamapp.utils.log_e
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtm.*
import java.lang.Exception

class VideoActivity : BaseActivity() {

    private lateinit var binding: VideoActivityBinding
    private var channelName: String? = ""
    private var userRole = 0
    private var rtcEngine: RtcEngine? = null

    //desc: RTM Variables
    private lateinit var rtmClient: RtmClient
    private lateinit var callManager: RtmCallManager

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                // Call setupRemoteVideo to set the remote video view after getting uid from the onUserJoined callback.
                setupRemoteVideo(uid)
                Log.e("Joined", "on user joined the room $uid")
                initializeRTM(uid.toString())
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                onRemoteUserLeft()
                Log.e("Left", "on remote user left the room $uid")
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                Log.e("Success", "on join channel success $uid")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = VideoActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra("ChannelName")
        userRole = intent.getIntExtra("UserRole", -1)

        initAgoraEngineAndJoinChannel()
    }

    override fun onDestroy() {
        super.onDestroy()

        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        Definations.isAdded = false
    }

    private fun initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine()

        rtcEngine!!.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        rtcEngine!!.setClientRole(userRole)
        rtcEngine!!.setLogFile(FileUtil.rtmLogFile(this))



        rtcEngine!!.enableVideo()
        rtcEngine!!.enableAudio()

        if (userRole == 1) {
            setupLocalVideo()
        } else {
            val localVideoCanvas = binding.localVideoViewContainer
            localVideoCanvas.isVisible = true
        }

        joinChannel()
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, getString(R.string.app_id), mRtcEventHandler)
        } catch (e: Exception) {
            Log.e("VideoActivity", "an expected error occured ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    private fun setupLocalVideo() {
        val container = binding.localVideoViewContainer
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        surfaceView.setZOrderMediaOverlay(true)
        container.addView(surfaceView)
        rtcEngine!!.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, 0))
    }

    private fun joinChannel() {
        rtcEngine!!.joinChannel(getString(R.string.agora_token), "streamAppChannel", null, 0)
    }

    private fun setupRemoteVideo(uid: Int) {
        val container = binding.remoteVideoViewContainer
        if (container.childCount >= 1)
            return
        val surfaceView = RtcEngine.CreateRendererView(baseContext)
        container.addView(surfaceView)
        rtcEngine!!.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
        surfaceView.tag = uid
    }

    private fun onRemoteUserLeft() {
        val container = binding.remoteVideoViewContainer
        container.removeAllViews()
        Toast.makeText(this, "Kullanıcı odadan ayrıldı!", Toast.LENGTH_LONG).show()
        finish()
    }

    fun onLocalAudioMuteClicked(view: View) {
        val iv = view as ImageView

        if (iv.isSelected) {
            iv.isSelected = false
            iv.clearColorFilter()
        } else {
            iv.isSelected = true
            iv.setColorFilter(resources.getColor(R.color.design_default_color_primary), PorterDuff.Mode.MULTIPLY)
        }

        rtcEngine!!.muteLocalAudioStream(iv.isSelected)
    }

    fun onSwitchCameraClicked(view: View) {
        rtcEngine!!.switchCamera()
    }

    fun onEndCallClicked(view: View) {
        finish()
    }


    fun initializeRTM(userID: String) {
        val appToken = getString(R.string.rtm_access_token)
        val appID = getString(R.string.app_id)
        val apiManager = APIManager().instance(this)
        apiManager.getCustomer().observe(this, {
            rtmClient =
                RtmClient.createInstance(baseContext, appID, rtmClientListener)

            rtmClient.login(null, userID, object : ResultCallback<Void?> {
                override fun onSuccess(aVoid: Void?) {
                    Log.e("VideoActivity", "on success!!! $aVoid")
                }

                override fun onFailure(errorInfo: ErrorInfo) {
                    Log.e("VideoActivity", "an error occuredv $errorInfo")
                }
            })

            callManager = rtmClient.rtmCallManager
            callManager.setEventListener(engineEventListener)

            val localInvitation: LocalInvitation = callManager.createLocalInvitation(userID)
            callManager.sendLocalInvitation(localInvitation, object : ResultCallback<Void?> {
                override fun onSuccess(p0: Void?) {
                    Log.e("VideoActivity 3", "on success local invitation")
                }

                override fun onFailure(p0: ErrorInfo?) {
                    Log.e("VideoActivity 3", "on failture local invitation")
                }
            })
        })
    }

    val rtmClientListener: RtmClientListener = object : RtmClientListener {
        override fun onConnectionStateChanged(p0: Int, p1: Int) {
            log_e("onConnectionStateChanged = $p0, $p1")
        }

        override fun onMessageReceived(p0: RtmMessage?, p1: String?) {
            log_e("onMessageReceived = $p0, $p1")
        }

        override fun onTokenExpired() {
            log_e("onTokenExpired")
        }

        override fun onPeersOnlineStatusChanged(p0: MutableMap<String, Int>?) {
            log_e("onPeersOnlineStatusChanged = $p0")
        }
    }

    var engineEventListener: RtmCallEventListener = object : RtmCallEventListener {
        override fun onLocalInvitationReceivedByPeer(p0: LocalInvitation?) {
            log_e("onLocalInvitationReceivedByPeer = $p0")
        }

        override fun onLocalInvitationAccepted(p0: LocalInvitation?, p1: String?) {
            log_e("onLocalInvitationAccepted = $p0, $p1")
        }

        override fun onLocalInvitationRefused(p0: LocalInvitation?, p1: String?) {
            log_e("onLocalInvitationRefused = $p0, $p1")
        }

        override fun onLocalInvitationCanceled(p0: LocalInvitation?) {
            log_e("onLocalInvitationCanceled = $p0")
        }

        override fun onLocalInvitationFailure(p0: LocalInvitation?, p1: Int) {
            log_e("onLocalInvitationFailure = $p0, $p1")
        }

        override fun onRemoteInvitationReceived(p0: RemoteInvitation?) {
            log_e("on Remote Invitation Received")

            callManager.acceptRemoteInvitation(p0, object : ResultCallback<Void?> {
                override fun onSuccess(p0: Void?) {
                    log_e("accepted remote invitation")
                }

                override fun onFailure(p0: ErrorInfo?) {
                    log_e("an error occured remote invitation = $p0")
                }
            })
        }

        override fun onRemoteInvitationAccepted(p0: RemoteInvitation?) {
            log_e("onRemoteInvitationAccepted = $p0")
        }

        override fun onRemoteInvitationRefused(p0: RemoteInvitation?) {
            log_e("onRemoteInvitationRefused = $p0")
        }

        override fun onRemoteInvitationCanceled(p0: RemoteInvitation?) {
            log_e("onRemoteInvitationCanceled = $p0")
        }

        override fun onRemoteInvitationFailure(p0: RemoteInvitation?, p1: Int) {
            log_e("onRemoteInvitationFailure = $p0, $p1")
        }
    }
}
