package com.fcbengal.android.attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import com.fcbengal.android.attendance.adapter.GroundListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Ground
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.helper.CustomGenericMapperDialog
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.fcbengal.android.attendance.utils.ExcelGeneratorUtilV2
import com.fcbengal.android.attendance.utils.KLoadingSpin
import com.fcbengal.android.attendance.view.GroupAttendanceView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseError
import java.util.*
import kotlin.collections.ArrayList

class AttendanceReportActivity : AppCompatActivity() {

    private val mTAG = this::class.java.simpleName
//    lateinit var groupRecyclerView: RecyclerView
//    lateinit var groupListRecyclerAdapter: GroupListRecyclerAdapter
//    private var selectedGroupId: String? = null
//    var selectedGroupName: String? = null
//
//    lateinit var timeScheduleRecyclerView: RecyclerView
//    lateinit var timeScheduleListRecyclerAdapter: TimeScheduleListRecyclerAdapter

    private lateinit var loadingSpinner: KLoadingSpin
    private lateinit var contentMain: LinearLayout

    private lateinit var llDateTimeControl: LinearLayout
    private lateinit var buttonDateRange: ImageButton
    private lateinit var fromDate: TextView
    private lateinit var toDate: TextView
    private lateinit var buttonSubmit: Button
//    private lateinit var mSelectedTime: String
//    private lateinit var mSelectedDate: String
    private var mSelectedGroupTimeScheduleIdList = ArrayList<String>()
    private var userEmail = ""
    private var userName = ""
    private val EMAIL_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendace_report)

        if(null != intent.getStringExtra(getString(R.string.email_to_address_key))){
            userEmail = intent.getStringExtra(getString(R.string.email_to_address_key))
        }
        if(null != intent.getStringExtra(getString(R.string.user_name_key))){
            userName = intent.getStringExtra(getString(R.string.user_name_key))
        }

        loadingSpinner = findViewById(R.id.KLoadingSpin)
        contentMain = findViewById(R.id.content_main)
        llDateTimeControl = findViewById(R.id.llDateTimeControl)
//        llDateTimeControl.visibility = View.INVISIBLE
        buttonDateRange = findViewById(R.id.buttonDateRange)
        buttonDateRange.setOnClickListener {
            loadDateTime(true)
        }
        fromDate = findViewById(R.id.fromDate)
        toDate = findViewById(R.id.toDate)
        buttonSubmit = findViewById(R.id.buttonSubmit)

