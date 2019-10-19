package com.fcbengal.android.attendance

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.fcbengal.android.attendance.adapter.MetaDataFragmentAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.metadata.*
import com.fcbengal.android.attendance.utils.KLoadingSpin
import com.fcbengal.android.attendance.view.EventId
import com.fcbengal.android.attendance.view.EventWrapper
import com.google.android.gms.auth.api.Auth
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_meta_data.*
import org.greenrobot.eventbus.EventBus

class MetaDataActivity : AppCompatActivity(),
    GroupFragment.GroupFragmentListener,
    TimeScheduleFragment.TimeScheduleFragmentListener,
    GroupTimeScheduleMapFragment.GroupTimeScheduleFragmentListener,
    PlayerFragment.PlayerFragmentListener, GroundFragment.GroundFragmentListener
{
    private val TAG = this::class.java.simpleName
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var loadingSpinner : KLoadingSpin

    private var selectedGroup : Group? = null
    private var selectedTimeSchedule : TimeSchedule? = null

    private val groupFragmentTag = "android:switcher:" + R.id.view_pager + ":" + 0
    private val timeScheduleFragmentTag = "android:switcher:" + R.id.view_pager + ":" + 1
    private val groupTimeScheduleFragmentTag = "android:switcher:" + R.id.view_pager + ":" + 2
    private val playerFragmentTag = "android:switcher:" + R.id.view_pager + ":" + 3
    private val groundFragmentTag = "android:switcher:" + R.id.view_pager + ":" + 4
    private var mSelectedTabIndex = -1
    private var groupFrag : GroupFragment? = null
    private var timeScheduleFrag : TimeScheduleFragment? = null
    private var groupTimeScheduleFrag : GroupTimeScheduleMapFragment? = null
    private var playerFrag : PlayerFragment? = null
    private var groundFrag : GroundFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meta_data)
//        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
//        val viewPager: ViewPager = findViewById(R.id.view_pager)
//        viewPager.adapter = sectionsPagerAdapter
//        val tabs: TabLayout = findViewById(R.id.tabs)
//        tabs.setupWithViewPager(viewPager)
//        val fab: FloatingActionButton = findViewById(R.id.fab)
//
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
        val toolbar : Toolbar = toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Configure"
        loadingSpinner = findViewById(R.id.KLoadingSpin)

        tabLayout = tab_layout
        viewPager = view_pager
        val adapter =
            MetaDataFragmentAdapter(supportFragmentManager)
        viewPager.offscreenPageLimit = 4
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(null != tab){
                    Log.e("Tab Selected ",tab.position.toString())
                    mSelectedTabIndex = tab.position
                    when(tab.position){
                        0 ->{
                            groupFrag = supportFragmentManager.findFragmentByTag(groupFragmentTag) as GroupFragment?
                        }
                        1 -> {
                            timeScheduleFrag = supportFragmentManager.findFragmentByTag(timeScheduleFragmentTag) as TimeScheduleFragment?
                            if(null != selectedGroup){
                                timeScheduleFrag?.setSelectedGroup(selectedGroup!!)
                            }
                        }
                        2 -> {
                            groupTimeScheduleFrag = supportFragmentManager.findFragmentByTag(groupTimeScheduleFragmentTag) as GroupTimeScheduleMapFragment?
                        }
                        3 -> {
                            playerFrag = supportFragmentManager.findFragmentByTag(playerFragmentTag) as PlayerFragment?
                            if(null != selectedGroup){
                                playerFrag?.setSelectedGroup(selectedGroup!!)
                            }
                        }
                        4 -> {
                            groundFrag = supportFragmentManager.findFragmentByTag(groundFragmentTag) as GroundFragment?
                        }
                    }
                }

            }
        })

    }




    override fun showLoader() {
        loadingSpinner.setIsVisible(true)
        loadingSpinner.startAnimation()
    }

    override fun stopLoader() {
        loadingSpinner.stopAnimation()
    }

    override fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String) {
        loadingSpinner.stopAnimation()
        if (!isCompletedSuccess && !TextUtils.isEmpty(msg)) {
            Log.e(TAG, msg)
            Toast.makeText(this, "Error occurred", LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if(loadingSpinner.mIsVisible){
            loadingSpinner.stopAnimation()
        }else{
            super.onBackPressed()
        }
    }

    override fun onPlayerFragmentInstance(){
        EventBus.getDefault().post(
            EventWrapper(
                selectedGroup,
                EventId.GROUP_EVENT
            )
        )
    }

    override fun onGroupData(groupData: Group?) {
        selectedGroup = groupData
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.refresh, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                when(mSelectedTabIndex){
                    0 -> {
                        groupFrag?.loadGroupData()
                    }
                    1 -> {
                        if(null != selectedGroup){
                            timeScheduleFrag?.setSelectedGroup(selectedGroup!!)
                        }else{
                            Toast.makeText(this@MetaDataActivity, "Please select Group", LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        groupTimeScheduleFrag?.loadGroupData()
                    }
                    3 -> {
                        if(null != selectedGroup){
                            playerFrag?.setSelectedGroup(selectedGroup!!)
                        } else {
                            Toast.makeText(this@MetaDataActivity, "Please select Group", LENGTH_SHORT).show()
                        }
                    }
                    4 -> {
                        groundFrag?.loadGroundData()
                    }
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onTimeScheduleData(timeScheduleData: TimeSchedule?) {
        selectedTimeSchedule = timeScheduleData

    }
}