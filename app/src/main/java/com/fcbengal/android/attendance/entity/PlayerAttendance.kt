package com.fcbengal.android.attendance.entity

class PlayerAttendance {
    var playerId : String = ""
    var groupTimeScheduleId : String = ""
    var entryTime : Int = 0
    var exitTime : Int = 0
    var delayMinutes : Int = 0
    var date : String = ""
    var createdTimeStamp : String = ""
    var modifiedTimeStamp : String = ""
    var status : AttendanceStatus = AttendanceStatus.DEFAULT
    var groundId : String = ""
    var groundName : String = ""
}