//        groupRecyclerView = findViewById(R.id.group_recycler_view)
//        timeScheduleRecyclerView = findViewById(R.id.time_schedule_recycler_view)

        /*if (null == selectedGroupId) {
            loadGroupData()
        }*/

        llDateTimeControl.visibility = View.VISIBLE
        loadDateTime()

        buttonSubmit.setOnClickListener {
//            if (!TextUtils.isEmpty(selectedGroupId) && mSelectedGroupTimeScheduleIdList.isNotEmpty()) {
                selectGround()
//            } else {
//                Toast.makeText(this, "Please select Group", LENGTH_SHORT).show()
//            }
        }
    }

    private fun createExcelReport(
        groupAttendanceView : GroupAttendanceView,
        monthId: Int
    ) {
        ExcelGeneratorUtilV2(
            groupAttendanceView,
            monthId,
            this,
            object :
                ExcelGeneratorUtilV2.ExcelGeneratorListener {
                override fun onSuccess(uri: Uri, month: String) {
                    stopLoader()
                    sendEmail(uri, month)
                }

                override fun onProgress(status: String) {
                    updateLoaderText(status)
                }

                override fun onError() {
                    stopLoader()
                    Toast.makeText(
                        this@AttendanceReportActivity,
                        "Error generating report",
                        LENGTH_SHORT
                    ).show()
                }
            })
    }

    /*private fun loadGroupData() {
        showLoader()
        DatabaseUtil.loadGroupData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.details)
            }

            override fun onDataChange(data: Any) {
                val groupDataMap = data as HashMap<String, Group>
                val groupList = ArrayList<Group>()
                groupDataMap.forEach { (_, v) ->
//                    Log.e("Group Data", k)
                    groupList.add(v)
                }
                groupListRecyclerAdapter =
                    GroupListRecyclerAdapter(
                        this@AttendanceReportActivity,
                        groupList,
                        object :
                            GroupListRecyclerAdapter.OnGroupSelectedListener {
                            override fun onSelectedGroup(group: Group) {
                                Log.e("Groupid selected", group.id)
                                selectedGroupId = group.id
                                selectedGroupName = group.name
                                loadGroupTimeScheduleData(group.id)
                            }

                            override fun onLongClick(data: String) {
                                //Code to call POC
                            }

                            override fun getAllSelectedGroups(selectedGroupMap: HashMap<String, Group>) {
                                //Not required to modify here
                            }
                        })
                val mLayoutManager = LinearLayoutManager(this@AttendanceReportActivity)
                groupRecyclerView.layoutManager = mLayoutManager
                groupRecyclerView.addItemDecoration(
                    DividerItemDecoration(
                        this@AttendanceReportActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
                groupRecyclerView.itemAnimator = DefaultItemAnimator()
                groupRecyclerView.adapter = groupListRecyclerAdapter
                stopLoader()
            }
        })
    }

    private fun loadGroupTimeScheduleData(groupId: String) {
        //Loading Group time schedule map
        showLoader()
        DatabaseUtil.loadGroupTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.details)
            }

            override fun onDataChange(data: Any) {
                val groupTimeScheduleMap = data as HashMap<String, GroupTimeSchedule>
                val timeScheduleIdList = ArrayList<String>()
                groupTimeScheduleMap.forEach { (_, v) ->
                    if (v.groupId == groupId && v.active) {
                        timeScheduleIdList.add(v.timeScheduleId)
                    }
                }
                loadTimeScheduleData(timeScheduleIdList)
            }
        })
    }

    private fun loadTimeScheduleData(timeScheduleIdList: ArrayList<String>) {
        DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.details)
            }

            override fun onDataChange(data: Any) {
                val response = data as HashMap<*, *>
                val allowedTimeScheduleList = ArrayList<TimeSchedule>()
                response.forEach { (k, v) ->
                    if (timeScheduleIdList.contains(k)) {
                        allowedTimeScheduleList.add(v as TimeSchedule)
                    }
                }
                timeScheduleListRecyclerAdapter =
                    TimeScheduleListRecyclerAdapter(
                        this@AttendanceReportActivity,
                        allowedTimeScheduleList,
                        object :
                            TimeScheduleListRecyclerAdapter.OnTimeScheduleSelectedListner {
                            override fun onSelectedTimeSchedule(timeSchedule: TimeSchedule) {
                                Log.e("TimeSchedule Select", timeSchedule.id)
                                llDateTimeControl.visibility = View.VISIBLE
                                loadDateTime()
                            }

                            override fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>) {
                                mSelectedGroupTimeScheduleIdList.clear()
                                timeScheduleMap.forEach { (k, _) ->
                                    Log.e("MultiTimeSchedule", k)
                                    val groupTimeScheduleId =
                                        StringBuilder().append(selectedGroupId)
                                            .append(DatabaseUtil.constantKeySeparator)
                                            .append(k)
                                    Log.e(
                                        "MultiSelect",
                                        "Adding $groupTimeScheduleId in mSelectedGroupTimeScheduleIdList"
                                    )
                                    mSelectedGroupTimeScheduleIdList.add(groupTimeScheduleId.toString())
                                }
                            }
                        },
                        true
                    )

                val mLayoutManager = GridLayoutManager(this@AttendanceReportActivity, 2)
                timeScheduleRecyclerView.layoutManager = mLayoutManager
                if(timeScheduleRecyclerView.itemDecorationCount == 0) {
                    timeScheduleRecyclerView.addItemDecoration(
                        GridSpacingItemDecoration(
                            2,
                            ConverterUtils.dpToPx(this@AttendanceReportActivity),
                            true
                        )
                    )
                }
                timeScheduleRecyclerView.itemAnimator = DefaultItemAnimator()
                timeScheduleRecyclerView.adapter = timeScheduleListRecyclerAdapter
                stopLoader()
            }
        })
    }*/

    private fun loadDateTime(showUi: Boolean = false) {
        val calendarInstance = Calendar.getInstance()
        val mYear = calendarInstance.get(Calendar.YEAR)
        val mMonth = calendarInstance.get(Calendar.MONTH) + 1
        val mDay = calendarInstance.get(Calendar.DAY_OF_MONTH)
        val maxDateInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
        val minDateInMonth = 1

        if (showUi) {
            val toDatePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    toDate.text = StringBuilder()
                        .append(dayOfMonth)
                        .append(DatabaseUtil.constantDateUISeparator)
                        .append(monthOfYear + 1)
                        .append(DatabaseUtil.constantDateUISeparator)
                        .append(mYear).toString()
                }, mYear, mMonth, mDay
            )

            val fromDatePickerDialog = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    fromDate.text = StringBuilder()
                        .append(dayOfMonth)
                        .append(DatabaseUtil.constantDateUISeparator)
                        .append(monthOfYear + 1)
                        .append(DatabaseUtil.constantDateUISeparator)
                        .append(mYear).toString()

                    val minDateCal = Calendar.getInstance()
                    minDateCal.set(Calendar.MONTH, monthOfYear)
                    minDateCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    minDateCal.set(Calendar.YEAR, year)
                    toDatePickerDialog.datePicker.minDate = minDateCal.timeInMillis

                    minDateCal.set(Calendar.DAY_OF_MONTH, minDateCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                    toDatePickerDialog.datePicker.maxDate = minDateCal.timeInMillis
                    toDatePickerDialog.show()

                }, mYear, mMonth, mDay
            )
