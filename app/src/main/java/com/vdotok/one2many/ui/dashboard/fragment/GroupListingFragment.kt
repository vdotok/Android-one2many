package com.vdotok.one2many.ui.dashboard.fragment

import android.annotation.TargetApi
import android.app.Activity
import android.content.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ObservableField
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.vdotok.network.models.AllGroupsResponse
import com.vdotok.network.models.DeleteGroupModel
import com.vdotok.network.network.NetworkConnectivity
import com.vdotok.network.models.GroupModel
import com.vdotok.network.network.HttpResponseCodes
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.*
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.one2many.R
import com.vdotok.one2many.adapter.GroupsAdapter
import com.vdotok.one2many.databinding.FragmentGroupListingBinding
import com.vdotok.one2many.dialogs.UpdateGroupNameDialog
import com.vdotok.one2many.extensions.*
import com.vdotok.one2many.fragments.CallMangerListenerFragment
import com.vdotok.one2many.models.*
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.service.ProjectionService
import com.vdotok.one2many.ui.account.AccountsActivity
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.ApplicationConstants
import com.vdotok.network.network.Result
import com.vdotok.one2many.utils.showDeleteGroupAlert
import kotlinx.coroutines.*
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 1:29 PM in 2021
 *
 * This class displays the list of groups that a user is connected to
 */
class GroupListingFragment : CallMangerListenerFragment(), GroupsAdapter.InterfaceOnGroupMenuItemClick {

    private lateinit var binding: FragmentGroupListingBinding
    private lateinit var prefs: Prefs

    lateinit var adapter: GroupsAdapter
    private lateinit var callClient: CallClient
    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false
    var isVideo :Boolean = true
    var multiSelect : Boolean = false
    var isInternalAudioIncluded :Boolean = false

    var userName = ObservableField<String>()
    private var groupList = ArrayList<GroupModel>()
    var user : String? = null

    var groupModel: GroupModel?= null
    private val viewModel : AllGroupsFragmentViewModel by viewModels()



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentGroupListingBinding.inflate(inflater, container, false)
        prefs = Prefs(activity)
        CallClient.getInstance(activity as Context)?.let {
            callClient = it
        }

        init()


