package com.vdotok.one2many

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.models.CallParams


/**
 * Created By: VdoTok
 * Date & Time: On 10/11/2021 At 1:21 PM in 2021
 */
class VdoTok : Application() {

    lateinit var callClient: CallClient
    private lateinit var prefs: Prefs
    var callParam1: CallParams? = null
    var callParam2: CallParams? = null
    var camView: Boolean = true
    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                connectClient()
                if ((callParam1?.mediaType == MediaType.VIDEO && callParam1?.sessionType == SessionType.CALL) || (callParam2?.mediaType == MediaType.VIDEO && callParam2?.sessionType == SessionType.CALL)) {
                    if (camView) {
                        if (callParam1?.sessionType == SessionType.CALL) {
                            callClient.resumeVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam1?.sessionUUID.toString()
                            )
                        } else {
                            callClient.resumeVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam2?.sessionUUID.toString()
                            )
                        }
                    } else {
                        if (callParam1?.sessionType == SessionType.CALL) {
                            callClient.pauseVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam1?.sessionUUID.toString()
                            )
                        } else {
                            callClient.pauseVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam2?.sessionUUID.toString()
                            )
                        }
                    }
                }
            }
            Lifecycle.Event.ON_PAUSE -> {
                if ((callParam1?.mediaType == MediaType.VIDEO && callParam1?.sessionType == SessionType.CALL) || (callParam2?.mediaType == MediaType.VIDEO && callParam2?.sessionType == SessionType.CALL)) {
                    if (callParam1?.sessionType == SessionType.CALL) {
                        callClient.pauseVideo(
                            prefs.loginInfo?.refId.toString(),
                            callParam1?.sessionUUID.toString()
                        )
                    } else {
                        callClient.pauseVideo(
                            prefs.loginInfo?.refId.toString(),
                            callParam2?.sessionUUID.toString()
                        )
                    }
                }
            }
            else -> {}
        }
    }

    private fun connectClient() {
        prefs.loginInfo?.mediaServer?.let {
            if (!callClient.isConnected())
                callClient.connect(
                    getMediaServerAddress(it),
                    it.endPoint
                )
        }
    }

    private fun getMediaServerAddress(mediaServer: LoginResponse.MediaServerMap): String {
        return "https://${mediaServer.host}:${mediaServer.port}"
    }

    override fun onCreate() {
        super.onCreate()
        vdotok = this
        callClient = CallClient.getInstance(this)!!
        prefs = Prefs(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
    }

    companion object {
        private var vdotok: VdoTok? = null

        fun getVdotok(): VdoTok? {
            return vdotok
        }
    }
}