//            val minFromDateCal = Calendar.getInstance()
//            minFromDateCal.set(Calendar.DAY_OF_MONTH, minDateInMonth)
//            fromDatePickerDialog.datePicker.minDate = minFromDateCal.timeInMillis

            val maxFromDateCal = Calendar.getInstance()
            maxFromDateCal.set(Calendar.DAY_OF_MONTH, maxDateInMonth)
            fromDatePickerDialog.datePicker.maxDate = maxFromDateCal.timeInMillis
            fromDatePickerDialog.show()

        } else {
            val fromDateBuilder = StringBuilder()
                .append(minDateInMonth)
                .append(DatabaseUtil.constantDateUISeparator)
                .append(mMonth)
                .append(DatabaseUtil.constantDateUISeparator)
                .append(mYear)
            fromDate.text = fromDateBuilder

            val toDateBuilder = StringBuilder()
                .append(maxDateInMonth)
                .append(DatabaseUtil.constantDateUISeparator)
                .append(mMonth)
                .append(DatabaseUtil.constantDateUISeparator)
                .append(mYear)
            toDate.text = toDateBuilder
        }
    }

    private fun selectGround(){
        showLoader()
        DatabaseUtil.loadGroundData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                stopLoader()
                val groundList = ArrayList<Ground>()
                val response = data as HashMap<*, *>
                var selectedGroundName = ""
                response.forEach { (_, V) ->
                    groundList.add(V as Ground)
                }
                val groundListRecyclerAdapter =
                    GroundListRecyclerAdapter(
                        this@AttendanceReportActivity,
                        groundList,
                        object :
                            GroundListRecyclerAdapter.OnGroundSelectedListener {
                            override fun onSelectedGround(ground: Ground) {
                                selectedGroundName = ground.name
                            }

                            override fun getAllSelectedGrounds(groundMap: HashMap<String, Ground>) {

                            }
                        },
                        false,
                        true
                    )
                val customGenericMapperDialog =
                    CustomGenericMapperDialog(
                        this@AttendanceReportActivity,
                        groundListRecyclerAdapter,
                        object :
                            CustomGenericMapperDialog.OnClickListener {
                            override fun onClick(value: Boolean) {
                                if (value && selectedGroundName.isNotEmpty()) {
                                    submitData(selectedGroundName)
                                }
                            }
                        })
                customGenericMapperDialog.show()
                customGenericMapperDialog.setCanceledOnTouchOutside(false)

            }
        })
    }

    private fun submitData(groundName: String){
        showLoader()
        val fromDateMonthYearArray = fromDate.text.split(DatabaseUtil.constantDateUISeparator)
        val fromMonthYear = StringBuilder().append(fromDateMonthYearArray[1]).append(
            DatabaseUtil.constantKeySeparator)
            .append(fromDateMonthYearArray[2])
        val toDateMonthYearArray = toDate.text.split(DatabaseUtil.constantDateUISeparator)

        DatabaseUtil.loadAllAttendanceData(
            fromMonthYear.toString(),
            fromDateMonthYearArray[0].toInt(),
            toDateMonthYearArray[0].toInt(),
            mSelectedGroupTimeScheduleIdList,
            groundName,
            object : DatabaseUtil.OnDataCompletedListener {
                override fun onCancelled(error: DatabaseError) {
                    stopLoader(false, error.message)
                }

                override fun onDataChange(data: Any) {
                    /*val playerAttendanceData = data as AttendanceView
                    if (!playerAttendanceData.sortedDateList.isEmpty()) {
                        DatabaseUtil.loadPlayerData(selectedGroupId!!, object : DatabaseUtil.OnDataCompletedListener {
                            override fun onCancelled(error: DatabaseError) {
                                stopLoader(false, error.message)
                            }

                            override fun onDataChange(data: Any) {
                                val playerIdMap = data as HashMap<String, Player>
                                createExcelReport(
                                    playerAttendanceData,
                                    playerIdMap,
                                    selectedGroupName!!,
                                    groundName,
                                    fromDateMonthYearArray[1].toInt()
                                )
                            }
                        })
                    }*/

                    val groupAttendanceViewResponse = data as GroupAttendanceView
                    if(groupAttendanceViewResponse.groupAttendanceMap.isNotEmpty()){
                        DatabaseUtil.loadGroupPlayerData(object : DatabaseUtil.OnDataCompletedListener{
                            override fun onCancelled(error: DatabaseError) {
                                stopLoader(false, error.message)
                            }

                            override fun onDataChange(data: Any) {
                                val groupPlayerResponseMap = data as HashMap<String, ArrayList<Player>>
                                groupAttendanceViewResponse.groupPlayerMap = groupPlayerResponseMap

                                DatabaseUtil.loadGroundData(object : DatabaseUtil.OnDataCompletedListener{
                                    override fun onCancelled(error: DatabaseError) {
                                        stopLoader(false, error.message)
                                    }

                                    override fun onDataChange(data: Any) {
                                        val groundMapResponse = data as HashMap<String, Ground>
                                        groupAttendanceViewResponse.groundMap = groundMapResponse
                                        createExcelReport(
                                            groupAttendanceViewResponse,
                                            fromDateMonthYearArray[1].toInt()
                                        )
                                    }
                                })
                            }
                        })
                    }
                }
            })
    }

    private fun showLoader() {
        contentMain.visibility = View.INVISIBLE
        loadingSpinner.setIsVisible(true)
        loadingSpinner.startAnimation()
    }

    private fun updateLoaderText(status: String) {
//        loadingSpinner.setText(status)
        Log.e(mTAG, status)
    }

    private fun stopLoader(isCompletedSuccess: Boolean = true, msg: String = "") {
        contentMain.visibility = View.VISIBLE
        loadingSpinner.stopAnimation()
        if (!isCompletedSuccess && !TextUtils.isEmpty(msg)) {
            Toast.makeText(this, msg, LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed(){
        if(loadingSpinner.mIsVisible){
            stopLoader()
        } else {
            super.onBackPressed()
        }
    }

    private fun sendEmail(uri: Uri, month : String){
        if(userEmail.isNotEmpty()){
            Snackbar.make(contentMain, "Send email?", Snackbar.LENGTH_INDEFINITE).setAction("OK"){
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "plain/text"
                emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                emailIntent.putExtra(Intent.EXTRA_BCC, arrayOf(getString(R.string.email_bcc)))
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject, month))
                emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
                emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body, userName, month))
                startActivityForResult(Intent.createChooser(emailIntent, "Complete using.."), EMAIL_REQUEST_CODE)
            }.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == EMAIL_REQUEST_CODE && resultCode == RESULT_OK){
            Toast.makeText(this@AttendanceReportActivity, "Success", LENGTH_SHORT).show()
        }else if(requestCode == EMAIL_REQUEST_CODE && resultCode != RESULT_OK) {
            Log.e(mTAG, "Email send failure code $resultCode")
//            Toast.makeText(this@AttendanceReportActivity,"Failed to send email", LENGTH_SHORT).show()
        }
    }
}