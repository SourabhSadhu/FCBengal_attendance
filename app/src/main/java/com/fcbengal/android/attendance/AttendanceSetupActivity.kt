package com.fcbengal.android.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import androidx.transition.TransitionManager
import com.fcbengal.android.attendance.adapter.GroupListRecyclerAdapter
import com.fcbengal.android.attendance.adapter.TimeScheduleListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.GroupTimeSchedule
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.utils.*
import com.fcbengal.android.attendance.utils.animation.ResizeAnimation
import com.fcbengal.android.attendance.utils.animation.Rotate
import com.google.firebase.database.DatabaseError
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AttendanceSetupActivity : AppCompatActivity() {

    lateinit var groupRecyclerView: RecyclerView
    lateinit var groupListRecyclerAdapter : GroupListRecyclerAdapter
    var selectedGroupId : String? = null

    lateinit var timeScheduleRecyclerView: RecyclerView
    lateinit var timeScheduleListRecyclerAdapter : TimeScheduleListRecyclerAdapter

    lateinit var groupMap : HashMap<String, Group>
    lateinit var timeScheduleMap : HashMap<String, TimeSchedule>
    lateinit var groupTimeScheduleMap : HashMap<String, GroupTimeSchedule>

    private lateinit var loadingSpinner : KLoadingSpin
    private lateinit var contentMain : LinearLayout

    private lateinit var groupListLayout : LinearLayout
    private lateinit var groupListExpandImage : ImageView
    private var isGroupListExpanded = false
    private var initialHeight = 0

    private lateinit var llDateTimeControl : LinearLayout
    private lateinit var buttonDateTimeChange : ImageButton
    private lateinit var textDate : TextView
    private lateinit var textTime : TextView
    private lateinit var buttonSubmit : Button
    private lateinit var mSelectedTime : String
    private lateinit var mSelectedDate : String
    private lateinit var mSelectedGroupTimeScheduleId : String
    private var mSelectedTimeScheduleDayOfTheWeek = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance_setup)

        loadingSpinner = findViewById(R.id.KLoadingSpin)
        contentMain = findViewById(R.id.content_main)
        llDateTimeControl = findViewById(R.id.llDateTimeControl)
        llDateTimeControl.visibility = View.INVISIBLE
        buttonDateTimeChange = findViewById(R.id.buttonDateTimeChange)
        buttonDateTimeChange.setOnClickListener { loadDateTime(true) }
        textDate = findViewById(R.id.textDate)
        textTime = findViewById(R.id.textTime)
        buttonSubmit = findViewById(R.id.buttonSubmit)
        groupRecyclerView = findViewById(R.id.group_recycler_view)
        timeScheduleRecyclerView = findViewById(R.id.time_schedule_recycler_view)
        groupListLayout = findViewById(R.id.linearLayout1)
        groupListExpandImage = findViewById(R.id.groupListExpandImage)

        if(!::groupMap.isInitialized ) {
            groupMap = HashMap()
            timeScheduleMap = HashMap()
            groupTimeScheduleMap = HashMap()
            loadGroupData()
        }

        buttonSubmit.setOnClickListener {
            if(::mSelectedGroupTimeScheduleId.isInitialized){
                if(textDate.text.toString().isNotEmpty()){
                    mSelectedDate = textDate.text.toString().replace(DatabaseUtil.constantDateUISeparator, DatabaseUtil.constantKeySeparator)
                    mSelectedTime = textTime.text.toString()
                    val attendanceActivityIntent = Intent(this,
                        AttendanceActivity::class.java)
                    attendanceActivityIntent.putExtra(getString(R.string.mSelectedGroupTimeScheduleId), mSelectedGroupTimeScheduleId)
                    attendanceActivityIntent.putExtra(getString(R.string.mSelectedGroupId), groupTimeScheduleMap[mSelectedGroupTimeScheduleId]!!.groupId)
                    attendanceActivityIntent.putExtra(getString(R.string.mSelectedDate), mSelectedDate)
                    attendanceActivityIntent.putExtra(getString(R.string.mSelectedTime), mSelectedTime)
                    startActivity(attendanceActivityIntent)
                    finish()
                }else{
                    Toast.makeText(this,"Please select Date", LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this,"Please select Group", LENGTH_SHORT).show()
            }
        }

        initialHeight = groupListLayout.layoutParams.height
        groupListExpandImage.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_arrow_drop_down))
        groupListExpandImage.setOnClickListener{
            TransitionManager.beginDelayedTransition(contentMain, Rotate())
            if(!isGroupListExpanded){
                expandRecyclerView()
            }else{
                collapseRecyclerView()
            }
            isGroupListExpanded = !isGroupListExpanded
        }
    }

    private fun expandRecyclerView(){
        val resizeAnimation = ResizeAnimation(groupListLayout,LinearLayout.LayoutParams.MATCH_PARENT,initialHeight)
        resizeAnimation.duration = 30
        groupListLayout.startAnimation(resizeAnimation)
        groupListExpandImage.rotation = 180f
    }

    private fun collapseRecyclerView(){
        val resizeAnimation = ResizeAnimation(groupListLayout,initialHeight,0)
        resizeAnimation.duration = 30
        groupListLayout.startAnimation(resizeAnimation)
        groupListExpandImage.rotation = 0f
    }

    override fun onBackPressed() {
        if(loadingSpinner.mIsVisible){
            stopLoader()
        }else{
            super.onBackPressed()
        }
    }

    private fun loadGroupData(){
        showLoader()
        //Loading Groups
        DatabaseUtil.loadGroupData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val response = data as ArrayList<*>
                val groupArrayList = ArrayList<Group>()

                response.forEach { responseEntity ->
                    val group = responseEntity as Group
                    groupMap[group.id] = group
                    groupArrayList.add(group)
                }

                groupListRecyclerAdapter =
                    GroupListRecyclerAdapter(
                        this@AttendanceSetupActivity,
                        groupArrayList,
                        object :
                            GroupListRecyclerAdapter.OnGroupSelectedListener {
                            override fun onSelectedGroup(group: Group) {
                                Log.e("Groupid selected", group.id)
                                selectedGroupId = group.id
                                loadGroupTimeScheduleData(group.id)
                                collapseRecyclerView()
                            }

                            override fun onLongClick(data: String) {
                                //Code to call POC
                            }

                            override fun getAllSelectedGroups(selectedGroupMap: HashMap<String, Group>) {
                                //Not required to modify here
                            }
                        },
                        multiSelect = false,
                        sendGroupId = false,
                        restrictInactiveData = true
                    )

                val mLayoutManager = LinearLayoutManager(this@AttendanceSetupActivity)
                groupRecyclerView.layoutManager = mLayoutManager
                groupRecyclerView.addItemDecoration(DividerItemDecoration(this@AttendanceSetupActivity, DividerItemDecoration.VERTICAL))
                groupRecyclerView.itemAnimator = DefaultItemAnimator()
                groupRecyclerView.adapter = groupListRecyclerAdapter
                expandRecyclerView()
                stopLoader()
            }
        })
    }

    private fun loadGroupTimeScheduleData(groupId : String){
        //Loading Group time schedule map
        showLoader()
        DatabaseUtil.loadGroupTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val data = data as HashMap<String, GroupTimeSchedule>
                val timeScheduleIdList = ArrayList<String>()
                data.forEach{(k, v) ->
                    if(v.groupId == groupId){
                        groupTimeScheduleMap[k] = v
                        timeScheduleIdList.add(v.timeScheduleId)
                    }
                }
                loadTimeScheduleData(timeScheduleIdList)
            }
        })
    }

    private fun loadTimeScheduleData(timeScheduleIdList : ArrayList<String>){
        //Loading Time Schedule
        DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val data = data as HashMap<String, TimeSchedule>
                val allowedTimeScheduleList = ArrayList<TimeSchedule>()
                data.forEach{(k, v) ->
                    if(timeScheduleIdList.contains(v.id) && v.active){
                        timeScheduleMap = data
                        allowedTimeScheduleList.add(v)
                    }
                }

                timeScheduleListRecyclerAdapter =
                    TimeScheduleListRecyclerAdapter(
                        this@AttendanceSetupActivity,
                        allowedTimeScheduleList,
                        object :
                            TimeScheduleListRecyclerAdapter.OnTimeScheduleSelectedListner {
                            override fun onSelectedTimeSchedule(timeSchedule: TimeSchedule) {
                                Log.e("TimeSchedule Select", timeSchedule.id)
                                var selectedDay = ""
                                groupTimeScheduleMap.forEach { (K, V) ->
                                    if (V.timeScheduleId == timeSchedule.id) {
                                        mSelectedGroupTimeScheduleId = K
                                        selectedDay = timeSchedule.day
                                    }
                                }
                                llDateTimeControl.visibility = View.VISIBLE
                                mSelectedTimeScheduleDayOfTheWeek = ConverterUtils.convertDayToDayOfWeek(selectedDay)
                                loadDateTime()
                            }

                            override fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>) {

                            }
                        }, multiSelect = false, isLiteUI = false, restrictInactiveData = true
                    )

                val mLayoutManager = GridLayoutManager(this@AttendanceSetupActivity, 2)
                timeScheduleRecyclerView.layoutManager = mLayoutManager
                if(timeScheduleRecyclerView.itemDecorationCount == 0){
                    timeScheduleRecyclerView.addItemDecoration(
                        GridSpacingItemDecoration(
                            2,
                            ConverterUtils.dpToPx(this@AttendanceSetupActivity),
                            true
                        )
                    )
                }
                timeScheduleRecyclerView.itemAnimator = DefaultItemAnimator()
                timeScheduleRecyclerView.adapter = timeScheduleListRecyclerAdapter
                stopLoader()
            }
        })
    }



    private fun loadDateTime(showUi : Boolean = false){
        if(showUi){
            val calendar = Calendar.getInstance()
            val mYear = calendar.get(Calendar.YEAR)
            val mMonth = calendar.get(Calendar.MONTH)
            val mDay = calendar.get(Calendar.DAY_OF_MONTH)
            val mHour = calendar.get(Calendar.HOUR_OF_DAY)
            val mMinute = calendar.get(Calendar.MINUTE)


            val timePickerDialog = TimePickerDialog(
                this,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    Log.e("Time", "$hourOfDay:$minute")
                    textTime.text = StringBuilder().append(hourOfDay).append(DatabaseUtil.constantTimeSeparator).append(minute).toString()
                },
                mHour, mMinute, true
            )

            val datePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    Log.e("Date", dayOfMonth.toString() + DatabaseUtil.constantDateUISeparator + (monthOfYear + 1) + DatabaseUtil.constantDateUISeparator + year)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    calendar.set(Calendar.MONTH, monthOfYear)
                    if(calendar.get(Calendar.DAY_OF_WEEK) == mSelectedTimeScheduleDayOfTheWeek) {
                        textDate.text = StringBuilder()
                            .append(dayOfMonth)
                            .append(DatabaseUtil.constantDateUISeparator)
                            .append(monthOfYear + 1)
                            .append(DatabaseUtil.constantDateUISeparator)
                            .append(mYear).toString()
                        timePickerDialog.show()
                    }else{
                        Toast.makeText(this@AttendanceSetupActivity, "Invalid date selected $mSelectedTimeScheduleDayOfTheWeek", LENGTH_SHORT).show()
                    }
                }, mYear, mMonth, mDay
            )
            calendar.set(mYear, mMonth, mDay, 0, 0)
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 1)
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.show()
        }else {
            val calendarInstance = Calendar.getInstance()
            if(calendarInstance.get(Calendar.DAY_OF_WEEK) == mSelectedTimeScheduleDayOfTheWeek) {
                val textDateBuilder = StringBuilder()
                    .append(calendarInstance.get(Calendar.DAY_OF_MONTH))
                    .append(DatabaseUtil.constantDateUISeparator)
                    .append((calendarInstance.get(Calendar.MONTH)) + 1)
                    .append(DatabaseUtil.constantDateUISeparator)
                    .append(calendarInstance.get(Calendar.YEAR))
                textDate.text = textDateBuilder
            } else {
                textDate.text = ""
            }

            val textMinute : String = timeScheduleMap[groupTimeScheduleMap[mSelectedGroupTimeScheduleId]?.timeScheduleId]?.timeFrom.toString()

            if(!TextUtils.isEmpty(textMinute) && textMinute.length > 2) {
                val timeString = StringBuilder()
                    .append(textMinute.substring(0, (textMinute.length - 2)))
                    .append(DatabaseUtil.constantTimeSeparator)
                    .append(textMinute.substring(2, textMinute.length))
                textTime.text = timeString
            }
        }

    }

    private fun showLoader(){
        contentMain.visibility = View.INVISIBLE
        loadingSpinner.setIsVisible(true)
        loadingSpinner.startAnimation()
    }

    private fun stopLoader(isCompletedSuccess : Boolean = true, msg : String = ""){
        contentMain.visibility = View.VISIBLE
        loadingSpinner.setIsVisible(false)
        loadingSpinner.stopAnimation()
        if(!isCompletedSuccess && !TextUtils.isEmpty(msg)) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
