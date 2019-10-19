package com.fcbengal.android.attendance.utils

import android.os.Handler
import android.util.Log
import com.fcbengal.android.attendance.entity.*
import com.fcbengal.android.attendance.view.AttendanceView
import com.fcbengal.android.attendance.view.GroupAttendanceView
import com.google.firebase.database.*


object DatabaseUtil {
    private var databaseReference: DatabaseReference
    private const val constantConfig = "Config"
    private const val constantGroups = "Groups"
    private const val constantTimeSchedule = "TimeSchedule"
    private const val constantGroupTimeSchedule = "GroupTimeSchedule"
    private const val constantGround = "Ground"
    private const val constantPlayer = "Player"
    const val constantKeySeparator = "="
    const val constantTimeSeparator = ":"
    const val constantChildSeparator = "/"
    const val constantDateUISeparator = "/"
    private const val constantAttendance = "Attendance"
    private var TAG: String = this::class.java.simpleName

    init {
        FirebaseDatabase.getInstance().setPersistenceEnabled(false)
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    interface OnDataCompletedListener {
        fun onCancelled(error: DatabaseError)
        fun onDataChange(data: Any)
    }

    interface OnDatabaseConnectivityListener {
        fun onStatusUpdated(key: String, value: String)
    }

    fun loadConfig(gmail: String, listener: OnDataCompletedListener) {
        val eventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                gotResult = true
                listener.onCancelled(p0)
            }

            override fun onDataChange(p0: DataSnapshot) {
                gotResult = true
                Log.e(TAG, p0.childrenCount.toString())
                try {
                    val config: Config? = p0.getValue(Config::class.java)
                    if (null != config) {
                        Log.e(TAG, "Value $config")
                        listener.onDataChange(config)
                    } else {
                        Log.e(TAG, "WTF")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception", e)
                }
            }
        }
        databaseReference.child(constantConfig).child(
            convertGmailToKey(gmail)
        ).addListenerForSingleValueEvent(eventListener)
        timeOutListener(eventListener, listener)
    }

