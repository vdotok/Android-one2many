package com.norgic.vdotokcall_mtm.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.norgic.callsdks.CallClient
import com.norgic.callsdks.commands.CallInfoResponse
import com.norgic.callsdks.commands.RegisterResponse
import com.norgic.callsdks.enums.*
import com.norgic.callsdks.interfaces.CallSDKListener
import com.norgic.callsdks.interfaces.StreamCallback
import com.norgic.callsdks.models.*
import com.norgic.callsdks.stats.StatsInterface
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.ActivityDashBoardBinding
import com.norgic.vdotokcall_mtm.interfaces.FragmentRefreshListener
import com.norgic.vdotokcall_mtm.models.LoginResponse
import com.norgic.vdotokcall_mtm.models.MediaServerMap
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants
import com.norgic.vdotokcall_mtm.utils.NetworkStatusLiveData
import com.norgic.vdotokcall_mtm.utils.ViewUtils.setStatusBarGradient
import kotlinx.android.synthetic.main.layout_fragment_call.*
import org.webrtc.VideoTrack

/**
 * Created By: Norgic
 * Date & Time: On 5/19/21 At 6:29 PM in 2021
 *
 * This class displays the connection between user and socket
 */
class DashBoardActivity : AppCompatActivity(), CallSDKListener, StreamCallback, StatsInterface {

    private lateinit var binding: ActivityDashBoardBinding
    var localStreamScreen: VideoTrack? = null
    var localStreamVideo: VideoTrack? = null

    lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var sessionId: String? = null
    var sessionId2: String? = null
    var sessionCount = 0
    var streamCount = 1
    var isCallInitiator = false
    var isCallInitiator2 = false
    private var internetConnectionRestored = false
    var mListener: FragmentRefreshListener? = null
    private lateinit var mLiveDataNetwork: NetworkStatusLiveData
    var callParams1: CallParams? = null
    var callParams2: CallParams? = null
    var participantList: ArrayList<String>? = null
    var isMultiSession = false
    var isMulti = false
    var handler: Handler? = null

