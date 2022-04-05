package com.vdotok.one2many.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.vdotok.network.models.LoginResponse
import com.google.gson.Gson
import com.vdotok.network.models.CallerData
import com.vdotok.one2many.R
import com.vdotok.one2many.base.BaseActivity
import com.vdotok.one2many.databinding.ActivityDashBoardBinding
import com.vdotok.one2many.interfaces.FragmentRefreshListener
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.utils.ApplicationConstants
import com.vdotok.one2many.utils.NetworkStatusLiveData
import com.vdotok.one2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.commands.CallInfoResponse
import com.vdotok.streaming.commands.RegisterResponse
import com.vdotok.streaming.enums.CallStatus
import com.vdotok.streaming.enums.EnumConnectionStatus
import com.vdotok.streaming.enums.RegistrationStatus
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.interfaces.CallSDKListener
import com.vdotok.streaming.models.*
import com.vdotok.streaming.utils.checkInternetAvailable
import kotlinx.android.synthetic.main.layout_fragment_call.*
import org.json.JSONObject
import org.webrtc.VideoTrack

/**
 * Created By: VdoTok
 * Date & Time: On 5/19/21 At 6:29 PM in 2021
 *
 * This class displays the connection between user and socket
 */
class DashBoardActivity : AppCompatActivity(), CallSDKListener {

    private lateinit var binding: ActivityDashBoardBinding

    var localStreamScreen: VideoTrack? = null
    var localStreamVideo: VideoTrack? = null
    var sessionId: String? = null
    var sessionId2: String? = null
    var sessionCount = 0
    var streamCount = 1
    lateinit var callClient: CallClient
    lateinit var prefs: Prefs
    var isInternetConnectionRestored = false

    var isCallInitiator = false
    var isCallInitiator2 = false
    var mListener: FragmentRefreshListener? = null

    private lateinit var mLiveDataNetwork: NetworkStatusLiveData
    private var audioManager: AudioManager? = null
    private var isResumeState = false
    private var reConnectStatus = false
    var incomingName :String? = null

    var callParams1: CallParams? = null
    var callParams2: CallParams? = null
    var participantList: ArrayList<String>? = null
    var isMultiSession = false
    var isMulti = false
    var handler: Handler? = null

    val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

    val gson = Gson()
    var callerName: JSONObject = JSONObject()


