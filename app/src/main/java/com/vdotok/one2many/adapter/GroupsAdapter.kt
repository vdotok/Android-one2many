package com.vdotok.one2many.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.vdotok.one2many.R
import com.vdotok.one2many.databinding.GroupChatRowBinding
import com.vdotok.one2many.models.GroupModel
import com.vdotok.one2many.utils.performSingleClick


/**
 * Created By: VdoTok
 * Date & Time: On 1/21/21 At 1:17 PM in 2021
 *
 * Adapter Class for inflating list of all groups or chats a user is included into\
 */
class GroupsAdapter(private val username:String , private val dataSet: List<GroupModel>, private val context: Context,
        private val callbacks: InterfaceOnGroupMenuItemClick) :
    RecyclerView.Adapter<GroupsAdapter.AllGroupsListViewHolder>() {

    var dataList: ArrayList<GroupModel> = ArrayList()

    init {
        dataList.addAll(dataSet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllGroupsListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return AllGroupsListViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: AllGroupsListViewHolder, position: Int) {
        val listItem = dataList[position]
        holder.binding?.groupModel = listItem


        //to get group name other user name if single group other wise group title
       listItem.let {
            if(listItem.autoCreated == 1){
                it.participants.forEach {name->
                    if (name.fullname?.equals(username) == false) {
                        holder.binding?.groupTitle?.text = name.fullname

                    }
                }
            } else{
                holder.binding?.groupTitle?.text = it.groupTitle
            }
        }
       holder.binding?.root?.performSingleClick {
           callbacks.onGroupClick(listItem)
       }
        holder.binding?.imgMore?.setOnClickListener {
            val popupWindowObj = showMenuPopupWindow(listItem)
            holder.binding?.imgMore?.x?.toInt()?.let { x ->
                holder.binding?.imgMore?.y?.toInt()?.let { y ->
                    popupWindowObj.showAsDropDown(holder.binding?.imgMore,
                        x + 50, y
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(userModelList: List<GroupModel>) {
        dataList.clear()
        dataList.addAll(userModelList)
        notifyDataSetChanged()
    }

    private fun showMenuPopupWindow(groupModel: GroupModel): PopupWindow {
        val popupWindow = PopupWindow()

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view: View = inflater.inflate(R.layout.group_menu_items, null)

        val tvEdit: TextView = view.findViewById(R.id.btn_edit)

        if (groupModel.autoCreated == 0){
            tvEdit.isEnabled = true
            tvEdit.setTextColor(Color.BLACK)
        }else{
            tvEdit.isEnabled = false
            tvEdit.setTextColor(Color.GRAY)
        }

        tvEdit.setOnClickListener {
            callbacks.onEditClick(groupModel)
            popupWindow.dismiss()
        }

        val tvDelete: TextView = view.findViewById(R.id.btn_delete)
        tvDelete.setOnClickListener {
            groupModel.id?.let {
                callbacks.onDeleteClick(it)
                popupWindow.dismiss()
            }
        }

        popupWindow.isFocusable = true
        popupWindow.width = WindowManager.LayoutParams.WRAP_CONTENT
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        popupWindow.contentView = view
        return popupWindow
    }

    class AllGroupsListViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
        RecyclerView.ViewHolder(inflater.inflate(R.layout.group_chat_row, parent, false)) {
        var binding: GroupChatRowBinding? = null
        init {
            binding = DataBindingUtil.bind(itemView)
        }
    }

    interface InterfaceOnGroupMenuItemClick {
        fun onEditClick(groupModel: GroupModel)
        fun onDeleteClick(position: Int)
        fun onGroupClick(groupModel: GroupModel)
    }
}

