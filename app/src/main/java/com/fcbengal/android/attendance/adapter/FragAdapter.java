package com.fcbengal.android.attendance.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.fcbengal.android.attendance.metadata.GroupFragment;
import com.fcbengal.android.attendance.metadata.GroupTimeScheduleMapFragment;
import com.fcbengal.android.attendance.metadata.PlayerFragment;
import com.fcbengal.android.attendance.metadata.TimeScheduleFragment;

public class FragAdapter extends FragmentPagerAdapter {

    private static GroupFragment groupFragment;
    private static TimeScheduleFragment timeScheduleFragment;
    private static GroupTimeScheduleMapFragment groupTimeScheduleMapFragment;
    private static PlayerFragment playerFragment;

    public FragAdapter(FragmentManager fm) {
        super(fm);
        groupFragment = new GroupFragment();
        timeScheduleFragment = new TimeScheduleFragment();
        groupTimeScheduleMapFragment = new GroupTimeScheduleMapFragment();
        playerFragment = new PlayerFragment();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0 : {
                return groupFragment;
            }
            case 1 : {
                return timeScheduleFragment;
            }
            case 2 : {
                return groupTimeScheduleMapFragment;
            }
            case 3 : {
                return playerFragment;
            }
        }
        return null;
    }

    @Override
    public int getCount(){
        return 4;
    }
}
