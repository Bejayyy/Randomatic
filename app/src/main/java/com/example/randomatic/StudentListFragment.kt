package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.randomatic.databinding.FragmentStudentListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class StudentListFragment : Fragment() {

    private lateinit var binding: FragmentStudentListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private var sectionId: String? = null
    private var sectionName: String? = null
    private val studentsList = mutableListOf<String>()
    private val selectedStudents = mutableSetOf<String>() // Set to track selected students
    private lateinit var adapter: StudentsAdapter
    private var isRemoveMode = false // Flag to control checkbox visibility

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStudentListBinding.inflate(inflater, container, false)

        // Retrieve data from arguments
        sectionId = arguments?.getString("section_id")
        sectionName = arguments?.getString("section_name")

        binding.sectionTitle.text = sectionName ?: "Unknown Section"

        // Set up RecyclerView
        adapter = StudentsAdapter(studentsList, isRemoveMode) { student, isSelected ->
            if (isSelected) {
                selectedStudents.add(student) // Add selected student
            } else {
                selectedStudents.remove(student) // Remove unselected student
            }
            // Update the "Remove" button text based on selected students
            updateRemoveButtonText()
        }
        binding.studentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.studentsRecyclerView.adapter = adapter

        binding.arrowBack.setOnClickListener{
            it.findNavController().navigate(R.id.action_studentListFragment_to_listManager)
        }

        // Load students from Firestore if section exists
        fetchStudents()

        // Add student button listener
        binding.addStudentButton.setOnClickListener { addStudent() }

        // Toggle remove mode
        binding.removeStudentButton.setOnClickListener {
            if (isRemoveMode) {
                removeSelectedStudents()
            } else {
                isRemoveMode = true
                // Show cancel button when remove mode is activated
                binding.cancelRemoveButton.visibility = View.VISIBLE
                // Update the remove mode and refresh the adapter
                adapter.updateRemoveMode(isRemoveMode)
                updateRemoveButtonText() // Update button text after toggling remove mode
            }
        }

        // Cancel remove mode without deleting students
        binding.cancelRemoveButton.setOnClickListener {
            cancelRemoveMode()
        }

        // Initially hide cancel button
        binding.cancelRemoveButton.visibility = View.GONE

        return binding.root
    }

    private var originalStudentsList = mutableListOf<String>() // To store the original list

    private fun fetchStudents() {
        user?.uid?.let { uid ->
            sectionId?.let { id ->
                firestore.collection("users").document(uid)
                    .collection("sections").document(id)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Toast.makeText(requireContext(), "Error loading students", Toast.LENGTH_SHORT).show()
                            return@addSnapshotListener
                        }

                        val students = snapshot?.get("students") as? List<String> ?: emptyList()
                        originalStudentsList.clear()
                        originalStudentsList.addAll(students) // Save the original list
                        studentsList.clear()
                        studentsList.addAll(students)
                        adapter.notifyDataSetChanged()
                    }
            }
        }
    }

    private fun addStudent() {
        val studentName = binding.studentNameInput.text.toString().trim()
        if (studentName.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a student name", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if student already exists in the local list
        if (studentsList.contains(studentName)) {
            Toast.makeText(requireContext(), "Student already exists", Toast.LENGTH_SHORT).show()
            binding.studentNameInput.text.clear() // Clear the input after checking
            return
        }

        user?.uid?.let { uid ->
            sectionId?.let { id ->
                // Check if the student already exists in Firestore
                firestore.collection("users").document(uid)
                    .collection("sections").document(id)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val students = documentSnapshot.get("students") as? List<String> ?: emptyList()

                        // If the student is not already in Firestore, add the student
                        if (!students.contains(studentName)) {
                            firestore.collection("users").document(uid)
                                .collection("sections").document(id)
                                .update("students", FieldValue.arrayUnion(studentName))  // Append instead of overwrite
                                .addOnSuccessListener {
                                    // Now update the local list only after the Firestore update is successful
                                    fetchStudents()  // Refresh the students list after successful addition
                                    Toast.makeText(requireContext(), "Student added", Toast.LENGTH_SHORT).show()
                                    binding.studentNameInput.text.clear()  // Clear the input after adding
                                }
                                .addOnFailureListener {
                                    Toast.makeText(requireContext(), "Error adding student", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(requireContext(), "Student already exists in Firestore", Toast.LENGTH_SHORT).show()
                            binding.studentNameInput.text.clear()  // Clear the input
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error fetching students", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }


    private fun removeSelectedStudents() {
        if (selectedStudents.isEmpty()) {
            Toast.makeText(requireContext(), "No students selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Store the current students list as backup
        val backupList = studentsList.toList()

        user?.uid?.let { uid ->
            sectionId?.let { id ->
                firestore.collection("users").document(uid)
                    .collection("sections").document(id)
                    .update("students", FieldValue.arrayRemove(*selectedStudents.toTypedArray())) // Remove selected students
                    .addOnSuccessListener {
                        // Update local list after deletion
                        studentsList.removeAll(selectedStudents)
                        selectedStudents.clear()
                        adapter.notifyDataSetChanged()

                        // Reset the "Remove" button text and hide checkboxes
                        updateRemoveButtonText()

                        // Hide checkboxes and exit remove mode
                        isRemoveMode = false
                        adapter.updateRemoveMode(isRemoveMode)
                        binding.cancelRemoveButton.visibility = View.GONE // Hide cancel button

                        Toast.makeText(requireContext(), "Selected students removed", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        // If deletion fails, restore the backup
                        studentsList.clear()
                        studentsList.addAll(backupList)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(requireContext(), "Error removing students, reverting changes", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateRemoveButtonText() {
        if (selectedStudents.isNotEmpty()) {
            binding.removeStudentButton.text = "Delete" // Change button text to "Delete" if students are selected
        } else {
            binding.removeStudentButton.text = "Remove" // Keep text as "Remove" if no students are selected
        }
    }

    // New function to cancel remove mode without deleting any student
    private fun cancelRemoveMode() {
        selectedStudents.clear() // Clear any selected students
        isRemoveMode = false // Exit remove mode
        adapter.updateRemoveMode(isRemoveMode) // Update adapter to hide checkboxes
        updateRemoveButtonText() // Reset button text
        binding.cancelRemoveButton.visibility = View.GONE // Hide cancel button
    }
}









