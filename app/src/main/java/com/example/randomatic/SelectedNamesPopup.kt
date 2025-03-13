package com.example.randomatic.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.randomatic.R

class SelectedNamesPopup : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.popup_selected_names, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleTextView: TextView = view.findViewById(R.id.popupTitle)
        val namesTextView: TextView = view.findViewById(R.id.popupNames)
        val closeButton: Button = view.findViewById(R.id.closeButton)

        arguments?.let {
            titleTextView.text = "Selected Names for ${it.getString("title")}"
            namesTextView.text = it.getStringArrayList("names")?.joinToString("\n")
        }

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(title: String, names: List<String>): SelectedNamesPopup {
            val fragment = SelectedNamesPopup()
            val args = Bundle()
            args.putString("title", title)
            args.putStringArrayList("names", ArrayList(names))
            fragment.arguments = args
            return fragment
        }
    }
}
