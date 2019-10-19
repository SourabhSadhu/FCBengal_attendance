package com.fcbengal.android.attendance.view

import com.fcbengal.android.attendance.entity.PlayerAttendance

class AttendanceView {
    var sortedDateList = ArrayList<Int>()
    var datePlayerAttendanceMap = HashMap<Int, ArrayList<PlayerAttendance>>()
}