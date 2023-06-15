package com.vdotok.one2many.ui.account.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import com.google.gson.Gson
import com.google.zxing.integration.android.IntentIntegrator
import com.vdotok.one2many.R
import com.vdotok.network.network.Result
import com.vdotok.network.utils.Constants.BASE_URL
import com.vdotok.one2many.QrCodeScannerContract
import com.vdotok.one2many.databinding.LayoutFragmentLoginBinding
import com.vdotok.one2many.extensions.*
import com.vdotok.one2many.models.QRCodeModel
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.account.viewmodel.AccountViewModel
import com.vdotok.one2many.utils.*
import com.vdotok.one2many.utils.ApplicationConstants.SDK_PROJECT_ID


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
    private val viewModel: AccountViewModel by viewModels()

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

        binding.scanner.performSingleClick{
            activity?.runOnUiThread {
                qrCodeScannerLauncher.launch(IntentIntegrator.forSupportFragment(this))
            }
        }

        binding.SignInButton.setOnClickListener {  validateAndLogin()}

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
        activity?.let { it ->
            if (!prefs.userProjectId.isNullOrEmpty() && !prefs.userBaseUrl.isNullOrEmpty()){
                viewModel.loginUser(email, password, projectId = prefs.userProjectId.toString()).observe(viewLifecycleOwner) {
                when (it) {
                    Result.Loading -> {
                        binding.progressBar.toggleVisibility()
                    }
                    is Result.Success ->  {
                        binding.progressBar.toggleVisibility()
                        handleLoginResponse(requireContext(), it.data, prefs, binding.root)
                        binding.SignInButton.enable()
                    }
                    is Result.Failure -> {
                        binding.progressBar.toggleVisibility()
                        if (isInternetAvailable(this@LoginFragment.requireContext()).not())
                            binding.root.showSnackBar(getString(R.string.no_network_available))
                        else
                            binding.root.showSnackBar(it.exception.message)
                        binding.SignInButton.enable()
                    }
                }
            }
            }else{
                binding.root.showSnackBar("Kindly scan QR code to setup project")
            }

        }
    }

    private fun checkValidationForEmail() {
        val view = binding.SignInButton
        if (view.checkedPassword(password.get().toString()) && view.checkedEmail(email.get().toString(), true)) {
            loginAction()
        }
    }

    private val qrCodeScannerLauncher = registerForActivityResult(QrCodeScannerContract()){
        if (!it.contents.isNullOrEmpty()){
            Log.e("RESULT_INTENT", it.contents)
            val data: QRCodeModel? = Gson().fromJson(it.contents, QRCodeModel::class.java)
            prefs.userProjectId = data?.project_id.toString().trim()
            prefs.userBaseUrl = data?.tenant_api_url.toString().trim()
            if (!prefs.userProjectId.isNullOrEmpty() &&   !prefs.userBaseUrl.isNullOrEmpty()){
                SDK_PROJECT_ID = prefs.userProjectId.toString().trim()
                BASE_URL =  prefs.userBaseUrl.toString().trim()
            }
            Log.d("RESULT_INTENT",data.toString())
        }else{
            binding.root.showSnackBar("QR CODE is not correct!!!")
        }
    }


    private fun validateAndLogin() {
        val inputText = email.get().toString()

        when {
            binding.root.checkedEmail(inputText) -> checkValidationForEmail()
            else -> checkValidationForUsername()
        }
    }

    private fun checkValidationForUsername() {
        if (binding.root.checkedPassword(password.get().toString()) && binding.root.checkedUserName(email.get().toString(), true)) {
            loginAction()
        }
    }

    private fun loginAction(){
        loginUser(email.get().toString(), password.get().toString())
        binding.SignInButton.disable()
    }
}