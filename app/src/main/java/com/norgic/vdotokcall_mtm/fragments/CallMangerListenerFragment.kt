package com.norgic.vdotokcall_mtm.fragments

import android.app.Activity
import androidx.fragment.app.Fragment
import com.norgic.vdotokcall_mtm.interfaces.FragmentRefreshListener
import com.norgic.vdotokcall_mtm.ui.dashboard.DashBoardActivity


/**
 * Created By: Norgic
 * Date & Time: On 5/26/21 At 3:21 PM in 2021
 */
abstract class CallMangerListenerFragment: Fragment(), FragmentRefreshListener {

    override fun onStart() {
        super.onStart()
        (activity as DashBoardActivity).mListener = this
    }
}