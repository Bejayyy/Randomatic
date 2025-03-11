package com.example.randomatic

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.randomatic.databinding.FragmentListManagerBinding
import com.example.randomatic.databinding.ItemSectionBinding
import com.example.randomatic.databinding.ItemStudentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.QuerySnapshot

data class Section(
    var sectionId: String = "", // ✅ Add this
    @get:PropertyName("section_name") @set:PropertyName("section_name")
    var sectionName: String = "",
    @get:PropertyName("students") @set:PropertyName("students")
    var students: List<String> = emptyList()
)


class ListManager : Fragment() {

    private lateinit var binding: FragmentListManagerBinding
    private lateinit var sectionsAdapter: SectionsAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentListManagerBinding.inflate(inflater, container, false)

        binding.sectionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            sectionsAdapter = SectionsAdapter(emptyList()) { section ->
                navigateToStudents(section)
            }
            adapter = sectionsAdapter
        }

        binding.createSectionButton.setOnClickListener { showCreateSectionDialog() }
        binding.arrowBack.setOnClickListener { it.findNavController().navigate(R.id.action_listManager_to_homePage) }

        fetchSections()
        return binding.root
    }

    private fun showCreateSectionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_form, null)
        val sectionNameInput = dialogView.findViewById<EditText>(R.id.section_name_input)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Create New Section")
            .setView(dialogView)
            .setPositiveButton("Next") { _, _ ->
                val sectionName = sectionNameInput.text.toString().trim()
                if (sectionName.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a section name", Toast.LENGTH_SHORT).show()
                } else {
                    saveSectionAndNavigate(sectionName)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun saveSectionAndNavigate(sectionName: String) {
        user?.uid?.let { uid ->
            val sectionData = mapOf(
                "section_name" to sectionName,
                "students" to emptyList<String>() // Initially, no students
            )

            firestore.collection("users").document(uid).collection("sections")
                .add(sectionData)
                .addOnSuccessListener { documentReference ->
                    val sectionId = documentReference.id
                    Toast.makeText(requireContext(), "Section created", Toast.LENGTH_SHORT).show()

                    val bundle = Bundle().apply {
                        putString("section_id", sectionId)
                        putString("section_name", sectionName)
                    }
                    view?.findNavController()?.navigate(R.id.action_listManager_to_studentListFragment, bundle)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to create section", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createSection(sectionName: String, studentsList: List<String>) {
        user?.uid?.let { uid ->
            val sectionData = mapOf(
                "section_name" to sectionName,
                "students" to studentsList
            )
            firestore.collection("users").document(uid).collection("sections")
                .add(sectionData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Section created successfully", Toast.LENGTH_SHORT).show()
                    fetchSections()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to create section", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchSections() {
        user?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("sections")
                .addSnapshotListener { snapshot: QuerySnapshot?, error ->
                    if (error != null) {
                        Log.e("ListManager", "Error fetching sections: ${error.message}")
                        return@addSnapshotListener
                    }
                    val sections = snapshot?.map { document ->
                        val section = document.toObject(Section::class.java)
                        section.sectionId = document.id  // ✅ Store Firestore document ID
                        section
                    } ?: emptyList()
                    sectionsAdapter.updateSections(sections)
                }
        }
    }


    private fun navigateToStudents(section: Section) {
        val bundle = Bundle().apply {
            putString("section_id", section.sectionId)  // ✅ Pass section ID
            putString("section_name", section.sectionName)
        }
        view?.findNavController()?.navigate(R.id.action_listManager_to_studentListFragment, bundle)
    }

}

class SectionsAdapter(private var sections: List<Section>, private val onSectionClick: (Section) -> Unit) :
    androidx.recyclerview.widget.RecyclerView.Adapter<SectionsAdapter.SectionViewHolder>() {

    inner class SectionViewHolder(private val binding: ItemSectionBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(section: Section) {
            binding.sectionName.text = section.sectionName.ifEmpty { "No Section Name" }
            binding.root.setBackgroundColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (adapterPosition % 2 == 0) R.color.firstcardBackgroundColor else R.color.secondcardBackgroundColor
                )
            )
            binding.root.setOnClickListener { onSectionClick(section) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(sections[position])
    }

    override fun getItemCount() = sections.size

    fun updateSections(newSections: List<Section>) {
        sections = newSections
        notifyDataSetChanged()
    }
}

// StudentsAdapter class added here
class StudentsAdapter(
    private var students: List<String>,
    private var isRemoveMode: Boolean, // Flag to show or hide checkboxes
    private val onStudentSelected: (String, Boolean) -> Unit // Callback for checkbox selection
) : androidx.recyclerview.widget.RecyclerView.Adapter<StudentsAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(student: String) {
            binding.studentName.text = student
            binding.checkbox.visibility = if (isRemoveMode) View.VISIBLE else View.GONE
            binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                // Notify the fragment whether the student is selected or not
                onStudentSelected(student, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount() = students.size

    // Update the remove mode flag
    fun updateRemoveMode(isRemoveMode: Boolean) {
        this.isRemoveMode = isRemoveMode
        notifyDataSetChanged()
    }

    fun updateStudents(newStudents: List<String>) {
        students = newStudents
        notifyDataSetChanged()
    }
}
