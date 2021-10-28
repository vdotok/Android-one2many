package com.vdotok.one2many.ui.dashboard.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import com.vdotok.one2many.R
import com.vdotok.one2many.adapter.AllUserListAdapter
import com.vdotok.one2many.adapter.OnInboxItemClickCallbackListener
import com.vdotok.one2many.databinding.FragmentUserListBinding
import com.vdotok.one2many.dialogs.CreateGroupDialog
import com.vdotok.one2many.extensions.*
import com.vdotok.one2many.models.AllGroupsResponse
import com.vdotok.one2many.models.CreateGroupModel
import com.vdotok.one2many.models.GetAllUsersResponseModel
import com.vdotok.one2many.models.UserModel
import com.vdotok.one2many.network.ApiService
import com.vdotok.one2many.network.HttpResponseCodes
import com.vdotok.one2many.network.Result
import com.vdotok.one2many.network.RetrofitBuilder
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.utils.ApplicationConstants
import com.vdotok.one2many.utils.ApplicationConstants.API_ERROR
import com.vdotok.one2many.utils.ViewUtils.setStatusBarGradient
import com.vdotok.one2many.utils.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
/**
 * Created By: VdoTok
 * Date & Time: On 6/17/21 At 1:29 PM in 2021
 *
 * This class displays the list of users that are  connected to
 */
class UserListFragment : Fragment(), OnInboxItemClickCallbackListener {

    private lateinit var binding: FragmentUserListBinding
    private lateinit var prefs: Prefs
    lateinit var adapter: AllUserListAdapter
    var edtSearch = ObservableField<String>()

    var screenSharingApp :Boolean = false
    var screenSharingMic :Boolean = false
    var cameraCall :Boolean = false
    var isVideo :Boolean = true
    var isInternalAudioIncluded :Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =  FragmentUserListBinding.inflate(inflater, container, false)

