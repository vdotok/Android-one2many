package com.vdotok.one2many.ui.account

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.LayoutFragmentSignupBinding
import com.vdotok.one2many.extensions.*
import com.vdotok.one2many.models.CheckUserModel
import com.vdotok.one2many.models.LoginResponse
import com.vdotok.one2many.models.SignUpModel
import com.vdotok.one2many.network.HttpResponseCodes
import com.vdotok.one2many.network.Result
import com.vdotok.one2many.network.RetrofitBuilder
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import com.vdotok.one2many.utils.*
import com.vdotok.one2many.utils.ApplicationConstants.API_ERROR
import com.vdotok.one2many.utils.ApplicationConstants.SDK_PROJECT_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException


/**
 * Created By: VdoTok
 * Date & Time: On 5/3/21 At 1:26 PM in 2021
 *
 * This class display the sign-up form
 */
class SignUpFragment: Fragment() {

    private lateinit var binding: LayoutFragmentSignupBinding
    var email : ObservableField<String> = ObservableField<String>()
    var fullName : ObservableField<String> = ObservableField<String>()
    var password : ObservableField<String> = ObservableField<String>()
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = LayoutFragmentSignupBinding.inflate(inflater, container, false)

        binding.userEmail = email
        binding.fullName = fullName
        binding.password = password

        init()

        return binding.root
    }

    /**
     * Function for setOnClickListeners and to check validation
     * */
    private fun init() {
        prefs = Prefs(activity)

        binding.SignUpButton.setOnClickListener {
            if (it.checkedUserName(fullName.get().toString()) &&
                it.checkedPassword(password.get().toString()) &&
                it.checkedEmail(email.get().toString())) {
                checkUserEmail(email.get().toString())
                binding.SignUpButton.disable()
            }
        }

       binding.SignInButton.setOnClickListener {
           moveToLogin(it)
        }

        configureBackPress()
    }

    /**
     * Function to call checkEmail api to verify the email (that same email is not in  use by any other user )
     * @param email email object we will be sending to the server
     * */
    private fun checkUserEmail(email: String) {
        activity?.let {
            binding.progressBar.toggleVisibility()
            val service = RetrofitBuilder.makeRetrofitService(it)
            CoroutineScope(Dispatchers.IO).launch {
                val response = safeApiCall { service.checkEmail (CheckUserModel(email)) }
                withContext(Dispatchers.Main) {
                    binding.SignUpButton.enable()
                    try {
                        when (response) {
                            is Result.Success -> {
                                handleCheckFullNameResponse(response.data)
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

    private fun handleCheckFullNameResponse(response: LoginResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                signUp()
            } else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    /**
     * Function to call signup Api to register user
     * */

    private fun signUp() {
        binding.SignUpButton.disable()
        activity?.let {
            binding.progressBar.toggleVisibility()
            val service = RetrofitBuilder.makeRetrofitService(it)
            CoroutineScope(Dispatchers.IO).launch {
                val response = safeApiCall { service.signUp (
                    SignUpModel(fullName.get().toString(), email.get().toString(),
                        password.get().toString(), project_id = SDK_PROJECT_ID)
                ) }

                withContext(Dispatchers.Main) {
                    binding.SignUpButton.enable()
                    try {
                        when (response) {
                            is Result.Success -> {
                                handleSignUpResponse(response.data)
                            }
                            is Result.Error -> {
                                try {
                                    when {
                                        response is Result.Success && response.data.status == HttpResponseCodes.SUCCESS.value -> {
                                            handleCheckFullNameResponse(response.data)
                                        }
                                        response is Result.Error -> {
                                            if (activity?.isInternetAvailable()?.not() == true)
                                                binding.root.showSnackBar(getString(R.string.no_network_available))
                                            else
                                                binding.root.showSnackBar(response.error.message)
                                        }
                                        else -> binding.root.showSnackBar(response.error.message)
                                    }
                                } catch (e: HttpException) {
                                    Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                                } catch (e: Throwable) {
                                    Log.e(API_ERROR, "signUpUser: ${e.printStackTrace()}")
                                }

//                                if (response.error.responseCode == ApplicationConstants.HTTP_CODE_NO_NETWORK) {
//                                    binding.root.showSnackBar(getString(R.string.no_network_available))
//                                } else {
//                                    binding.root.showSnackBar(response.error.message)
//                                }
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

    private fun handleSignUpResponse(response: LoginResponse) {
        when(response.status) {
            HttpResponseCodes.SUCCESS.value -> {
                binding.root.showSnackBar(resources.getString(R.string.account_created_success))
                Handler(Looper.getMainLooper()).postDelayed({
                    prefs.loginInfo = response
                    startActivity(activity?.applicationContext?.let { DashBoardActivity.createDashboardActivity(it) })
                }, 1500)
            }
            else -> {
                binding.root.showSnackBar(response.message)
            }
        }
    }

    private fun moveToLogin(view: View) {
        Navigation.findNavController(view).navigate(R.id.action_move_to_login_user)
    }

    private fun configureBackPress() {
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    moveToLogin(binding.root)
                }
            })
    }
}