    private val mLiveDataEndCall: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }

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

    /**
     * function to connect socket successfully
     * */

    private fun initCallClient() {
        handler = Handler(Looper.getMainLooper())
        CallClient.getInstance(this)?.setConstants(ApplicationConstants.SDK_PROJECT_ID)
        CallClient.getInstance(this)?.let {
            callClient = it
            callClient.setListener(this, this, this)
        }

        connectClient()

        mLiveDataEndCall.observe(this, {
            if (it) {
                callParams1 = null
                callParams2 = null
                sessionId = null
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


    fun connectClient() {
        prefs.loginInfo?.mediaServerMap?.let {
            if (callClient.isConnected() == null || callClient.isConnected() == false)
                callClient.connect(
                    getMediaServerAddress(it),
                    it.endPoint
                )
        }
    }

    private fun getMediaServerAddress(mediaServer: MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
    }

    /**
     * Function to mute call
     * */
    fun muteUnMuteCall(screenShare : Boolean) {
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
                                refId = it.refId!!
                        )
                    }
                }
            }
            EnumConnectionStatus.NOT_CONNECTED -> {
                mListener?.onConnectionFail()
                prefs.loginInfo?.mediaServerMap?.let {
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
                prefs.loginInfo?.mediaServerMap?.let {
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
        isCallInitiator = false

//        if (callParams1 == null) {
//            callParams1 = callParams
//            mListener?.onIncomingCall(callParams)
//        } else {
//            callParams2 = callParams
//            callParams2?.let { callParam ->
//                prefs.loginInfo?.let {
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        sessionId2 = callClient.acceptIncomingCall(it.refId.toString(), callParam)
//                    }, 3000)
//                }
//            }
//        }

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
        } else if (callParams1 != null && !isMultiSession) {
            callParams1?.let {
                mListener?.onIncomingCall(it)
            }
        }

        Log.e(
                "incomingCall",
                " incomingCall callParam : " + callParams1 + "-- callParams2" + callParams2
        )
    }

    override fun onDestroy() {
        callParams1 = null
        callParams2 = null
        callClient.disConnectSocket()
        super.onDestroy()
    }

    fun acceptMultiCall() {
        prefs.loginInfo?.let {
            callParams2?.let { it1 ->
                callClient.acceptIncomingCall(it.refId.toString(), it1)
            }
          isMultiSession = false
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

    fun dialOne2ManyPublicCall(callParams: CallParams, mediaProjection: MediaProjection?, isGroupSession: Boolean) {
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

        callParams1?.sessionUUID = pair.first
        callParams2?.sessionUUID = pair.second
    }

    fun dialOne2ManyVideoCall(callParams: CallParams) {

        sessionId2 = callClient.dialOne2ManyCall(callParams)
        callParams.sessionUUID = sessionId2.toString()
        callParams2 = callParams
    }

    fun endCall() {
        isMulti = false
        localStreamVideo = null
        localStreamScreen = null
        localView?.let {
            localView.clearImage()
            localView.release()
        }
        remoteView?.let {
            remoteView.clearImage()
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

        Log.e("videostate","videoState : "+ "   --- isScreenSharing : "+isScreenShare +" ---- isMultiSession : "+isMultiSession)

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

    override fun sessionEndedData(callParams: CallParams) {
//        TODO("Not yet implemented")
    }

    override fun onSessionReady(mediaProjection: MediaProjection?, isInternalAudio: Boolean) {
        runOnUiThread { mListener?.sessionStart(mediaProjection) }
    }

    override fun participantCount(participantCount: Int) {
       mListener?.acceptedUser(participantCount)
    }


    override fun callStatus(callInfoResponse: CallInfoResponse) {

        runOnUiThread {

            Toast.makeText(
                    this,
                    "Call Status: ${callInfoResponse.callStatus}", Toast.LENGTH_SHORT
            ).show()
        }
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
            CallStatus.OUTGOING_CALL_ENDED,
            CallStatus.NO_SESSION_EXISTS -> {
                isMulti = false
                mLiveDataEndCall.postValue(true)
            }
            CallStatus.CALL_MISSED -> {
                sessionId?.let {
                    if (callClient.getActiveSessionClient(it) == null)
                        mLiveDataEndCall.postValue(true)
                } ?: kotlin.run {
                    mListener?.onCallMissed()
                }
            }
            CallStatus.PARTICIPANT_LEFT_CALL -> {
                mLiveDataLeftParticipant.postValue(callInfoResponse.callParams?.toRefIds?.get(0))
            }
            CallStatus.NO_ANSWER_FROM_TARGET -> {
                mLiveDataLeftParticipant.postValue(callInfoResponse.callParams?.toRefIds?.get(0))
            }
            else -> {
            }
        }


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
//                    binding.root.showSnackBar("Socket Connected!")
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

    private fun addInternetConnectionObserver() {
        mLiveDataNetwork = NetworkStatusLiveData(this.application)

        mLiveDataNetwork.observe(this, { isInternetConnected ->
            when {
                isInternetConnected == true && internetConnectionRestored -> {
                    connectClient()
                    mListener?.onConnectionSuccess()
                }
                isInternetConnected == false -> {
                    internetConnectionRestored = true
                    mListener?.onCallEnd()
                    mListener?.onConnectionFail()
                }
                else -> {
                }
            }
        })
    }

    override fun memoryUsageDetails(memoryUsage: Long) {
//        TODO("Not yet implemented")
    }

    override fun sendCurrentDataUsage(sessionKey: String, usage: Usage) {
        prefs.loginInfo?.refId?.let { refId ->
            Log.e("statsSdk", "currentSentUsage: ${usage.currentSentBytes}, currentReceivedUsage: ${usage.currentReceivedBytes}")
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