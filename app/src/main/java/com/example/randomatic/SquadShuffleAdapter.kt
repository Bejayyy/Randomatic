package com.example.randomatic.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.randomatic.R
import com.example.randomatic.models.SquadShuffleHistory

class SquadShuffleAdapter(
    private val historyList: MutableList<SquadShuffleHistory>, // Make the list mutable
    private val context: Context,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SquadShuffleAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shuffleTitle: TextView = itemView.findViewById(R.id.shuffleTitle)
        val shuffleDate: TextView = itemView.findViewById(R.id.shuffleDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_squad_shuffle, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = historyList[position]
        holder.shuffleTitle.text = history.title
        holder.itemView.setOnClickListener { onItemClick(history.title) }
    }

    override fun getItemCount(): Int = historyList.size

    // Add this function to update the list
    fun updateList(newList: List<SquadShuffleHistory>) {
        historyList.clear()
        historyList.addAll(newList)
        notifyDataSetChanged()
    }
}