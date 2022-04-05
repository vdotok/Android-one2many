package com.vdotok.one2many.ui.dashboard.fragment

import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableField
import androidx.navigation.Navigation
import com.vdotok.network.models.GroupModel
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.CallType
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.FragmentMultiSelectionBinding
import com.vdotok.one2many.dialogs.CreateUrlDialog
import com.vdotok.one2many.dialogs.CreateUrlLinkDialog
import com.vdotok.one2many.dialogs.CreateUrlLinkDialog2
import com.vdotok.one2many.extensions.hide
import com.vdotok.one2many.extensions.showSnackBar
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.models.AcceptCallModel
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.service.ProjectionService
import com.vdotok.one2many.ui.account.AccountsActivity
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.*
import org.webrtc.VideoTrack


class MultiSelectionFragment : CallMangerListenerFragment() {

    private lateinit var binding: FragmentMultiSelectionBinding
    private lateinit var callClient: CallClient
    private lateinit var prefs: Prefs


    var groupModel : GroupModel? = null

    var userName = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()
    var isInternalAudioIncluded :Boolean = false
    var screenSharingApp :Boolean = false
    var screenSharingMic : Boolean = false
    var cameraCall : Boolean = false

    var groupCast = false
    var publicCast = false
    var user : String? = null
    var url : String? = null
    var multiSelect :Boolean = false
    var urlCheck :Boolean = false
    var count :Int = 0



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMultiSelectionBinding.inflate(inflater, container, false)

