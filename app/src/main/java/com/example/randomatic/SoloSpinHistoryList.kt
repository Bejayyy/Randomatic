package com.example.randomatic

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SoloSpinHistoryList : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SpinHistoryAdapter
    private val spinHistoryList = mutableListOf<SpinHistory>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_solo_spin_history_list, container, false)

        recyclerView = view.findViewById(R.id.sections_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = SpinHistoryAdapter(spinHistoryList, requireContext()) { title ->
            fetchSelectedNames(title) // Fetch names when a title is clicked
        }
        recyclerView.adapter = adapter

        fetchSoloSpinHistory()

        return view
    }

    private fun fetchSoloSpinHistory() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()
        val spinHistoryRef = firestore.collection("users").document(userId).collection("spinHistory")

        spinHistoryRef.get()
            .addOnSuccessListener { documents ->
                spinHistoryList.clear()
                for (document in documents) {
                    val category = document.getString("category") ?: ""
                    val title = document.getString("title") ?: ""

                    if (category == "SoloSpin") {
                        spinHistoryList.add(SpinHistory(title, category))
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("SoloSpinHistory", "Error fetching spin history", exception)
            }
    }

    private fun fetchSelectedNames(title: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        val spinHistoryRef = firestore.collection("users").document(userId)
            .collection("spinHistory")
            .whereEqualTo("title", title)

        spinHistoryRef.get()
            .addOnSuccessListener { documents ->
                val selectedNames = mutableListOf<String>()
                for (document in documents) {
                    val selectedName = document.getString("selectedName") ?: ""
                    selectedNames.add(selectedName)
                }

                // Show the names in a custom popup
                SelectedNamesPopup.newInstance(title, selectedNames).show(childFragmentManager, "SelectedNamesPopup")
            }
            .addOnFailureListener { exception ->
                Log.e("SoloSpinHistory", "Error fetching selected names", exception)
            }
    }

    data class SpinHistory(var title: String = "", val category: String = "")
}

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