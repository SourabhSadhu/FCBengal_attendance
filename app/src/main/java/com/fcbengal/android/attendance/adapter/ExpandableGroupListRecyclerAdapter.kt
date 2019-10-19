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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.R

class ExpandableGroupListRecyclerAdapter(
    private val context: Context,
    private val groupList: ArrayList<Group>,
    private val groupTimeScheduleMaps: HashMap<String, ArrayList<TimeSchedule>>,
    private val listener: OnGroupSelectedListener,
    private val multiSelect: Boolean = false,
    private val sendGroupId: Boolean = false
) :
    RecyclerView.Adapter<ExpandableGroupListRecyclerAdapter.MyViewHolder>() {

    private val multiSelectedGroupMap = HashMap<String, Group>()

    class MyViewHolder(
        private val context: Context,
        view: View/*,
        private val sendGroupId: Boolean*/) : RecyclerView.ViewHolder(view) {

        private val name: TextView = view.findViewById(R.id.name)
        private val poc: TextView = view.findViewById(R.id.poc)
        private val coach1: TextView = view.findViewById(R.id.coach1)
        private val coach2: TextView = view.findViewById(R.id.coach2)
        private val desc: TextView = view.findViewById(R.id.desc)
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val expandCollapse: ImageView = view.findViewById(R.id.expand_collapse)
        private val cardView : CardView = view.findViewById(R.id.card_view)
        val expandableTimeScheduleRecyclerView : RecyclerView = view.findViewById(R.id.expandable_time_schedule_recycler_view)

        fun setViewHolder(groupDataList: ArrayList<Group>, position: Int) {
            val groupData = groupDataList[position]
            name.text = groupData.name
            if (TextUtils.isEmpty(groupData.contactPersonName)) {
                poc.visibility = View.GONE
            }else{
                poc.text = groupData.contactPersonName
            }
            coach1.visibility = View.GONE
            coach2.visibility = View.GONE
            desc.text = groupData.desc
            if (TextUtils.isEmpty(groupData.groupImageUrl)) {
                thumbnail.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_player_image))
            } else {
                Glide.with(context).load(groupData.groupImageUrl).into(thumbnail)
            }
            if(groupData.selectedUI){
                cardView.background = ContextCompat.getDrawable(context, R.drawable.gradient_present)
            }else{
                cardView.setBackgroundColor(Color.WHITE)
            }
            if(groupData.active){
                name.setTextColor(Color.BLACK)
                poc.setTextColor(Color.BLACK)
                coach1.setTextColor(Color.BLACK)
                coach2.setTextColor(Color.BLACK)
                desc.setTextColor(Color.BLACK)
            }else{
                name.setTextColor(Color.GRAY)
                poc.setTextColor(Color.GRAY)
                coach1.setTextColor(Color.GRAY)
                coach2.setTextColor(Color.GRAY)
                desc.setTextColor(Color.GRAY)
            }

            if(groupData.selectedUI){
                expandCollapse.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_drop_up))
            }else{
                expandCollapse.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_arrow_drop_down))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.expandable_group_list_item, parent, false)

        return MyViewHolder(
            context,
            itemView/*, sendGroupId*/
        )
    }

    override fun getItemCount(): Int {
        return groupList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setViewHolder(groupList, position)

        holder.itemView.setOnClickListener {
            val viewObject = groupList[position]
            Log.e("GroupAdapterListener", "Clicked on name ${viewObject.name}, id ${viewObject.id}")
            changeSelectedItemBackground(viewObject, holder)

            listener.onSelectedGroup(viewObject)
            if(multiSelect) {
                var isAlreadyAdded = false
                multiSelectedGroupMap.forEach{(K,_) ->
                    if(K == viewObject.id){
                        isAlreadyAdded = true
                    }
                }
                if(isAlreadyAdded){
                    Log.e("Removing", viewObject.id)
                    multiSelectedGroupMap.remove(viewObject.id)
                }else{
                    Log.e("Adding", viewObject.id)
                    multiSelectedGroupMap[viewObject.id] = viewObject
                }
            }
            listener.getAllSelectedGroups(multiSelectedGroupMap)
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

        /**
         * Expandable time schedule implementation
         */
        val timeSchedule = groupTimeScheduleMaps[groupList[position].id]
        if(null != timeSchedule) {
            val timeScheduleListRecyclerAdapter =
                TimeScheduleListRecyclerAdapter(
                    context,
                    timeSchedule,
                    object :
                        TimeScheduleListRecyclerAdapter.OnTimeScheduleSelectedListner {
                        override fun onSelectedTimeSchedule(timeSchedule: TimeSchedule) {
                            listener.getSelectedTimeSchedule(groupList[position].id, timeSchedule)
                        }

                        override fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>) {
                        }
                    },
                    multiSelect = false,
                    isLiteUI = true
                )
            holder.expandableTimeScheduleRecyclerView.layoutManager = LinearLayoutManager(context)
            if (groupList[position].selectedUI) {
                holder.expandableTimeScheduleRecyclerView.visibility = View.VISIBLE
            } else {
                holder.expandableTimeScheduleRecyclerView.visibility = View.GONE
            }
            holder.expandableTimeScheduleRecyclerView.adapter = timeScheduleListRecyclerAdapter
        }
    }

    private fun changeSelectedItemBackground(viewObject: Group, holder: MyViewHolder){
        if(multiSelect){
            viewObject.selectedUI = !viewObject.selectedUI
        }else {
            val previousState = viewObject.selectedUI
            groupList.forEach {
                it.selectedUI = false
            }
            viewObject.selectedUI = !previousState
        }

        notifyDataSetChanged()
    }

    interface OnGroupSelectedListener {
        fun onSelectedGroup(group: Group)
        fun onLongClick(data : String)
        fun getAllSelectedGroups(selectedGroupMap : HashMap<String, Group>)
        fun getSelectedTimeSchedule(groupId : String, timeSchedule : TimeSchedule)
    }
}