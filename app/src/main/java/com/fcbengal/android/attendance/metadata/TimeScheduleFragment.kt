package com.fcbengal.android.attendance.metadata

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.crashlytics.android.Crashlytics
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.adapter.TimeScheduleListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.GroupTimeSchedule
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.utils.ConverterUtils
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.fcbengal.android.attendance.utils.GridSpacingItemDecoration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseError
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TimeScheduleFragment : Fragment() {
    private val TAG = "TimeScheduleFragment"
    private val timeSeparator = ":"
    private lateinit var etTimeFrom: TextInputEditText
    private lateinit var etTimeTo: TextInputEditText
    private lateinit var imageClock: ImageView
    private lateinit var radioGroupDate: RadioGroup
    private lateinit var radioSunday: RadioButton
    private lateinit var radioMonday: RadioButton
    private lateinit var radioTuesday: RadioButton
    private lateinit var radioWednesday: RadioButton
    private lateinit var radioThursday: RadioButton
    private lateinit var radioFriday: RadioButton
    private lateinit var radioSaturday: RadioButton
    private lateinit var buttonSubmit: Button
    private lateinit var buttonReset: Button
    private lateinit var toggleActive : ToggleButton
    private lateinit var timeScheduleRecyclerView: RecyclerView

    private var selectedGroup: Group? = null
    private var timeScheduleMap = HashMap<String, TimeSchedule>()
    private var groupTimeScheduleMap = HashMap<String, GroupTimeSchedule>()
    private var mSelectedGroupTimeScheduleId: String = ""
    private lateinit var mSelectedTimeSchedule: TimeSchedule
    private lateinit var listener: TimeScheduleFragmentListener
    private lateinit var timeScheduleListRecyclerAdapter: TimeScheduleListRecyclerAdapter
    private lateinit var allowedTimeScheduleList: ArrayList<TimeSchedule>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_time_schedule, container, false)
        etTimeFrom = view.findViewById(R.id.et_from_time)
        etTimeTo = view.findViewById(R.id.et_to_time)
        imageClock = view.findViewById(R.id.image_clock)
        radioGroupDate = view.findViewById(R.id.radio_group_date)
        radioSunday = view.findViewById(R.id.radio_sunday)
        radioMonday = view.findViewById(R.id.radio_monday)
        radioTuesday = view.findViewById(R.id.radio_tuesday)
        radioWednesday = view.findViewById(R.id.radio_wednesday)
        radioThursday = view.findViewById(R.id.radio_thursday)
        radioFriday = view.findViewById(R.id.radio_friday)
        radioSaturday = view.findViewById(R.id.radio_saturday)
        timeScheduleRecyclerView = view.findViewById(R.id.time_schedule_recycler_view)
        toggleActive = view.findViewById(R.id.toggle_active)
        etTimeFrom.isEnabled = false
        etTimeTo.isEnabled = false
        imageClock.setOnClickListener {
            loadDateTime(true)
        }
        buttonSubmit = view.findViewById(R.id.button_submit)
        buttonSubmit.setOnClickListener {
            if (validateTime()) {
                listener.showLoader()
                if(!::mSelectedTimeSchedule.isInitialized) {
                    mSelectedTimeSchedule = TimeSchedule()
                }
                mSelectedTimeSchedule.day = getSelectedRadioDay()
                if(null != selectedGroup) {
                    mSelectedTimeSchedule.groupId = selectedGroup!!.id
                }
                mSelectedTimeSchedule.timeFrom = etTimeFrom.text.toString().trim().replace(timeSeparator, "").toInt()
                mSelectedTimeSchedule.timeTo = etTimeTo.text.toString().trim().replace(timeSeparator, "").toInt()
                mSelectedTimeSchedule.active = toggleActive.isChecked
                mSelectedTimeSchedule.selectedUI = false
                if(mSelectedTimeSchedule.id.isEmpty()){
                    Log.e(TAG, "empty time schedule id")
                    mSelectedTimeSchedule.id = StringBuilder().append(mSelectedTimeSchedule.day).append(
                        DatabaseUtil.constantKeySeparator).append(etTimeFrom.text.toString().trim().replace(timeSeparator, "")).toString()
                }

                DatabaseUtil.upsertTimeSchedule(mSelectedTimeSchedule, object : DatabaseUtil.OnDataUpsertListener{
                    override fun onSuccess(id: String) {
                        listener.stopLoader()
                        Toast.makeText(context!!,"Upload success", Toast.LENGTH_SHORT).show()

                        selectedGroup?.let {

                            //If group is selected then create mapping in group time schedule map
                            val groupTimeSchedule =
                                GroupTimeSchedule()
                            groupTimeSchedule.groupId = selectedGroup!!.id
                            groupTimeSchedule.timeScheduleId = mSelectedTimeSchedule.id
                            groupTimeSchedule.id = StringBuilder()
                                .append(groupTimeSchedule.groupId)
                                .append(DatabaseUtil.constantKeySeparator)
                                .append(groupTimeSchedule.timeScheduleId)
                                .toString()
                            DatabaseUtil.upsertGroupTimeSchedule(groupTimeSchedule, object : DatabaseUtil.OnDataUpsertListener{
                                override fun onSuccess(id: String) {
                                    Toast.makeText(context!!, "Mapping created", Toast.LENGTH_SHORT).show()
                                }

                                override fun onFailure(e: Exception) {
                                    Log.e(TAG, e.message, e)
                                    Toast.makeText(context!!, "Mapping error", Toast.LENGTH_SHORT).show()
                                }
                            })

                            loadGroupTimeScheduleData(it.id)
                        }
                        resetUI()
                    }

                    override fun onFailure(e: Exception) {
                        Log.e(TAG, e.message, e)
                        Crashlytics.logException(e)
                        resetUI()
                        if(null != e.message){
                            listener.stopLoaderWithError(false, e.message.toString())
                        }else{
                            listener.stopLoaderWithError(false, "Unable to add time schedule")
                        }

                    }
                })

            }
        }
        buttonReset = view.findViewById(R.id.button_reset)
        buttonReset.setOnClickListener {
            resetUI()
        }

        return view
    }

    companion object {
        fun newInstance(): TimeScheduleFragment =
            TimeScheduleFragment()
    }

    private fun resetUI(){
        resetRadios()
        etTimeTo.setText("")
        etTimeFrom.setText("")
        mSelectedTimeSchedule = TimeSchedule()
        mSelectedGroupTimeScheduleId = ""
    }

    interface TimeScheduleFragmentListener {
        fun showLoader()
        fun stopLoader()
        fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String)
        fun onTimeScheduleData(timeScheduleData: TimeSchedule?)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e(TAG, " ===================== onAttachCalled ===================== ")
        if (context is TimeScheduleFragmentListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement TimeScheduleFragmentListener")
        }
    }

    fun setSelectedGroup(group : Group){
        Log.e(TAG, "selected groupId ${group.id}")
        selectedGroup = group
        loadGroupTimeScheduleData(group.id)
    }

    private fun loadGroupTimeScheduleData(groupId: String) {
        //Loading Group time schedule map
        listener.showLoader()
        DatabaseUtil.loadGroupTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                listener.stopLoaderWithError(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val data = data as HashMap<String, GroupTimeSchedule>
                val timeScheduleIdList = ArrayList<String>()
                data.forEach { (k, v) ->
                    groupTimeScheduleMap[k] = v
                    if (v.groupId == groupId) {
                        timeScheduleIdList.add(v.timeScheduleId)
                    }
                }
                loadTimeScheduleData(timeScheduleIdList)
            }
        })
    }

    private fun loadTimeScheduleData(timeScheduleIdList: ArrayList<String>) {
        //Loading Time Schedule
        DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                listener.stopLoaderWithError(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val data = data as HashMap<String, TimeSchedule>
                allowedTimeScheduleList = ArrayList()
                timeScheduleMap = data
                data.forEach { (_, v) ->
                    if (timeScheduleIdList.contains(v.id) && v.active) {
                        allowedTimeScheduleList.add(v)
                    }
                }
                timeScheduleListRecyclerAdapter =
                    TimeScheduleListRecyclerAdapter(
                        context!!,
                        allowedTimeScheduleList,
                        object :
                            TimeScheduleListRecyclerAdapter.OnTimeScheduleSelectedListner {
                            override fun onSelectedTimeSchedule(timeSchedule: TimeSchedule) {
                                Log.e("TimeSchedule Select", timeSchedule.id)
                                groupTimeScheduleMap.forEach { (K, V) ->
                                    if (V.timeScheduleId == timeSchedule.id) {
                                        mSelectedGroupTimeScheduleId = K
                                    }
                                }
                                mSelectedTimeSchedule = timeSchedule
                                loadDateTime()
                                setRadioGroup(timeSchedule.day)
                                toggleActive.isChecked = mSelectedTimeSchedule.active
                                listener.onTimeScheduleData(mSelectedTimeSchedule)
                            }

                            override fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>) {

                            }
                        })

                val mLayoutManager = GridLayoutManager(context!!, 2)
                timeScheduleRecyclerView.layoutManager = mLayoutManager
                if (timeScheduleRecyclerView.itemDecorationCount == 0) {
                    timeScheduleRecyclerView.addItemDecoration(
                        GridSpacingItemDecoration(
                            2,
                            ConverterUtils.dpToPx(context!!),
                            true
                        )
                    )
                }
                timeScheduleRecyclerView.itemAnimator = DefaultItemAnimator()
                timeScheduleRecyclerView.adapter = timeScheduleListRecyclerAdapter
                listener.stopLoader()
            }
        })
    }

    private fun loadDateTime(showUi: Boolean = false) {
        if (showUi) {
            val calendar = Calendar.getInstance()
            val mHour = calendar.get(Calendar.HOUR_OF_DAY)
            val mMinute = calendar.get(Calendar.MINUTE)

            val toTimePickerDialog = TimePickerDialog(
                context!!,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    Log.e("Time", "$hourOfDay:$minute")
                    var hourString = ""
                    var minuteString = ""
                    if(hourOfDay < 10) hourString = "0"
                    if(minute < 10) minuteString = "0"
                    etTimeTo.setText(StringBuilder().append(hourString).append(hourOfDay).append(timeSeparator).append(minuteString).append(minute).toString())
                },
                mHour, mMinute, true
            )
            toTimePickerDialog.setTitle("To")

            val fromTimePickerDialog = TimePickerDialog(
                context!!,
                TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
                    Log.e("Time", "$hourOfDay:$minute")
                    var hourString = ""
                    var minuteString = ""
                    if(hourOfDay < 10) hourString = "0"
                    if(minute < 10) minuteString = "0"
                    etTimeFrom.setText(StringBuilder().append(hourString).append(hourOfDay).append(timeSeparator).append(minuteString).append(minute).toString())
                    toTimePickerDialog.updateTime(hourOfDay, minute)
                    toTimePickerDialog.show()
                },
                mHour, mMinute, true
            )
            fromTimePickerDialog.setTitle("From")
            fromTimePickerDialog.show()
        } else {
            if (::mSelectedTimeSchedule.isInitialized) {
                etTimeFrom.setText(timeToTimeUI(mSelectedTimeSchedule.timeFrom))
                etTimeTo.setText(timeToTimeUI(mSelectedTimeSchedule.timeTo))
            }
        }
    }

    private fun timeToTimeUI(time: Int): String {
        if (time in 1..2400) {
            val divider = time / 100
            val dividend = time % 100
            var hour = ""
            var minute = ""
            if(divider < 10){
                hour = "0$divider"
            }else{
                hour = divider.toString()
            }
            if(dividend < 10){
                minute = "0$dividend"
            }else{
                minute = dividend.toString()
            }
            return StringBuilder().append(hour).append(timeSeparator).append(minute).toString()
        }
        return ""
    }

    private fun setRadioGroup(day: String) {
        when (day) {
            "Sunday" -> radioSunday.isChecked = true
            "Monday" -> radioMonday.isChecked = true
            "Tuesday" -> radioTuesday.isChecked = true
            "Wednesday" -> radioWednesday.isChecked = true
            "Thursday" -> radioThursday.isChecked = true
            "Friday" -> radioFriday.isChecked = true
            "Saturday" -> radioSaturday.isChecked = true
        }
    }

    private fun resetRadios() {
        radioSunday.isChecked = false
        radioMonday.isChecked = false
        radioTuesday.isChecked = false
        radioWednesday.isChecked = false
        radioThursday.isChecked = false
        radioFriday.isChecked = false
        radioSaturday.isChecked = false
    }

    private fun getSelectedRadioDay() : String {
        var selectedDay = ""
        when {
            radioSunday.isChecked -> selectedDay = radioSunday.text.toString()
            radioMonday.isChecked -> selectedDay = radioMonday.text.toString()
            radioTuesday.isChecked -> selectedDay = radioTuesday.text.toString()
            radioWednesday.isChecked -> selectedDay = radioWednesday.text.toString()
            radioThursday.isChecked -> selectedDay = radioThursday.text.toString()
            radioFriday.isChecked -> selectedDay = radioFriday.text.toString()
            radioSaturday.isChecked -> selectedDay = radioSaturday.text.toString()
        }
        return selectedDay
    }

    private fun validateTime() : Boolean {
        return (
                etTimeFrom.text!!.isNotEmpty() && etTimeFrom.text!!.contains(timeSeparator) && etTimeFrom.text!!.length == 5
                        && etTimeTo.text!!.isNotEmpty() && etTimeTo.text!!.contains(timeSeparator) && etTimeTo.text!!.length == 5
                )
    }

}