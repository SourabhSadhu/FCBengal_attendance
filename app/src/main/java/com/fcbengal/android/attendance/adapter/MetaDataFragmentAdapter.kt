package com.fcbengal.android.attendance.adapter

import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.fcbengal.android.attendance.metadata.*

class MetaDataFragmentAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private val TAG = this::class.java.simpleName
    private var groupFragment : GroupFragment = GroupFragment.newInstance()
    private var timeScheduleFragment = TimeScheduleFragment.newInstance()
    private var groupTimeScheduleMapFragment = GroupTimeScheduleMapFragment.newInstance()
    private var playerFragment = PlayerFragment.newInstance()
    private var groundFragment = GroundFragment.newInstance()

//    companion object {
//
//        private var groupFragment = GroupFragment.newInstance()
//        private var timeScheduleFragment = TimeScheduleFragment.newInstance()
//        private var groupTimeScheduleMapFragment = GroupTimeScheduleMapFragment.newInstance()
//        private var playerFragment = PlayerFragment.newInstance()
//    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val createdFragment =  super.instantiateItem(container, position)
        when(position){
            0 -> {
                Log.e(TAG, "groupFragment instance hash code ${groupFragment.hashCode()}")
                groupFragment = createdFragment as GroupFragment
            }
            1 -> {
                Log.e(TAG, "timeScheduleFragment instance hash code ${timeScheduleFragment.hashCode()}")
                timeScheduleFragment = createdFragment as TimeScheduleFragment
            }
            2 -> {
                groupTimeScheduleMapFragment = createdFragment as GroupTimeScheduleMapFragment
            }
            3 -> {
                Log.e(TAG, "playerFragment instance hash code ${playerFragment.hashCode()}")
                playerFragment = createdFragment as PlayerFragment
            }
            4->{
                groundFragment = createdFragment as GroundFragment
            }
        }
        return createdFragment
    }

    override fun getItem(position: Int): Fragment? {
        Log.e(TAG, "Calling fragment at $position")
        when (position) {
            0 -> {
                Log.e(TAG, "groupFragment hash code ${groupFragment.hashCode()}")
                return groupFragment
            }
            1 -> {
                Log.e(TAG, "timeScheduleFragment hash code ${timeScheduleFragment.hashCode()}")
                return timeScheduleFragment
            }
            2 -> {
                Log.e(TAG, "groupTimeScheduleMapFragment hash code ${groupTimeScheduleMapFragment.hashCode()}")
                return groupTimeScheduleMapFragment
            }
            3 -> {
                Log.e(TAG, "playerFragment hash code ${playerFragment.hashCode()}")
                return playerFragment
            }
            4 -> {
                return groundFragment
            }
        }
        return null
    }

    override fun getCount(): Int {
        return 5
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Group"
            1 -> "Schedule"
            2 -> "Map"
            3 -> "Player"
            4 -> "Ground"
            else -> ""
        }
    }
}