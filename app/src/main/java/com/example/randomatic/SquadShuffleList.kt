package com.example.randomatic

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.randomatic.models.SquadShuffleHistory
import com.example.randomatic.adapters.SquadShuffleAdapter
import com.example.randomatic.ui.SelectedNamesPopup

class SquadShuffleList : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SquadShuffleAdapter
    private val squadShuffleHistoryList = mutableListOf<SquadShuffleHistory>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_squad_shuffle_list, container, false)

        recyclerView = view.findViewById(R.id.sections_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = SquadShuffleAdapter(squadShuffleHistoryList, requireContext()) { title ->
            fetchSelectedNames(title)
        }

        recyclerView.adapter = adapter

        view.findViewById<ImageView>(R.id.back_arrow).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set up filter chips


        fetchSquadShuffleHistory()
        return view
    }

    private fun fetchSquadShuffleHistory() {
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("spinHistory")
                .whereEqualTo("category", "Squad Shuffle") // Filter by category
                .get()
                .addOnSuccessListener { documents ->
                    squadShuffleHistoryList.clear()
                    for (document in documents) {
                        document.getString("title")?.let { title ->
                            squadShuffleHistoryList.add(SquadShuffleHistory(title))
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e("SquadShuffle", "Error fetching squad shuffle history", exception)
                    Toast.makeText(requireContext(), "Failed to load history", Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e("SquadShuffle", "User ID is null")
    }

    private fun filterSquadShuffleHistory(filterType: String) {
        val filteredList = when (filterType) {
            "AtoZ" -> squadShuffleHistoryList.sortedBy { it.title }
            "ZtoA" -> squadShuffleHistoryList.sortedByDescending { it.title }
            "Newest" -> squadShuffleHistoryList.sortedByDescending { it.title } // Assuming title contains date or timestamp
            "Oldest" -> squadShuffleHistoryList.sortedBy { it.title } // Assuming title contains date or timestamp
            else -> squadShuffleHistoryList
        }
        adapter.updateList(filteredList)
    }

    private fun fetchSelectedNames(title: String) {
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("spinHistory")
                .whereEqualTo("title", title)
                .get()
                .addOnSuccessListener { documents ->
                    val groupMap = mutableMapOf<String, List<String>>()

                    for (document in documents) {
                        val groups = document.get("groups") as? Map<*, *>
                        groups?.forEach { (groupName, studentList) ->
                            val key = groupName.toString()
                            val students = when (studentList) {
                                is List<*> -> studentList.mapNotNull { it as? String }
                                is String -> studentList.split(",").map { it.trim() }
                                else -> emptyList()
                            }
                            groupMap[key] = students
                        }
                    }

                    if (groupMap.isNotEmpty()) {
                        showGroupPopup(title, groupMap)
                    } else {
                        Log.e("SquadShuffle", "No groups found for this title")
                        Toast.makeText(requireContext(), "No groups found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("SquadShuffle", "Error fetching groups", exception)
                    Toast.makeText(requireContext(), "Failed to load groups", Toast.LENGTH_SHORT).show()
                }
        } ?: Log.e("SquadShuffle", "User ID is null")
    }

    private fun showGroupPopup(title: String, groups: Map<String, List<String>>) {
        val namesList = mutableListOf<String>()

        for ((groupName, students) in groups) {
            namesList.add("$groupName:")
            namesList.addAll(students.map { "- $it" })
            namesList.add("") // Add space between groups
        }

        SelectedNamesPopup.newInstance(title, namesList)
            .show(childFragmentManager, "SelectedNamesPopup")
    }
}