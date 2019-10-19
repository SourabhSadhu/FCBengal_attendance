package com.fcbengal.android.attendance.entity

enum class AttendanceStatus(val text : String){
    PRESENT("Present"), PRESENT_DELAYED("Delayed"), ABSENT("Absent"), ABSENT_INFORMED("Informed"), DEFAULT("Default")
}