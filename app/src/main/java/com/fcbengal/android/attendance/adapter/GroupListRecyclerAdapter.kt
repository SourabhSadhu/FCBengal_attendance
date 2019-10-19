package com.fcbengal.android.attendance.adapter

import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.R

class GroupListRecyclerAdapter(
    private val context: Context,
    private val groupList: ArrayList<Group>,
    private val listener: OnGroupSelectedListener,
    private val multiSelect: Boolean = false,
    private val sendGroupId: Boolean = false,
    private val restrictInactiveData : Boolean = false
) :
    RecyclerView.Adapter<GroupListRecyclerAdapter.MyViewHolder>() {

    private val multiSelectedGroupMap = HashMap<String, Group>()

    class MyViewHolder(private val context: Context, view: View, private val sendGroupId: Boolean) : RecyclerView.ViewHolder(view) {

        private val name: TextView = view.findViewById(R.id.name)
        private val poc: TextView = view.findViewById(R.id.poc)
//        private val coach1: TextView = view.findViewById(R.id.coach1)
//        private val coach2: TextView = view.findViewById(R.id.coach2)
        private val desc: TextView = view.findViewById(R.id.desc)
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        private val contactPoc: ImageView = view.findViewById(R.id.contactPoc)
        private val cardView : CardView = view.findViewById(R.id.card_view)

        fun setViewHolder(viewObject : Group) {
            name.text = viewObject.name
            if (TextUtils.isEmpty(viewObject.contactPersonName)) {
                poc.visibility = View.GONE
            }else{
                poc.text = viewObject.contactPersonName
            }
//            coach1.visibility = View.GONE
//            coach2.visibility = View.GONE
            desc.text = viewObject.desc
            if (TextUtils.isEmpty(viewObject.groupImageUrl)) {
                thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_player_image))
            } else {
                Glide.with(context).load(viewObject.groupImageUrl).into(thumbnail)
            }
            if(viewObject.selectedUI){
                cardView.background = ContextCompat.getDrawable(context, R.drawable.gradient_present)
            }else{
                cardView.setBackgroundColor(Color.WHITE)
            }
            //Hiding Call button for send groupid on long click instead of poc no
            if(sendGroupId){
                contactPoc.visibility = View.INVISIBLE
            }
            if(viewObject.active){
                name.setTextColor(Color.BLACK)
                poc.setTextColor(Color.BLACK)
                desc.setTextColor(Color.BLACK)
            }else{
                name.setTextColor(Color.GRAY)
                poc.setTextColor(Color.GRAY)
                desc.setTextColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.group_list_item, parent, false)

        return MyViewHolder(
            context,
            itemView,
            sendGroupId
        )
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setViewHolder(groupList[position])
        holder.itemView.setOnClickListener {
            val viewObject = groupList[position]
            Log.e("GroupAdapterListener", "Clicked on name ${viewObject.name}, id ${viewObject.id}")
            if(!restrictInactiveData || (restrictInactiveData && viewObject.active)) {
                changeSelectedItemBackground(position)
                listener.onSelectedGroup(viewObject)
                if (multiSelect) {
                    var isAlreadyAdded = false
                    multiSelectedGroupMap.forEach { (K, _) ->
                        if (K == viewObject.id) {
                            isAlreadyAdded = true
                        }
                    }
                    if (isAlreadyAdded) {
                        Log.e("Removing", viewObject.id)
                        multiSelectedGroupMap.remove(viewObject.id)
                    } else {
                        Log.e("Adding", viewObject.id)
                        multiSelectedGroupMap[viewObject.id] = viewObject
                    }
                }
                listener.getAllSelectedGroups(multiSelectedGroupMap)
            }else{
                Toast.makeText(context, context.getString(R.string.text_disable_data), LENGTH_SHORT).show()
            }
        }
        holder.itemView.setOnLongClickListener {
            Log.e("Long Click", " ${groupList[position].contactPersonNo}")
            if(sendGroupId){
                listener.onLongClick(groupList[position].id)
            }else {
                listener.onLongClick(groupList[position].contactPersonNo)
            }
            true
        }
    }

    private fun changeSelectedItemBackground(position: Int){
        if(multiSelect){
            groupList[position].selectedUI = !groupList[position].selectedUI
        }else {
            groupList.forEach {
                it.selectedUI = false
            }
            groupList[position].selectedUI = true
        }
            notifyDataSetChanged()
    }

    interface OnGroupSelectedListener {
        fun onSelectedGroup(group: Group)
        fun onLongClick(data : String)
        fun getAllSelectedGroups(selectedGroupMap : HashMap<String, Group>)
    }
}