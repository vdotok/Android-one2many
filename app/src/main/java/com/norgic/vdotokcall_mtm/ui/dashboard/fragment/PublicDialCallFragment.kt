package com.norgic.vdotokcall_mtm.ui.dashboard.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.norgic.callsdks.CallClient
import com.norgic.callsdks.models.CallParams
import com.norgic.callsdks.models.SessionStateInfo
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.FragmentDialCallPublicBinding
import com.norgic.vdotokcall_mtm.extensions.hide
import com.norgic.vdotokcall_mtm.extensions.show
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.fragments.CallMangerListenerFragment
import com.norgic.vdotokcall_mtm.models.GroupModel
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.utils.performSingleClick
import org.webrtc.VideoTrack


/**
 * Created By: Norgic
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays incoming and outgoing call
 */
class PublicDialCallFragment : CallMangerListenerFragment() {
    private var isIncomingCall: Boolean = false
    private lateinit var binding: FragmentDialCallPublicBinding
    var groupModel : GroupModel? = null
    var username : String? = null

    var acceptCallModel : CallParams? = null
    var isVideoCall: Boolean = false

    private var isInternalAudioIncluded = false
    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false
    var url : String? = null
    var multi : Boolean = false
    var participantsCount = 0


    var userName : ObservableField<String> = ObservableField<String>()

    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDialCallPublicBinding.inflate(inflater, container, false)
        prefs = Prefs(activity)
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        setArgumentsData()
        setDataForDialCall()

        return binding.root
    }



    /**
     * Function to get ths pass data from other fragment
     * */
    private fun setArgumentsData() {

            isVideoCall = arguments?.getBoolean(IS_VIDEO_CALL) ?: false
            isIncomingCall = arguments?.getBoolean("isIncoming") ?: false
            screenSharingApp = arguments?.getBoolean("screenApp")?: false
            screenSharingMic = arguments?.getBoolean("screenMic")?: false
            cameraCall = arguments?.getBoolean("video")?: false
            url = arguments?.getString("url")
            multi = arguments?.getBoolean("multi")?: false
            isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false

    }
    /**
     * Function to set data when outgoing call dial is implemented and setonClickListener
     * */
    private fun setDataForDialCall() {
        binding.btnCopy.setOnClickListener {
            copyTextToClipboard()
        }

        binding.imgCallOff.performSingleClick {
            rejectCall()
        }
        if(screenSharingApp && cameraCall){
            binding.imgscreenn.show()
            binding.imgCamera.show()
            if (!isInternalAudioIncluded && screenSharingApp){
                binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                binding.internalAudio.isEnabled = false
            }
            binding.imgMute.hide()
        }else if (screenSharingMic && cameraCall){
            binding.imgscreenn.show()
            binding.imgCamera.show()
            binding.internalAudio.hide()
            binding.imgMute.show()
        }else if (screenSharingApp){
            if (!isInternalAudioIncluded && screenSharingApp){
                binding.internalAudio.setImageResource(R.drawable.ic_internal_audio_disable)
                binding.internalAudio.isEnabled = false
            }
            binding.imgCamera.hide()
            binding.imgMute.hide()
        }else if(screenSharingMic){
            if (!isInternalAudioIncluded && screenSharingMic){
                binding.imgMute.show()
            }
            binding.imgCamera.hide()
            binding.internalAudio.hide()
        }else if (cameraCall){
            binding.internalAudio.hide()
            binding.imgscreenn.hide()
        }
    }
    private fun copyTextToClipboard() {
        val textToCopy = url
        val clipboardManager = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", textToCopy)
        clipboardManager.setPrimaryClip(clipData)
        binding.root.showSnackBar(getString(R.string.copy_url_text))
    }
    fun rejectCall() {
        if (isIncomingCall) {
            prefs.loginInfo?.let {
                acceptCallModel?.let { it1 -> callClient.rejectIncomingCall(
                    it.refId!!,
                    it1.sessionUUID
                )
                }
            }
        } else {
            (activity as DashBoardActivity).endCall()
        }
        try {
            Navigation.findNavController(binding.root).navigate(R.id.action_open_selection_fragment)
        } catch (e: Exception) {}
    }



    companion object {
        const val IS_VIDEO_CALL = "IS_VIDEO_CALL"

        const val TAG = "PublicDialCallFragment"
        @JvmStatic
        fun newInstance() = PublicDialCallFragment()

    }

    override fun onIncomingCall(model: CallParams) {}


    override fun onStartCalling() {
        activity?.let {
            it.runOnUiThread {
                val bundle = Bundle()
                bundle.putParcelable(GroupModel.TAG, groupModel)
                bundle.putBoolean(IS_VIDEO_CALL, isVideoCall)
                bundle.putBoolean("isIncoming", false)
                bundle.putBoolean("screenApp",screenSharingApp)
                bundle.putBoolean("screenMic",screenSharingMic)
                bundle.putBoolean("video",cameraCall)
                bundle.putBoolean("internalAudio",isInternalAudioIncluded)
                bundle.putString("url",url)
                bundle.putInt("participantCount",participantsCount)
                bundle.putBoolean("multi",multi)
                try {
                    Navigation.findNavController(binding.btnCopy).navigate(R.id.action_open_call_public_fragment, bundle)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun outGoingCall(toPeer: GroupModel) {
        closeFragmentWithMessage("Call Missed!")
    }

//    override fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}
    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {}

    override fun onCallRejected(reason: String) {
//        closeFragmentWithMessage(reason)
    }

    override fun onParticipantLeftCall(refId: String?) {

    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
    }

    override fun acceptedUser(participantCount: Int) {
        participantsCount = participantCount - 1
    }


    override fun onCallMissed() {
       closeFragmentWithMessage("Call Missed!")
    }

    override fun onCallEnd() {
        activity?.runOnUiThread {
            try {
                Navigation.findNavController(binding.root).navigate(R.id.action_open_selection_fragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPublicURL(publicURL: String) {
       url = publicURL
    }

    override fun checkCallType() {
        //        TODO("Not yet implemented")
    }

    private fun moveToDashboard() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }
    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }

}