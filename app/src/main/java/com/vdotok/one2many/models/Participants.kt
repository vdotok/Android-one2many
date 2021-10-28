package com.vdotok.one2many.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Created By: VdoTok
 * Date & Time: On 1/21/21 At 1:17 PM in 2021
 *
 * Response model map class to get details of the participants involved in groups
 */
@Parcelize
data class Participants(

    @SerializedName("color_code")
    var colorCode: String? = null,

    @SerializedName("color_id")
    var colorId: Int? = null,

    @SerializedName("email")
    var email: String? = null,

    @SerializedName("full_name")
    var fullname: String? = null,

    @SerializedName("ref_id")
    var refId: String? = null,

    @SerializedName("user_id")
    var userId: String? = null,

): Parcelable