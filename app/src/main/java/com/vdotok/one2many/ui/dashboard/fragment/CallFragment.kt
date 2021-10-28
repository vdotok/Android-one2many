package com.vdotok.one2many.ui.dashboard.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.LayoutFragmentCallBinding
import com.vdotok.one2many.extensions.hide
import com.vdotok.one2many.extensions.show
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.models.AcceptCallModel
import com.vdotok.one2many.models.GroupModel
import com.vdotok.one2many.models.Participants
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.TimeUtils.getTimeFromSeconds
import com.vdotok.one2many.utils.performSingleClick
import kotlinx.android.synthetic.main.layout_fragment_call.*
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import java.util.*


/**
 * Created By: VdoTok
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays the on connected call
 */
const val TAG_VIEW_CAMERA = "TAG_ViewCamera"
const val TAG_VIEW_SCREEN_SHARING = "TAG_VIEW_SCREEN_SHARING"

class CallFragment : CallMangerListenerFragment() {

    private var callParams: CallParams? = null
    private var isIncomingCall = false
    private lateinit var binding: LayoutFragmentCallBinding
    private var isInternalAudioIncluded = false
    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false

    private lateinit var callClient: CallClient
    private var groupModel : GroupModel? = null
    private var name : String? = null
    private lateinit var prefs: Prefs

    private var userName : ObservableField<String> = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()
    private var isInternalAudioResume = false
    private var isMuted = false
    private var isSpeakerOff = true
    private var isaudioOff = true
    private var isVideoCameraCall = true
    private var isScreenSharingCall = true
    private var callDuration = 0
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    private var screenWidth = 0
    private var screenHeight = 0

    private var rightDX = 0
    private var rightDY = 0

    private var xPoint = 0.0f
    private var yPoint = 0.0f
    var user : String? = null
    var isFullStreamingView :Boolean = false
    var viewSwitched = false
    var rootEglBase: EglBase? = null
    var participantsCount = 0
    var loop = 0


