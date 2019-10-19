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
import com.fcbengal.android.attendance.adapter.GroundListRecyclerAdapter
import com.fcbengal.android.attendance.entity.Ground
import com.fcbengal.android.attendance.utils.ConverterUtils
import com.fcbengal.android.attendance.utils.DatabaseUtil
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.fragment_ground.*

class GroundFragment : Fragment(){

    private val mTAG = "GroundFragment"
    private lateinit var contentMain : LinearLayout
    private lateinit var groundRecyclerView : RecyclerView
    private lateinit var etGroundName : TextInputEditText
    private lateinit var etGroundDesc : TextInputEditText
    private lateinit var etAddress1 : TextInputEditText
    private lateinit var etAddress2 : TextInputEditText
    private lateinit var buttonSubmit : Button
    private lateinit var buttonReset : Button
    private lateinit var toggleActive : ToggleButton

    private var groundList = ArrayList<Ground>()
    private var groundMap = HashMap<String, Ground>()
    private var selectedGroundId = ""
    private lateinit var groundListRecyclerAdapter: GroundListRecyclerAdapter
    private lateinit var listener: GroundFragmentListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is GroundFragmentListener){
            listener = context
        }else{
            throw RuntimeException("$context must implement GroupFragmentListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_ground, container, false)

        contentMain = view.findViewById(R.id.fragment_ground_main)
        groundRecyclerView = view.findViewById(R.id.ground_recycler_view)
        etGroundName = view.findViewById(R.id.et_ground_name)
        etAddress1 = view.findViewById(R.id.et_ground_address1)
        etAddress2 = view.findViewById(R.id.et_ground_address2)
        etGroundDesc = view.findViewById(R.id.et_ground_description)
        toggleActive = view.findViewById(R.id.toggle_active)
        buttonSubmit = view.findViewById(R.id.button_submit)
        buttonReset = view.findViewById(R.id.button_reset)
        loadGroundData()
        buttonSubmit.setOnClickListener {
            submitData()
        }

        buttonReset.setOnClickListener{
            resetView()
        }

        return view
    }
    companion object{
        fun newInstance() : GroundFragment {
            var fragment : GroundFragment? = null
            if(null == fragment){
                fragment = GroundFragment()
            }
            Log.e(GroundFragment::class.java.simpleName, "Hash code ${this.hashCode()}")
            return fragment
        }
    }

    interface GroundFragmentListener{
        fun showLoader()
        fun stopLoader()
        fun stopLoaderWithError(isCompletedSuccess: Boolean, msg: String)
    }

    fun loadGroundData() {
        showLoader()
        DatabaseUtil.loadGroundData(object : DatabaseUtil.OnDataCompletedListener {
            override fun onCancelled(error: DatabaseError) {
                stopLoader(false, error.message)
            }

            override fun onDataChange(data: Any) {
                val groundMapResponse = data as HashMap<*, *>
                groundList = ArrayList()
                groundMapResponse.forEach { (k, v) ->
                    groundList.add(v as Ground)
                    groundMap[k as String] = v
                }
                groundListRecyclerAdapter =
                    GroundListRecyclerAdapter(context!!,
                        groundList,
                        object :
                            GroundListRecyclerAdapter.OnGroundSelectedListener {
                            override fun onSelectedGround(ground: Ground) {
                                selectedGroundId = ground.id
                                populateGroundData(ground)
                            }

                            override fun getAllSelectedGrounds(groundMap: HashMap<String, Ground>) {
                                //Not required to modify here
                            }
                        })
                val mLayoutManager = LinearLayoutManager(context!!)
                groundRecyclerView.layoutManager = mLayoutManager
                groundRecyclerView.addItemDecoration(
                    DividerItemDecoration(context!!,DividerItemDecoration.VERTICAL)
                )
                groundRecyclerView.itemAnimator = DefaultItemAnimator()
                groundRecyclerView.adapter = groundListRecyclerAdapter
                stopLoader()
            }
        })
    }

    private fun populateGroundData(ground: Ground){
        etGroundName.setText(ground.name)
        etAddress1.setText(ground.address1)
        etAddress2.setText(ground.address2)
        etGroundDesc.setText(ground.description)
        toggleActive.isChecked = ground.active
    }

    private fun resetView(){
        etGroundName.text?.clear()
        etAddress1.text?.clear()
        etAddress2.text?.clear()
        etGroundDesc.text?.clear()
        toggleActive.isChecked = false
    }

    private fun populateGroundObject() : Ground {
        val ground = Ground()
        if(selectedGroundId.isNotEmpty()){
            ground.id = selectedGroundId
        }
        ground.name = etGroundName.text.toString().trim()
        ground.description = etGroundDesc.text.toString().trim()
        ground.address1 = etAddress1.text.toString().trim()
        ground.address2 = etAddress2.text.toString().trim()
        ground.active = toggleActive.isChecked
        if(ground.createdDate.isEmpty()) {
            ground.createdDate = ConverterUtils.getStringDate()
        }
        ground.modifiedDate = ConverterUtils.getStringDate()
        return ground
    }

    private fun validateGroundData() : Boolean{
        var isValid = true

        TransitionManager.beginDelayedTransition(contentMain)
        if(etGroundName.text.toString().trim().isEmpty()){
            layout_ground_name.isErrorEnabled = true
            layout_ground_name.error = "Enter Ground Name"
            isValid = false
        }else{
            layout_ground_name.isErrorEnabled = false
        }
        if(etAddress1.text.toString().trim().isEmpty()){
            layout_ground_address1.isErrorEnabled = true
            layout_ground_address1.error = "Enter Ground Address"
            isValid = false
        }else{
            layout_ground_address1.isErrorEnabled = false
        }
        return isValid
    }

    private fun submitData(){
        if(validateGroundData()){
            showLoader()
            val entity = populateGroundObject()
            entity.selectedUI = false
            DatabaseUtil.upsertGroundData(entity, object : DatabaseUtil.OnDataUpsertListener{
                override fun onSuccess(id: String) {
                    stopLoader()
                    resetView()
                    if(groundList.isEmpty()){
                        groundList = ArrayList()
                    }
                    if(entity.id.isEmpty()) {
                        entity.id = id
                        groundList.add(entity)
                    }
                    groundListRecyclerAdapter.notifyDataSetChanged()
                    Toast.makeText(context,"Operation success", Toast.LENGTH_SHORT).show()
                    resetView()
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