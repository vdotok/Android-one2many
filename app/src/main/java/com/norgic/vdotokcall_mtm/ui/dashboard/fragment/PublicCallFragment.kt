package com.norgic.vdotokcall_mtm.feature.dashBoard.fragment

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.core.view.isVisible
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.norgic.callsdks.CallClient
import com.norgic.callsdks.models.CallParams
import com.norgic.callsdks.models.SessionStateInfo
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.LayoutFragmentCallPublicBinding
import com.norgic.vdotokcall_mtm.extensions.hide
import com.norgic.vdotokcall_mtm.extensions.show
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.fragments.CallMangerListenerFragment
import com.norgic.vdotokcall_mtm.models.GroupModel
import com.norgic.vdotokcall_mtm.models.Participants
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.ui.dashboard.fragment.DialCallFragment
import com.norgic.vdotokcall_mtm.ui.dashboard.fragment.PublicDialCallFragment
import com.norgic.vdotokcall_mtm.utils.TimeUtils.getTimeFromSeconds
import com.norgic.vdotokcall_mtm.utils.performSingleClick
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import java.util.*



/**
 * Created By: Norgic
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays the on connected call
 */
class PublicCallFragment : CallMangerListenerFragment() {
    private var isIncomingCall = false
    private lateinit var binding: LayoutFragmentCallPublicBinding
    private var isInternalAudioIncluded = false
    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false
    var multi :Boolean = false
    var rootEglBase: EglBase? = null



    private lateinit var callClient: CallClient
    private var groupModel : GroupModel? = null
    private lateinit var prefs: Prefs

    private var userName : ObservableField<String> = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()
    private var isInternalAudioResume = false
    private var isMuted = false
    private var isSpeakerOff = true
    private var isaudioOff = true
    private var isVideoCall = true
    private var isVideoCall1 = true
    private var isCallTypeAudio = false
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
    var url : String? = null

    private val listUser =  ArrayList<Participants>()
    var count : Int? = 0
    var participantsCount = 0
    var loop = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = LayoutFragmentCallPublicBinding.inflate(inflater, container, false)
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


        binding.tvCallType.text = getString(R.string.your_url)

        (activity as DashBoardActivity).mListener = this

        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

            isIncomingCall = arguments?.get("isIncoming") as Boolean
            screenSharingApp = arguments?.getBoolean("screenApp")?: false
            screenSharingMic = arguments?.getBoolean("screenMic")?: false
            cameraCall = arguments?.getBoolean("video")?: false
            isVideoCall = arguments?.getBoolean(PublicDialCallFragment.IS_VIDEO_CALL) ?: false
            isVideoCall1 = arguments?.getBoolean(PublicDialCallFragment.IS_VIDEO_CALL)?: false
            isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false
            url = arguments?.getString("url")
            participantsCount = arguments?.getInt("participantCount")!!
            multi =  arguments?.getBoolean("multi")?: false


