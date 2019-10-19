package com.fcbengal.android.attendance.metadata

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.adapter.GroupListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Group
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.fragment_group.*

class GroupFragment : Fragment(){

    private val mTAG = "GroupFragment"
    private lateinit var contentMain : LinearLayout
    private lateinit var groupRecyclerView : RecyclerView
    private lateinit var etGroupName : TextInputEditText
    private lateinit var etGroupDesc : TextInputEditText
    private lateinit var etCoachName : TextInputEditText
    private lateinit var etCoachContact : TextInputEditText
    private lateinit var etPOC : TextInputEditText
    private lateinit var etGroupPOCNumber : TextInputEditText
    private lateinit var buttonSubmit : Button
    private lateinit var buttonReset : Button
    private lateinit var toggleActive : ToggleButton

    lateinit var groupListRecyclerAdapter: GroupListRecyclerAdapter
    private var selectedGroupId: String? = null
    private var groupList = ArrayList<Group>()
    private var groupMap = HashMap<String, Group>()
    private lateinit var listener: GroupFragmentListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is GroupFragmentListener){
            listener = context
        }else{
            throw RuntimeException("$context must implement GroupFragmentListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_group, container, false)

        etGroupName = view.findViewById(R.id.et_group_name)
        etGroupDesc = view.findViewById(R.id.et_group_desc)
        etCoachName = view.findViewById(R.id.et_coach_name)
        etCoachContact = view.findViewById(R.id.et_coach_contact)
        etPOC = view.findViewById(R.id.et_poc)
        etGroupPOCNumber = view.findViewById(R.id.et_poc_number)
        buttonSubmit = view.findViewById(R.id.button_submit)
        contentMain = view.findViewById(R.id.fragment_group_main)
        buttonReset = view.findViewById(R.id.button_reset)
        groupRecyclerView = view.findViewById(R.id.group_recycler_view)
        toggleActive = view.findViewById(R.id.toggle_active)
        loadGroupData()
        buttonSubmit.setOnClickListener {
            submitData()
        }

        buttonReset.setOnClickListener{
            resetView()
        }

        return view
    }
    companion object{
        fun newInstance() : GroupFragment {
            var fragment : GroupFragment? = null
            if(null == fragment){
                fragment = GroupFragment()
            }
            Log.e(GroupFragment::class.java.simpleName, "Hash code ${this.hashCode()}")
            return fragment
        }
    }

    interface GroupFragmentListener{
        fun showLoader()
        fun stopLoader()
        fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String)
        fun onGroupData(groupData : Group?)
    }

    fun loadGroupData() {
        showLoader()
        DatabaseUtil.loadGroupData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val groupResponse = data as ArrayList<*>
                groupList.clear()
                groupResponse.forEach { groupEntity ->
//                    Log.e("Group Data", k as String)
                    val group = groupEntity as Group
                    groupList.add(group)
                    groupMap[group.id] = group
                }
                groupListRecyclerAdapter =
                    GroupListRecyclerAdapter(context!!,
                        groupList,
                        object :
                            GroupListRecyclerAdapter.OnGroupSelectedListener {
                            override fun onSelectedGroup(group: Group) {
                                Log.e("Group id selected", group.id)
                                selectedGroupId = group.id
                                listener.onGroupData(group)
                                populateGroupData(group)
                            }

                            override fun onLongClick(data: String) {
                                //Code to call POC
                            }

                            override fun getAllSelectedGroups(selectedGroupMap: HashMap<String, Group>) {
                                //Not required to modify here
                            }
                        })
                val mLayoutManager = LinearLayoutManager(context!!)
                groupRecyclerView.layoutManager = mLayoutManager
                groupRecyclerView.addItemDecoration(
                    DividerItemDecoration(context!!,DividerItemDecoration.VERTICAL)
                )
                groupRecyclerView.itemAnimator = DefaultItemAnimator()
                groupRecyclerView.adapter = groupListRecyclerAdapter
                stopLoader()
            }
        })
    }

    private fun populateGroupData(group: Group){
        etGroupName.setText(group.name)
        etGroupDesc.setText(group.desc)
        etCoachName.setText(group.coach1)
        etCoachContact.setText(group.coach1Contact)
        etPOC.setText(group.contactPersonName)
        etGroupPOCNumber.setText(group.contactPersonNo)
        toggleActive.isChecked = group.active
    }

    private fun resetView(){
        etGroupName.text?.clear()
        etGroupDesc.text?.clear()
        etCoachName.text?.clear()
        etCoachContact.text?.clear()
        etPOC.text?.clear()
        etGroupPOCNumber.text?.clear()
        selectedGroupId = null
        toggleActive.isChecked = false
        listener.onGroupData(null)
    }

    private fun populateGroupObject() : Group {
        val group = Group()
        if(null != selectedGroupId){
            group.id = selectedGroupId!!
        }
        group.name = etGroupName.text.toString().trim()
        group.desc = etGroupDesc.text.toString().trim()
        group.coach1 = etCoachName.text.toString().trim()
        group.coach1Contact = etCoachContact.text.toString().trim()
        group.contactPersonName = etPOC.text.toString().trim()
        group.contactPersonNo = etGroupPOCNumber.text.toString().trim()
        group.active = toggleActive.isChecked
        return group
    }

    private fun validateGroupData() : Boolean{
        var isValid = true

        TransitionManager.beginDelayedTransition(contentMain)
        if(etGroupName.text.toString().trim().isEmpty()){
            layout_group_name.isErrorEnabled = true
            layout_group_name.error = "Enter Group Name"
            isValid = false
        }else{
            layout_group_name.isErrorEnabled = false
        }
        if(etGroupDesc.text.toString().trim().isEmpty()){
            layout_group_desc.isErrorEnabled = true
            layout_group_desc.error = "Enter Group Description"
            isValid = false
        }else{
            layout_group_desc.isErrorEnabled = false
        }
        if(etCoachName.text.toString().trim().isEmpty()){
            layout_coach_name.isErrorEnabled = true
            layout_coach_name.error = "Enter Coach Name"
            isValid = false
        }else{
            layout_coach_name.isErrorEnabled = false
        }
        if(etCoachContact.text.toString().trim().isEmpty()){
            layout_coach_contact.isErrorEnabled = true
            layout_coach_contact.error = "Enter Coach Number"
            isValid = false
        }else{
            var coachNoValidated = true
            try{
                val coachContact = etCoachContact.text.toString().trim()
                coachContact.toInt()
            } catch (e : Exception){
                coachNoValidated = false
                layout_coach_contact.isErrorEnabled = true
                layout_coach_contact.error = "Enter valid number"
            }
            if(coachNoValidated){
                layout_coach_contact.isErrorEnabled = false
            }
        }
        if(etPOC.text.toString().trim().isEmpty()){
            layout_poc.isErrorEnabled = true
            layout_poc.error = "Enter POC Name"
            isValid = false
        }else{
            layout_poc.isErrorEnabled = false
        }
        if(etGroupPOCNumber.text.toString().trim().isEmpty()){
            layout_poc_number.isErrorEnabled = true
            layout_poc_number.error = "Enter POC Number"
            isValid = false
        }else{
            var pocNoValidated = true
            try{
                val pocContact = etGroupPOCNumber.text.toString().trim()
                pocContact.toInt()
            } catch (e : Exception){
                pocNoValidated = false
                layout_poc_number.isErrorEnabled = true
                layout_poc_number.error = "Enter valid number"
            }
            if(pocNoValidated){
                layout_poc_number.isErrorEnabled = false
            }
        }
        return isValid
    }

    private fun submitData(){
        if(validateGroupData()){
            showLoader()
            val entity = populateGroupObject()
            entity.selectedUI = false
            DatabaseUtil.upsertGroupData(selectedGroupId, entity, object : DatabaseUtil.OnDataUpsertListener{
                override fun onSuccess(id: String) {
                    stopLoader()
                    resetView()
                    groupList.add(entity)
                    groupListRecyclerAdapter.notifyItemInserted(groupList.size - 1)
                    Toast.makeText(context,"Operation success", Toast.LENGTH_SHORT).show()

                    selectedGroupId = id
                    entity.id = id
                    listener.onGroupData(entity)

                    resetView()
                    loadGroupData()

                }

                override fun onFailure(e: Exception) {
                    Log.e(mTAG, e.message, e)
                    stopLoader(false, "Operation failed")
                }
            })
        }
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