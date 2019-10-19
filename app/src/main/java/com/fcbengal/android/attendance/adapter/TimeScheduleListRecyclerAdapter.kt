package com.fcbengal.android.attendance.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.helper.SimpleAnalogClock
import com.fcbengal.android.attendance.R

class TimeScheduleListRecyclerAdapter(
    private val context: Context,
    private val timeScheduleList: ArrayList<TimeSchedule>,
    private val listener: OnTimeScheduleSelectedListner,
    private val multiSelect:Boolean = false,
    val isLiteUI : Boolean = false,
    private val restrictInactiveData : Boolean = false
) :
    RecyclerView.Adapter<TimeScheduleListRecyclerAdapter.MyViewHolder>() {

    private val multiSelectedTimeScheduleMap = HashMap<String, TimeSchedule>()
    class MyViewHolder(private val context: Context, view: View,val isLiteUI: Boolean) : RecyclerView.ViewHolder(view) {

        private val day : TextView = view.findViewById(R.id.day)
        private val from : TextView = view.findViewById(R.id.from)
        private val to : TextView = view.findViewById(R.id.to)
        private val thumbnail: SimpleAnalogClock = view.findViewById(R.id.thumbnail)
        private val cardView : CardView = view.findViewById(R.id.card_view)

        fun setViewHolder(view: TimeSchedule) {
            day.text = view.day
            from.text = view.timeFrom.toString()
            to.text = view.timeTo.toString()
            if(!isLiteUI && view.timeFrom > 99){
                val timeHour = view.timeFrom / 100
                val timeMinute = view.timeFrom % 100
                thumbnail.setTime(timeHour, timeMinute, 0, 0)
            }else{
                thumbnail.visibility = View.GONE
            }
            if(view.selectedUI) {
                cardView.background = ContextCompat.getDrawable(context, R.drawable.gradient_present)
            }else{
                cardView.setBackgroundColor(Color.WHITE)
            }
            if(view.active){
                day.setTextColor(Color.BLACK)
                from.setTextColor(Color.BLACK)
                to.setTextColor(Color.BLACK)
            }else{
                day.setTextColor(Color.GRAY)
                from.setTextColor(Color.GRAY)
                to.setTextColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.time_schedule_list_item, parent, false)

        return MyViewHolder(
            context,
            itemView,
            isLiteUI
        )
    }

    override fun getItemCount(): Int {
        return timeScheduleList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setViewHolder(timeScheduleList[position])
        holder.itemView.setOnClickListener {
            val item = timeScheduleList[position]
            if(!restrictInactiveData || (restrictInactiveData && item.active)) {
                changeSelectedItemBackground(position)
                listener.onSelectedTimeSchedule(item)

                if (multiSelect) {
                    var isAlreadyAdded = false
                    multiSelectedTimeScheduleMap.forEach { (K, _) ->
                        if (K == item.id) {
                            isAlreadyAdded = true
                        }
                    }
                    if (isAlreadyAdded) {
                        multiSelectedTimeScheduleMap.remove(item.id)
                    } else {
                        multiSelectedTimeScheduleMap[item.id] = item
                    }
                    listener.getAllSelectedTimeSchedule(multiSelectedTimeScheduleMap)
                }
            }else{
                Toast.makeText(context, context.getString(R.string.text_disable_data),Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changeSelectedItemBackground(position: Int){
        if(multiSelect) {
            timeScheduleList[position].selectedUI = !timeScheduleList[position].selectedUI
        }else{
            timeScheduleList.forEach {
                it.selectedUI = false
            }
            timeScheduleList[position].selectedUI = true
        }
        notifyDataSetChanged()
    }

    interface OnTimeScheduleSelectedListner {
        fun onSelectedTimeSchedule(timeSchedule: TimeSchedule)
        fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>)
    }
}