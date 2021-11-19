package com.vdotok.one2many.ui.account.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.LayoutFragmentSignupBinding
import com.vdotok.one2many.extensions.*
import com.vdotok.one2many.network.HttpResponseCodes
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.account.viewmodel.AccountViewModel
import com.vdotok.one2many.utils.*
import com.vdotok.one2many.utils.ApplicationConstants.SDK_PROJECT_ID
import com.vdotok.network.network.Result


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
    private val viewModel: AccountViewModel by viewModels()

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
            if (it.checkedUserName(fullName.get().toString(), true) &&
                it.checkedEmail(email.get().toString(), true) &&
                it.checkedPassword(password.get().toString())
            ) {
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

            viewModel.checkEmailAlreadyExist(email).observe(viewLifecycleOwner) {

                when (it) {
                    Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success ->  {
                        binding.progressBar.toggleVisibility()
                        handleCheckFullNameResponse(it.data)
                        binding.SignUpButton.enable()
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                        binding.SignInButton.enable()
                    }
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

        viewModel.signUp(
            com.vdotok.network.models.SignUpModel(
                fullName.get().toString(), email.get().toString(),
                password.get().toString(), project_id = SDK_PROJECT_ID
            )
        ).observe(viewLifecycleOwner) {

            when (it) {
                Result.Loading -> {
                    binding.progressBar.toggleVisibility()
                }
                is Result.Success ->  {
                    binding.progressBar.toggleVisibility()
                    handleLoginResponse(requireContext(), it.data, prefs, binding.root)
                }
                is Result.Failure -> {
                    binding.progressBar.toggleVisibility()
                    if (isInternetAvailable(this@SignUpFragment.requireContext()).not())
                        binding.root.showSnackBar(getString(R.string.no_network_available))
                    else
                        binding.root.showSnackBar(it.exception.message)
                }
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