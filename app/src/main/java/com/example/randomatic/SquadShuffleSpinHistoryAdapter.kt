package com.example.randomatic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.randomatic.models.SpinHistory

class SquadShuffleSpinHistoryAdapter(
    private val spinHistoryList: MutableList<SpinHistory>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SquadShuffleSpinHistoryAdapter.ViewHolder>() {

    // ViewHolder to hold item views
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shuffleTitle: TextView = itemView.findViewById(R.id.shuffleTitle)
        val shuffleDate: TextView = itemView.findViewById(R.id.tv_spin_category)

        fun bind(spinHistory: SpinHistory) {
            shuffleTitle.text = spinHistory.title
            shuffleDate.text = spinHistory.date // Ensure `SpinHistory` has a `date` field
            itemView.setOnClickListener { onItemClick(spinHistory.title) }
        }
    }

    // Inflate the item layout and create ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_squad_shuffle, parent, false)
        return ViewHolder(view)
    }

    // Bind data to the ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(spinHistoryList[position])
    }

    // Get the total count of items
    override fun getItemCount(): Int = spinHistoryList.size
}
