package com.norgic.vdotokcall_mtm.ui.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.norgic.vdotokcall_mtm.R
import com.norgic.vdotokcall_mtm.databinding.LayoutFragmentLoginBinding
import com.norgic.vdotokcall_mtm.extensions.checkValidation
import com.norgic.vdotokcall_mtm.extensions.checkedPassword
import com.norgic.vdotokcall_mtm.extensions.showSnackBar
import com.norgic.vdotokcall_mtm.extensions.toggleVisibility
import com.norgic.vdotokcall_mtm.models.LoginResponse
import com.norgic.vdotokcall_mtm.models.LoginUserModel
import com.norgic.vdotokcall_mtm.network.HttpResponseCodes
import com.norgic.vdotokcall_mtm.network.Result
import com.norgic.vdotokcall_mtm.network.RetrofitBuilder
import com.norgic.vdotokcall_mtm.prefs.Prefs
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants
import com.norgic.vdotokcall_mtm.utils.ApplicationConstants.API_ERROR
import com.norgic.vdotokcall_mtm.utils.disable
import com.norgic.vdotokcall_mtm.utils.enable
import com.norgic.vdotokcall_mtm.utils.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException


/**
 * Created By: Norgic
 * Date & Time: On 5/3/21 At 1:26 PM in 2021
 *
 * This class displays the sign-in form to get in application
 */
class LoginFragment: Fragment() {

    private lateinit var binding: LayoutFragmentLoginBinding
    var email : ObservableField<String> = ObservableField<String>()
    var password : ObservableField<String> = ObservableField<String>()
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = LayoutFragmentLoginBinding.inflate(inflater, container, false)

        binding.userEmail = email
        binding.password = password

        init()

        return binding.root
    }

    /**
     * Function to Set the click Listeners and to initialize the data for UI
     * */
    private fun init() {

        prefs = Prefs(activity)

        binding.SignInButton.setOnClickListener {
            if (it.checkedPassword(password.get().toString()) && checkValidation(it, email.get().toString())) {
                loginUser(email.get().toString(), password.get().toString())
                binding.SignInButton.disable()
            }
        }
        binding.SignUpButton.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_move_to_signup_user)
        }

    }

    /**
     * Function to call login User API
     * @param email email object we will be sending to the server
     * @param password password object we will be sending to the server
     * */
    private fun loginUser(email: String, password: String) {
        activity?.let {
            binding.progressBar.toggleVisibility()
            val service = RetrofitBuilder.makeRetrofitService(it)
            CoroutineScope(Dispatchers.IO).launch {
                val response = safeApiCall { service.loginUser (LoginUserModel(email, password)) }
                withContext(Dispatchers.Main) {
                    binding.SignInButton.enable()
                    try {
                        when (response) {
                            is Result.Success -> {
                                handleLoginResponse(response.data)
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

    private fun handleLoginResponse(response: LoginResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                prefs.loginInfo = response
                startActivity(activity?.applicationContext?.let { DashBoardActivity.createDashboardActivity(it) })
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

}