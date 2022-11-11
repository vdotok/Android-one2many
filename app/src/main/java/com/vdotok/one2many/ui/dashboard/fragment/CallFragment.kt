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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.vdotok.network.models.GroupModel
import com.vdotok.network.models.Participants
import com.vdotok.one2many.CustomCallView
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.one2many.R
import com.vdotok.one2many.VdoTok
import com.vdotok.one2many.databinding.LayoutFragmentCallBinding
import com.vdotok.one2many.extensions.hide
import com.vdotok.one2many.extensions.show
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.models.AcceptCallModel
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.TimeUtils.getTimeFromSeconds
import com.vdotok.one2many.utils.performSingleClick
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.views.CallViewRenderer
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
    var rootEglBase: EglBase? = null
    var participantsCount = 0
    var loop = 0
    var swap = false
    var isCamSwitch = false

    private lateinit var screenRemoteViewReference: CustomCallView
    private lateinit var videoRemoteViewReference:  CustomCallView


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
       screenRemoteViewReference = binding.localView
       videoRemoteViewReference = binding.remoteView

        binding.username = userName
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
        callClient.setSpeakerEnable(true)
        binding.tvcount.text = participantsCount.toString()
        displayUi(isIncomingCall,screenSharingApp,screenSharingMic,cameraCall)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            (activity as DashBoardActivity).endCall()
            binding.remoteView.release()
            binding.localView.release()
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
                screenSharingMic && cameraCall -> { (activity as DashBoardActivity).muteUnMuteCall(false)
                    (activity as DashBoardActivity).muteUnMuteCall(true) }
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
            if (!isCamSwitch){
                binding.remoteView.preview.setMirror(false)
            }else{
                binding.remoteView.preview.setMirror(true)
            }
            isCamSwitch = isCamSwitch.not()
            (activity as DashBoardActivity).switchCamera()
        }

        binding.imgCamera.setOnClickListener {
            if (isVideoCameraCall) {
                binding.remoteView.showHideAvatar(true)
                binding.imgCamera.setImageResource(R.drawable.ic_video_off)
                (activity as DashBoardActivity).pauseVideo(false)
                (activity?.application as VdoTok).camView = false
            } else {
                binding.remoteView.showHideAvatar(false)
                (activity as DashBoardActivity).resumeVideo(false)
                binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
                (activity?.application as VdoTok).camView = true
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

            }

        }

        if (isIncomingCall) {
           screenRemoteViewReference.setOnClickListener {
               activity?.let { activity ->
                   screenRemoteViewReference.swapViews(activity,screenRemoteViewReference,videoRemoteViewReference)
                   swap = swap.not()
               }
           }
        } else {
            addTouchEventListener()
        }
        binding.imgscreenn.setOnClickListener {
            if (isScreenSharingCall) {
                    if (screenSharingMic && cameraCall || screenSharingApp && cameraCall){
                        binding.localView.showHideAvatar(true)
                    }else{
                        binding.remoteView.show()
                        binding.tvScreen.hide()
                        binding.remoteView.showHideAvatar(true)
                    }
                (activity as DashBoardActivity).pauseVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.ic_screensharing_off)
            } else {
                if (screenSharingMic && cameraCall || screenSharingApp && cameraCall){
                    binding.localView.showHideAvatar(false)
                }else{
                    binding.tvScreen.show()
                    binding.remoteView.hide()
                    binding.remoteView.showHideAvatar(false)
                }
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
        isIncomingCall: Boolean,
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean
    ) {
        if (!isAdded) {
            return
        }

            if (isIncomingCall) {
                binding.tvCallType.text = getString(R.string.incoming_calling)
                binding.imgMute.hide()
                binding.imgscreenn.hide()
                binding.imgCamera.hide()
                binding.internalAudio.hide()
                binding.ivCamSwitch.hide()
                binding.tvScreen.hide()

            } else {
                binding.ivSpeaker.hide()
                if (screenSharingApp && cameraCall) {
                    binding.tvCallType.text = getString(R.string.screen_video_calling)
                    binding.imgscreenn.show()
                    binding.imgCamera.show()
                    binding.tvScreen.hide()
                    binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
                    if (!isInternalAudioIncluded && screenSharingApp) {
                        binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                        binding.internalAudio.isEnabled = false
                    }

                    binding.ivCamSwitch.show()
                } else if (screenSharingMic && cameraCall) {
                    binding.tvCallType.text = getString(R.string.screen_video_calling)
                    binding.imgscreenn.show()
                    binding.imgCamera.show()
                    binding.internalAudio.hide()
                    binding.imgMute.show()
                    binding.tvScreen.hide()
                    binding.ivCamSwitch.show()
                    binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
                } else if (screenSharingApp) {
                    binding.tvCallType.text = getString(R.string.screen_calling)
                    if (!isInternalAudioIncluded && screenSharingApp) {
                        binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                        binding.internalAudio.isEnabled = false
                    }
                    binding.tvScreen.show()
                    binding.imgCamera.hide()
                    binding.ivCamSwitch.hide()
                    binding.imgMute.hide()
                    binding.localView.hide()
                    binding.remoteView.hide()
                } else if (screenSharingMic) {
                    binding.tvCallType.text = getString(R.string.screen_calling)
                    if (!isInternalAudioIncluded && screenSharingMic) {
                        binding.imgMute.show()
                    }
                    binding.tvScreen.show()
                    binding.imgCamera.hide()
                    binding.ivCamSwitch.hide()
                    binding.internalAudio.hide()
                    binding.localView.hide()
                    binding.remoteView.hide()
                } else if (cameraCall) {
                    binding.tvCallType.text = getString(R.string.video_calling)
                    binding.internalAudio.hide()
                    binding.imgscreenn.hide()
                    binding.ivCamSwitch.show()
                    binding.localView.hide()
                    binding.tvScreen.hide()
                    binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
                }
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
        if (callClient.isSpeakerEnabled()) {
            callClient.setSpeakerEnable(false)
            binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
        } else {
            callClient.setSpeakerEnable(true)
            binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
        }
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
        activity?.runOnUiThread {
            if ((activity as DashBoardActivity).callParams1?.sessionUUID == sessionID) {
                Log.e("remotestream","isinitializeFullScree")
                binding.remoteView.preview.setMirror(false)
                setUserNameUI(refId)
                try {
                    stream.addSink(binding.remoteView.setView())
                } catch (e: Exception) {
                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
                }

            } else {
                binding.localView.show()
                Log.e("remotestream","only else")
                try {
                    stream.addSink(binding.localView.setView())
                } catch (e: Exception) {
                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
                }

            }

            if ((activity as DashBoardActivity).isMultiSession) {
                (activity as DashBoardActivity).acceptMultiCall()
            }
        }

    }

    override fun onCreated(created: Boolean) {
        super.onCreated(created)
        activity?.runOnUiThread {
            binding.sessionEnable = created
        }
    }


    override fun onRemoteStreamReceived(refId: String, sessionID: String) {
    }


    private fun setUserNameUI( refId: String) {
         userName.set(name)
    }

    override fun onCameraStreamReceived(stream: VideoTrack) {
        val myRunnable = Runnable {
                try {
                    binding.localView.hide()
                    stream.addSink(binding.remoteView.setView())
                    binding.remoteView.preview.setMirror(true)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

        }
            activity?.let { Handler(it.mainLooper) }?.post(myRunnable)
    }
    override fun onCameraAudioOff(sessionStateInfo: SessionStateInfo, isMultySession: Boolean) {
        activity?.runOnUiThread {
            Log.d("sessionState", sessionStateInfo.toString())
            if (!(activity as DashBoardActivity).callParams1?.sessionUUID.isNullOrEmpty() && !(activity as DashBoardActivity).callParams2?.sessionUUID.isNullOrEmpty()) {
                if (sessionStateInfo.isScreenShare == true) {
                    when {
                        sessionStateInfo.videoState == 1 -> {
                            screenRemoteViewReference.showHideAvatar(false)
                        }
                        sessionStateInfo.videoState == 0 -> {
                            screenRemoteViewReference.showHideAvatar(true)
                        }
                        sessionStateInfo.audioState == 1 -> {
                            screenRemoteViewReference.showHideMuteIcon(false)
                        }
                        else -> {
                            screenRemoteViewReference.showHideMuteIcon(true)
                        }
                    }
                } else {
                    when {
                        sessionStateInfo.videoState == 1 -> {
                            videoRemoteViewReference.showHideAvatar(false)
                        }
                        sessionStateInfo.videoState == 0 -> {
                            videoRemoteViewReference.showHideAvatar(true)
                        }
                        sessionStateInfo.audioState == 1 -> {
                            screenRemoteViewReference.showHideMuteIcon(false)
                        }
                        else -> {
                            screenRemoteViewReference.showHideMuteIcon(true)
                        }
                    }
                }
            } else {
                when {
                    sessionStateInfo.videoState == 1 -> {
                        videoRemoteViewReference.showHideAvatar(false)
                    }
                    sessionStateInfo.videoState == 0 -> {
                        videoRemoteViewReference.showHideAvatar(true)
                    }
                    sessionStateInfo.audioState == 1 -> {
                        screenRemoteViewReference.showHideMuteIcon(false)
                    }
                    else -> {
                        screenRemoteViewReference.showHideMuteIcon(true)
                    }
                }
            }
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
            binding.remoteView.release()
            binding.localView.release()
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        } catch (e: Exception) {}
    }

    override fun onPublicURL(publicURL: String) {
    }

    override fun checkCallType() {
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

    override fun onInsufficientBalance() {
        closeFragmentWithMessage("Insufficient Balance!")
    }

    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            (activity as DashBoardActivity).callParams1 = null
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }
}