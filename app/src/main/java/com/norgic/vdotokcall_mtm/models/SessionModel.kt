package com.norgic.vdotokcall_mtm.models

import android.os.Parcelable
import android.view.View
import com.norgic.callsdks.enums.CallType
import com.norgic.callsdks.enums.MediaType
import com.norgic.callsdks.enums.SessionType
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue


/**
 * Created By: Norgic
 * Date & Time: On 6/14/21 At 1:26 PM in 2021
 */

@Parcelize
data class SessionModel(
    var sessionId: String,
    var refIds: ArrayList<String>?,
    var remoteView: @RawValue View?,
    var mediaType: MediaType?,
    var callType: CallType?,
    var sessionType: SessionType?
): Parcelable
