package com.example.randomatic;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

class SpinHistoryAdapter(
    private val spinList: MutableList<SoloSpinHistoryList.SpinHistory>,
    private val context: Context,
    private val onItemClick: (String) -> Unit // Callback for item click
) : RecyclerView.Adapter<SpinHistoryAdapter.ViewHolder>() {

    private var deleteMode = false;

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val historyTitle: TextView = itemView.findViewById(R.id.historyTitle);
        val selectRadioButton: RadioButton = itemView.findViewById(R.id.selectRadioButton);
        val editIcon: ImageView = itemView.findViewById(R.id.editIcon);
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_solo_spin_history, parent, false);
        return ViewHolder(view);
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = spinList[position];
        holder.historyTitle.text = item.title;

        // Handle Delete Mode (Show/Hide RadioButton)
        holder.selectRadioButton.visibility = if (deleteMode) View.VISIBLE else View.GONE;

        // Handle Edit Click
        holder.editIcon.setOnClickListener {
            showRenameDialog(holder.historyTitle, position);
        };

        // Set background color
        holder.itemView.setBackgroundColor(Color.parseColor("#24A7A1"));
        // Set text color to white
        holder.historyTitle.setTextColor(Color.WHITE);

        // Handle item click
        holder.itemView.setOnClickListener {
            onItemClick(item.title); // Pass title to fragment
        };
    }

    override fun getItemCount(): Int = spinList.size;

    // Enable/Disable Delete Mode
    fun toggleDeleteMode() {
        deleteMode = !deleteMode;
        notifyDataSetChanged();
    }

    // Show Rename Dialog
    private fun showRenameDialog(titleView: TextView, position: Int) {
        val builder = AlertDialog.Builder(context);
        builder.setTitle("Rename Spin History");

        val viewInflated = LayoutInflater.from(context).inflate(R.layout.dialog_rename, null);
        val input = viewInflated.findViewById<EditText>(R.id.renameEditText);
        input.setText(titleView.text.toString());

        builder.setView(viewInflated);
        builder.setPositiveButton("OK") { dialog, _ ->
            val newName = input.text.toString().trim();
            if (newName.isNotEmpty()) {
                titleView.text = newName;
                spinList[position].title = newName; // Update data
                notifyItemChanged(position);
            }
            dialog.dismiss();
        };

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel(); };
        builder.show();
    }
}
