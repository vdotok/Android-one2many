package com.vdotok.one2many.base

import android.media.projection.MediaProjection
import androidx.fragment.app.Fragment
import com.vdotok.network.models.GroupModel
import com.vdotok.one2many.interfaces.FragmentRefreshListener
import com.vdotok.streaming.models.CallParams
import com.vdotok.streaming.models.SessionStateInfo
import org.webrtc.VideoTrack


/**
 * Created By: VdoTok
 * Date & Time: On 5/26/21 At 3:21 PM in 2021
 */
open class BaseFragment: Fragment(), FragmentRefreshListener {

    override fun onStart() {
        BaseActivity.mListener = this
        super.onStart()
    }

    override fun onIncomingCall(model: CallParams) {}

    override fun onStartCalling() {}

    override fun outGoingCall(toPeer: GroupModel) {}

    override fun onCameraStreamReceived(stream: VideoTrack) {}

    override fun onCameraAudioOff(sessionStateInfo: SessionStateInfo, isMultySession: Boolean) {}

    override fun onCallMissed() {}

    override fun onCallRejected(reason: String) {}

    override fun onPublicURL(publicURL: String) {}

    override fun checkCallType() {}

    override fun onParticipantLeftCall(refId: String?) {}

    override fun sessionStart(mediaProjection: MediaProjection?) {}

    override fun acceptedUser(participantCount: Int) {}

}