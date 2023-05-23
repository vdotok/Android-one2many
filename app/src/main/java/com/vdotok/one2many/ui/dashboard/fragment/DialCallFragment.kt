package com.vdotok.one2many.ui.dashboard.fragment

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.projection.MediaProjection
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.vdotok.network.models.GroupModel
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.FragmentDialCallBinding
import com.vdotok.one2many.extensions.hide
import com.vdotok.one2many.extensions.launchPeriodicAsync
import com.vdotok.one2many.extensions.show
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.models.AcceptCallModel
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.performSingleClick
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 2/25/21 At 12:14 PM in 2021
 *
 * This class displays incoming and outgoing call
 */
class DialCallFragment : CallMangerListenerFragment() {
    private var isIncomingCall: Boolean = false
    private lateinit var binding: FragmentDialCallBinding
    var groupModel: GroupModel? = null
    var username: String? = null

    var acceptCallModel: CallParams? = null
    private var groupList = ArrayList<GroupModel>()
    var isVideoCall: Boolean = false
    private var isInternalAudioIncluded = false
    var screenSharingApp: Boolean = false
    var screenSharingMic: Boolean = false
    var cameraCall: Boolean = false

    var userName: ObservableField<String> = ObservableField<String>()
    var incomingCallTitle: ObservableField<String> = ObservableField<String>()
    var player: MediaPlayer? = null
    private var timerFro30sec: Deferred<Unit>? = null
    private var refList = ArrayList<String>()

    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var participantsCount = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentDialCallBinding.inflate(inflater, container, false)
        prefs = Prefs(activity)
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        setArgumentsData()
        setBindingData()

        when {
            isIncomingCall -> setDataForIncomingCall()
            else -> setDataForDialCall()
        }


        if (isIncomingCall) {
            timerFro30sec = CoroutineScope(Dispatchers.IO).launchPeriodicAsync(1000 * 15) {
                test++
                if (test >= 2) {
                    activity?.runOnUiThread {
                        rejectCall()
                    }
                    timerFro30sec?.cancel()
                }
            }
        }

