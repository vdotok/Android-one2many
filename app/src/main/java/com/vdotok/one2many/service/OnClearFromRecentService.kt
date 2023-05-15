package com.vdotok.one2many.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.vdotok.one2many.VdoTok

class OnClearFromRecentService : Service() {


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("ClearFromRecentService", "Service Started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ClearFromRecentService", "Service Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.e("ClearFromRecentService", "END")
        val sessionList = ArrayList<String>().apply {
            (application as VdoTok).callParam1?.sessionUuid?.let {
                add(it)
            }
            (application as VdoTok).callParam2?.sessionUuid?.let {
                add(it)
            }
        }
        (application as VdoTok).callParam1 = null
        (application as VdoTok).callParam2 = null
        (application as VdoTok).callClient.endCallSession(sessionList)
        (application as VdoTok).prefs.loginInfo?.let {
            (application as VdoTok).callClient.unRegister(
                ownRefId = it.refId.toString()
            )
        }
        (application as VdoTok).callClient.disConnectSocket()
        stopSelf()
    }
}