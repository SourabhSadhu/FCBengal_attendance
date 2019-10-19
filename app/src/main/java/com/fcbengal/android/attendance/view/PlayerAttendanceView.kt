package com.fcbengal.android.attendance.view

import com.fcbengal.android.attendance.entity.AttendanceStatus

class PlayerAttendanceView {
    var id : String = ""
    var fName : String = ""
    var mName : String = ""
    var lName : String = ""
    var doj : String = "DD/MM/YYYY"
    var dob : String = "DD/MM/YYYY"
    var contactNo : String = ""
    var groupTimeScheduleId : String = ""
    var groupId : String = ""

    var entryTime : Int = 0
    var exitTime : Int = 0
//    var isPresent : Boolean = false
//    var isDelayed : Boolean = false
    var delayMinutes : Int = 0
    var date : String = ""
    var createdTimeStamp : String = ""
    var modifiedTimeStamp : String = ""
    var status : AttendanceStatus = AttendanceStatus.DEFAULT
}