    private val mLiveDataLeftParticipant: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStatusBarGradient(this, R.drawable.rectangle_white_bg)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_dash_board)
        prefs = Prefs(this)

        initCallClient()
        addInternetConnectionObserver()
        askForPermissions()
        initCallObserver()
    }


    fun initCallClient() {

        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this)
        }
        connectClient()
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
                binding
                Snackbar.make(
                    binding.root,
                    getString(R.string.socket_connection_failed),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } ?: kotlin.run {
            Snackbar.make(
                binding.root,
                (getString(R.string.socket_connected)),
                Snackbar.LENGTH_LONG
            ).show()

            Snackbar.make(
                binding.root,
                getString(R.string.no_user_data_found),
                Snackbar.LENGTH_LONG
            ).show()
        }
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

    fun incomingUserName() {
        val callerData = CallerData(
            calleName = incomingName
        )
        callerName = JSONObject(gson.toJson(callerData))
    }

    /**
     * Function for asking permissions to user
     * */
    private fun askForPermissions() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) &&
            (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                ApplicationConstants.MY_PERMISSIONS_REQUEST
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                ApplicationConstants.MY_PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                ApplicationConstants.MY_PERMISSIONS_REQUEST_CAMERA
            )
        }
    }


    private fun initCallObserver(){
        mLiveDataEndCall.observe(this, {
            if (it) {
                callParams1 = null
                callParams2 = null
                sessionId = null
                incomingName = null
                sessionId2 = null
                mListener?.onCallEnd()
            }
        })
        mLiveDataLeftParticipant.observe(this, {
            if (!TextUtils.isEmpty(it)) {
                mListener?.onParticipantLeftCall(it)
            }
        })

    }


    private fun getMediaServerAddress(mediaServer: LoginResponse.MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
    }

    /**
     * Function to mute call
     * */
    fun muteUnMuteCall(screenShare: Boolean) {
        val session = if (screenShare) {
            if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams1?.sessionUUID.toString()
            } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams2?.sessionUUID.toString()
            } else null
        } else if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                ?.getSessionType() == SessionType.CALL
        ) {
            callParams1?.sessionUUID.toString()
        } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                ?.getSessionType() == SessionType.CALL
        ) {
            callParams2?.sessionUUID.toString()
        } else null

        session?.let {
            callClient.muteUnMuteMic(
                sessionKey = it,
                refId = prefs.loginInfo?.refId!!
            )
        }
    }

    fun getActiveExistingSession(sessionType: SessionType): Boolean {
        val session = callClient.getActiveSessionClient(sessionId!!)
        return session!!.callParams.sessionType == sessionType

    }

    /**
     * Function to switch Camera
     * */
    fun switchCamera() {
        val session = if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                ?.getSessionType() == SessionType.CALL
        ) {
            callParams1?.sessionUUID.toString()
        } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                ?.getSessionType() == SessionType.CALL
        ) {
            callParams2?.sessionUUID.toString()
        } else null
        session?.let { callClient.switchCamera(it) }
    }

    /**
     * Callback method when socket server is connected successfully
     * */
    override fun onError(cause: String) {
        Log.e("OnError:", cause)
    }

    override fun onPublicURL(publicURL: String) {
        mListener?.onPublicURL(publicURL)
    }

    override fun connectionStatus(enumConnectionStatus: EnumConnectionStatus) {
        when (enumConnectionStatus) {
            EnumConnectionStatus.CONNECTED -> {
                mListener?.onConnectionSuccess()
                prefs.loginInfo?.let {
                    runOnUiThread {
                        callClient.register(
                            authToken = it.authorizationToken!!,
                            refId = it.refId!!, reconnectStatus = if (reConnectStatus) 1 else 0
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

                handler?.removeCallbacks(runnableConnectClient)
                handler?.postDelayed(runnableConnectClient, 20000)

            }
            else -> {
            }
        }
    }


    val runnableConnectClient by lazy {
        object : Runnable {
            override fun run() {
                connectClient()
            }
        }
    }

    override fun onClose(reason: String) {
        mListener?.onCallRejected(reason)
    }

    override fun audioVideoState(sessionStateInfo: SessionStateInfo) {

        runOnUiThread {
            mListener?.onCameraAudioOff(sessionStateInfo, isMulti)
        }

    }

    override fun incomingCall(callParams: CallParams) {
        if (callParams.customDataPacket != null) {
            val dataValue = JSONObject(callParams.customDataPacket.toString())
            val callerName: CallerData = gson.fromJson(dataValue.toString(), CallerData::class.java)
            callParams.customDataPacket = callerName.calleName
        }
        isCallInitiator = false
            if (sessionId?.let { callClient.getActiveSessionClient(it) } != null || sessionId2?.let {
                    callClient.getActiveSessionClient(
                        it
                    )
                } != null) {
                callClient.sessionBusy(callParams.refId, callParams.sessionUUID)
            } else {
                if (callParams1 == null) {
                    callParams1 = callParams.copy()
                } else {
                    callParams2 = callParams.copy()
                    isMulti = true
                    isMultiSession = true
                }

                if ((callParams1 != null && callParams2 != null) && isMultiSession) {
                    callParams1?.let {
                        mListener?.onIncomingCall(it)
                    }
                } else if (callParams1 != null && !isMultiSession && callParams.associatedSessionUUID.isEmpty()) {
                    callParams1?.let {
                        mListener?.onIncomingCall(it)
                    }
                }
            }

        Log.e(
            "incomingCall",
            " incomingCall callParam : " + callParams1 + "-- callParams2" + callParams2
        )
    }

    override fun onDestroy() {
        isResumeState = false
        callParams1 = null
        callParams2 = null
        callClient.disConnectSocket()
        super.onDestroy()
    }

    override fun onResume() {
        isResumeState = true
        askForPermissions()
        super.onResume()
     }

    fun acceptMultiCall() {
        prefs.loginInfo?.let {
            callParams2?.let { it1 ->
                callClient.acceptIncomingCall(it.refId.toString(), it1)
            }
            isMultiSession = false
        }
    }

    private fun turnSpeakerOff() {
          audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
          audioManager?.let {
          it.isSpeakerphoneOn = false
        }
     }

    fun acceptIncomingCall(callParams: CallParams) {
        prefs.loginInfo?.let {
            sessionId = callClient.acceptIncomingCall(it.refId.toString(), callParams)
            callParams.sessionUUID = sessionId.toString()
        }
        callParams1 = callParams
    }

    fun dialOne2ManyCall(callParams: CallParams, mediaProjection: MediaProjection?) {
        isCallInitiator = true
        isCallInitiator2 = true
        participantList = callParams.toRefIds

        sessionId = callClient.startSession(callParams, mediaProjection)
        callParams.sessionUUID = sessionId.toString()
        callParams1 = callParams

    }

    fun dialOne2ManyPublicCall(
        callParams: CallParams,
        mediaProjection: MediaProjection?,
        isGroupSession: Boolean
    ) {
        isCallInitiator = true
        isCallInitiator2 = true
        participantList = callParams.toRefIds

        callParams1 = CallParams(
            callParams.refId,
            callParams.toRefIds,
            callParams.mcToken,
            callParams.sessionUUID,
            callParams.requestId
        )
        callParams2 = callParams.copy()

        val pair = callClient.startMultiSession(callParams, mediaProjection, isGroupSession)

        callParams1?.sessionUUID = pair?.first.toString()
        callParams2?.sessionUUID = pair?.second.toString()
    }

    fun dialOne2ManyVideoCall(callParams: CallParams) {

        sessionId2 = callClient.dialOne2ManyCall(callParams)
        callParams.sessionUUID = sessionId2.toString()
        callParams2 = callParams
    }

    fun endCall() {
        turnSpeakerOff()
        incomingName = null
        isMulti = false
        localStreamVideo = null
        localStreamScreen = null
        localView?.let {
           // localView.clearImage()
            localView.release()
        }
        remoteView?.let {
          //  remoteView.clearImage()
            remoteView.release()
        }
        val sessionList = ArrayList<String>().apply {
            callParams1?.sessionUUID?.let {
                add(it)
            }
            callParams2?.sessionUUID?.let {
                add(it)
            }
        }
        callClient.endCallSession(sessionList)
        sessionId = null
        callParams1 = null
        callParams2 = null
    }

    fun pauseVideo(isScreenShare: Boolean) {

        val session = if (isScreenShare) {
            if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams1?.sessionUUID.toString()
            } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams2?.sessionUUID.toString()
            } else null
        } else {

            if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.CALL
            ) {
                callParams1?.sessionUUID.toString()
            } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.CALL
            ) {
                callParams2?.sessionUUID.toString()
            } else null
        }

        session?.let {
            callClient.pauseVideo(
                sessionKey = it,
                refId = prefs.loginInfo?.refId.toString(),
            )
        }

        Log.e(
            "videostate",
            "videoState : " + "   --- isScreenSharing : " + isScreenShare + " ---- isMultiSession : " + isMultiSession
        )

    }

    fun resumeVideo(isScreenShare: Boolean) {

        val session = if (isScreenShare) {
            // in case of screen sharing no change video state of camera
            if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams1?.sessionUUID.toString()
            } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.SCREEN
            ) {
                callParams2?.sessionUUID.toString()
            } else null
        } else {

            if (callClient.getActiveSessionClient(callParams1?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.CALL
            ) {
                callParams1?.sessionUUID.toString()
            } else if (callClient.getActiveSessionClient(callParams2?.sessionUUID.toString())
                    ?.getSessionType() == SessionType.CALL
            ) {
                callParams2?.sessionUUID.toString()
            } else null

        }
        session?.let {
            callClient.resumeVideo(
                sessionKey = it,
                refId = prefs.loginInfo?.refId.toString(),
            )
        }
    }

    override fun onSessionReady(mediaProjection: MediaProjection?) {
        runOnUiThread { mListener?.sessionStart(mediaProjection) }
    }

    override fun participantCount(participantCount: Int, participantRefIdList: ArrayList<String>) {
        mListener?.acceptedUser(participantCount)
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


    override fun callStatus(callInfoResponse: CallInfoResponse) {

        when (callInfoResponse.callStatus) {
            CallStatus.CALL_CONNECTED -> {
                runOnUiThread {
                    mListener?.onStartCalling()

                    // todo check if first call is Screen sharing than 2nd one will be camera video  call by initiator if camera is selected

                    if (isCallInitiator) {
                        mListener?.checkCallType()

                        isCallInitiator = false

                    }

                }
            }
            CallStatus.SERVICE_SUSPENDED,
            CallStatus.OUTGOING_CALL_ENDED,
            CallStatus.NO_SESSION_EXISTS -> {
                turnSpeakerOff()
                isMulti = false
                mLiveDataEndCall.postValue(true)
            }
            CallStatus.CALL_MISSED -> {
                sessionId?.let {
                    if (callClient.getActiveSessionClient(it) == null)
                        mLiveDataEndCall.postValue(true)
                } ?: kotlin.run {
                    mListener?.onCallMissed()
                    mLiveDataEndCall.postValue(true)
                }
            }
            CallStatus.PARTICIPANT_LEFT_CALL -> {
                mLiveDataLeftParticipant.postValue(callInfoResponse.callParams?.toRefIds?.get(0))
            }
            CallStatus.NO_ANSWER_FROM_TARGET -> {
                mLiveDataLeftParticipant.postValue(callInfoResponse.callParams?.toRefIds?.get(0))
            }
            CallStatus.TARGET_IS_BUSY -> {
                mListener?.onCallerAlreadyBusy()
            }
            CallStatus.INSUFFICIENT_BALANCE ->{
                mListener?.onInsufficientBalance()
            }
            else -> {
            }
        }


    }


    /**
     * Callback method to get Video Stream of user
     * */
    override fun onRemoteStream(stream: VideoTrack, refId: String, sessionID: String) {
        if (sessionId != sessionID)
            sessionId = sessionID
        else
            sessionId2 = sessionID

        if (callClient.getActiveSessionClient(sessionID)?.getSessionType() == SessionType.SCREEN) {
            localStreamScreen = stream
            mListener?.onRemoteStreamReceived(stream, refId, sessionID, false)
        } else {
            localStreamVideo = stream
            mListener?.onRemoteStreamReceived(stream, refId, sessionID, true)
        }

        Log.e("onRemoteStream", "onRemoteStream:  videoTrack : " + stream.toString())
        streamCount++
    }

    override fun onRemoteStream(refId: String, sessionID: String) {
        mListener?.onRemoteStreamReceived(refId, sessionID)
    }

    override fun onCameraStream(stream: VideoTrack) {
        localStreamVideo = stream
        mListener?.onCameraStreamReceived(stream)
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

    fun logout() {
        callClient.unRegister(
            ownRefId = prefs.loginInfo?.refId.toString()
        )
    }

    companion object {
        const val TAG = "DASHBOARD_ACTIVITY"
        fun createDashboardActivity(context: Context) = Intent(
            context,
            DashBoardActivity::class.java
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }
    }
}