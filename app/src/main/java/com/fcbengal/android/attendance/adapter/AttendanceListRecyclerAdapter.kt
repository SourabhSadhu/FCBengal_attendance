package com.fcbengal.android.attendance.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.entity.AttendanceStatus.*
import com.fcbengal.android.attendance.view.PlayerAttendanceView
import java.lang.StringBuilder

class AttendanceListRecyclerAdapter(private val context: Context, private val attendanceList: List<PlayerAttendanceView>) :
    RecyclerView.Adapter<AttendanceListRecyclerAdapter.MyViewHolder>() {

    private val TAG = AttendanceListRecyclerAdapter::class.java.simpleName
    class MyViewHolder(private val context: Context,view: View) : RecyclerView.ViewHolder(view) {

        var name: TextView = view.findViewById(R.id.name)
        var contact: TextView = view.findViewById(R.id.contact)
        var doj: TextView = view.findViewById(R.id.doj)
        var thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        var viewBackground: RelativeLayout = view.findViewById(R.id.view_background)
        var viewForeground: RelativeLayout = view.findViewById(R.id.view_foreground)
        var presence: ImageView = view.findViewById(R.id.presence)
        var time: TextView = view.findViewById(R.id.time)
        var delay: TextView = view.findViewById(R.id.delay)
        private val TAG = "AttndncLstRecylAdapHldr"

        fun setHolder(view: PlayerAttendanceView) {
            name.text = StringBuilder().append(view.fName).append(" ").append(view.lName)
            contact.text = view.contactNo
            doj.text = view.doj
            time.text = StringBuilder().append(view.entryTime).append("-").append(view.exitTime).toString()
            Log.e(TAG, "Status ${view.status} for ${view.fName} ${view.lName}")
            when(view.status){
                PRESENT -> {
                    presence.visibility = View.VISIBLE
                    presence.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ok))
                    delay.text = ""
                }
                PRESENT_DELAYED -> {
                    presence.visibility = View.VISIBLE
                    presence.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_ok))
                    delay.text = context.getString(R.string.text_delay, view.delayMinutes.toString())
                }
                ABSENT -> {
                    presence.visibility = View.VISIBLE
                    presence.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_wrong_red))
                    delay.text = ABSENT.text
                }
                ABSENT_INFORMED -> {
                    presence.visibility = View.VISIBLE
                    presence.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_wrong_red))
                    delay.text = ABSENT_INFORMED.text
                }
                DEFAULT -> {
                    presence.visibility = View.INVISIBLE
                    delay.text = ""
                }
            }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): MyViewHolder {
        val itemView = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.attendance_list_item, viewGroup, false)
        return MyViewHolder(
            context,
            itemView
        )
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = attendanceList[position]
        holder.setHolder(item)
    }

    override fun getItemCount(): Int {
        return attendanceList.size
    }

    fun markPresent(item: PlayerAttendanceView, position: Int, viewHolder : MyViewHolder) {
        Log.e(TAG, "Marking Present for position $position, user ${item.fName} ${item.lName}")
        val view = attendanceList[position]
        view.status = item.status
        view.delayMinutes = item.delayMinutes
        view.entryTime = item.entryTime
        notifyItemChanged(position)
    }

    fun markAbsent(item: PlayerAttendanceView, position: Int, viewHolder: MyViewHolder) {
        Log.e(TAG, "Marking Absent for position $position, user ${item.fName} ${item.lName}")
        val view = attendanceList[position]
        view.status = item.status
        notifyItemChanged(position)
    }
}
