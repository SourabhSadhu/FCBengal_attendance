package com.fcbengal.android.attendance.view

import com.fcbengal.android.attendance.entity.Ground
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.Player

class GroupAttendanceView {
    var groupIdList = ArrayList<String>()
    var groupAttendanceMap = HashMap<String, AttendanceView>()
    var groupPlayerMap = HashMap<String, ArrayList<Player>>()
    var groundMap = HashMap<String, Ground>()
    var groupMap = HashMap<String, Group>()
}