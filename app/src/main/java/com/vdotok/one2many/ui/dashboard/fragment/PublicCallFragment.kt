package com.vdotok.one2many.feature.dashBoard.fragment

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
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
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
import com.vdotok.one2many.VdoTok.Companion.getVdotok
import com.vdotok.one2many.databinding.LayoutFragmentCallPublicBinding
import com.vdotok.one2many.extensions.hide
import com.vdotok.one2many.extensions.show
import com.vdotok.one2many.extensions.showSnackBar
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.ui.dashboard.fragment.CallFragment
import com.vdotok.one2many.ui.dashboard.fragment.DialCallFragment
import com.vdotok.one2many.ui.dashboard.fragment.PublicDialCallFragment
import com.vdotok.one2many.utils.TimeUtils.getTimeFromSeconds
import com.vdotok.one2many.utils.performSingleClick
import com.vdotok.streaming.views.CallViewRenderer
import org.webrtc.EglBase
import org.webrtc.VideoTrack
import java.util.*



/**
 * Created By: VdoTok
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
    var isCamSwitch = false
    var rootEglBase: EglBase? = null

    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs

    private var isInternalAudioResume = false
    private var isMuted = false
    private var isaudioOff = true
    private var isVideoCall = true
    private var isVideoCall1 = true
    private var isCallTypeAudio = false
    private var callDuration = 0
    private var timer: Timer? = null
    private var timerTask: TimerTask? = null

    private var screenWidth = 0
    private var screenHeight = 0

    var user : String? = null
    var url : String? = null

    private val listUser =  ArrayList<Participants>()
    var count : Int? = 0
    var participantsCount = 0
    var loop = 0

    private lateinit var videoRemoteViewReference: CustomCallView

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
        videoRemoteViewReference = binding.remoteView
        initiateCallViews()

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

        callClient.setSpeakerEnable(true)
        binding.tvcount.text = participantsCount.toString()
        displayUi(screenSharingApp,screenSharingMic,cameraCall)

        binding.imgCallOff.performSingleClick {
            stopTimer()
            (activity as DashBoardActivity).endCall()
            binding.remoteView.release()
            Navigation.findNavController(binding.root).navigate(R.id.action_open_multiSelectionFragment)
        }

        binding.copyURL.setOnClickListener {
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
                screenSharingMic && cameraCall -> { (activity as DashBoardActivity).muteUnMuteCall(false)
                    (activity as DashBoardActivity).muteUnMuteCall(true) }
                cameraCall -> (activity as DashBoardActivity).muteUnMuteCall(false)
                else -> (activity as DashBoardActivity).muteUnMuteCall(true)
            }
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true // default to enabled
        ) { override fun handleOnBackPressed() {}
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            this.viewLifecycleOwner,
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
            if (isCallTypeAudio) {
                return@setOnClickListener
            }
            if (isVideoCall) {
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
            }

        }

        binding.imgscreenn.setOnClickListener {
            if (isVideoCall) {
                if (!cameraCall){
                    binding.remoteView.show()
                    binding.tvScreen.hide()
                    binding.remoteView.showHideAvatar(true)
                }
                (activity as DashBoardActivity).pauseVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.ic_screensharing_off)
            } else {
                if (!cameraCall){
                    binding.tvScreen.show()
                    binding.remoteView.hide()
                    binding.remoteView.showHideAvatar(false)
                }
                (activity as DashBoardActivity).resumeVideo(true)
                binding.imgscreenn.setImageResource(R.drawable.screen_sharing_icon)
            }
            isVideoCall = !isVideoCall
        }


        screenWidth = context?.resources?.displayMetrics?.widthPixels!!
        screenHeight = context?.resources?.displayMetrics?.heightPixels!!

        binding.root.setTag("1")

        (activity as DashBoardActivity).publicSessionUrl?.let {
            setPublicUrlValue(it)
        }

    }

    private fun initiateCallViews() {
        getVdotok()?.rootEglBaseContext?.let { binding.remoteView.initiateCallView(it) }
    }

    override fun onCreated(created: Boolean) {
        activity?.runOnUiThread {
            binding.sessionEnable = created
        }
    }

    /**
     * Function to set ui related to audio and video
     * @param videoCall videoCall to check whether its an audio or video call
     * */
    private fun displayUi(
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean
    ) {
        if (!isAdded) {
            return
        }

        if (screenSharingApp && cameraCall) {
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
            binding.imgscreenn.show()
            binding.imgCamera.show()
            binding.internalAudio.hide()
            binding.imgMute.show()
            binding.tvScreen.hide()
            binding.ivCamSwitch.show()
            binding.imgCamera.setImageResource(R.drawable.ic_call_video_rounded)
        } else if (screenSharingApp) {
            if (!isInternalAudioIncluded && screenSharingApp) {
                binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                binding.internalAudio.isEnabled = false
            }
            binding.tvScreen.show()
            binding.imgCamera.hide()
            binding.ivCamSwitch.hide()
            binding.imgMute.hide()
            binding.remoteView.hide()
        } else if (screenSharingMic) {
            if (!isInternalAudioIncluded && screenSharingMic) {
                binding.imgMute.show()
            }
            binding.tvScreen.show()
            binding.imgCamera.hide()
            binding.ivCamSwitch.hide()
            binding.internalAudio.hide()
            binding.remoteView.hide()
        } else if (cameraCall) {
            binding.internalAudio.hide()
            binding.imgscreenn.hide()
            binding.ivCamSwitch.show()
            binding.tvScreen.hide()
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
                openCallFragment(toPeer)
            }
        }

    }


    private fun openCallFragment(toPeer: GroupModel) {
        val bundle = Bundle()
        bundle.putParcelable(GroupModel.TAG, toPeer)
        bundle.putBoolean(DialCallFragment.IS_VIDEO_CALL, true)
        bundle.putBoolean("isIncoming", false)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_dial_fragment, bundle)
    }

    private fun setPublicUrlValue(publicURL: String) {
        activity?.runOnUiThread {
            if (publicURL.isNotEmpty()) binding.copyURL.show()
        }
        url = publicURL
    }

    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String, isCameraStream: Boolean) {

    }

    override fun onRemoteStreamReceived(refId: String, sessionID: String) {
    }


    override fun onCameraStreamReceived(stream: VideoTrack) {
        val myRunnable = Runnable {
            try {
                stream.addSink(binding.remoteView.setView())
                binding.remoteView.preview.setMirror(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        activity?.let { Handler(it.mainLooper) }?.post(myRunnable)
    }


    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {
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
        setPublicUrlValue(publicURL)
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

    private fun copyTextToClipboard() {
        val textToCopy = url
        val clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        binding.root.showSnackBar(getString(R.string.copy_url_text))
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