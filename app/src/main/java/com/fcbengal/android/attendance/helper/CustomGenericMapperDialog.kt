package com.fcbengal.android.attendance.helper

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.R
import kotlinx.android.synthetic.main.dialog_generic_mapper.*

class CustomGenericMapperDialog(
    private val dialogContext: Context,
    private val adapter: RecyclerView.Adapter<*>,
    private val listener : OnClickListener
)
    : Dialog(dialogContext), View.OnClickListener {

    private var recyclerView: RecyclerView? = null
    private var mLayoutManager: RecyclerView.LayoutManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_generic_mapper)

        recyclerView = recycler_view as RecyclerView
        mLayoutManager = LinearLayoutManager(dialogContext)
        recyclerView?.layoutManager = mLayoutManager
        recyclerView?.adapter = adapter

        yes.setOnClickListener(this)
        no.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.yes -> {
                dismiss()
                listener.onClick(true)
            }
            R.id.no -> {
                dismiss()
                listener.onClick(false)
            }
        }
    }

    interface OnClickListener{
        fun onClick(value : Boolean)
    }
}