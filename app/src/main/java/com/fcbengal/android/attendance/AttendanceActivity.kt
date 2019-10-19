package com.fcbengal.android.attendance

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.*
import com.fcbengal.android.attendance.adapter.AttendanceListRecyclerAdapter
import com.fcbengal.android.attendance.adapter.GroundListRecyclerAdapter
import com.fcbengal.android.attendance.entity.*
import com.fcbengal.android.attendance.helper.CustomGenericMapperDialog
import com.fcbengal.android.attendance.helper.RecyclerItemTouchHelper
import com.fcbengal.android.attendance.utils.ConverterUtils
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.fcbengal.android.attendance.utils.KLoadingSpin
import com.fcbengal.android.attendance.view.PlayerAttendanceView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AttendanceActivity : AppCompatActivity(), RecyclerItemTouchHelper.RecyclerItemTouchHelperListener{

    private val TAG = this::class.java.simpleName
    private lateinit var recyclerView : RecyclerView
    private lateinit var submitData : Button
    private lateinit var loadingSpinner : KLoadingSpin
    private var attendanceViewList: ArrayList<PlayerAttendanceView>? = null
    private var attendanceEntityMap = HashMap<String, PlayerAttendance>()
    private var mAttendanceRecyclerAdapter: AttendanceListRecyclerAdapter? = null

//    private var playerMap = HashMap<String, Player>()
    private var playerList = ArrayList<Player>()
    private val ATTENDANCE = "Attendance"

    //Data fetched from previous activity
    private lateinit var mSelectedDate : String
    private lateinit var mSelectedGroupId : String
    private lateinit var mSelectedGroupTimeScheduleId: String
    private var timeScheduleMap: HashMap<String, TimeSchedule>? = null
    private var timeScheduleId = ""
    private lateinit var contentMain : ConstraintLayout
    private var date = ""
    private var monthYear = ""
    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private var mSelectedGround : Ground? = null

    private lateinit var groundListRecyclerAdapter : GroundListRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        contentMain = findViewById(R.id.constraint_layout)
        recyclerView = findViewById(R.id.recycler_view)
        loadingSpinner = findViewById(R.id.KLoadingSpin)

        attendanceViewList = ArrayList()
        mAttendanceRecyclerAdapter =
            AttendanceListRecyclerAdapter(
                this@AttendanceActivity,
                attendanceViewList!!
            )
        val mLayoutManager : RecyclerView.LayoutManager = LinearLayoutManager(applicationContext)
        recyclerView.layoutManager = mLayoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = mAttendanceRecyclerAdapter

        //Getting Data
        if(null != intent.getStringExtra(getString(R.string.mSelectedGroupTimeScheduleId))){
            mSelectedGroupTimeScheduleId = intent.getStringExtra(getString(R.string.mSelectedGroupTimeScheduleId))
        }
        if(null != intent.getStringExtra(getString(R.string.mSelectedGroupId))) {
            mSelectedGroupId = intent.getStringExtra(getString(R.string.mSelectedGroupId))
        }
        if(null != intent.getStringExtra(getString(R.string.mSelectedDate))) {
            mSelectedDate = intent.getStringExtra(getString(R.string.mSelectedDate))
            supportActionBar?.title = "Attendance - ${mSelectedDate.replace(DatabaseUtil.constantKeySeparator, DatabaseUtil.constantDateUISeparator)}"
        }else{
            supportActionBar?.title = "Attendance"
        }

        val dateMonthYear = mSelectedDate.split(DatabaseUtil.constantKeySeparator)
        date = dateMonthYear[0]
        monthYear = StringBuilder().append(dateMonthYear[1]).append(DatabaseUtil.constantKeySeparator).append(dateMonthYear[2]).toString()
        val splitedText = mSelectedGroupTimeScheduleId.split(DatabaseUtil.constantKeySeparator)
        if(TextUtils.isEmpty(timeScheduleId) && splitedText.size == 3){
            timeScheduleId = mSelectedGroupTimeScheduleId.substring(splitedText[0].length+1, mSelectedGroupTimeScheduleId.length)
            Log.e(TAG, "TimeScheduleId : $timeScheduleId")
        }

        submitData = findViewById(R.id.submit)
        submitData.setOnClickListener {

            if(dateMonthYear.size == 3) {
                selectGround(monthYear, date)
            }else{
                Log.e(TAG, "Format error")
            }
        }
        val itemTouchHelperCallback =
            RecyclerItemTouchHelper(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
                this
            )
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

        try {
            loadTimeSchedule()
        } catch (e : Exception){
            stopLoader(false, e.toString())
            Log.e(TAG, "Error", e)
        }
    }

    private fun loadTimeSchedule(){
        if(null == timeScheduleMap){
            timeScheduleMap = HashMap()
        }
        showLoader()
        DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {

                val response = data as HashMap<*, *>
                response.forEach{( K, V) ->
                    timeScheduleMap!![K as String] = V as TimeSchedule
                }
                loadPlayerData()
            }
        })
    }

    private fun loadPlayerData(){
        DatabaseUtil.loadPlayerData(mSelectedGroupId, object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
                Toast.makeText(this@AttendanceActivity, "Unable to load Player list", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(data: Any) {
                val playerResponse = data as ArrayList<*>
                playerResponse.forEach {
                    val player = it as Player
//                    playerMap[player.id] = player
                    playerList.add(player)
                }
//                playerMap = data as HashMap<String, Player>
                loadAttendance()
            }
        })

    }

    fun loadAttendance(){
        attendanceViewList?.clear()
        DatabaseUtil.loadAttendanceDataByGroupTimeScheduleData(
            monthYear,date,mSelectedGroupTimeScheduleId,object : DatabaseUtil.OnDataCompletedListener{
                override fun onCancelled(error: DatabaseError) {
                    stopLoader(false, error.message)
                }

                override fun onDataChange(data: Any) {
                    val playerIdAttendanceMapResponse = data as HashMap<*, *>
                    val playerIdAttendanceMap = HashMap<String, PlayerAttendance>()
                    playerIdAttendanceMapResponse.forEach { (K, V) ->
                        playerIdAttendanceMap[K as String] = V as PlayerAttendance
                    }
//                    if(playerIdAttendanceMapResponse.size > 0){
//                        playerIdAttendanceMapResponse.forEach {(K, V) ->
//                            val playerId = K as String
//                            val playerAttendance = V as PlayerAttendance
//                            val playerEntity = playerMap[playerId]
//                            Log.e(TAG, "PlayerData -> ${playerEntity?.fName}")
//                            if(null != playerEntity) {
//                                attendanceViewList?.add(ConverterUtils.playerToPlayerAttendanceView(playerEntity, playerAttendance))
//                                attendanceEntityMap[playerId] = ConverterUtils.playerToPlayerAttendance(playerEntity, playerAttendance)
//                            }else{
//                                Log.e(TAG, "Player data not found in playerMap for $playerId")
//                            }
//                        }
//                    }else{
//                        playerMap.forEach { (playerId, playerEntity)->
//
//                            attendanceViewList?.add(ConverterUtils.playerToPlayerAttendanceView(playerEntity, null))
//                            attendanceEntityMap[playerId] = ConverterUtils.playerToPlayerAttendance(playerEntity, null)
//                        }
//                    }
                    if(playerList.isNotEmpty()){
                        playerList.forEach { playerEntity ->
                            val playerId = playerEntity.id
                            if(playerIdAttendanceMap.size > 0 && null != playerIdAttendanceMap[playerEntity.id]) {
                                val playerAttendance = playerIdAttendanceMap[playerEntity.id]
                                Log.e(TAG, "PlayerData -> ${playerEntity.fName}")
                                attendanceViewList?.add(ConverterUtils.playerToPlayerAttendanceView(playerEntity, playerAttendance))
                                attendanceEntityMap[playerId] = ConverterUtils.playerToPlayerAttendance(playerEntity, playerAttendance)
                            }else{
                                attendanceViewList?.add(ConverterUtils.playerToPlayerAttendanceView(playerEntity, null))
                                attendanceEntityMap[playerId] = ConverterUtils.playerToPlayerAttendance(playerEntity, null)
                            }
                        }
                    }

                    mAttendanceRecyclerAdapter?.notifyDataSetChanged()
                    stopLoader()
                }
            }
        )
    }

    override fun onLeftSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int) {
        if (viewHolder is AttendanceListRecyclerAdapter.MyViewHolder) {
            val view : PlayerAttendanceView = attendanceViewList!!.get(position)
            Log.e(TAG, "Marking attendance for ${view.fName} ${view.lName}")
            view.entryTime = timeScheduleMap!![timeScheduleId]!!.timeFrom
            view.exitTime = timeScheduleMap!![timeScheduleId]!!.timeTo
            view.status = AttendanceStatus.PRESENT
            if(view.createdTimeStamp.isEmpty()) {
                view.createdTimeStamp = ConverterUtils.getStringDate()
            }
            view.modifiedTimeStamp = ConverterUtils.getStringDate()
            view.date = ConverterUtils.getStringDate()
            Snackbar.make(contentMain,"Add delay?", Snackbar.LENGTH_SHORT).setAction("OK") {
                val timePickerDialog = TimePickerDialog(
                    this,
                    TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                        Log.e("Time", "$hourOfDay:$minute")
                        view.delayMinutes = (hourOfDay * 60) + minute
                        view.status = AttendanceStatus.PRESENT_DELAYED
                        view.entryTime = ConverterUtils.getDelayedEntryTime(view.entryTime, view.delayMinutes)

                        attendanceEntityMap[view.id] = ConverterUtils.playerAttendanceViewToPlayerAttendance(view)
                        mAttendanceRecyclerAdapter!!.markPresent(view, /*viewHolder.getAdapterPosition()*/position, viewHolder)
                    },
                    0, 0, true
                )
                timePickerDialog.show()
            }.show()
            Log.e(TAG, "Delay ${view.delayMinutes}")
            attendanceEntityMap[view.id] = ConverterUtils.playerAttendanceViewToPlayerAttendance(view)
            mAttendanceRecyclerAdapter!!.markPresent(view, /*viewHolder.getAdapterPosition()*/position, viewHolder)
        }
    }

    override fun onRightSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int, position: Int) {
        if (viewHolder is AttendanceListRecyclerAdapter.MyViewHolder) {
            val view = attendanceViewList!!.get(position)
            Log.e(TAG, "Marking absent for ${view.fName} ${view.lName}")
            view.entryTime = 0
            view.exitTime = 0
            view.status = AttendanceStatus.ABSENT
            Snackbar.make(contentMain,"Player informed?", Snackbar.LENGTH_SHORT).setAction("OK") {
                view.status = AttendanceStatus.ABSENT_INFORMED
                attendanceEntityMap[view.id] = ConverterUtils.playerAttendanceViewToPlayerAttendance(view)
                mAttendanceRecyclerAdapter!!.markAbsent(view, position, viewHolder)
            }.show()
            view.modifiedTimeStamp = ConverterUtils.getStringDate()
            attendanceEntityMap[view.id] = ConverterUtils.playerAttendanceViewToPlayerAttendance(view)
            mAttendanceRecyclerAdapter!!.markAbsent(view, position, viewHolder)

        }
    }

    private fun selectGround(month_year: String, date: String){
        showLoader()
        DatabaseUtil.loadGroundData(object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                stopLoader()
                val groundList = ArrayList<Ground>()
                val response = data as HashMap<*, *>
                response.forEach { (_, V) ->
                    groundList.add(V as Ground)
                }
                groundListRecyclerAdapter =
                    GroundListRecyclerAdapter(
                        this@AttendanceActivity,
                        groundList,
                        object :
                            GroundListRecyclerAdapter.OnGroundSelectedListener {
                            override fun onSelectedGround(ground: Ground) {
                                mSelectedGround = ground
                            }

                            override fun getAllSelectedGrounds(goundMap: HashMap<String, Ground>) {

                            }
                        },
                        false,
                        true
                    )
                val customGenericMapperDialog =
                    CustomGenericMapperDialog(
                        this@AttendanceActivity,
                        groundListRecyclerAdapter,
                        object :
                            CustomGenericMapperDialog.OnClickListener {
                            override fun onClick(value: Boolean) {
                                if (value && null != mSelectedGround) {
                                    submitAttendanceData(month_year, date)
                                }
                            }
                        })
                customGenericMapperDialog.show()
                customGenericMapperDialog.setCanceledOnTouchOutside(false)

            }
        })
    }

    private fun submitAttendanceData(month_year : String, date : String){
        showLoader()
        var count = 0
        val failureList = ArrayList<String>()

        attendanceEntityMap.forEach{(key, value) ->
            value.groundName = mSelectedGround!!.name
            value.groundId = mSelectedGround!!.id
            if (value.status == AttendanceStatus.DEFAULT) {
                value.status = AttendanceStatus.ABSENT
            }
            databaseReference.child(ATTENDANCE).child(month_year).child(mSelectedGroupTimeScheduleId).child(date).child(key).setValue(value)
                .addOnCompleteListener {
                    Log.e(TAG, "Success for ${value.playerId}")
                    count++
                    if(count == attendanceEntityMap.size){
                        showPostStatus(failureList)
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Failed for ${value.playerId}")
                    count++
                    /*if(null != playerMap[value.playerId] && playerMap[value.playerId]!!.fName.isNotEmpty()) {
                        failureList.add(playerMap[value.playerId]!!.fName)
                    }
                    if(count == attendanceEntityMap.size){
                        showPostStatus(failureList)
                    }*/
                }
        }

    }

    private fun showPostStatus(failureList : ArrayList<String>){
        stopLoader()
        if(failureList.size == 0){
            Toast.makeText(this@AttendanceActivity, "Success", Toast.LENGTH_SHORT).show()
            finish()
        }else{
            Toast.makeText(this@AttendanceActivity, "Unable to post all data", Toast.LENGTH_SHORT).show()
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

    override fun onBackPressed() {
        if(loadingSpinner.mIsVisible){
            stopLoader()
        }else{
            super.onBackPressed()
        }
    }
}