        return binding.root
    }

    var test = 0

    /**
     * Function to link binding data
     * */
    private fun setBindingData() {
        binding.username = userName
        binding.incomingCallTitle = incomingCallTitle
    }

    /**
     * Function to get ths pass data from other fragment
     * */
    private fun setArgumentsData() {
        groupList.clear()
        arguments?.get(GroupModel.TAG)?.let {
            isVideoCall = arguments?.getBoolean(IS_VIDEO_CALL) ?: false
            groupModel = it as GroupModel?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
            screenSharingApp = arguments?.getBoolean("screenApp") ?: false
            screenSharingMic = arguments?.getBoolean("screenMic") ?: false
            cameraCall = arguments?.getBoolean("video") ?: false
            isInternalAudioIncluded = arguments?.getBoolean("internalAudio") ?: false
        } ?: kotlin.run {
            groupList =
                arguments?.getParcelableArrayList<GroupModel>("grouplist") as ArrayList<GroupModel>
            username = arguments?.get("userName") as String?
            acceptCallModel = arguments?.get(AcceptCallModel.TAG) as CallParams?
            isIncomingCall = arguments?.get("isIncoming") as Boolean
        }
    }

    /**
     * Function to set data when outgoing call dial is implemented and setonClickListener
     * */
    private fun setDataForDialCall() {

        getUsername()
        refList.clear()
        groupModel?.participants?.forEach {
            refList.add(it.refID!!)
        }
        binding.imgCallAccept.hide()
        binding.imgmic.show()
        binding.imgCamera.show()
        incomingCallTitle.set(getString(R.string.calling))

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }

    }


    /**
     * Function to set user/users  name when outgoing call dial is implemented
     * */
    private fun getUsername() {
        groupModel.let { it ->
            if (groupModel?.autoCreated == 1) {
                it?.participants?.forEach { name ->
                    if (name.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        userName.set(name.fullname)

                    }
                }
            } else {
                var participantNames = ""
                it?.participants?.forEach {
                    if (it.fullname?.equals(prefs.loginInfo?.fullName) == false) {
                        participantNames += it.fullname.plus("\n")
                    }
                }
                userName.set(participantNames)
            }
        }
    }

    /**
     * Function to set data when incoming call dial is implemented and setonClickListener
     * */
    private fun setDataForIncomingCall() {
        player = MediaPlayer.create(this.requireContext(), Settings.System.DEFAULT_RINGTONE_URI)


        playTone()
        if (username.isNullOrEmpty()) {
            userName.set("User")
        } else {
            userName.set(username)
        }
        when (acceptCallModel?.sessionType) {
            SessionType.SCREEN -> {
                incomingCallTitle.set(getString(R.string.incoming_call))
            }
            else -> {
                incomingCallTitle.set(getString(R.string.incoming_video_call))
            }
        }

        binding.imgCallAccept.performSingleClick {
            acceptIncomingCall()
        }

        binding.imgCallReject.performSingleClick {
            rejectCall()
        }
    }

    private fun rejectCall() {
        timerFro30sec?.cancel()
        if (isIncomingCall) {
            prefs.loginInfo?.let {
                if ((activity as DashBoardActivity).callParams1 != null && (activity as DashBoardActivity).callParams2 != null) {
                    (activity as DashBoardActivity).callParams1?.let { it1 ->
                        (activity as DashBoardActivity).sessionIdList.remove(it1.sessionUuid)
                        callClient.rejectIncomingCall(
                            it.refId!!,
                            it1.sessionUuid
                        )
                    }
                    (activity as DashBoardActivity).callParams2?.let { it1 ->
                        (activity as DashBoardActivity).sessionIdList.remove(it1.sessionUuid)
                        callClient.rejectIncomingCall(
                            it.refId!!,
                            it1.sessionUuid
                        )

                    }
                } else if ((activity as DashBoardActivity).callParams1 != null) {
                    (activity as DashBoardActivity).callParams1?.let { it1 ->
                        (activity as DashBoardActivity).sessionIdList.remove(it1.sessionUuid)
                        callClient.rejectIncomingCall(
                            it.refId!!,
                            it1.sessionUuid
                        )

                    }
                } else {
                    (activity as DashBoardActivity).callParams2?.let { it1 ->
                        callClient.rejectIncomingCall(
                            it.refId!!,
                            it1.sessionUuid
                        )

                    }

                }
            }
        } else {
            (activity as DashBoardActivity).endCall()
        }
        (activity as DashBoardActivity).mLiveDataEndCall.postValue(true)
    }

    /**
     * Function to be call when incoming dial call is accepted
     * */
    private fun acceptIncomingCall() {

        acceptCallModel?.let {

            (activity as DashBoardActivity).acceptIncomingCall(
                it
            )
            openCallFragment()
        }
        timerFro30sec?.cancel()
    }

    private fun playTone() {
        player?.let {
            if (!it.isPlaying)
                player?.start()
        }
    }

    private fun stopTune() {
        player?.let {
            if (it.isPlaying)
                player?.stop()
        }
        player = null
    }

    override fun onDetach() {
        timerFro30sec?.cancel()
        super.onDetach()
        player?.stop()
    }

    override fun onDestroy() {
        Log.e("NavTest", "onDestroy: ")
        timerFro30sec?.cancel()
        (activity as DashBoardActivity).dialCallOpen = false
        super.onDestroy()
        stopTune()
    }

    override fun onDestroyView() {
        Log.e("NavTest", "onDestroyView: ")
        timerFro30sec?.cancel()
        (activity as DashBoardActivity).dialCallOpen = false
        super.onDestroyView()
        stopTune()
    }

    /**
     * Function to pass data to oter fragment in case of incoming call dial
     * */
    private fun openCallFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("grouplist", groupList)
        bundle.putString("userName", userName.get())
        bundle.putBoolean(IS_VIDEO_CALL, acceptCallModel?.mediaType == MediaType.VIDEO)
        bundle.putParcelable(AcceptCallModel.TAG, acceptCallModel)
        bundle.putInt("participant", participantsCount)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_call_fragment, bundle)
    }

    companion object {
        const val IS_VIDEO_CALL = "IS_VIDEO_CALL"

        const val TAG = "DialCallFragment"

        @JvmStatic
        fun newInstance() = DialCallFragment()

    }

    override fun onIncomingCall(model: CallParams) {}


    override fun onStartCalling() {
        activity?.let {
            it.runOnUiThread {
                val bundle = Bundle()
                bundle.putParcelable(GroupModel.TAG, groupModel)
                bundle.putBoolean(IS_VIDEO_CALL, isVideoCall)
                bundle.putBoolean("isIncoming", false)
                bundle.putBoolean("screenApp", screenSharingApp)
                bundle.putBoolean("screenMic", screenSharingMic)
                bundle.putBoolean("video", cameraCall)
                bundle.putInt("participantsCount", participantsCount)
                bundle.putBoolean("internalAudio", isInternalAudioIncluded)
                Navigation.findNavController(binding.root).navigate(
                    R.id.action_open_call_fragment,
                    bundle
                )
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
    ) {
    }

    override fun onCallRejected(reason: String) {
//     closeFragmentWithMessage("Call Missed!")
    }

    override fun onCallerAlreadyBusy() {
        closeFragmentWithMessage("Target is busy!")
    }

    override fun onParticipantLeftCall(refId: String?) {

    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
        //        TODO("Not yet implemented")
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
                (activity as DashBoardActivity).isMultiSession = false
                Navigation.findNavController(binding.root)
                    .navigate(R.id.action_open_selection_fragment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPublicURL(publicURL: String) {
        //// TODO("Not yet implemented")
    }

    override fun checkCallType() {
        if ((screenSharingApp && !isInternalAudioIncluded) || (screenSharingMic && !isInternalAudioIncluded)
            || (screenSharingApp && isInternalAudioIncluded)
        ) {
//            moveToDashboard()
        }
    }

    private fun moveToDashboard() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun closeFragmentWithMessage(message: String?) {
        activity?.runOnUiThread {
            (activity as DashBoardActivity).callParams1 = null
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            onCallEnd()
        }
    }

    override fun onInsufficientBalance() {
        closeFragmentWithMessage("Insufficient Balance!")
    }

}