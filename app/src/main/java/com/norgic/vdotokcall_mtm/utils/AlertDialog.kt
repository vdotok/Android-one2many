package com.norgic.vdotokcall_mtm.utils

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.norgic.vdotokcall_mtm.R


fun showDeleteGroupAlert(activity: FragmentActivity?, dialogListener: DialogInterface.OnClickListener) {
    activity?.let {
        val alertDialog = AlertDialog.Builder(it)
            .setMessage(activity.getString(R.string.delete_group))
            .setPositiveButton(R.string.delete, dialogListener)
            .setNegativeButton(R.string.cancel, null).create()
        alertDialog.show()
    }
}
fun showInternalAlert(
    activity: FragmentActivity?,
    dialogListener: DialogInterface.OnClickListener
) {
    activity?.let {
        val alertDialog = AlertDialog.Builder(it)
            .setMessage(activity.getString(R.string.internal_audio))
            .setPositiveButton(R.string.yes, dialogListener)
            .setNegativeButton(R.string.no, dialogListener)
        alertDialog.show()
    }
}
    fun showAlert(
        activity: FragmentActivity?,
        dialogListener: DialogInterface.OnClickListener
    ) {
        activity?.let {
            val alertDialog = AlertDialog.Builder(it)
                .setTitle(activity.getString(R.string.alert))
                .setMessage(activity.getString(R.string.alert_msg))
                .setPositiveButton(R.string.ok, dialogListener).create()
            alertDialog.show()
        }

    }