    fun checkDBStatus(listener: OnDatabaseConnectivityListener) {
        val connectedRef = FirebaseDatabase.getInstance().getReference(".info")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    //                    Log.e(TAG, it.key)
//                    Log.e(TAG, "Value ${it.value}")
                    if (it.key == "connected") {
                        if (it.value == false) {
                            listener.onStatusUpdated("connected", "Viewing offline Data")
                        } else {
                            listener.onStatusUpdated("connected", "Back online")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listener was cancelled")
            }
        })
    }

    fun loadGroupData(listener: OnDataCompletedListener) {
        databaseReference.child(constantGroups).orderByChild("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onCancelled(p0)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val groupList = ArrayList<Group>()
                    val groupListResponse = p0.children
                    groupListResponse.forEach { dataSnapShot ->
                        val groupData = dataSnapShot.getValue(Group::class.java)
                        groupData?.let {group ->
                            groupList.add(group)
                        }
                    }
                    listener.onDataChange(groupList)
                }
            })
    }

    fun loadTimeScheduleData(listener: OnDataCompletedListener) {
        databaseReference.child(constantTimeSchedule)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onCancelled(p0)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val timeScheduleList = p0.children
                    val timeScheduleMap = HashMap<String, TimeSchedule>()
                    timeScheduleList.forEach {
                        val timeScheduleData = it.getValue(TimeSchedule::class.java)
                        timeScheduleData!!.let {
                            timeScheduleMap[timeScheduleData.id] = timeScheduleData
                        }
                    }
                    Log.e(TAG, "Time schedule size ${timeScheduleMap.size}")
                    listener.onDataChange(timeScheduleMap)
                }
            })
    }

    fun loadGroupTimeScheduleData(listener: OnDataCompletedListener) {
        databaseReference.child(constantGroupTimeSchedule)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    listener.onCancelled(p0)
                }

                override fun onDataChange(p0: DataSnapshot) {
                    val groupTimeScheduleList = p0.children
                    val groupTimeScheduleMap = HashMap<String, GroupTimeSchedule>()
                    groupTimeScheduleList.forEach {
                        val groupTimeScheduleData = it.getValue(GroupTimeSchedule::class.java)
                        groupTimeScheduleData!!.let {
                            groupTimeScheduleMap[groupTimeScheduleData.id] = groupTimeScheduleData
                        }
                    }
                    listener.onDataChange(groupTimeScheduleMap)
                }
            })
    }

    fun loadGroundData(listener: OnDataCompletedListener){
        val valueEventListener = object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                gotResult = true
                listener.onCancelled(p0)
            }

            override fun onDataChange(p0: DataSnapshot) {
                gotResult = true
                val responseMap = HashMap<String, Ground>()
                p0.children.forEach {
                    val response = it.getValue(Ground::class.java)
                    if(null != response){
                        responseMap[response.id] = response
                    }
                }
                listener.onDataChange(responseMap)
            }
        }
        databaseReference.child(constantGround).addListenerForSingleValueEvent(valueEventListener)
        timeOutListener(
            valueEventListener,
            listener
        )
    }

    fun loadAllAttendanceData(
        month_year: String,
        fromDate: Int,
        toDate: Int,
        groupTimeScheduleIdList: ArrayList<String>,
        groundName : String,
        listener: OnDataCompletedListener
    ) {
        val groupAttendanceResponseData = GroupAttendanceView()

        loadGroupData(object : OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                listener.onCancelled(error)
            }

            override fun onDataChange(data: Any) {
                val groupResponseMap = data as ArrayList<*>
                val groupResponse = HashMap<String, Group>()
                groupResponseMap.forEach {response ->
                    val group = response as Group
                    groupResponse[group.id] = group
                }
                groupAttendanceResponseData.groupMap = groupResponse

                databaseReference.child(constantAttendance).child(month_year).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        listener.onCancelled(p0)
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        Log.e("Month Year Data", p0.key)
                        try {
                            if (p0.hasChildren()) {
                                val groupTimeSchedule = p0.children
                                Log.e("date_month id", p0.ref.key)
                                groupTimeSchedule.forEach { groupTimeScheduleData ->

                                    Log.e("GroupTimeSchedule 2nd", groupTimeScheduleData.key)
                                    if (groupTimeScheduleData.hasChildren() ||
                                        (groupTimeScheduleIdList.isNotEmpty() && groupTimeScheduleIdList.contains(
                                            groupTimeScheduleData.key
                                        ))
                                    ) {
                                        val idKeys = groupTimeScheduleData.key?.split(constantKeySeparator)
                                        if(null != idKeys){
                                            val groupId = idKeys[0]
                                            if(groupId.isNotEmpty()){
                                                val groupId = groupResponse[groupId]!!.id
                                                if(!groupAttendanceResponseData.groupIdList.contains(groupId)) {
                                                    groupAttendanceResponseData.groupIdList.add(groupId)
                                                }
                                                if(groupAttendanceResponseData.groupAttendanceMap[groupId] == null){
                                                    groupAttendanceResponseData.groupAttendanceMap[groupId] = AttendanceView()
                                                }
                                                val responseData = groupAttendanceResponseData.groupAttendanceMap[groupId]
                                                val attendanceDate = groupTimeScheduleData.children
                                                attendanceDate.forEach { attendanceDateData ->

                                                    Log.e("Date 3rd", attendanceDateData.key)
                                                    attendanceDateData.key?.let {
                                                        var date = 0
                                                        try {
                                                            date = attendanceDateData.key.toString().toInt()
                                                        } catch (e: Exception) {
                                                            Log.e(TAG, "Unable to parse date ${e.message}", e)
                                                        }
                                                        if (attendanceDateData.hasChildren() && date >= fromDate && date <= toDate) {

                                                            responseData!!.sortedDateList.add(date)
                                                            val playerAttendance = attendanceDateData.children
                                                            playerAttendance.forEach { playerAttendanceData ->
                                                                Log.e("Player 4th", playerAttendanceData.key)
                                                                Log.e("Player 4th data", "Val ${playerAttendanceData.value}")
                                                                val playerAttendance =
                                                                    playerAttendanceData.getValue(
                                                                        PlayerAttendance::class.java
                                                                    )
                                                                playerAttendance?.let {
                                                                    if (responseData.datePlayerAttendanceMap[date] == null ||
                                                                        responseData.datePlayerAttendanceMap[date].isNullOrEmpty()) {
                                                                        responseData.datePlayerAttendanceMap[date] = ArrayList()
                                                                    }
                                                                    //Ground specific attendance
                                                                    if(groundName.isNotEmpty() && playerAttendance.groundName == groundName){
                                                                        responseData.datePlayerAttendanceMap[date]?.add(playerAttendance)
                                                                    } else {
                                                                        //TODO:Future scope to populate all ground data [Note: same player can play on different ground on different day]
                                                                        responseData.datePlayerAttendanceMap[date]?.add(playerAttendance)
                                                                    }
                                                                }
                                                            }
                                                        } else {
                                                            Log.e(TAG, "No data found for date $date, Error data -> ${attendanceDateData.toString()}")
                                                        }
                                                    }
                                                }
                                                responseData!!.sortedDateList.sort()
                                            } else {
                                                Log.e(TAG, "Invalid group time schedule ${groupTimeScheduleData}")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "No data under group or invalid group data -> $groupTimeScheduleData")
                                        listener.onCancelled(DatabaseError.fromCode(DatabaseError.OPERATION_FAILED))
                                    }
                                    listener.onDataChange(groupAttendanceResponseData)
                                }
                            } else {
                                Log.e("P0", p0.toString())
                                listener.onCancelled(DatabaseError.fromCode(DatabaseError.NETWORK_ERROR))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, e.toString(), e)
                            listener.onCancelled(DatabaseError.fromCode(DatabaseError.OPERATION_FAILED))
                        }
                    }
                })
            }
        })



    }

    fun loadAttendanceDataByGroupTimeScheduleData(
        month_year: String,
        date: String,
        groupTimeScheduleId: String,
        listener: OnDataCompletedListener
    ){
        databaseReference.child(constantAttendance).child(month_year).child(groupTimeScheduleId).child(date)
            .addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
                listener.onCancelled(p0)
            }

            override fun onDataChange(p0: DataSnapshot) {
                val playerIdAttendanceMapResponse = HashMap<String, PlayerAttendance>()
                p0.children.forEach { playerAttendance ->
                    val playerAttendanceEntity = playerAttendance.getValue(PlayerAttendance::class.java)
                    playerAttendanceEntity?.let {
                        playerIdAttendanceMapResponse[it.playerId] = it
                    }
                }
                listener.onDataChange(playerIdAttendanceMapResponse)
            }
        })
    }

    fun loadPlayerData(groupId: String, listener: OnDataCompletedListener) {
        val eventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                gotResult = true
                listener.onCancelled(p0)
            }

            override fun onDataChange(p0: DataSnapshot) {
                gotResult = true
                val playerListResponse = p0.children
                val playerList = ArrayList<Player>()
                /*val playerIdMap = HashMap<String, Player>()
                playerList.forEach { playerListData ->
                    val player = playerListData.getValue(Player::class.java)
                    player?.let {
                        playerIdMap[playerListData.key.toString()] = player
                    }
                }*/
                playerListResponse.forEach {
                    val player = it.getValue(Player::class.java)
                    player?.let {
                        playerList.add(it)
                    }
                }
                listener.onDataChange(playerList)
            }
        }
        databaseReference.child(constantPlayer).child(groupId).orderByChild("fname").addListenerForSingleValueEvent(eventListener)
        timeOutListener(eventListener, listener)
    }

    fun loadGroupPlayerData(listener: OnDataCompletedListener){
        val eventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                gotResult = true
                listener.onCancelled(p0)
            }

            override fun onDataChange(p0: DataSnapshot) {
                gotResult = true
                val groupPlayerResponseMap = HashMap<String, ArrayList<Player>>()
                //Loading groupId
                if(p0.hasChildren()){
                    val childGroups = p0.children
                    childGroups.forEach {childGroup ->
                        Log.e(TAG, "Child Group id ${childGroup.key}")
                        val groupId = childGroup.key
                        groupId?.let {
                            if (childGroup.hasChildren()){
                                val childPlayers = childGroup.children
                                val playerList = ArrayList<Player>()
                                childPlayers.forEach { childPlayer ->
                                    val player = childPlayer.getValue(Player::class.java)
                                    if(null != player) {
                                        playerList.add(player)
                                    }
                                }
                                if (playerList.isNotEmpty()){
                                    groupPlayerResponseMap[it] = playerList
                                }
                            }
                        }
                    }
                }
                listener.onDataChange(groupPlayerResponseMap)
            }
        }
        databaseReference.child(constantPlayer).addListenerForSingleValueEvent(eventListener)
        timeOutListener(eventListener, listener)
    }

    var gotResult = false
    private fun timeOutListener(valueListener: ValueEventListener, callbackListener : OnDataCompletedListener? = null, callbackListener2 : OnDataUpsertListener? = null) {
        gotResult = false
        val handler = Handler()
        handler.postDelayed({
            if (!gotResult) {
                databaseReference.removeEventListener(valueListener)
                callbackListener?.onCancelled(DatabaseError.fromCode(DatabaseError.DISCONNECTED))
                callbackListener2?.onFailure(RuntimeException("The operation had to be aborted due to a network disconnect"))
            }
        }, 20000)
    }

    private fun convertGmailToKey(gmail: String): String {
        val gmailR = gmail.replace("@",
            constantKeySeparator
        )
        return gmailR.replace(".",
            constantKeySeparator
        )
    }

    interface OnDataUpsertListener {
        fun onSuccess(id: String)
        fun onFailure(e: Exception)
    }

    fun upsertGroupData(groupId: String?, entity: Group, listener: OnDataUpsertListener) {
        val dbUrl: String
        if (null == groupId) {
            dbUrl = StringBuilder().append(constantGroups).toString()
            val generatedGroupId = databaseReference.child(dbUrl).push().key
            if (null != generatedGroupId) {
                entity.id = generatedGroupId
                databaseReference.child(dbUrl).child(generatedGroupId).setValue(entity)
                    .addOnSuccessListener {
                        listener.onSuccess(generatedGroupId)
                    }.addOnFailureListener {
                    listener.onFailure(it)
                }
            } else {
                listener.onFailure(RuntimeException("Failed to push key"))
            }
        } else {
            dbUrl = StringBuilder().append(constantGroups).append("/").append(groupId).toString()
            databaseReference.child(dbUrl).setValue(entity).addOnSuccessListener {
                listener.onSuccess(groupId)
            }.addOnFailureListener {
                listener.onFailure(it)
            }
        }

    }

    fun upsertTimeSchedule(entity: TimeSchedule, listener: OnDataUpsertListener) {
        val dbUrlBuilder = StringBuilder().append(constantTimeSchedule)
        if (entity.id.isNotEmpty()) {
            dbUrlBuilder.append("/").append(entity.id)
            databaseReference.child(dbUrlBuilder.toString()).setValue(entity)
                .addOnCompleteListener {
                    listener.onSuccess(entity.id)
                }.addOnFailureListener { listener.onFailure(it) }
        } else {
            listener.onFailure(RuntimeException("Invalid time schedule ID"))
        }
    }

    fun upsertGroupTimeSchedule(entity: GroupTimeSchedule, listener: OnDataUpsertListener) {
        val dbUrlBuilder = StringBuilder().append(constantGroupTimeSchedule)
        dbUrlBuilder.append("/").append(entity.id)
        databaseReference.child(dbUrlBuilder.toString()).setValue(entity).addOnCompleteListener {
            databaseReference.child(constantTimeSchedule).child(entity.timeScheduleId)
                .child("groupId").setValue(entity.groupId)
                .addOnCompleteListener {
                    listener.onSuccess(entity.id)
                }.addOnFailureListener { listener.onFailure(it) }
        }.addOnFailureListener { listener.onFailure(it) }
    }

    fun upsertGroundData(entity : Ground, listener: OnDataUpsertListener){
        if(entity.id.isEmpty()){
            val pushedId = databaseReference.child(
                constantGround
            ).push().key
            if(null != pushedId){
                entity.id = pushedId
            }else{
                listener.onFailure(RuntimeException("Invalid Ground id"))
            }
        }
        databaseReference.child(constantGround).child(entity.id).setValue(entity).addOnCompleteListener {
            listener.onSuccess(entity.id)
        }.addOnFailureListener { listener.onFailure(it) }
    }

    fun upsertPlayer(entity: Player, listener: OnDataUpsertListener) {
        if (entity.groupId.isEmpty()) {
            listener.onFailure(RuntimeException("Invalid Group ID -> ${entity.groupId}"))
        }
        if (entity.id.isEmpty()) {
            var dbUrl = StringBuilder().append(constantPlayer).append("/").append(entity.groupId)
            var key = databaseReference.child(dbUrl.toString()).push().key
            entity.createddt = ConverterUtils.getStringDate()
            entity.modifieddt = ConverterUtils.getStringDate()
            if (null != key) {
                entity.id = key
            } else {
                listener.onFailure(RuntimeException("Unable to push key"))
            }
            var allowedTimeSchedules = ""
            loadGroupTimeScheduleData(object :
                OnDataCompletedListener {
                override fun onCancelled(error: DatabaseError) {
                    listener.onFailure(RuntimeException("Unable to load group time schedule"))
                }

                override fun onDataChange(data: Any) {
                    val timeScheduleMap =
                        data as HashMap<String, GroupTimeSchedule>
                    timeScheduleMap.forEach { (_, V) ->
                        if (V.groupId == entity.groupId && V.active) {
                            if (allowedTimeSchedules.isNotEmpty()) {
                                allowedTimeSchedules += ","
                            }
                            allowedTimeSchedules += V.timeScheduleId
                        }
                    }
                    entity.groupTimeScheduleId = allowedTimeSchedules
                    databaseReference.child(dbUrl.toString())
                        .child(key!!).setValue(entity)
                        .addOnCompleteListener {
                            listener.onSuccess(key)
                        }.addOnFailureListener { listener.onFailure(it) }
                }
            })
        } else {
            entity.modifieddt = ConverterUtils.getStringDate()
            databaseReference.child(constantPlayer).child(entity.groupId).child(entity.id)
                .setValue(entity)
                .addOnCompleteListener { listener.onSuccess(entity.id) }
                .addOnFailureListener { listener.onFailure(it) }
        }
    }
}