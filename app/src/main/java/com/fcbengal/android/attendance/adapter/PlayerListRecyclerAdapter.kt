package com.fcbengal.android.attendance.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fcbengal.android.attendance.entity.Player
import com.fcbengal.android.attendance.R

class PlayerListRecyclerAdapter(
    private val context: Context,
    private val playerList: ArrayList<Player>,
    private val listener: OnPlayerSelectedListener
) :
    RecyclerView.Adapter<PlayerListRecyclerAdapter.MyViewHolder>() {

    private val TAG = "PlayerListRecyclrAdaptr"

    class MyViewHolder(private val context: Context, view: View) : RecyclerView.ViewHolder(view) {

        private val fName: TextView = view.findViewById(R.id.f_name)
        private val lName: TextView = view.findViewById(R.id.l_name)
        private val doj: TextView = view.findViewById(R.id.doj)
        private val cardView : CardView = view.findViewById(R.id.card_view)

        fun setViewHolder(viewObject : Player) {
            fName.text = viewObject.fName
            lName.text = viewObject.lName
            doj.text = viewObject.doj
            if(viewObject.selectedUI){
                cardView.background = ContextCompat.getDrawable(context, R.drawable.gradient_present)
            }else{
                cardView.setBackgroundColor(Color.WHITE)
            }
            if(viewObject.active){
                fName.setTextColor(Color.BLACK)
                lName.setTextColor(Color.BLACK)
                doj.setTextColor(Color.BLACK)
            }else{
                fName.setTextColor(Color.GRAY)
                lName.setTextColor(Color.GRAY)
                doj.setTextColor(Color.GRAY)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.player_list_item, parent, false)

        return MyViewHolder(
            context,
            itemView
        )
    }

    override fun getItemCount(): Int {
        return playerList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val viewObject = playerList[position]
        holder.setViewHolder(viewObject)
        holder.itemView.setOnClickListener {
            Log.e(TAG, "Clicked on name ${viewObject.fName}, id ${viewObject.id}")
            changeSelectedItemBackground(position)
            listener.onSelectedPlayer(viewObject)
        }
        holder.itemView.setOnLongClickListener {
            Log.e(TAG, "Long Click id ${viewObject.id}")
            listener.onLongClick(playerList[position].id)
            true
        }
    }

    private fun changeSelectedItemBackground(position: Int){
        playerList.forEach {
            it.selectedUI = false
        }
        playerList[position].selectedUI = true
        notifyDataSetChanged()
    }

    interface OnPlayerSelectedListener {
        fun onSelectedPlayer(player: Player)
        fun onLongClick(data : String)
    }
}