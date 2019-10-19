package com.fcbengal.android.attendance.metadata

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.adapter.PlayerListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.fcbengal.android.attendance.utils.DatabaseUtil.constantChildSeparator
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.fragment_player.*
import java.util.*
import kotlin.collections.ArrayList

class PlayerFragment : Fragment() {
    private val mTAG = "PlayerFragment"

    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var fragmentPlayerMain : LinearLayout

    private var selectedGroup: Group? = null
    private var selectedPlayer : Player? = null
    private lateinit var playerList: ArrayList<Player>
    private lateinit var listener: PlayerFragmentListener
    private lateinit var playerListRecyclerAdapter: PlayerListRecyclerAdapter

    private lateinit var fName : TextInputEditText
    private lateinit var lName : TextInputEditText
    private lateinit var dob : TextInputEditText
    private lateinit var doj : TextInputEditText
    private lateinit var contact : TextInputEditText
    private lateinit var buttonDOB : ImageButton
    private lateinit var buttonDOJ : ImageButton
    private lateinit var buttonSubmit : Button
    private lateinit var buttonReset : Button
    private lateinit var toggleActive: ToggleButton


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_player, container, false)
        fragmentPlayerMain = view.findViewById(R.id.fragment_player_main)
        playerRecyclerView = view.findViewById(R.id.player_recycler_view)
        fName = view.findViewById(R.id.et_first_name)
        lName = view.findViewById(R.id.et_last_name)
        dob = view.findViewById(R.id.et_dob)
        doj = view.findViewById(R.id.et_doj)
        contact = view.findViewById(R.id.et_contact_no)
        buttonDOB = view.findViewById(R.id.button_dob)
        buttonDOJ = view.findViewById(R.id.button_doj)
        buttonSubmit = view.findViewById(R.id.button_submit)
        buttonReset = view.findViewById(R.id.button_reset)
        toggleActive = view.findViewById(R.id.toggle_active)
        doj.isEnabled = false
        dob.isEnabled = false

        val calendar = Calendar.getInstance()
        val mYear = calendar.get(Calendar.YEAR)
        val mMonth = calendar.get(Calendar.MONTH)
        val mDay = calendar.get(Calendar.DAY_OF_MONTH)

        buttonDOB.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    dob.setText(StringBuilder()
                        .append(dayOfMonth)
                        .append(constantChildSeparator)
                        .append(monthOfYear + 1)
                        .append(constantChildSeparator)
                        .append(year).toString())
                }, mYear, mMonth, mDay
            )
            datePickerDialog.setTitle("Select DOB")
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
            datePickerDialog.show()
        }

        buttonDOJ.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context!!,
                DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                    doj.setText(StringBuilder()
                        .append(dayOfMonth)
                        .append(constantChildSeparator)
                        .append(monthOfYear + 1)
                        .append(constantChildSeparator)
                        .append(year).toString())
                }, mYear, mMonth, mDay
            )
            datePickerDialog.setTitle("Select DOB")
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis
            datePickerDialog.show()
        }

        buttonSubmit.setOnClickListener {
            submitData()
        }

        buttonReset.setOnClickListener {
            resetUI()
        }

        return view
    }

    companion object {
        fun newInstance() : PlayerFragment {
            var fragment : PlayerFragment? = null
            if(null == fragment){
                fragment = PlayerFragment()
            }
            Log.e(PlayerFragment::class.java.simpleName, "Hash code ${this.hashCode()}")
            return fragment
        }
    }

    private fun resetUI(){
        fName.setText("")
        lName.setText("")
        dob.setText("")
        doj.setText("")
        contact.setText("")
        toggleActive.isChecked = false
        selectedPlayer = null
    }

    private fun validateInputs() : Boolean {
        var isValid = true
        if(fName.text.toString().isEmpty()){
            layout_first_name.isErrorEnabled = true
            layout_first_name.error = "Enter first name"
            isValid = false
        }else{
            layout_first_name.isErrorEnabled = true
        }
        if(lName.text.toString().isEmpty()){
            layout_last_name.isErrorEnabled = true
            layout_last_name.error = "Enter last name"
            isValid = false
        }else{
            layout_last_name.isErrorEnabled = false
        }
        if(contact.text.toString().isEmpty()){
            layout_contact_no.isErrorEnabled = true
            layout_contact_no.error = "Enter contact no"
            isValid = false
        }else{
            layout_contact_no.isErrorEnabled = true
        }
        if(dob.text.toString().isEmpty()){
            layout_dob.isErrorEnabled = true
            layout_dob.error = "Select DOB"
            isValid = false
        }else{
            layout_dob.isErrorEnabled = false
        }
        if(doj.text.toString().isEmpty()){
            layout_doj.isErrorEnabled = true
            layout_doj.error = "Select DOJ"
        }else{
            layout_doj.isErrorEnabled = false
        }
        return isValid
    }

    private fun submitData(){
        if(validateInputs() && null != selectedGroup){
            showLoader()
            if(null == selectedPlayer){
                selectedPlayer = Player()
            }
            selectedPlayer!!.fName = fName.text.toString().trim()
            selectedPlayer!!.lName = lName.text.toString().trim()
            selectedPlayer!!.dob = dob.text.toString()
            selectedPlayer!!.doj = doj.text.toString()
            selectedPlayer!!.contactNo = contact.text.toString()
            selectedPlayer!!.groupId = selectedGroup!!.id
            selectedPlayer!!.active = toggleActive.isChecked
            selectedPlayer!!.selectedUI = false
            DatabaseUtil.upsertPlayer(selectedPlayer!!, object : DatabaseUtil.OnDataUpsertListener{
                override fun onSuccess(id: String) {
                    stopLoader()
                    Toast.makeText(context!!, "Success", LENGTH_SHORT).show()
                    resetUI()
                    loadPlayerData(selectedGroup!!.id)
                }

                override fun onFailure(e: Exception) {
                    Log.e(mTAG, e.message, e)
                    stopLoader(false, e.message!!)
                }
            })
        }else if(null == selectedGroup){
            Toast.makeText(context!!,"Please select a group", LENGTH_SHORT).show()
        }
    }

    interface PlayerFragmentListener {
        fun showLoader()
        fun stopLoader()
        fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String)
        fun onPlayerFragmentInstance()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.e(mTAG, " ===================== onAttachCalled ===================== ")
        if (context is PlayerFragmentListener) {
            listener = context
            listener.onPlayerFragmentInstance()
        } else {
            throw RuntimeException("$context must implement PlayerFragment")
        }
    }

    fun setSelectedGroup(group : Group){
        Log.e(mTAG, "selected groupId ${group.id}")
        selectedGroup = group
        loadPlayerData(group.id)
    }

    private fun loadPlayerData(groupId : String){
        showLoader()
        DatabaseUtil.loadPlayerData(groupId,object : DatabaseUtil.OnDataCompletedListener{
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                stopLoader()
//                val playerMap = data as HashMap<*, *>
                val response = data as ArrayList<*>
                playerList = ArrayList()
                response.forEach {
                    playerList.add(it as Player)
                }
//                playerMap.forEach { (_, V) ->
//                    playerList.add(V as Player)
//                }
                playerListRecyclerAdapter =
                    PlayerListRecyclerAdapter(
                        context!!,
                        playerList,
                        object :
                            PlayerListRecyclerAdapter.OnPlayerSelectedListener {
                            override fun onSelectedPlayer(player: Player) {
                                selectedPlayer = player
                                populatePlayerData(player)
                            }

                            override fun onLongClick(data: String) {

                            }
                        })
                playerRecyclerView.layoutManager = LinearLayoutManager(context!!)
                playerRecyclerView.addItemDecoration(DividerItemDecoration(context!!, DividerItemDecoration.VERTICAL))
                playerRecyclerView.itemAnimator = DefaultItemAnimator()
                playerRecyclerView.adapter = playerListRecyclerAdapter

            }
        })
    }

    private fun populatePlayerData(player: Player){
        fName.setText(player.fName)
        lName.setText(player.lName)
        dob.setText(player.dob)
        doj.setText(player.doj)
        contact.setText(player.contactNo)
        toggleActive.isChecked = player.active
    }

    private fun showLoader(){
        fragmentPlayerMain.visibility = View.INVISIBLE
        listener.showLoader()
    }

    private fun stopLoader(isCompletedSuccess: Boolean = true, msg: String = ""){
        fragmentPlayerMain.visibility = View.VISIBLE
        if(isCompletedSuccess) {
            listener.stopLoader()
        }else{
            listener.stopLoaderWithError(false, msg)
        }
    }
}