package com.vdotok.one2many.models

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