        prefs = Prefs(activity)

        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        init()
        return binding.root
    }

    override fun onIncomingCall(model: CallParams) {
        activity?.runOnUiThread {
            val bundle = Bundle()
            bundle.putParcelableArrayList("grouplist", groupList)
            bundle.putString("userName", model.customDataPacket?.toString())
            bundle.putParcelable(AcceptCallModel.TAG, model)
            bundle.putBoolean("isIncoming", true)
            bundle.putBoolean(DialCallFragment.IS_VIDEO_CALL, model.mediaType == MediaType.VIDEO)
            Navigation.findNavController(binding.root).navigate(R.id.action_open_dial_fragment, bundle)
        }
    }

    override fun onStartCalling() {
//        TODO("Not yet implemented")
    }

    override fun outGoingCall(toPeer: GroupModel) {
    }


    override fun onCameraStreamReceived(stream: VideoTrack) {
//        TODO("Not yet implemented")
    }

    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {
       //// TODO("Not yet implemented")
    }

    override fun onCallMissed() {
       //// TODO("Not yet implemented")
    }

    override fun onCallRejected(reason: String) {
       //// TODO("Not yet implemented")
    }

    override fun onPublicURL(publicURL: String) {
        if (publicURL.isNullOrEmpty()) {
            urlCheck = false
        } else {
            urlCheck = true
            url = publicURL
        }
    }

    override fun onParticipantLeftCall(refId: String?) {
//        TODO("Not yet implemented")
    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
        if (multiSelect){
            initiatePublicMultiBroadcast(isInternalAudioIncluded, mediaProjection,false)
        }else {
            startSession(mediaProjection)
        }
    }

    override fun acceptedUser(participantCount: Int) {
    count = 0
    }


    private fun initiatePublicMultiBroadcast(internalAudioIncluded: Boolean, mediaProjection: MediaProjection?, isGroupSession: Boolean) {
        val refIdList = ArrayList<String>()

        if (callClient.isConnected() == true) {

            prefs.loginInfo?.let {
                (activity as DashBoardActivity).dialOne2ManyPublicCall(
                    callParams = CallParams(
                        refId = it.refId!!,
                        toRefIds = refIdList,
                        callType = CallType.ONE_TO_MANY,
                        isAppAudio = isInternalAudioIncluded,
                        customDataPacket = (activity as DashBoardActivity).callerName.toString()
                    ),
                    mediaProjection,
                    isGroupSession
                )
            }
//            Handler(Looper.getMainLooper()).postDelayed({
//                activity?.supportFragmentManager?.let {
//                    CreateUrlLinkDialog2(callClient,url = true, this::navToPublicCall).show(
//                        it,
//                        CreateUrlLinkDialog.TAG
//                    )
//                }
//            },2000)
            navToPublicCall()

            } else {
            (activity as DashBoardActivity).connectClient()
        }

    }
    private fun navToPublicCall() {
        openPublicDialCallFragment(
            screenSharingApp,
            screenSharingMic,
            cameraCall,
            isInternalAudioIncluded,
            true,
            url
        )
    }

    override fun onResume() {
        super.onResume()
        if (callClient.isConnected() == true) {
            binding.tvLed.setImageResource(R.drawable.led_connected)
        } else {
            binding.tvLed.setImageResource(R.drawable.led_error)
        }
    }

    override fun onConnectionSuccess() {
        binding.tvLed.setImageResource(R.drawable.led_connected)
    }

    override fun onConnectionFail() {
        binding.tvLed.setImageResource(R.drawable.led_error)
    }

    override fun checkCallType() {
//   TODO("Not yet implemented")
    }

    private fun init() {
        prefs = Prefs(this.context)
        binding.customToolbar.tvTitle.text = getString(R.string.avail_feature)
        binding.customToolbar.imgArrowBack.hide()
        binding.customToolbar.imgDone.hide()

        binding.screenSharingApp = false
        binding.screenSharingMic = false
        binding.cameraCall = false
        binding.optionSelected = false

        binding.radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            when(i){
                binding.publicCast.id->{
                   groupCast = false
                   publicCast = true
                }
                binding.groupCast.id->{
                    groupCast = true
                    publicCast = false
                }
            }
        }

        binding.btnSharingAppAudio.setOnClickListener {
            if (binding.screenSharingApp == true){
                screenSharingApp = false
                binding.screenSharingApp = false
                binding.optionSelected = binding.cameraCall == true
            }else if (binding.screenSharingMic == true){
                screenSharingApp = false
                binding.screenSharingApp = false
                binding.root.showSnackBar("Screen Sharing with mic audio is already selected")
            }else {
                screenSharingApp = true
                binding.screenSharingApp = true
                binding.btnConfirm.isEnabled = true
                binding.optionSelected = true
            }

        }

        binding.btnSharingMicAudio.setOnClickListener {
            if (binding.screenSharingMic == true){
                screenSharingMic = false
                binding.screenSharingMic = false
                binding.optionSelected = binding.cameraCall == true
            }else if (binding.screenSharingApp == true){
                screenSharingMic = false
                binding.screenSharingMic = false
                binding.root.showSnackBar("Screen Sharing with app audio is already selected")
            }else {
                screenSharingMic = true
                binding.screenSharingMic = true
                binding.btnConfirm.isEnabled = true
                binding.optionSelected = true
            }

        }

        binding.btnCamera.setOnClickListener {
            if (binding.cameraCall == true){
                cameraCall = false
                binding.cameraCall = false
                binding.optionSelected = binding.screenSharingMic == true || binding.screenSharingApp == true
            }else {
                cameraCall = true
                binding.cameraCall = true
                binding.btnConfirm.isEnabled = true
                binding.optionSelected = true
            }

        }

        binding.btnConfirm.setOnClickListener {
            if(callClient.isConnected() == true ){
            if (screenSharingApp && cameraCall && groupCast){
                binding.btnConfirm.isEnabled = true
                binding.optionSelected = true
                checkVersion(screenSharingApp,screenSharingMic,cameraCall)
            }else if(screenSharingApp && groupCast ){
                binding.btnConfirm.isEnabled = true
                binding.optionSelected = true
                checkVersion(screenSharingApp,screenSharingMic,cameraCall)
            }else if (screenSharingMic && cameraCall && groupCast){
                isInternalAudioIncluded = false
                multiSelect = true
                openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded,multiSelect)
            }else if (screenSharingMic && groupCast || cameraCall && groupCast) {
                isInternalAudioIncluded = false
                multiSelect = false
                openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded,multiSelect)
            } else if (!screenSharingMic && !screenSharingApp && !cameraCall){
                binding.btnConfirm.isEnabled = false
                binding.optionSelected = false
            }else if ((screenSharingApp && cameraCall && publicCast) || (screenSharingApp && publicCast )
                || (screenSharingMic && publicCast)
                || (cameraCall && publicCast) ){
                activity?.supportFragmentManager?.let { CreateUrlDialog(this::checkVersionPublic).show(it, CreateUrlDialog.TAG) }
            }else{
                binding.root.showSnackBar("Kindly select the type of broadcast")
            }
            }else{
                binding.root.showSnackBar("Socket Disconnect")
                (activity as DashBoardActivity).connectClient()

            }

        }

        binding.username = userName
        userName.set(prefs.loginInfo?.fullName)

        binding.tvLogout.setOnClickListener {
            prefs.deleteKeyValuePair(ApplicationConstants.LOGIN_INFO)
            startActivity(AccountsActivity.createAccountsActivity(this.requireContext()))
        }

    }

    /**
     * Function to get UserName at incoming side
     * @param model model object is used to get username from the list of user achieved from server
     * */
    private fun getUsername(refId: String) : String? {
        groupList.let {
            it.forEach { name ->
                name.participants.forEach { username->
                    if (username.refID?.equals(refId) == true) {
                        user = username.fullname
                        return user
                    }
                }
            }
        }
        return user
    }

    private fun checkVersion(screenSharingApp: Boolean, screenSharingMic: Boolean, cameraCall: Boolean) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                dialogInternalAudio(screenSharingApp,screenSharingMic,cameraCall)
            } else {
                dialogAlert(screenSharingApp,screenSharingMic,cameraCall)
            }
    }

    private fun dialogAlert(screenSharingApp: Boolean, screenSharingMic: Boolean, cameraCall: Boolean) {
        showAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                    isInternalAudioIncluded = false
                   if (screenSharingApp && cameraCall || screenSharingMic && cameraCall){
                    multiSelect = true
                   }else{
                       multiSelect = false
                   }
                    openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded,multiSelect)

            }
        })
    }

    private fun dialogInternalAudio(screenSharingApp: Boolean, screenSharingMic: Boolean, cameraCall: Boolean) {
        showInternalAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (which.equals(DialogInterface.BUTTON_POSITIVE)){
                    isInternalAudioIncluded = true
                    if (screenSharingApp && cameraCall || screenSharingMic && cameraCall){
                        multiSelect = true
                    }else{
                        multiSelect = false
                    }
                    openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded,multiSelect)
                }else{
                    isInternalAudioIncluded = false
                    if (screenSharingApp && cameraCall || screenSharingMic && cameraCall){
                        multiSelect = true
                    }else{
                        multiSelect = false
                    }
                    openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded,multiSelect)

                }
            }
        })
    }


    private fun checkVersionPublic() {
        (activity as DashBoardActivity).incomingName = "Public Group"
        (activity as DashBoardActivity).incomingUserName()
        if (screenSharingApp && cameraCall){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
                multiSelect = true
                dialogInternalAudioPublic(multiSelect)
            } else{
                multiSelect = true
                dialogAlertPublic(multiSelect)
            }
        }else if (screenSharingMic && cameraCall) {
            multiSelect = true
            isInternalAudioIncluded = false
            startScreenCapture()
        }else if (cameraCall){
            dialOneToOneCall(MediaType.VIDEO,SessionType.CALL)
        }else if (screenSharingMic){
            multiSelect = false
            isInternalAudioIncluded = false
            startScreenCapture()
        }else if(screenSharingApp){
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
                multiSelect = false
                dialogInternalAudioPublic(multiSelect)
            } else{
                multiSelect = false
                dialogAlertPublic(multiSelect)
            }
        }

    }

    fun dialogAlertPublic(multiSelect: Boolean) {
        showAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                isInternalAudioIncluded = false
                this@MultiSelectionFragment.multiSelect = multiSelect
                startScreenCapture()
            }
        })
    }


    private fun dialogInternalAudioPublic(multiSelect: Boolean) {
        showInternalAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                if (which.equals(DialogInterface.BUTTON_POSITIVE)){
                    this@MultiSelectionFragment.multiSelect = multiSelect
                    isInternalAudioIncluded = true
                    startScreenCapture()
                }else{
                    isInternalAudioIncluded = false
                    this@MultiSelectionFragment.multiSelect = multiSelect
                    startScreenCapture()
                }
            }
        })
    }

    private fun openGroupFragment(
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean,
        isInternalAudioIncluded: Boolean,
        multiSelect: Boolean
    ) {
       val bundle = Bundle()
        bundle.putBoolean("screenApp",screenSharingApp)
        bundle.putBoolean("screenMic",screenSharingMic)
        bundle.putBoolean("video",cameraCall)
        bundle.putBoolean("internalAudio",isInternalAudioIncluded)
        bundle.putBoolean("multiSelect",multiSelect)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList,bundle)
    }
    private fun openPublicDialCallFragment(
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean,
        isInternalAudioIncluded: Boolean,
        isVideoCall: Boolean,
        url: String?
    ) {
        val bundle = Bundle()
        bundle.putBoolean("screenApp",screenSharingApp)
        bundle.putBoolean("screenMic",screenSharingMic)
        bundle.putBoolean(PublicDialCallFragment.IS_VIDEO_CALL,isVideoCall)
        bundle.putBoolean("video",cameraCall)
        bundle.putBoolean("isIncoming", false)
        bundle.putBoolean("internalAudio",isInternalAudioIncluded)
        bundle.putString("url",url)
        bundle.putBoolean("multi",multiSelect)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_call_public_fragment,bundle)
    }
    var mService: ProjectionService? = null
    var mBound = false

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ProjectionService.LocalBinder = service as ProjectionService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    @TargetApi(21)
    fun startScreenCapture() {
        val intent = Intent(context, ProjectionService::class.java)
        context?.bindService(intent, mConnection, AppCompatActivity.BIND_AUTO_CREATE)

        val mediaProjectionManager =
            activity?.application?.getSystemService(AppCompatActivity.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(),
            callClient.CAPTURE_PERMISSION_REQUEST_CODE
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == callClient.CAPTURE_PERMISSION_REQUEST_CODE) {
                context?.let {
                    callClient.initSession(data, resultCode, it, isInternalAudioIncluded)
                }
            }
        }
    }


    private fun startSession(mediaProjection: MediaProjection?) {
        val refIdList = ArrayList<String>()

        if (callClient.isConnected() == true) {

            prefs.loginInfo?.let {
                (activity as DashBoardActivity).dialOne2ManyCall(
                    CallParams(
                        refId = it.refId.toString(),
                        toRefIds = refIdList,
                        mcToken = it.mcToken.toString(),
                        mediaType = MediaType.VIDEO,
                        callType = CallType.ONE_TO_MANY,
                        sessionType = SessionType.SCREEN,
                        isAppAudio = isInternalAudioIncluded,
                        isBroadcast = 1,
                        customDataPacket = (activity as DashBoardActivity).callerName.toString()
                        
                    ),
                    mediaProjection
                )
            }
//            activity?.supportFragmentManager?.let { CreateUrlLinkDialog(callClient,url = true,this::navToPublicCall).show(it, CreateUrlLinkDialog.TAG) }
            navToPublicCall()
        } else {
            (activity as DashBoardActivity).connectClient()
        }
    }

    private fun dialOneToOneCall(mediaType: MediaType, sessionType: SessionType) {

        val refIdList = ArrayList<String>()

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
                        isAppAudio = false,
                        isBroadcast = 1,
                        customDataPacket = (activity as DashBoardActivity).callerName.toString()
                    )
                )
            }
//            activity?.supportFragmentManager?.let { CreateUrlLinkDialog(callClient,url = true,this::navToPublicCall).show(it, CreateUrlLinkDialog.TAG) }
            navToPublicCall()
        } else {
            (activity as DashBoardActivity).connectClient()
        }
    }



}