        binding.tvcount.text = participantsCount.toString()
        displayUi(isVideoCall,isIncomingCall,screenSharingApp,screenSharingMic,cameraCall,count)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            (activity as DashBoardActivity).endCall()
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        }

        binding.tvCallType.setOnClickListener {
            copyTextToClipboard()
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
            isSpeakerOff = isSpeakerOff.not()
            if (isSpeakerOff) {
                binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_off)
            } else {
                binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
            }
            callClient.toggleSpeakerOnOff()
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true // default to enabled
        ) { override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,
            callback
        )
        binding.ivCamSwitch.setOnClickListener { (activity as DashBoardActivity).switchCamera() }

        binding.imgCamera.setOnClickListener {
            if (isCallTypeAudio) {
                return@setOnClickListener
            }
            if (isVideoCall) {
                binding.localView.hide()
                (activity as DashBoardActivity).pauseVideo(false)
                binding.imgCamera.setImageResource(R.drawable.ic_video_off)
            } else {
                binding.localView.show()
                refreshLocalCameraView()
                (activity as DashBoardActivity).resumeVideo(false)
                binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
            }
            isVideoCall = !isVideoCall
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

        binding.imgscreenn.setOnClickListener {
            if (isVideoCall1) {
                (activity as DashBoardActivity).pauseVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.ic_screensharing_off)
            } else {
                (activity as DashBoardActivity).resumeVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.screen_sharing_icon)
            }
            isVideoCall1 = !isVideoCall1
        }
        addTouchEventListener()

        screenWidth = context?.resources?.displayMetrics?.widthPixels!!
        screenHeight = context?.resources?.displayMetrics?.heightPixels!!

        (activity as DashBoardActivity).localStreamVideo?.let { onCameraStreamReceived(it) }

        binding.root.setTag("1")


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
        cameraCall: Boolean,
        count: Int?
    ) {
        if (!isAdded) {
            return
        }
        if(screenSharingApp && cameraCall){
            binding.imgscreenn.show()
            binding.imgCamera.show()
            if (!isInternalAudioIncluded && screenSharingApp){
                binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                binding.internalAudio.isEnabled = false
            }
            binding.ivCamSwitch.show()
        }else if (screenSharingMic && cameraCall){
            binding.imgscreenn.show()
            binding.imgCamera.show()
            binding.internalAudio.hide()
            binding.imgMute.show()
            binding.ivCamSwitch.show()
        }else if (screenSharingApp){
            if (!isInternalAudioIncluded && screenSharingApp){
                binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                binding.internalAudio.isEnabled = false
            }
            binding.imgCamera.hide()
            binding.ivCamSwitch.hide()
            binding.imgMute.hide()
           // binding.tvcount.text = count.toString()
        }else if(screenSharingMic){
            if (!isInternalAudioIncluded && screenSharingMic){
                binding.imgMute.show()
            }
            binding.imgCamera.hide()
            binding.ivCamSwitch.hide()
            binding.internalAudio.hide()
           // binding.tvcount.text = count.toString()
        }else if (cameraCall){
            binding.internalAudio.hide()
            binding.imgscreenn.hide()
            binding.ivCamSwitch.show()
           // binding.tvcount.text = count.toString()
        }

        if(videoCall) {
            binding.groupAudioCall.hide()
            binding.remoteView.show()

            binding.ivSpeaker.setImageResource(R.drawable.ic_speaker_on)
            binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
        }
    }

