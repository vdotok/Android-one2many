package com.norgic.vdotokcall_mtm.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AuthenticationRequest(

    @SerializedName("auth_token")
    var authToken: String,

    @SerializedName("project_id")
    var projectId: String

): Parcelable