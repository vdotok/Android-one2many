package com.vdotok.one2many.utils

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.extensions.showSnackBar
import com.vdotok.one2many.network.HttpResponseCodes
import com.vdotok.one2many.network.ParsedError
import com.vdotok.one2many.network.Result
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.one2many.ui.dashboard.DashBoardActivity
import retrofit2.Response


/**
 * Wrap a suspending API [call] in try/catch. In case an exception is thrown, a [Result.Error] is
 * created based on the error message.
 */
suspend fun <T : Any> request(call: suspend () -> Result<T>): Result<T> {
    return try {
        call()
    } catch (e: Exception) {
        Result.Error(ParsedError(e.message.orEmpty(), ApplicationConstants.HTTP_CODE_NO_NETWORK))
    }
}

suspend fun <T : Any> safeApiCall(onRequest: suspend () -> Response<T>): Result<T> {
    return request {
        with(onRequest()) {
            if (isSuccessful) {
                Result.Success(body() ?: Unit as T)
            } else {
                Result.Error(ParsedError(getLogMessage(this), ApplicationConstants.HTTP_CODE_NO_NETWORK))
            }
        }
    }
}

private fun <T> getLogMessage(response: Response<T>): String {
    val logStringBuilder = StringBuilder()

    val networkResponseRequest = response.raw().networkResponse?.request

    val responseCode = response.raw().code
    val requestMethod = networkResponseRequest?.method
    val requestUrl = networkResponseRequest?.url

    logStringBuilder.appendln("HipoExceptionsAndroid")
    logStringBuilder.appendln("--->")
    logStringBuilder.appendln("$responseCode $requestMethod $requestUrl ")
    logStringBuilder.appendln("HEADERS { ")
    val headers = networkResponseRequest?.headers
    headers?.names()?.forEach { headerName ->
        logStringBuilder.appendln("\t$headerName: ${headers[headerName]}")
    }
    logStringBuilder.appendln("}\n<--")

    return logStringBuilder.toString()
}


fun hasNetworkAvailable(context: Context): Boolean {
    val service =  Context.CONNECTIVITY_SERVICE
    val manager = context.getSystemService(service) as ConnectivityManager?
    val network = manager?.activeNetworkInfo
    return (network != null)
}

fun isInternetAvailable(context: Context): Boolean {
    return ConnectivityStatus(context).isConnected()
}

fun handleLoginResponse(context: Context, response: LoginResponse, prefs: Prefs, view: View) {
    when(response.status) {
        HttpResponseCodes.SUCCESS.value -> {
            saveResponseToPrefs(prefs, response)
            context.startActivity(DashBoardActivity.createDashboardActivity(context))
        }
        else -> {
             view.showSnackBar(response.message)
        }
    }
}
