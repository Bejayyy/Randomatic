package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentSquadShuffleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SquadShuffle : Fragment() {
    private lateinit var binding: FragmentSquadShuffleBinding
    private val namesList = mutableListOf<String>()  // Mutable list to hold names
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val sectionNames = mutableListOf<String>()
    private val sectionIds = mutableListOf<String?>()
    private var shuffledGroups: List<List<String>>? = null
    private var selectedSection: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentSquadShuffleBinding.inflate(inflater, container, false)

        // Back navigation
        binding.backArrow.setOnClickListener {
            it.findNavController().navigate(R.id.action_squadShuffle_to_homePage)
        }

        setupNumberInputs()

        // Setup class list dropdown
        setupClassDropdown()

        // Handle adding names when 'Enter' is pressed
        binding.editTextName.setOnEditorActionListener { _, _, _ ->
            addName()
            true
        }

        // Restore previous state (if available)
        savedInstanceState?.let {
            shuffledGroups = it.getSerializable("shuffledGroups") as? List<List<String>>
            selectedSection = it.getString("selectedSection")
        }

        // Restore UI state
        selectedSection?.let { section ->
            binding.classDropdown.setText(section, false)
        }

        shuffledGroups?.let { groups ->
            // User already shuffled, so go directly to GroupDisplayFragment
            navigateToGroupDisplay(groups)
        }


        // Handle add button click using a touch listener for the drawable
        binding.editTextName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_add, 0)
        binding.editTextName.setOnTouchListener { _, event ->
            val DRAWABLE_RIGHT = 2
            if (event.action == MotionEvent.ACTION_UP) {
                if (binding.editTextName.compoundDrawables[DRAWABLE_RIGHT] != null &&
                    event.rawX >= (binding.editTextName.right - binding.editTextName.compoundDrawables[DRAWABLE_RIGHT].bounds.width())
                ) {
                    addName()
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Initial visibility setup for empty view
        if (namesList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.listViewNames.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.listViewNames.visibility = View.VISIBLE
        }

        // Handle start button click to divide names into groups
        binding.startButton.setOnClickListener {
            val numGroups = binding.editTextGroups.text.toString().toIntOrNull()
                ?: return@setOnClickListener

            val numMembers = binding.editTextMembers.text.toString().toIntOrNull()

            if (namesList.isEmpty()) {
                showToast("Please add student names first")
                return@setOnClickListener
            }

            // Call divide function to get the groups
            val groups = divideNamesIntoGroups(numGroups, numMembers)

            // Pass the groups to the next fragment using Bundle
            val bundle = Bundle().apply {
                putSerializable(
                    "groups",
                    ArrayList(groups)
                )  // Convert to ArrayList for serialization
            }

            it.findNavController()
                .navigate(R.id.action_squadShuffle_to_groupDisplayFragment, bundle)
        }

        return binding.root
    }

    private fun setupClassDropdown() {
        // Fetch section names from Firestore
        fetchSectionNames()

        // Set up the dropdown adapter
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            sectionNames
        )

        binding.classDropdown.setAdapter(adapter)

        // Handle selection
        binding.classDropdown.setOnItemClickListener { _, _, position, _ ->
            if (position < sectionIds.size) {
                sectionIds[position]?.let { sectionId ->
                    fetchStudentsForSection(sectionId)
                }
            }
        }
    }

    private fun fetchSectionNames() {
        val uid = user?.uid ?: return

        firestore.collection("users").document(uid).collection("sections")
            .get()
            .addOnSuccessListener { querySnapshot ->
                sectionNames.clear()
                sectionIds.clear()

                // Add default option
                sectionNames.add("Select a class list")
                sectionIds.add(null)

                querySnapshot.documents.forEach { document ->
                    sectionNames.add(document.getString("section_name") ?: "Unknown Section")
                    sectionIds.add(document.id)
                }

                // Update the dropdown adapter
                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    sectionNames
                )
                binding.classDropdown.setAdapter(adapter)
            }
            .addOnFailureListener {
                showToast("Error fetching class lists")
            }
    }

    private fun fetchStudentsForSection(sectionId: String) {
        val uid = user?.uid ?: return

        firestore.collection("users").document(uid).collection("sections")
            .document(sectionId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                namesList.clear()

                val students = documentSnapshot.get("students") as? List<String> ?: emptyList()
                namesList.addAll(students)

                // Update the ListView
                updateNamesListView()
                updateStudentCount()

                // Update placeholder to indicate selected class
                val selectedClass = sectionNames[sectionIds.indexOf(sectionId)]
                binding.editTextName.hint = "Add a student to $selectedClass"

                showToast("Loaded ${students.size} students")
            }
            .addOnFailureListener {
                showToast("Error fetching students")
            }
    }


    private fun addName() {
        val name = binding.editTextName.text.toString().trim()
        if (name.isNotBlank()) {
            namesList.add(name)
            updateNamesListView()
            binding.editTextName.text.clear()

            // Update student count
            updateStudentCount()
        }
    }

    private fun updateStudentCount() {
        // Update the student count in the header
        binding.studentCount.text = "${namesList.size} students"
    }

    private fun updateNamesListView() {
        // Update the ListView with the latest names
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, namesList)
        binding.listViewNames.adapter = adapter

        // Show/hide the "Empty List" view based on list content
        if (namesList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.listViewNames.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.listViewNames.visibility = View.VISIBLE
        }
    }

    private fun divideNamesIntoGroups(numGroups: Int, numMembers: Int?): List<List<String>> {
        if (namesList.isEmpty()) return emptyList()

        val shuffledNames = namesList.shuffled() // Shuffle the names to randomize group assignment

        // If specific number of members is provided, use that to determine number of groups
        if (numMembers != null && numMembers > 0) {
            val actualNumGroups = (shuffledNames.size + numMembers - 1) / numMembers
            val groups = MutableList(actualNumGroups) { mutableListOf<String>() }

            for ((index, name) in shuffledNames.withIndex()) {
                val groupIndex = index / numMembers
                if (groupIndex < groups.size) {
                    groups[groupIndex].add(name)
                }
            }

            return groups
        } else {
            // Otherwise, distribute names evenly across specified number of groups
            val groups = MutableList(numGroups) { mutableListOf<String>() }

            for ((index, name) in shuffledNames.withIndex()) {
                groups[index % numGroups].add(name)
            }

            return groups
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun setupNumberInputs() {
        // Set initial values
        binding.editTextGroups.setText("1")

        // Set up decrease/increase buttons for groups
        binding.decreaseGroups.setOnClickListener {
            val currentValue = binding.editTextGroups.text.toString().toIntOrNull() ?: 2
            if (currentValue > 1) {
                binding.editTextGroups.setText((currentValue - 1).toString())
            }
        }

        binding.increaseGroups.setOnClickListener {
            val currentValue = binding.editTextGroups.text.toString().toIntOrNull() ?: 2
            binding.editTextGroups.setText((currentValue + 1).toString())
        }

        // Set up decrease/increase buttons for members per group
        binding.decreaseMembers.setOnClickListener {
            val currentValue =
                binding.editTextMembers.text.toString().toIntOrNull() ?: return@setOnClickListener
            if (currentValue > 1) {
                binding.editTextMembers.setText((currentValue - 1).toString())
            }
        }

        binding.increaseMembers.setOnClickListener {
            val currentValue = binding.editTextMembers.text.toString().toIntOrNull() ?: 0
            binding.editTextMembers.setText((currentValue + 1).toString())
        }

    }
    // Save instance state
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("shuffledGroups", ArrayList(shuffledGroups ?: emptyList()))
        outState.putString("selectedSection", selectedSection)
    }

    // Navigate to GroupDisplayFragment with existing shuffled groups
    private fun navigateToGroupDisplay(groups: List<List<String>>) {
        val bundle = Bundle().apply {
            putSerializable("groups", ArrayList(groups))
        }
        view?.findNavController()?.navigate(R.id.action_squadShuffle_to_groupDisplayFragment, bundle)
    }
}