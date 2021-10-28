package com.vdotok.one2many.ui.dashboard.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.vdotok.one2many.R
import com.vdotok.one2many.models.UserCallModel
import kotlinx.android.synthetic.main.layout_calling_user.view.*
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer


/**
 * Created By: VdoTok
 * Date & Time: On 6/24/21 At 5:27 PM in 2021
 */

const val TYPE_FULL = 0
const val TYPE_HALF = 1
const val TYPE_QUARTER = 2


class GridUserAdapter : RecyclerView.Adapter<GridUserAdapter.MyViewHolder>() {

    val userList = ArrayList<UserCallModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(
            R.layout.layout_calling_user,
            parent,
            false
        )
        return MyViewHolder(v)
    }

    override fun getItemViewType(position: Int): Int {

        return userList.get(position).cellType

//        return super.getItemViewType(position)
    }

    fun updateUser(user: UserCallModel) {
        userList.add(user)

        if (userList.size == 1) {
            userList.get(0).cellType = TYPE_FULL
        } else if (userList.size == 2) {
            userList.get(0).cellType = TYPE_HALF
            userList.get(1).cellType = TYPE_HALF
        } else if (userList.size == 3) {
            userList.get(0).cellType = TYPE_QUARTER
            userList.get(1).cellType = TYPE_QUARTER
            userList.get(2).cellType = TYPE_HALF
        } else if (userList.size == 4) {
            userList.get(0).cellType = TYPE_QUARTER
            userList.get(1).cellType = TYPE_QUARTER
            userList.get(2).cellType = TYPE_QUARTER
            userList.get(3).cellType = TYPE_QUARTER
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView : TextView
        var videoView : SurfaceViewRenderer
        var groupAudio : Group

        init {
            textView = itemView.findViewById<View>(R.id.tvUserName) as TextView
            videoView = itemView.remoteView
            groupAudio = itemView.groupAudioCall
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.textView.text = userList.get(position).userName

        holder.videoView.setMirror(true)
        val rootEglBase = EglBase.create()
        holder.videoView.init(rootEglBase.eglBaseContext, null)
        holder.videoView.setZOrderMediaOverlay(false)
        holder.videoView.setEnableHardwareScaler(false)
        ViewCompat.setElevation(holder.videoView, -1f)

        userList.get(position).videoStream?.addSink(holder.videoView)


    }

    fun removeUser(refId: String) {

        userList.remove(userList.find { it.refId == refId })
        notifyDataSetChanged()

    }


}