    private val listUser =  ArrayList<Participants>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = LayoutFragmentCallBinding.inflate(inflater, container, false)
        init()
        startTimer()
        rootEglBase = EglBase.create()
        return binding.root
    }

    /**
     * Function for setOnClickListeners and receiving data from outgoing and incoming call dial
     * */
    private fun init() {
        prefs = Prefs(this.requireContext())

        binding.username = userName

        binding.tvCallType.text = getString(R.string.video_calling)

        (activity as DashBoardActivity).mListener = this

        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }
        groupList.clear()
        isVideoCameraCall = arguments?.getBoolean(DialCallFragment.IS_VIDEO_CALL) ?: false
        isScreenSharingCall = arguments?.getBoolean(DialCallFragment.IS_VIDEO_CALL)?: false
        arguments?.get(GroupModel.TAG)?.let {
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
            getUserName(groupModel!!,isVideoCameraCall)
            screenSharingApp = arguments?.getBoolean("screenApp")?: false
            screenSharingMic = arguments?.getBoolean("screenMic")?: false
            cameraCall = arguments?.getBoolean("video")?: false
            participantsCount = arguments?.getInt("participantsCount")!!
            isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false
        } ?: kotlin.run {
            groupList = arguments?.getParcelableArrayList<GroupModel>("grouplist") as ArrayList<GroupModel>
            name = (arguments?.get("userName") as CharSequence?).toString()
            callParams = arguments?.getParcelable(AcceptCallModel.TAG) as CallParams?
            isIncomingCall = true
            participantsCount = arguments?.getInt("participant")!!
        }

        binding.tvcount.text = participantsCount.toString()
        displayUi(isVideoCameraCall,isIncomingCall,screenSharingApp,screenSharingMic,cameraCall)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            (activity as DashBoardActivity).endCall()
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        }

        binding.imgMute.setOnClickListener {
            isMuted = !isMuted
            if (isMuted) {
                binding.imgMute.setImageResource(R.drawable.ic_mute_mic1)
            } else {
                binding.imgMute.setImageResource(R.drawable.ic_unmute_mic)
            }

            when {
                cameraCall -> (activity as DashBoardActivity).muteUnMuteCall(false)
                else -> (activity as DashBoardActivity).muteUnMuteCall(true)

            }
        }

        binding.ivSpeaker.setOnClickListener {
           speakerButtonAction()
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true // default to enabled
        ) { override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,  // LifecycleOwner
            callback
        )
        binding.ivCamSwitch.setOnClickListener {
            (activity as DashBoardActivity).switchCamera()
        }

        binding.imgCamera.setOnClickListener {
            if (isVideoCameraCall) {
                if (screenSharingApp || screenSharingMic) {
                    binding.localView.hide()
                } else {
                    binding.remoteView.hide()
                    binding.groupAudioCall.show()
                }
                binding.imgCamera.setImageResource(R.drawable.ic_video_off)
                (activity as DashBoardActivity).pauseVideo(false)
            } else {
                if (screenSharingApp || screenSharingMic) {
                    binding.localView.show()
                } else {
                    binding.remoteView.show()
                    binding.groupAudioCall.hide()
                }
                refreshLocalCameraView()
                (activity as DashBoardActivity).resumeVideo(false)
                binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
            }
            isVideoCameraCall = !isVideoCameraCall
        }


        binding.internalAudio.setOnClickListener {
            if (!isIncomingCall) {
                isInternalAudioResume = !isInternalAudioResume
                if (isInternalAudioResume) {
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                } else {
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_icon)
                }
                (activity as DashBoardActivity).muteUnMuteCall(true)
            } else {
                isaudioOff = isaudioOff.not()
                if (isaudioOff) {
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                } else {
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_icon)
                }
                callClient.toggleSpeakerOnOff()

            }

        }

        if (isIncomingCall) {
            binding.clickVideoView.setOnClickListener {
                swapVideoViews()
            }
        } else {
            addTouchEventListener()
        }

        binding.imgscreenn.setOnClickListener {
            if (isScreenSharingCall) {
                (activity as DashBoardActivity).pauseVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.ic_screensharing_off)
            } else {
                refreshLocalCameraView()
                (activity as DashBoardActivity).resumeVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.screen_sharing_icon)
            }
            isScreenSharingCall = !isScreenSharingCall
        }

        screenWidth = context?.resources?.displayMetrics?.widthPixels!!
        screenHeight = context?.resources?.displayMetrics?.heightPixels!!

        (activity as DashBoardActivity).localStreamVideo?.let { onCameraStreamReceived(it) }
    }


    /**
     * Function to set user name when call connected from outgoing call dial
     * @param videoCall videoCall to check whether its an audio or video call
     * @param groupModel groupModel object is to get group details
     * */
    private fun getUserName(groupModel: GroupModel?, videoCall: Boolean) {
       groupModel?.let { it ->
            if (groupModel.autoCreated == 1 && videoCall) {
                it.participants.forEach { name->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else {
                var participantNames = ""
                it.participants.forEach {
                    if (it.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        participantNames += it.fullname.plus("\n")
                    }
                }
                userName.set(participantNames)

            }
       }

    }

    /**
     * Function to set ui related to audio and video
     * @param videoCall videoCall to check whether its an audio or video call
     * */
    private fun displayUi(
        videoCall: Boolean,
        isIncomingCall: Boolean,
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean
    ) {

        if (!isAdded) {
            return
        }
        if (isIncomingCall) {
            binding.imgMute.hide()
            binding.imgscreenn.hide()
            binding.imgCamera.hide()
            binding.internalAudio.hide()
            binding.ivCamSwitch.hide()

        } else {
            if(screenSharingApp && cameraCall) {
                binding.imgscreenn.show()
                binding.imgCamera.show()
                if (!isInternalAudioIncluded && screenSharingApp){
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                    binding.internalAudio.isEnabled = false
                }

                binding.ivCamSwitch.show()
            } else if (screenSharingMic && cameraCall) {
                binding.imgscreenn.show()
                binding.imgCamera.show()
                binding.internalAudio.hide()
                binding.imgMute.show()
                binding.ivCamSwitch.show()
            } else if (screenSharingApp) {
                if (!isInternalAudioIncluded && screenSharingApp){
                    binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                    binding.internalAudio.isEnabled = false
                }
                binding.imgCamera.hide()
                binding.ivCamSwitch.hide()
                binding.imgMute.hide()
                binding.localView.hide()
            } else if(screenSharingMic) {
                if (!isInternalAudioIncluded && screenSharingMic){
                    binding.imgMute.show()
                }
                binding.imgCamera.hide()
                binding.ivCamSwitch.hide()
                binding.internalAudio.hide()
                binding.localView.hide()
            } else if (cameraCall) {
                binding.internalAudio.hide()
                binding.imgscreenn.hide()
                binding.ivCamSwitch.show()
                binding.localView.hide()
            }
        }

        if (videoCall) {
            binding.groupAudioCall.hide()
            binding.remoteView.show()

            binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
        }
    }

    /**
     * Function to start the timer when call is connected
     * */
    private fun startTimer() {
        if (timer != null) {
            stopTimer()
        }
        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    callDuration = callDuration.plus(1)
                    binding.tvTime.text = getTimeFromSeconds(callDuration)
                }
            }
        }
        timer?.scheduleAtFixedRate(timerTask, 1000, 1000)
    }

    /**
     * Function to stop the timer when call is disconnected
     * */
    private fun stopTimer() {
        callDuration = 0
        timerTask?.cancel()
        timerTask = null
        timer?.purge()
        timer?.cancel()
        timer = null
        binding.tvTime.text = getTimeFromSeconds(callDuration)
    }
    private fun speakerButtonAction() {
        isSpeakerOff = isSpeakerOff.not()
        when {
            isSpeakerOff -> binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
            else -> binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
        }

        callClient.toggleSpeakerOnOff()
    }

    companion object {
        const val VOICE_CALL = "VoiceCallFragment"
        const val THRESHOLD_VALUE = 70.0f

        @JvmStatic
        fun newInstance() = CallFragment()
    }

    override fun onIncomingCall(model: CallParams) {}

    override fun onStartCalling() {}

    override fun outGoingCall(toPeer: GroupModel) {}


    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String, isCameraStream: Boolean) {

        Log.e("one2m", "onRemoteStreamReceived : " + isCameraStream)
        activity?.runOnUiThread {

            if (!isFullStreamingView) {
                isFullStreamingView = true

                try {

                    binding.remoteView.release()

                    if (isCameraStream) {
                        binding.remoteView.setTag(TAG_VIEW_CAMERA)
                    } else {
                        binding.remoteView.setTag(TAG_VIEW_SCREEN_SHARING)
                    }

                    val videoView = binding.remoteView

                    videoView.setMirror(false)
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(false)
                    videoView.setZOrderOnTop(false)
                    videoView.setEnableHardwareScaler(true)
                    ViewCompat.setElevation(videoView, -1f)

                    stream.addSink(videoView)
                    setUserNameUI(refId)
                    videoView.show()
                    if (isSpeakerOff) {
                        isSpeakerOff = false
                        videoView.postDelayed({
                            callClient.toggleSpeakerOnOff()
                        }, 1000)
                        binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
                    }
                    binding.localView.setZOrderMediaOverlay(true)
                    binding.localView.setEnableHardwareScaler(true)
                    binding.localView.setZOrderOnTop(true)

                    binding.localView.hide()
                } catch (e: Exception) {
                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
                }

            } else {
                try {

                    binding.localView.show()
                    binding.localView.release()
                    val videoView = binding.localView
                    if(isCameraStream){
                    videoView.setMirror(true)
                    }else{
                    videoView.setMirror(false)
                    }
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setEnableHardwareScaler(true)
                    videoView.setZOrderMediaOverlay(true)
                    videoView.setZOrderOnTop(true)
                    refreshLocalCameraView()
                    if (isSpeakerOff) {
                        isSpeakerOff = false
                        videoView.postDelayed({
                            callClient.toggleSpeakerOnOff()
                        }, 1000)
                        binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
                    }
                    stream.addSink(videoView)
                    setUserNameUI(refId)


                    binding.remoteView.setZOrderMediaOverlay(false)
                    binding.remoteView.setEnableHardwareScaler(false)

                } catch (e: Exception) {
                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
                }

            }

            if ((activity as DashBoardActivity).isMultiSession) {
                (activity as DashBoardActivity).acceptMultiCall()
            }
        }

//        if (isCameraStream && isSpeakerOff) {
//            isSpeakerOff = false
//            binding.localView.postDelayed({
//                callClient.toggleSpeakerOnOff()
//            }, 1000)
//        }
    }

    override fun onRemoteStreamReceived(refId: String, sessionID: String) {
    }


    private fun setUserNameUI( refId: String) {
        groupModel?.participants?.let {

            it.forEach {
                if (it.refId == refId) {
                    userName.set(it.fullname)
                }
            }

        } ?: run {
            if (groupList.isEmpty()) {
                userName.set("User")
            } else {
                groupList.forEach { group ->
                    group.participants.forEach { participant ->
                        if (participant.refId == refId) {
                            userName.set(participant.fullname)
                        }
                    }
                }
            }
        }
    }
    private fun getUsername(refId: String) : String? {
        groupList.let {
            it.forEach { name ->
                name.participants.forEach { username->
                    if (username.refId?.equals(refId) == true) {
                        user = username.fullname
                        return user
                    }
                }
            }
        }
        return user
    }

    override fun onCameraStreamReceived(stream: VideoTrack) {

//        if (!(activity as DashBoardActivity).isCallInitiator2)
//            return

        Log.e("camera-stream", "screenSharingApp : " + screenSharingApp + "  -- screenSharingMic : " + screenSharingMic)

        val myRunnable = Runnable {
            if (screenSharingApp || screenSharingMic) {
                try {
                    binding.localView.show()
                    val videoView = binding.localView
                    videoView.setMirror(false)
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(true)
                    videoView.setEnableHardwareScaler(true)
                    stream.addSink(videoView)
                    refreshLocalCameraView()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                try {
                    val videoView = binding.remoteView
                    videoView.setMirror(false)
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(true)
                    videoView.setEnableHardwareScaler(true)
                    stream.addSink(videoView)
                    refreshLocalCameraView()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        activity?.let { Handler(it.mainLooper) }?.post(myRunnable)
    }

    override fun onCameraAudioOff(sessionStateInfo: SessionStateInfo, isMultySession: Boolean) {
        activity?.runOnUiThread {

            if (binding.remoteView.getTag() == TAG_VIEW_SCREEN_SHARING) {
                if (sessionStateInfo.isScreenShare == true) {

                    if (sessionStateInfo.videoState == 0) {
                        binding.remoteView.hide()
                    } else {
                        binding.remoteView.show()
                    }
                } else {
                    if (sessionStateInfo.videoState == 0) {
                        binding.localView.hide()
                    } else {
                        binding.localView.show()
                    }
                }
            } else {

                if (sessionStateInfo.isScreenShare == true) {
                    if (sessionStateInfo.videoState == 0) {
                        binding.localView.hide()
                    } else {
                        binding.localView.show()
                    }
                } else {
                    if (sessionStateInfo.videoState == 0) {
                        binding.remoteView.hide()
                    } else {
                        binding.remoteView.show()
                    }
                }
            }
        }
    }


    private fun refreshLocalCameraView() {

        if (binding.localView.isVisible) {

            binding.remoteView.setZOrderMediaOverlay(false)
            binding.localView.setEnableHardwareScaler(true)
            binding.localView.setZOrderMediaOverlay(true)
            binding.localView.requestFocus()

//            animateView(((screenWidth - (binding.localView.width + THRESHOLD_VALUE))),
//                (screenHeight / 2 + binding.localView.height / 2).toFloat()
//            )
//            binding.localView.show()
        }
    }

    override fun onCallMissed() {
        try {
            listUser.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        } catch (e: Exception) {}
    }

    override fun onCallEnd() {
        try {
            listUser.clear()
            (this.activity as DashBoardActivity).sessionId = null
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        } catch (e: Exception) {}
    }

    override fun onPublicURL(publicURL: String) {
    }

    override fun checkCallType() {
//        if ((screenSharingApp && isInternalAudioIncluded && cameraCall) || (screenSharingMic && !isInternalAudioIncluded && cameraCall)
//            ||(screenSharingApp && !isInternalAudioIncluded && cameraCall)) {
//            dialOneToManyVideoCall(mediaType = MediaType.VIDEO,sessionType = SessionType.CALL, groupModel!!)
//        }
        if ((screenSharingApp && !isInternalAudioIncluded) || (screenSharingMic && !isInternalAudioIncluded)
            || (screenSharingApp && isInternalAudioIncluded)){
            moveToDashboard()
        }
    }
    private fun moveToDashboard() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun dialOneToManyVideoCall(mediaType: MediaType, sessionType: SessionType, groupModel: GroupModel) {

        val refIdList = groupModel.participants.map { it.refId } as java.util.ArrayList<String>
        refIdList.remove(prefs.loginInfo?.refId)

        if (callClient.isConnected() == true) {

            prefs.loginInfo?.let {
                (activity as DashBoardActivity).dialOne2ManyVideoCall(
                    CallParams(
                        refId = it.refId!!,
                        toRefIds = refIdList,
                        mcToken = it.mcToken!!,
                        mediaType = mediaType,
                        callType = CallType.ONE_TO_MANY,
                        sessionType = sessionType,
                        isAppAudio = isInternalAudioIncluded,
                        isBroadcast = 0
                    )
                )
            }
        } else {
            (activity as DashBoardActivity).connectClient()
        }
    }

    override fun onParticipantLeftCall(refId: String?) {
        if((activity as DashBoardActivity).callParams1 != null &&(activity as DashBoardActivity).callParams2 != null){
            if (loop == 0){
                participantsCount -= 1
                binding.tvcount.text = participantsCount.toString()
                loop +=  1
            }else{
                loop = 0
            }

        }else {
            participantsCount -= 1
            binding.tvcount.text = participantsCount.toString()
        }

    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
        //        TODO("Not yet implemented")
    }

    override fun acceptedUser(participantCount: Int) {
        participantsCount = participantCount - 1
        binding.tvcount.text = participantsCount.toString()
    }

    override fun onCallRejected(reason: String) {}

    @SuppressLint("ClickableViewAccessibility")
    private fun addTouchEventListener() {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.localView.setOnTouchListener(View.OnTouchListener { view, event ->
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        rightDX = (binding.localView.x - event.rawX).toInt()
                        rightDY = (binding.localView.y - event.rawY).toInt()
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val displacementX = event.rawX + rightDX
                        val displacementY = event.rawY + rightDY

                        binding.localView.animate()
                            .x(displacementX)
                            .y(displacementY)
                            .setDuration(0)
                            .start()

                        Handler(Looper.getMainLooper()).postDelayed({

                            xPoint = view.x + view.width
                            yPoint = view.y + view.height

                            when {
                                xPoint > screenWidth / 2 && yPoint < screenHeight / 2 -> {
                                    //First Quadrant
                                    animateView(
                                        ((screenWidth - (view.width + THRESHOLD_VALUE))),
                                        (screenHeight / 2 - (view.height + THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint < screenHeight / 2 -> {
                                    //Second Quadrant
                                    animateView(
                                        THRESHOLD_VALUE,
                                        (screenHeight / 2 - (view.height + THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint > screenHeight / 2 -> {
                                    //Third Quadrant
                                    animateView(
                                        THRESHOLD_VALUE,
                                        (screenHeight / 2 + view.height / 2).toFloat()
                                    )
                                }
                                else -> {
                                    //Fourth Quadrant
                                    animateView(
                                        ((screenWidth - (view.width + THRESHOLD_VALUE))),
                                        (screenHeight / 2 + view.height / 2).toFloat()
                                    )
                                }
                            }

                        }, 100)

                    }
                    else -> { // Note the block
                        return@OnTouchListener false
                    }
                }
                true
            })
        }, 1500)
    }

    private fun animateView(xPoint: Float, yPoint: Float) {
        binding.localView.animate()
            .x(xPoint)
            .y(yPoint)
            .setDuration(200)
            .start()
    }


    fun swapVideoViews() {

        viewSwitched = !viewSwitched
        val paramsLocal = binding.localView.layoutParams as ConstraintLayout.LayoutParams
        val paramsRemote = binding.remoteView.layoutParams as ConstraintLayout.LayoutParams

        binding.localView.layoutParams = paramsRemote
        binding.remoteView.layoutParams = paramsLocal

        binding.remoteView.setZOrderMediaOverlay(viewSwitched)
        binding.remoteView.setZOrderOnTop(viewSwitched)

        binding.localView.setZOrderMediaOverlay(!viewSwitched)
        binding.localView.setZOrderOnTop(!viewSwitched)

        binding.localView.postDelayed({
            binding.localView.refreshDrawableState()
            binding.remoteView.refreshDrawableState()
            binding.root.requestLayout()

        }, 1000)

        if (viewSwitched) {
            ViewCompat.setElevation(binding.localView, 0f)
            ViewCompat.setElevation(binding.remoteView, 1f)
        } else {
            ViewCompat.setElevation(binding.localView, 1f)
            ViewCompat.setElevation(binding.remoteView, 0f)
        }
    }


}