        return binding.root
    }

    /**
     * Function for setOnClickListeners
     * */
    private fun init() {

        prefs = Prefs(this.context)
        binding.username = userName
        userName.set(prefs.loginInfo?.fullName)

        screenSharingApp = arguments?.getBoolean("screenApp")?: false
        screenSharingMic = arguments?.getBoolean("screenMic")?: false
        cameraCall = arguments?.getBoolean("video")?: false
        isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false
        multiSelect = arguments?.getBoolean("multiSelect")?: false

        binding.customToolbar.tvTitle.text = getString(R.string.group_list_title)
        binding.customToolbar.imgArrowBack.hide()

        binding.customToolbar.imgDone.setOnClickListener {
          openUserListFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded)
        }

        binding.btnNewChat.setOnClickListener {
            openUserListFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded)
        }

        binding.btnRefresh.setOnClickListener {
            getAllGroups()
        }

        binding.tvLogout.setOnClickListener {
            (activity as DashBoardActivity).logout()
            prefs.deleteKeyValuePair(ApplicationConstants.LOGIN_INFO)
            startActivity(AccountsActivity.createAccountsActivity(this.requireContext()))
        }

        initUserListAdapter()
        getAllGroups()
        addPullToRefresh()
    }
    /**
     * Function for refreshing the updated group
     * */
    private fun addPullToRefresh() {
        binding.swipeRefreshLay.setOnRefreshListener {
            getAllGroups()
            (activity as DashBoardActivity).connectClient()
        }
    }

    private fun initUserListAdapter() {
        adapter = GroupsAdapter(
            prefs.loginInfo?.fullName!!,
            ArrayList(),
            this.requireContext(),
            this
        )
        binding.rcvUserList.adapter = adapter
    }

    /**
     * Function to call api for getting all group on server
     * */
    private fun getAllGroups() {
        binding.swipeRefreshLay.isRefreshing = false
        binding.progressBar.toggleVisibility()

        activity?.let { activity ->
            viewModel.getAllGroups("Bearer ${prefs.loginInfo?.authToken}").observe(activity) {
                binding.progressBar.toggleVisibility()
                when (it) {
                    Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success ->  {
//                        binding.progressBar.toggleVisibility()
                        handleAllGroupsResponse(it.data)
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if(NetworkConnectivity.isNetworkAvailable(activity).not())
                            binding.root.showSnackBar(getString(R.string.no_internet))
                    }
                }
            }
        }
    }

    private fun handleAllGroupsResponse(response: AllGroupsResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                response.let { groupsResponse ->
                    if (groupsResponse.groups?.isEmpty() == true) {
                        binding.groupChatListing.show()
                        binding.rcvUserList.hide()
                    } else {
                        groupList.clear()
                        groupsResponse.groups?.forEach {
                            groupList.add(it)
                        }
                        binding.rcvUserList.show()
                        binding.groupChatListing.hide()
                        groupsResponse.groups?.let { adapter.updateData(it) }
                    }
                }
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }


    override fun onEditClick(groupModel: GroupModel) {
        activity?.supportFragmentManager?.let {
            UpdateGroupNameDialog(groupModel, this::getAllGroups).show(
            it,
            UpdateGroupNameDialog.UPDATE_GROUP_TAG
        ) }

    }

    override fun onResume() {
        super.onResume()
        if (callClient.isConnected() == true) {
            binding.tvLed.setImageResource(R.drawable.led_connected)
        } else {
            binding.tvLed.setImageResource(R.drawable.led_error)
        }
    }

    override fun onDeleteClick(position: Int) {
        dialogdeleteGroup(position)
    }

    override fun onGroupClick(groupModel: GroupModel) {
        if (groupModel.autoCreated == 1 ){
                    (activity as DashBoardActivity).incomingName = prefs.loginInfo?.fullName
        }else {
            (activity as DashBoardActivity).incomingName = groupModel.groupTitle.toString()
        }
        (activity as DashBoardActivity).incomingUserName()
        this.groupModel = groupModel
        if ((screenSharingApp && isInternalAudioIncluded && cameraCall) || (screenSharingMic && !isInternalAudioIncluded && cameraCall)
                ||(screenSharingApp && !isInternalAudioIncluded && cameraCall) ||
                (screenSharingApp && !isInternalAudioIncluded) || (screenSharingMic && !isInternalAudioIncluded)
            || (screenSharingApp && isInternalAudioIncluded)){
                      startScreenCapture()
        } else {
            dialOneToOneCall(mediaType = MediaType.VIDEO,sessionType = SessionType.CALL, groupModel)
        }

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
        groupModel?.participants?.forEach { participant ->
            if (participant.refID != prefs.loginInfo?.refId)
                participant.refID?.let { refIdList.add(it) }
        }

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
                        isBroadcast = 0,
                        customDataPacket = (activity as DashBoardActivity).callerName.toString()
                    ),
                    mediaProjection
                )
            }
            outGoingCall(groupModel!!)
        } else {
            (activity as DashBoardActivity).connectClient()
        }
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


    /**
     * Function to display Alert dialog box
     * @param groupId groupId object we will be sending to the server to delete group on its basis
     * */
    private fun dialogdeleteGroup(groupId: Int) {
        showDeleteGroupAlert(this.activity, object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
                val model = DeleteGroupModel()
                model.groupId = groupId
                deleteGroup(model)

            }
        })
    }
    /**
     * Function to call api for deleting a group from server
     * */
    private fun deleteGroup(model: DeleteGroupModel) {
        activity?.let { activity ->
            viewModel.deleteGroup("Bearer ${prefs.loginInfo?.authToken}",model).observe(activity) {
                binding.progressBar.toggleVisibility()
                when (it) {
                    Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success ->  {
                        if (it.data.status == ApplicationConstants.SUCCESS_CODE) {
                            getAllGroups()
                            binding.root.showSnackBar(getString(R.string.group_deleted))
                        } else {
                            binding.root.showSnackBar(it.data.message)
                        }
                        binding.progressBar.toggleVisibility()
                        binding.root.showSnackBar(getString(R.string.group_deleted))
                        getAllGroups()
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if(NetworkConnectivity.isNetworkAvailable(activity).not())
                            binding.root.showSnackBar(getString(R.string.no_internet))
                    }
                }
            }
        }
    }


    override fun onStartCalling() {
    }

    override fun outGoingCall(toPeer: GroupModel) {
        activity?.let {
            it.runOnUiThread {
                openCallFragment(toPeer, isVideo)
            }
        }    }

    /**
     * Function to pass data at outgoing side call
     * @param toPeer toPeer object is the group data from server
     * @param isVideo isVideo object is to check if its an audio or video call
     * */
    private fun openCallFragment(toPeer: GroupModel, isVideo: Boolean) {
        val bundle = Bundle()
        bundle.putParcelable(GroupModel.TAG, toPeer)
        bundle.putBoolean(DialCallFragment.IS_VIDEO_CALL, isVideo)
        bundle.putBoolean("isIncoming", false)
        bundle.putBoolean("screenApp",screenSharingApp)
        bundle.putBoolean("screenMic",screenSharingMic)
        bundle.putBoolean("video",cameraCall)
        bundle.putBoolean("internalAudio",isInternalAudioIncluded)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_dial_fragment, bundle)
    }


    override fun onCameraStreamReceived(stream: VideoTrack) {
//        TODO("Not yet implemented")
    }

    override fun onCameraAudioOff(
        sessionStateInfo: SessionStateInfo, isMultySession: Boolean
    ) {}

    override fun onCallMissed() {
//        TODO("Not yet implemented")
    }

    override fun onCallRejected(reason: String) {
//        TODO("Not yet implemented")
    }

    override fun onPublicURL(publicURL: String) {
       //// TODO("Not yet implemented")
    }

    override fun onConnectionSuccess() {
        binding.tvLed.setImageResource(R.drawable.led_connected)
    }

    override fun onConnectionFail() {
      binding.tvLed.setImageResource(R.drawable.led_error)
    }

    override fun checkCallType() {
//        TODO("Not yet implemented")
    }

    override fun onParticipantLeftCall(refId: String?) {
    }

    override fun sessionStart(mediaProjection: MediaProjection?) {
        if (multiSelect){
            initiatePublicMultiBroadcast(isInternalAudioIncluded, mediaProjection,true)
        }else {
            startSession(mediaProjection)
        }
    }

    override fun acceptedUser(participantCount: Int) {
      ////  TODO("Not yet implemented")
    }

    private fun initiatePublicMultiBroadcast(internalAudioIncluded: Boolean, mediaProjection: MediaProjection?, isGroupSession: Boolean) {
        val refIdList = ArrayList<String>()
        groupModel?.participants?.forEach { participant ->
            if (participant.refID != prefs.loginInfo?.refId)
                participant.refID?.let { refIdList.add(it) }
        }

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
            outGoingCall(groupModel!!)
        } else {
            (activity as DashBoardActivity).connectClient()
        }

    }

    override fun onIncomingCall(model: CallParams) {
        activity?.runOnUiThread {
            val bundle = Bundle()
            bundle.putParcelableArrayList("grouplist", groupList)
            bundle.putString("userName", model.customDataPacket.toString())
            bundle.putParcelable(AcceptCallModel.TAG, model)
            bundle.putBoolean("isIncoming", true)
            bundle.putBoolean(DialCallFragment.IS_VIDEO_CALL, model.mediaType == MediaType.VIDEO)
            Navigation.findNavController(binding.root).navigate(R.id.action_open_dial_fragment, bundle)
        }
    }
    /**
     * Function to get UserName at incoming side
     * @param model model object is used to get username from the list of user achieved from server
     * */
    private fun getUsername(refId: String) : String? {
        groupList.let {
            it.forEach { name ->
                name.participants.forEach { username ->
                    if (username.refID?.equals(refId) == true) {
                        user = username.fullname
                        return user
                    }
                }
            }
        }
        return user
    }


    private fun dialOneToOneCall(mediaType: MediaType, sessionType: SessionType, groupModel: GroupModel) {

        val refIdList = groupModel.participants.map { it.refID } as ArrayList<String>
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
                        isAppAudio = false,
                        isBroadcast = 0,
                        customDataPacket = (activity as DashBoardActivity).callerName.toString()
                    )
                )
            }
            outGoingCall(groupModel)
        } else {
            (activity as DashBoardActivity).connectClient()
        }
    }


    /**
     * Function for navigation between fragments
     * */
    private fun openUserListFragment(
        screenSharingApp: Boolean,
        screenSharingMic: Boolean,
        cameraCall: Boolean,
        isInternalAudioIncluded: Boolean
    ) {
        val bundle = Bundle()
        bundle.putBoolean("screenApp",screenSharingApp)
        bundle.putBoolean("screenMic",screenSharingMic)
        bundle.putBoolean("video",cameraCall)
        bundle.putBoolean("internalAudio",isInternalAudioIncluded)
        Navigation.findNavController(binding.root).navigate(R.id.action_open_userList,bundle)
    }
}