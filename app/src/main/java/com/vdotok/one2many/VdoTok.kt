package com.vdotok.one2many

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.vdotok.network.models.LoginResponse
import com.vdotok.one2many.prefs.Prefs
import com.vdotok.streaming.CallClient
import com.vdotok.streaming.enums.MediaType
import com.vdotok.streaming.enums.SessionType
import com.vdotok.streaming.models.CallParams
import org.webrtc.EglBase


/**
 * Created By: VdoTok
 * Date & Time: On 10/11/2021 At 1:21 PM in 2021
 */
class VdoTok : Application() {

    lateinit var callClient: CallClient
    lateinit var prefs: Prefs
    var callParam1: CallParams? = null
    var callParam2: CallParams? = null
    var camView: Boolean = true
    private val rootEglBase: EglBase = EglBase.create()
    val rootEglBaseContext: EglBase.Context = rootEglBase.eglBaseContext
    var appIsActive = false
    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if(appIsActive) connectClient()
                if ((callParam1?.mediaType == MediaType.VIDEO && callParam1?.sessionType == SessionType.CALL) || (callParam2?.mediaType == MediaType.VIDEO && callParam2?.sessionType == SessionType.CALL)) {
                    if (camView) {
                        if (callParam1?.sessionType == SessionType.CALL) {
                            callClient.resumeVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam1?.sessionUuid.toString()
                            )
                        } else {
                            callClient.resumeVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam2?.sessionUuid.toString()
                            )
                        }
                    } else {
                        if (callParam1?.sessionType == SessionType.CALL) {
                            callClient.pauseVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam1?.sessionUuid.toString()
                            )
                        } else {
                            callClient.pauseVideo(
                                prefs.loginInfo?.refId.toString(),
                                callParam2?.sessionUuid.toString()
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
                            callParam1?.sessionUuid.toString()
                        )
                    } else {
                        callClient.pauseVideo(
                            prefs.loginInfo?.refId.toString(),
                            callParam2?.sessionUuid.toString()
                        )
                    }
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                appIsActive = false
                rootEglBase.release()
            }
            else -> {}
        }
    }

    private fun connectClient() {
        if (!callClient.isConnected()) {
            Handler(Looper.getMainLooper()).postDelayed({
                prefs.loginInfo?.mediaServer?.let {
                    if (!callClient.isConnected())
                        callClient.connect(
                            getMediaServerAddress(it),
                            it.endPoint
                        )
                }
            }, 1000)
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