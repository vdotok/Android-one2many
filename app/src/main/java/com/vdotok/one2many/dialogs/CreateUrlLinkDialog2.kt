package com.vdotok.one2many.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.databinding.ObservableField
import androidx.fragment.app.DialogFragment
import com.vdotok.streaming.CallClient
import com.vdotok.one2many.databinding.UrlLinkCustomDialogueBinding

class CreateUrlLinkDialog2(private val callClient: CallClient, private val url: Boolean, private val createGroup : () -> Unit) : DialogFragment(){

    private lateinit var binding: UrlLinkCustomDialogueBinding
    var i :Int = 0
    var copyUrl = ObservableField<String>()

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

        binding = UrlLinkCustomDialogueBinding.inflate(inflater, container, false)
        binding.imgClose.setOnClickListener {
            if (callClient.isConnected() == true) {
                createGroup.invoke()
                dismiss()
            } else {
                Toast.makeText(context, "socket disconnection", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        viewProgress()
        return binding.root
    }


    private fun viewProgress() {
        binding.imgClose.isEnabled = false
        if (url) {
            i = binding.pBar.progress
            Thread(Runnable {
                while (i < 100) {
                    i += 5
                    activity?.runOnUiThread(Runnable {
                        if (callClient.isConnected() == false) {
                            Toast.makeText(context, "No url generated due to socket disconnection", Toast.LENGTH_SHORT).show()
                            dismiss()
                        }
                    if ( i == 100){
                        binding.imgClose.isEnabled = true
                    }

                    })
                    // Update the progress bar and display the current value
                    activity?.runOnUiThread(Runnable {
                        binding.pBar.progress = i
                    })

                    try {
                        Thread.sleep(200)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                }
            }).start()
        }
    }

    companion object{
        const val TAG = "CREATE_URL_Link_DIALOG"
    }

}