        setStatusBarGradient(this.requireActivity(), R.drawable.rectangle_white_bg)
        init()
        return binding.root
    }
    /**
     * Function for setOnClickListeners
     * */
    private fun init() {
        prefs = Prefs(context)
        screenSharingApp = arguments?.getBoolean("screenApp")?: false
        screenSharingMic = arguments?.getBoolean("screenMic")?: false
        cameraCall = arguments?.getBoolean("video")?: false
        isInternalAudioIncluded = arguments?.getBoolean("internalAudio")?: false

        initUserListAdapter()

        binding.search = edtSearch
        edtSearch.set("")
        binding.customToolbar.imgDone.setImageResource(R.drawable.ic_done)
        binding.customToolbar.tvTitle.text = getString(R.string.createGroupText)

        binding.customToolbar.imgDone.setOnClickListener {
            activity?.hideKeyboard()
            if (adapter.getSelectedUsers().isNotEmpty()) {
                onCreateGroupClick()
            } else {
                binding.root.showSnackBar(R.string.no_user_select)
            }

        }

        binding.customToolbar.imgArrowBack.setOnClickListener {
            activity?.hideKeyboard()
            activity?.onBackPressed()
        }

        textListenerForSearch()
        getAllUsers()
        addPullToRefresh()
    }
    /**
     * Function for refreshing the updated users list
     * */
    private fun addPullToRefresh() {
        binding.swipeRefreshLay.setOnRefreshListener {
            getAllUsers()
        }
    }

    private fun initUserListAdapter() {
        adapter = AllUserListAdapter(ArrayList(),this)
        binding.rcvUserList.adapter = adapter
    }

    private fun onCreateGroupClick() {
        val selectedUsersList: List<UserModel> = adapter.getSelectedUsers()

        if (selectedUsersList.isNotEmpty() && selectedUsersList.size == 1)
            createGroup(getGroupTitle(selectedUsersList))
        else
            activity?.supportFragmentManager?.let { CreateGroupDialog(this::createGroup).show(it, CreateGroupDialog.TAG) }
    }


    /**
     * Function to show creating group fragment
     * @param title title is the group title that is pass to create group
     * */
    private fun createGroup(title: String) {
        val selectedUsersList: List<UserModel> = adapter.getSelectedUsers()

        if(selectedUsersList.isNotEmpty()){

            val model = CreateGroupModel()
            model.groupTitle = title
            //model.auto_created -> set auto created group, set 1 for only single user, 0 for multiple users
            model.pariticpants = getParticipantsIds(selectedUsersList)

            when (selectedUsersList.size) {
                1 -> model.autoCreated = 1
                else -> model.autoCreated = 0
            }

            createGroupApiCall(model)
        }
    }

    /**
     * Function to create title of the group
     * @param selectedUsersList selectedUserList is the list of user pass to create group title
     * */
    private fun getGroupTitle(selectedUsersList: List<UserModel>): String {

        var title = prefs.loginInfo?.fullName.plus("-")

        //In this case, we have only one item in list
        selectedUsersList.forEach {
            title = title.plus(it.userName.toString())
        }
        return title
    }
    /**
     * Function to call api for creating a group on server
     * */
    private fun createGroupApiCall(model: CreateGroupModel) {
        activity?.let {
            binding.progressBar.toggleVisibility()
            val apiService: ApiService =
                RetrofitBuilder.makeRetrofitService(it)
            prefs.loginInfo?.authToken.let {
                CoroutineScope(Dispatchers.IO).launch {
                    val response =
                        safeApiCall { apiService.createGroup(auth_token = "Bearer $it", model) }
                    withContext(Dispatchers.Main) {
                        try {
                            when (response) {
                                is Result.Success -> {
                                    Snackbar.make(
                                        binding.root,
                                        R.string.group_created,
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                    handleCreateGroupSuccess(response.data)
                                }
                                is Result.Error -> {
                                    if (response.error.responseCode == ApplicationConstants.HTTP_CODE_NO_NETWORK) {
                                        binding.root.showSnackBar(getString(R.string.no_network_available))
                                    } else {
                                        binding.root.showSnackBar(response.error.message)
                                    }
                                }
                            }
                        } catch (e: HttpException) {
                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                        } catch (e: Throwable) {
                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                        }
                        binding.progressBar.toggleVisibility()
                    }
                }
            }
        }
    }


    private fun handleCreateGroupSuccess(response: AllGroupsResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value.toInt() -> {
                activity?.hideKeyboard()
                Handler(Looper.getMainLooper()).postDelayed({
                    openGroupFragment(screenSharingApp,screenSharingMic,cameraCall,isInternalAudioIncluded)
                },1000)
            } else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * Function for setting participants ids
     * @param selectedUsersList list of selected users to form a group with
     * @return Returns an ArrayList<Int> of selected user ids
     * */
    private fun getParticipantsIds(selectedUsersList: List<UserModel>): ArrayList<Int> {
        val list: ArrayList<Int> = ArrayList()
        selectedUsersList.forEach { userModel ->
            userModel.userId?.let { list.add(it.toInt()) }
        }
        return list
    }
    /**
     * Function to filter the search
     * */
    private fun textListenerForSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                adapter.filter?.filter(s)
            }
        })
    }
    /**
     * Function to call api for getting all user on server
     * */
    private fun getAllUsers() {
        activity?.let {
            binding.progressBar.toggleVisibility()
            val apiService: ApiService = RetrofitBuilder.makeRetrofitService(it)
            prefs.loginInfo?.authToken.let {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = safeApiCall { apiService.getAllUsers(auth_token = "Bearer $it") }
                    withContext(Dispatchers.Main) {
                        try {
                            when (response) {
                                is Result.Success -> {
                                    handleAllUsersResponse(response.data)
                                }
                                is Result.Error -> {
                                    if (response.error.responseCode == ApplicationConstants.HTTP_CODE_NO_NETWORK) {
                                        binding.root.showSnackBar(getString(R.string.no_network_available))
                                    } else {
                                        binding.root.showSnackBar(response.error.message)
                                    }
                                }
                            }
                        } catch (e: HttpException) {
                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                        } catch (e: Throwable) {
                            Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                        }
                        binding.progressBar.toggleVisibility()
                        binding.swipeRefreshLay.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun handleAllUsersResponse(response: GetAllUsersResponseModel) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                response.users.let { usersList ->
                    if (usersList.isEmpty()) {
                        binding.root.showSnackBar(getString(R.string.no_contacts))
                    } else {
                        populateDataToList(response)
                    }
                }
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }
    /**
     * Function to display all users
     * */
    private fun populateDataToList(response: GetAllUsersResponseModel) {
        adapter.updateData(response.users)
    }

    /**
     * Callback for click listeners
     * */
    override fun onItemClick(position: Int) {
        val item = adapter.dataList[position]
        item.isSelected = item.isSelected.not()
        adapter.notifyItemChanged(position)
    }

    override fun searchResult(position: Int) {
        edtSearch.get()?.isNotEmpty()?.let {
            if (position == 0 && it){
                binding.check.show()
                binding.rcvUserList.hide()
            }else{
                binding.check.hide()
                binding.rcvUserList.show()
            }
        }
    }
    /**
     * Function for the navigation to other fragment
     * */
    private fun openGroupFragment(
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
        Navigation.findNavController(binding.root).navigate(R.id.action_open_groupList,bundle)
    }


    companion object{

        fun createAllUsersActivity(context: Context) = Intent(context, UserListFragment::class.java)

    }
}