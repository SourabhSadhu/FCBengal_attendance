package com.fcbengal.android.attendance.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.R
import com.fcbengal.android.attendance.entity.Ground

class GroundListRecyclerAdapter(
    private val context: Context,
    private val groundList: ArrayList<Ground>,
    private val listener: OnGroundSelectedListener,
    private val multiSelect: Boolean = false,
    private val restrictInactiveData: Boolean = false
) :
    RecyclerView.Adapter<GroundListRecyclerAdapter.MyViewHolder>() {

    private val multiSelectedGroundMap = HashMap<String, Ground>()

    class MyViewHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {

        private val name: TextView = view.findViewById(R.id.name)
        private val address1: TextView = view.findViewById(R.id.address1)
        private val address2: TextView = view.findViewById(R.id.address2)
        private val desc: TextView = view.findViewById(R.id.desc)
        private val cardView: CardView = view.findViewById(R.id.card_view)

        fun setViewHolder(viewObject: Ground) {
            name.text = viewObject.name
            desc.text = viewObject.description
            address1.text = viewObject.address1
            address2.text = viewObject.address2

            if (viewObject.selectedUI) {
                cardView.background =
                    ContextCompat.getDrawable(context, R.drawable.gradient_present)
            } else {
                cardView.setBackgroundColor(Color.WHITE)
            }

            if (viewObject.active) {
                name.setTextColor(Color.BLACK)
                address1.setTextColor(Color.BLACK)
                address2.setTextColor(Color.BLACK)
                desc.setTextColor(Color.BLACK)
            } else {
                name.setTextColor(Color.GRAY)
                address1.setTextColor(Color.GRAY)
                address2.setTextColor(Color.GRAY)
                desc.setTextColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.ground_list_item, parent, false)

        return MyViewHolder(
            context,
            itemView
        )
    }

    override fun getItemCount(): Int {
        return groundList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setViewHolder(groundList[position])
        holder.itemView.setOnClickListener {
            val viewObject = groundList[position]
            if (!restrictInactiveData || (restrictInactiveData && viewObject.active)) {
                changeSelectedItemBackground(position)
                listener.onSelectedGround(viewObject)
                if (multiSelect) {
                    var isAlreadyAdded = false
                    multiSelectedGroundMap.forEach { (K, _) ->
                        if (K == viewObject.id) {
                            isAlreadyAdded = true
                        }
                    }
                    if (isAlreadyAdded) {
                        Log.e("Removing", viewObject.id)
                        multiSelectedGroundMap.remove(viewObject.id)
                    } else {
                        Log.e("Adding", viewObject.id)
                        multiSelectedGroundMap[viewObject.id] = viewObject
                    }
                }
                listener.getAllSelectedGrounds(multiSelectedGroundMap)
            } else {
                Toast.makeText(context, context.getString(R.string.text_disable_data), LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun changeSelectedItemBackground(position: Int) {
        if (multiSelect) {
            groundList[position].selectedUI = !groundList[position].selectedUI
        } else {
            groundList.forEach {
                it.selectedUI = false
            }
            groundList[position].selectedUI = true
        }
        notifyDataSetChanged()
    }

    interface OnGroundSelectedListener {
        fun onSelectedGround(ground: Ground)
        fun getAllSelectedGrounds(groundMap: HashMap<String, Ground>)
    }
}