package com.vdotok.one2many.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.vdotok.one2many.databinding.UrlCustomDialogueBinding

class CreateUrlDialog(private val checkValidation : () -> Unit) : DialogFragment(){

    private lateinit var binding: UrlCustomDialogueBinding
    init {
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        if (dialog != null && dialog?.window != null) {
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        }

        binding = UrlCustomDialogueBinding.inflate(inflater, container, false)

        binding.imgClose.setOnClickListener {
            dismiss()
        }

       init()

        return binding.root
    }

    private fun init() {
        binding.btnStartBroadcast.setOnClickListener {
           checkValidation.invoke()
           dismiss()
        }
    }


    companion object{
        const val TAG = "CREATE_URL_DIALOG"
    }

}
