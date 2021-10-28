package com.vdotok.one2many.models

import com.google.gson.annotations.SerializedName

/**
 * Created By: VdoTok
 * Date & Time: On 1/21/21 At 1:17 PM in 2021
 *
 * Response model map class for fetching all users in the list
 */
class GetAllUsersResponseModel {

    @SerializedName("message")
    var message: String? = null

    @SerializedName("process_time")
    var processTime: String? = null

    @SerializedName("users")
    var users: List<UserModel> = ArrayList()

    @SerializedName("status")
    var status: String? = null

}
