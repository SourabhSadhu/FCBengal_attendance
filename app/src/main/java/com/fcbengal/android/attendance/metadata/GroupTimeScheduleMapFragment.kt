package com.fcbengal.android.attendance.metadata

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.adapter.ExpandableGroupListRecyclerAdapter
import com.fcbengal.android.attendance.adapter.TimeScheduleListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.GroupTimeSchedule
import com.fcbengal.android.attendance.entity.TimeSchedule
import com.fcbengal.android.attendance.helper.CustomGenericMapperDialog
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.google.firebase.database.DatabaseError

class GroupTimeScheduleMapFragment : Fragment() {

    private val mTAG = "GrpTimeSchdMapFragment"
    private lateinit var parentView: View
    private lateinit var contentMain: ConstraintLayout
    private lateinit var recyclerView: RecyclerView
    lateinit var timeScheduleListRecyclerAdapter: TimeScheduleListRecyclerAdapter
    lateinit var customGenericMapperDialog: CustomGenericMapperDialog
    var selectedTimeSchedule : TimeSchedule? = null
    lateinit var listener : GroupTimeScheduleFragmentListener

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.e("GroupTimeScheduleMap", "onCreateView Called")
        parentView = inflater.inflate(R.layout.fragment_group_time_schedule_map, container, false)
        contentMain = parentView.findViewById(R.id.content_main)
        recyclerView = parentView.findViewById(R.id.group_recycler_view)
        loadGroupData()
        return parentView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is GroupTimeScheduleFragmentListener){
            listener = context
        }else{
            throw RuntimeException("$context must implement GroupTimeScheduleFragmentListener")
        }
    }

    companion object {
        fun newInstance(): GroupTimeScheduleMapFragment =
            GroupTimeScheduleMapFragment()
    }

    interface GroupTimeScheduleFragmentListener{
        fun showLoader()
        fun stopLoader()
        fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String)
    }

    fun loadGroupData() {
        showLoader()
        DatabaseUtil.loadGroupData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.details)
            }

            override fun onDataChange(data: Any) {
                stopLoader()
                val groupResponse = data as ArrayList<*>
                val groupList = ArrayList<Group>()
                groupResponse.forEach { groupEntity ->
                    groupList.add(groupEntity as Group)
                }

                //Loading time schedules
                DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
                    override fun onCancelled(error: DatabaseError) {
                        listener.stopLoaderWithError(false, error.message)
                    }

                    override fun onDataChange(data: Any) {
                        val responseData = data as HashMap<*, *>
                        val groupTimeScheduleMaps = HashMap<String, ArrayList<TimeSchedule>>()
                        for(groupIter in 0 until groupList.size){
                            val group = groupList[groupIter]
                            if(null == groupTimeScheduleMaps[group.id]){
                                groupTimeScheduleMaps[group.id] = ArrayList()
                            }
                            responseData.forEach{ (_, V) ->
                                if((V as TimeSchedule).groupId == group.id) {
                                    groupTimeScheduleMaps[group.id]!!.add(V)
                                }
                            }
                        }
                        listener.stopLoader()

                        //Initializing expandable group time schedule
                        Log.e(mTAG, "GroupList ${groupList.size}, GroupTimeSchedule ${groupTimeScheduleMaps.size}")
                        val expandableGroupListRecyclerAdapter =
                            ExpandableGroupListRecyclerAdapter(
                                context!!,
                                groupList,
                                groupTimeScheduleMaps,
                                object :
                                    ExpandableGroupListRecyclerAdapter.OnGroupSelectedListener {
                                    override fun onSelectedGroup(group: Group) {
                                    }

                                    override fun onLongClick(data: String) {
                                        createMapperDialog(data)
                                    }

                                    override fun getAllSelectedGroups(selectedGroupMap: HashMap<String, Group>) {
                                    }

                                    override fun getSelectedTimeSchedule(
                                        groupId: String,
                                        timeSchedule: TimeSchedule
                                    ) {
                                        // Code to Activate/De Activate time schedule
                                        Log.e(
                                            mTAG,
                                            "GrpTimeSchFrag -> ExpandableGroupListRecyclerAdapter -> timeScheduleId ${timeSchedule.id}"
                                        )
                                    }
                                },
                                multiSelect = false,
                                sendGroupId = true
                            )
                        val mLayoutManager = LinearLayoutManager(context!!)
                        recyclerView.layoutManager = mLayoutManager
                        recyclerView.addItemDecoration(
                            DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL)
                        )
                        recyclerView.itemAnimator = DefaultItemAnimator()
                        recyclerView.adapter = expandableGroupListRecyclerAdapter
                    }
                })

            }
        })
    }

    /**
     *
     */

    private fun createMapperDialog(groupId : String) {
        showLoader()
        val unmappedTimeScheduleList = ArrayList<TimeSchedule>()
        DatabaseUtil.loadTimeScheduleData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader()
                Toast.makeText(context!!, "Unable to load time schedule", Toast.LENGTH_SHORT).show()
            }

            override fun onDataChange(data: Any) {
                stopLoader()
                selectedTimeSchedule = null
                val timeScheduleMap = data as HashMap<*, *>
                timeScheduleMap.forEach { (_, V) ->
                    if ((V as TimeSchedule).groupId.isBlank()) {
                        unmappedTimeScheduleList.add(V)
                    }
                }
                if (unmappedTimeScheduleList.size > 0) {
                    timeScheduleListRecyclerAdapter =
                        TimeScheduleListRecyclerAdapter(
                            context!!,
                            unmappedTimeScheduleList,
                            object :
                                TimeScheduleListRecyclerAdapter.OnTimeScheduleSelectedListner {
                                override fun onSelectedTimeSchedule(timeSchedule: TimeSchedule) {
                                    selectedTimeSchedule = timeSchedule
                                }

                                override fun getAllSelectedTimeSchedule(timeScheduleMap: HashMap<String, TimeSchedule>) {
                                }
                            },
                            multiSelect = false,
                            isLiteUI = true
                        )
                    customGenericMapperDialog =
                        CustomGenericMapperDialog(
                            context!!,
                            timeScheduleListRecyclerAdapter,
                            object :
                                CustomGenericMapperDialog.OnClickListener {
                                override fun onClick(value: Boolean) {
                                    if (value) {
                                        if (null != selectedTimeSchedule) {
                                            if (groupId.isNotBlank()) {
                                                val id = StringBuilder()
                                                    .append(groupId)
                                                    .append(DatabaseUtil.constantKeySeparator)
                                                    .append(selectedTimeSchedule!!.id)
                                                val entity =
                                                    GroupTimeSchedule()
                                                entity.id = id.toString()
                                                entity.groupId = groupId
                                                entity.timeScheduleId = selectedTimeSchedule!!.id
                                                DatabaseUtil.upsertGroupTimeSchedule(
                                                    entity,
                                                    object :
                                                        DatabaseUtil.OnDataUpsertListener {
                                                        override fun onSuccess(id: String) {
                                                            Toast.makeText(
                                                                context!!,
                                                                "Suceess",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }

                                                        override fun onFailure(e: Exception) {
                                                            Log.e(mTAG, e.message, e)
                                                            Toast.makeText(
                                                                context!!,
                                                                "Unable to update",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    })
                                            } else {
                                                Log.e(mTAG, "Group id blank")
                                            }
                                        } else {
                                            Toast.makeText(
                                                context!!,
                                                "Please select time schedule",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            })
                    customGenericMapperDialog.show()
                    customGenericMapperDialog.setCanceledOnTouchOutside(false)
                }else{
                    Toast.makeText(context!!, "Unmapped time schedule not found", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun showLoader(){
        contentMain.visibility = View.INVISIBLE
        listener.showLoader()
    }

    private fun stopLoader(isCompletedSuccess: Boolean = true, msg: String = ""){
        contentMain.visibility = View.VISIBLE
        if(isCompletedSuccess) {
            listener.stopLoader()
        }else{
            listener.stopLoaderWithError(false, msg)
        }
    }

}