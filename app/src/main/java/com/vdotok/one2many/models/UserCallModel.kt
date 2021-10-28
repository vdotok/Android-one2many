package com.vdotok.one2many.models

import com.vdotok.one2many.ui.dashboard.adapter.TYPE_QUARTER
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 6/24/21 At 5:36 PM in 2021
 */
class UserCallModel(

    val userName : String? = null,
    val refId : String? = null,
    val videoStream : VideoTrack? = null,
    var cellType : Int = TYPE_QUARTER,
)