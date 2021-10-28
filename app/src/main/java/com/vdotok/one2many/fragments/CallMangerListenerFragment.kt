package com.vdotok.one2many.fragments

import androidx.fragment.app.Fragment
import com.vdotok.one2many.interfaces.FragmentRefreshListener
import com.vdotok.one2many.ui.dashboard.DashBoardActivity


/**
 * Created By: VdoTok
 * Date & Time: On 5/26/21 At 3:21 PM in 2021
 */
abstract class CallMangerListenerFragment: Fragment(), FragmentRefreshListener {

    override fun onStart() {
        super.onStart()
        (activity as DashBoardActivity).mListener = this
    }
}