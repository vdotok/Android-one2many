package com.vdotok.one2many.interfaces

import android.media.projection.MediaProjection
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import com.vdotok.network.models.GroupModel
import org.webrtc.VideoTrack

/**
 * Interface that are to be implemented in order provide callbacks to fragments
 * */
interface FragmentRefreshListener {
    fun onIncomingCall(model: CallParams)
    fun onStartCalling()
    fun outGoingCall(toPeer: GroupModel)
    //for video steam
    fun onRemoteStreamReceived(stream: VideoTrack, refId: String, sessionID: String, isCameraStream: Boolean) { }

    //for audio steam
    fun onRemoteStreamReceived(refId: String, sessionID: String) {}
    fun onCameraStreamReceived(stream: VideoTrack)
    fun onCameraAudioOff(sessionStateInfo: SessionStateInfo, isMultySession: Boolean)
    fun onCallMissed()
    fun onCallRejected(reason: String)
    fun onCallEnd() {}
    fun onPublicURL(publicURL: String)
    fun onConnectionSuccess() {}
    fun onConnectionFail() {}
    fun checkCallType()
    fun onParticipantLeftCall(refId: String?)
    fun sessionStart(mediaProjection: MediaProjection?)
    fun acceptedUser(participantCount: Int)
    fun onInsufficientBalance(){}
}