package com.norgic.vdotokcall_mtm.models

import com.norgic.vdotokcall_mtm.ui.dashboard.adapter.TYPE_QUARTER
import org.webrtc.VideoTrack


/**
 * Created By: Norgic
 * Date & Time: On 6/24/21 At 5:36 PM in 2021
 */
class UserCallModel(

    val userName : String? = null,
    val refId : String? = null,
    val videoStream : VideoTrack? = null,
    var cellType : Int = TYPE_QUARTER,
)