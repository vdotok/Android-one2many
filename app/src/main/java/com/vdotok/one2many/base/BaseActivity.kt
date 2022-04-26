package com.vdotok.one2many.base

import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.interfaces.FragmentRefreshListener
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.utils.ApplicationConstants
import com.vdotok.one2many.utils.NetworkStatusLiveData
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.enums.RegistrationStatus
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import com.vdotok.streaming.utils.checkInternetAvailable
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 11/1/21 At 6:11 PM in 2021
 */
abstract class BaseActivity: AppCompatActivity(), CallSDKListener {

    lateinit var callClient: CallClient
    lateinit var prefs: Prefs
    lateinit var mLiveDataNetwork: NetworkStatusLiveData
    var isInternetConnectionRestored = false
    var isResumeState = false
    var reConnectStatus = false

    val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    abstract fun getRootView(): View

    companion object {
        var mListener: FragmentRefreshListener? = null
        var localStream: VideoTrack? = null
        var remoteStream: VideoTrack? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
    }

    override fun onResume() {
        super.onResume()
        initCallClient()
    }

    /**
     * function to connect socket successfully
     * */

    fun initCallClient() {

        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this)
        }
        connectClient()
    }
    // when socket is disconnected
    override fun onClose(reason: String) {
        connectClient()
    }

    fun connectClient() {
        prefs.loginInfo?.mediaServer?.let {
            if (callClient.isConnected() == null || !callClient.isConnected())
                callClient.connect(
                    getMediaServerAddress(it),
                    it.endPoint
                )
        }
    }


    private fun getMediaServerAddress(mediaServer: LoginResponse.MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
    }


    override fun onError(cause: String) {
    }

    override fun onPublicURL(publicURL: String) {
    }

    override fun onSessionReady(mediaProjection: MediaProjection?) {
    }

    override fun audioVideoState(state: SessionStateInfo) {

    }

    override fun callStatus(callInfoResponse: CallInfoResponse) {
    }

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                if (prefs.loginInfo != null && prefs.loginInfo?.authorizationToken != null) {
                    runOnUiThread {
                        mListener?.onConnectionSuccess()
                        callClient.register(
                            authToken = prefs.loginInfo?.authorizationToken!!,
                            refId = prefs.loginInfo?.refId!!, reconnectStatus = if (reConnectStatus) 1 else 0
                        )
                        reConnectStatus = false
                    }
                }
            }
            EnumConnectionStatus.NOT_CONNECTED -> {
                mListener?.onConnectionFail()
                prefs.loginInfo?.mediaServer?.let {
                    callClient.connect(
                        getMediaServerAddress(it),
                        it.endPoint
                    )
                }

                runOnUiThread {
                    Toast.makeText(this, "Not Connected!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.ERROR -> {
                mListener?.onConnectionFail()
                prefs.loginInfo?.mediaServer?.let {
                    callClient.connect(
                        getMediaServerAddress(it),
                        it.endPoint
                    )
                }

                runOnUiThread {
                    Toast.makeText(this, "Connection Error!", Toast.LENGTH_SHORT).show()
                }
            }
            EnumConnectionStatus.SOCKET_PING -> {

//                handler?.removeCallbacks(runnableConnectClient)
//                handler?.postDelayed(runnableConnectClient, 20000)

            }
            else -> {
            }
        }
    }

    override fun incomingCall(callParams: CallParams) {
    }

    override fun onDestroy() {
        mLiveDataNetwork.removeObservers(this)
        super.onDestroy()
    }

    override fun registrationStatus(registerResponse: RegisterResponse) {

        when (registerResponse.registrationStatus) {
            RegistrationStatus.REGISTER_SUCCESS -> {

                val userModel: LoginResponse? = prefs.loginInfo
                userModel?.mcToken = registerResponse.mcToken.toString()
                runOnUiThread {
                    userModel?.let {
                        prefs.loginInfo = it
                    }
                    if (registerResponse.reConnectStatus == 1) {
                        callClient.initiateReInviteProcess()
                    }
                }

            }
            RegistrationStatus.UN_REGISTER,
            RegistrationStatus.REGISTER_FAILURE,
            RegistrationStatus.INVALID_REGISTRATION -> {
                Handler(Looper.getMainLooper()).post {
                    Log.e("register", "message: ${registerResponse.responseMessage}")
                }
            }
        }
    }

    // Stream callbacks
    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
    }

    override fun onCameraStream(stream: VideoTrack) {
        localStream = stream
        mListener?.onCameraStreamReceived(stream)
    }

    override fun onRemoteStream(refId: String, sessionID: String) {
    }

    override fun memoryUsageDetails(memoryUsage: Long) {
    }

    override fun sendCurrentDataUsage(sessionKey: String, usage: Usage) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e(
                "statsSdk",
                "currentSentUsage: ${usage.currentSentBytes}, currentReceivedUsage: ${usage.currentReceivedBytes}"
            )
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = PartialCallLogs(
                    upload_bytes = usage.currentSentBytes.toString(),
                    download_bytes = usage.currentReceivedBytes.toString()
                )
            )
        }
    }

    override fun sendEndDataUsage(sessionKey: String, sessionDataModel: SessionDataModel) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e("statsSdk", "sessionData: $sessionDataModel")
            callClient.sendEndCallLogs(
                refId = refId,
                sessionKey = sessionKey,
                stats = sessionDataModel
            )
        }
    }

    override fun sessionHold(sessionUUID: String) {
    }

    fun addInternetConnectionObserver() {
        mLiveDataNetwork = NetworkStatusLiveData(this.application)

        mLiveDataNetwork.observe(this, { isInternetConnected ->
            when {
                isInternetConnected == true && isInternetConnectionRestored && !isResumeState -> {
                    Log.e("Internet", "internet connection restored!")
                    performSocketReconnection()
                }
                isInternetConnected == false -> {
                    isInternetConnectionRestored = true
                    reConnectStatus = true
                    isResumeState = false
                    Log.e("Internet", "internet connection lost!")
                }
                else -> {
                }
            }
        })
    }

    private fun performSocketReconnection() {
        prefs.loginInfo?.let { loginResponse ->
            if (checkInternetAvailable(this)) {
                loginResponse.mediaServer?.let {
                    callClient.connect(getMediaServerAddress(it), it.endPoint)
                }
            } else {
                Snackbar.make(
                    getRootView(),
                    "Socket Connection failed! No Internet!",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } ?: kotlin.run {
            Snackbar.make(
                getRootView(),
                "No user data found please re-login",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }



}