//    private fun updateCallUIViews(listUser: List<Participants>?) {
//
//        listUser?.let {
//            if (it.size == 1) {
//                val params = binding.containerVideoFrame.container1.root.layoutParams as ConstraintLayout.LayoutParams
//                params.endToEnd = binding.containerVideoFrame.containerParent.id
//                params.bottomToBottom = binding.containerVideoFrame.containerParent.id
//                binding.containerVideoFrame.container1.root.layoutParams = params
//                binding.containerVideoFrame.container1.root.requestLayout()
//
//                ViewCompat.setElevation(binding.containerVideoFrame.container1.root, -1f)
//
//            }
//        }
//        Log.e("user", "user-list size " + listUser?.size)
//
//    }

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


    companion object {
        const val VOICE_CALL = "VoiceCallFragment"
        const val THRESHOLD_VALUE = 70.0f

        @JvmStatic
        fun newInstance() = CallFragment()
    }

    override fun onIncomingCall(model: CallParams) {
    }

    override fun onStartCalling() {}

    override fun outGoingCall(toPeer: GroupModel) {
        activity?.let {
            it.runOnUiThread {
                openCallFragment(toPeer, isVideoCall)
            }
        }

    }


    private fun openCallFragment(toPeer: GroupModel, videoCall: Boolean) {
        val bundle = Bundle()
        bundle.putParcelable(GroupModel.TAG, toPeer)
        bundle.putBoolean(DialCallFragment.IS_VIDEO_CALL, true)
        bundle.putBoolean("isIncoming", false)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_dial_fragment, bundle)
    }

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String, isCameraStream: Boolean) {

//        activity?.runOnUiThread {
//            Log.i("Socket", "onRemoteStreamReceived: isFullStreamingView : " + isFullStreamingView + " -- isCameraStream : "+ isCameraStream)
//
//            if (!isFullStreamingView) {
//                isFullStreamingView = true
//
//                try {
//
//                    val videoView = binding.containerVideoFrame.container1.remoteView
//
//                    videoView.setMirror(false)
//                    val rootEglBase = EglBase.create()
//                    videoView.init(rootEglBase.eglBaseContext, null)
//                    videoView.setZOrderMediaOverlay(false)
//                    videoView.setZOrderOnTop(false)
//                    videoView.setEnableHardwareScaler(true)
//                    ViewCompat.setElevation(videoView, -1f)
//
//                    stream.addSink(videoView)
//                    binding.localView.setZOrderMediaOverlay(true)
//                    binding.localView.setEnableHardwareScaler(true)
//                    binding.localView.setZOrderOnTop(true)
//
//                } catch (e: Exception) {
//                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
//                }
//
//            } else {
//                try {
//
//                    binding.containerVideoFrame.container1.remoteView.setZOrderMediaOverlay(false)
//                    binding.containerVideoFrame.container1.remoteView.setEnableHardwareScaler(false)
//
//                    val videoView = binding.localView
//                    videoView.setMirror(false)
//                    val rootEglBase = EglBase.create()
//                    videoView.init(rootEglBase.eglBaseContext, null)
//                    videoView.setZOrderMediaOverlay(true)
//                    videoView.setZOrderOnTop(true)
//                    videoView.setEnableHardwareScaler(true)
//                    ViewCompat.setElevation(videoView, 11f)
//                    videoView.show()
//
//                    stream.addSink(videoView)
//                    setUserNameUI(refId)
//                    if (isSpeakerOff) {
//                        isSpeakerOff = false
//                        videoView.postDelayed({
//                            callClient.toggleSpeakerOnOff()
//                        }, 1000)
//                    }
//
//                    binding.containerVideoFrame.container1.remoteView.setZOrderMediaOverlay(false)
//                    binding.containerVideoFrame.container1.remoteView.setEnableHardwareScaler(false)
//
//                } catch (e: Exception) {
//                    Log.i("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
//                }
//
//            }
//        }
    }

    override fun onRemoteStreamReceived(refId: String, sessionID: String) {
//        val mainHandler = activity?.mainLooper?.let { Handler(it) }
//        val myRunnable = Runnable {
//
//            try {
//
//                val container = binding.containerVideoFrame.container1
//                listUser.add(Participants(null, null, null, null, refId, null))
//                updateCallUIViews(listUser)
//                container.groupAudioCall.show()
//                container.remoteView.hide()
//            } catch (e: Exception) {
//                Log.e("SocketLog", "onStreamAvailable: exception" + e.printStackTrace())
//            }
//
//        }
//        mainHandler?.post(myRunnable)
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


    override fun onCameraStreamReceived(stream: VideoTrack) {
        Log.e("one2many", "onCameraStreamReceived")
        if (!(activity as DashBoardActivity).isCallInitiator2)
            return
        val myRunnable = Runnable {
            if (screenSharingApp || screenSharingMic) {
                try {
                    binding.localView.show()
                    val videoView = binding.localView
                    videoView.setMirror(true)
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(true)
                    videoView.setEnableHardwareScaler(true)
                    stream.addSink(videoView)
                    callClient.setView(videoView)
                    refreshLocalCameraView()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (cameraCall){
                try {
                    val videoView = binding.remoteView
                    videoView.setMirror(false)
                    videoView.init(rootEglBase?.eglBaseContext, null)
                    videoView.setZOrderMediaOverlay(true)
                    videoView.setEnableHardwareScaler(true)
                    stream.addSink(videoView)
                    callClient.setView(videoView)
                    refreshLocalCameraView()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        activity?.let { Handler(it.mainLooper) }?.post(myRunnable)
    }

    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {
//        activity?.runOnUiThread {
//            val view = userMap.get(refId)
//            view?.let {
//
//                if (videoState == 0) {
//                    view.groupAudioCall.show()
//                    view.remoteView.hide()
//                } else {
//                    view.groupAudioCall.hide()
//                    view.remoteView.show()
//
//                    userMap.get(refId)?.let {
//                        refreshLocalCameraView(it.remoteView)
//                    }
//                }
//            }
//        }
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
         url = publicURL
    }

    override fun checkCallType() {
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
                                        ((screenWidth - (view.width + CallFragment.THRESHOLD_VALUE))),
                                        (screenHeight / 2 - (view.height + CallFragment.THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint < screenHeight / 2 -> {
                                    //Second Quadrant
                                    animateView(
                                        CallFragment.THRESHOLD_VALUE,
                                        (screenHeight / 2 - (view.height + CallFragment.THRESHOLD_VALUE))
                                    )
                                }
                                xPoint < screenWidth / 2 && yPoint > screenHeight / 2 -> {
                                    //Third Quadrant
                                    animateView(
                                        CallFragment.THRESHOLD_VALUE,
                                        (screenHeight / 2 + view.height / 2).toFloat()
                                    )
                                }
                                else -> {
                                    //Fourth Quadrant
                                    animateView(
                                        ((screenWidth - (view.width + CallFragment.THRESHOLD_VALUE))),
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



    private fun copyTextToClipboard() {
        val textToCopy = url
        val clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        binding.root.showSnackBar(getString(R.string.copy_url_text))
    }




}