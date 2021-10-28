package com.vdotok.one2many.ui.account

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.LayoutFragmentLoginBinding
import com.vdotok.one2many.extensions.checkValidation
import com.vdotok.one2many.extensions.checkedPassword
import com.vdotok.one2many.extensions.showSnackBar
import com.vdotok.one2many.extensions.toggleVisibility
import com.vdotok.one2many.models.LoginResponse
import com.vdotok.one2many.models.LoginUserModel
import com.vdotok.one2many.network.HttpResponseCodes
import com.vdotok.one2many.network.Result
import com.vdotok.one2many.network.RetrofitBuilder
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.ApplicationConstants
import com.vdotok.one2many.utils.ApplicationConstants.API_ERROR
import com.vdotok.one2many.utils.ApplicationConstants.SDK_PROJECT_ID
import com.vdotok.one2many.utils.disable
import com.vdotok.one2many.utils.enable
import com.vdotok.one2many.utils.safeApiCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException


/**
 * Created By: VdoTok
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
                val response = safeApiCall { service.loginUser (LoginUserModel(email, password,
                    SDK_PROJECT_ID)) }
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