package com.vdotok.one2many.utils


/**
 * Created By: VdoTok
 * Date & Time: On 5/5/21 At 5:06 PM in 2021
 */
object ApplicationConstants {

//    API CONSTANTS
    const val API_BASE_URL = "https://tenant-api.vdotok.dev/"
    const val API_VERSION = "v0/"

//    SDK AUTH PARAMS
    const val SDK_AUTH_TOKEN = "3d9686b635b15b5bc2d19800407609fa"
    const val SDK_PROJECT_ID = "Add your project key here"

//    PREFS CONSTANTS
    const val isLogin = "isLogin"
    const val LOGIN_INFO = "savedLoginInfo"
    const val GROUP_MODEL_KEY = "group_model_key"
    const val SDK_AUTH_RESPONSE = "SDK_AUTH_RESPONSE"

//    API ERROR LOG TAGS
    const val API_ERROR = "API_ERROR"
    const val HTTP_CODE_NO_NETWORK = 600
    const val SUCCESS_CODE = 200

//    GROUP CONSTANTS
    const val MAX_PARTICIPANTS = 4 // max limit is 4 so including current user we can add up to 3 more users in a group

    // This error code means a local error occurred while parsing the received json.

    const val MY_PERMISSIONS_REQUEST_CAMERA = 100
    const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    const val MY_PERMISSIONS_REQUEST = 102

}