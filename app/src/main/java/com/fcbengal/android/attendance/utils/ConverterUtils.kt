package com.fcbengal.android.attendance.utils

import android.content.Context
import android.util.TypedValue
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.entity.PlayerAttendance
import com.fcbengal.android.attendance.view.PlayerAttendanceView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

object ConverterUtils {
    fun playerToPlayerAttendanceView(player: Player, existingAttendance : PlayerAttendance?): PlayerAttendanceView {
        val view = PlayerAttendanceView()
        view.id = player.id
        view.fName = player.fName
        view.mName = player.mName
        view.lName = player.lName
        view.doj = player.doj
        view.dob = player.dob
        view.contactNo = player.contactNo
        view.groupTimeScheduleId = player.groupTimeScheduleId
        view.groupId = player.groupId

        if(null != existingAttendance){
            view.entryTime = existingAttendance.entryTime
            view.exitTime = existingAttendance.exitTime
            view.delayMinutes = existingAttendance.delayMinutes
            view.date = existingAttendance.date
            view.createdTimeStamp = existingAttendance.createdTimeStamp
            view.modifiedTimeStamp = existingAttendance.modifiedTimeStamp
            view.status = existingAttendance.status
        }

        return view
    }

    fun playerToPlayerAttendance(player: Player, existingAttendance: PlayerAttendance?) : PlayerAttendance {
        if(null != existingAttendance){
            return existingAttendance
        }
        val view = PlayerAttendance()
        view.playerId = player.id
        view.groupTimeScheduleId = player.groupTimeScheduleId
        view.date = getStringDate()
        return view
    }

    fun playerAttendanceViewToPlayerAttendance(view: PlayerAttendanceView): PlayerAttendance {
        val entity = PlayerAttendance()
        entity.createdTimeStamp = view.createdTimeStamp
        entity.date = view.date
        entity.delayMinutes = view.delayMinutes
        entity.entryTime = view.entryTime
        entity.exitTime = view.exitTime
        entity.groupTimeScheduleId = view.groupTimeScheduleId
        entity.modifiedTimeStamp = view.modifiedTimeStamp
        entity.playerId = view.id
        entity.status = view.status
        return entity
    }

    fun getStringDate() : String {
        return SimpleDateFormat("dd_MM_yyyy HH:mm").format(Date())
    }

    fun getDelayedEntryTime(entryTime : Int, delayMinutes : Int) : Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, (entryTime/100))
        calendar.set(Calendar.MINUTE, (entryTime%100))
        calendar.timeInMillis = calendar.timeInMillis + (delayMinutes * 60 * 1000)
        return SimpleDateFormat("HHmm").format(calendar.timeInMillis).toInt()
    }

    fun dpToPx(context : Context): Int {
        val dp = 10
        val r = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            r.displayMetrics
        ).roundToInt()
    }

    fun convertDayToDayOfWeek(day : String) : Int {
        var dayOfWeek = 0
        when(day){
            "Sunday" -> dayOfWeek = Calendar.SUNDAY
            "Monday" -> dayOfWeek = Calendar.MONDAY
            "Tuesday" -> dayOfWeek = Calendar.TUESDAY
            "Wednesday" -> dayOfWeek = Calendar.WEDNESDAY
            "Thursday" -> dayOfWeek = Calendar.THURSDAY
            "Friday" -> dayOfWeek = Calendar.FRIDAY
            "Saturday" -> dayOfWeek = Calendar.SATURDAY
        }
        return dayOfWeek
    }
}