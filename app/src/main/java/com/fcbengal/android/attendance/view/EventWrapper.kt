package com.fcbengal.android.attendance.view

class EventWrapper(val eventData: Any?, val eventId: EventId)

enum class EventId {
    GROUP_EVENT, TIME_SCHEDULE_EVENT, GROUP_TIME_SCHEDULE_EVENT
}