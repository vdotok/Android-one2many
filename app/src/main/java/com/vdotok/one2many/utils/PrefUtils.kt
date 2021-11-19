package com.vdotok.one2many.utils

import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.prefs.Prefs

fun saveResponseToPrefs(prefs: Prefs, response: LoginResponse?) {
    prefs.loginInfo = response
}
