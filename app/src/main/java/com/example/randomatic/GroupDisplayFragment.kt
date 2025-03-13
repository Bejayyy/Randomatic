package com.example.randomatic

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentGroupDisplayBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class GroupDisplayFragment : Fragment() {

    private lateinit var binding: FragmentGroupDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGroupDisplayBinding.inflate(inflater, container, false)

        val groups = arguments?.getSerializable("groups") as? List<List<String>>

        groups?.let {
            binding.textViewTeamCount.text = it.size.toString()
            binding.backArrow.setOnClickListener {
                showExitConfirmationDialog(groups)
            }

            binding.gridLayoutGroups.removeAllViews()

            it.forEachIndexed { index, group ->
                val groupCard = MaterialCardView(requireContext()).apply {
                    radius = 8f
                    cardElevation = 2f
                    setCardBackgroundColor(Color.WHITE)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(16, 16, 16, 16)
                    }
                    strokeWidth = 1
                    strokeColor = ContextCompat.getColor(context, R.color.colorGroup6)
                }

                val groupLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }

                val groupTitle = TextView(requireContext()).apply {
                    text = "Group ${index + 1}"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(getGroupColor(index))
                }
                groupLayout.addView(groupTitle)

                val membersContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(8, 8, 8, 8)
                }

                group.forEach { member ->
                    val memberTextView = TextView(requireContext()).apply {
                        text = member
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        setPadding(16, 12, 16, 12)
                    }
                    membersContainer.addView(memberTextView)
                }

                groupLayout.addView(membersContainer)
                groupCard.addView(groupLayout)
                binding.gridLayoutGroups.addView(groupCard)
            }
        } ?: run {
            binding.textViewGroups.text = "No groups to display."
        }

        binding.btnSave.setOnClickListener {
            showSavePopup(groups)
        }

        return binding.root
    }

    private fun getGroupColor(index: Int): Int {
        val colors = listOf(
            R.color.colorGroup1, R.color.colorGroup2, R.color.colorGroup3,
            R.color.colorGroup4, R.color.colorGroup5, R.color.colorGroup6
        )
        return ContextCompat.getColor(requireContext(), colors[index % colors.size])
    }

    private fun showSavePopup(groups: List<List<String>>?) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.popup_save_spin, null)
        val input = view.findViewById<EditText>(R.id.etSpinTitle)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)
        val btnCancel = view.findViewById<TextView>(R.id.btnCancel)

        btnSave.setOnClickListener {
            val title = input.text.toString().trim()
            if (title.isNotEmpty() && groups != null) {
                saveToFirestore(title, groups)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Title cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun saveToFirestore(title: String, groups: List<List<String>>) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userId = user.uid
        val groupsMap = HashMap<String, List<String>>().apply {
            groups.forEachIndexed { index, group -> put("Group ${index + 1}", group) }
        }

        val spinData = hashMapOf(
            "category" to "Squad Shuffle",
            "title" to title,
            "groups" to groupsMap,
            "timestamp" to FieldValue.serverTimestamp()
        )

        db.collection("users").document(userId)
            .collection("spinHistory").add(spinData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Spin saved successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showExitConfirmationDialog(groups: List<List<String>>?) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.popup_exit_confirmation, null)
        val btnYes = view.findViewById<TextView>(R.id.btnYes)
        val btnNo = view.findViewById<TextView>(R.id.btnNo)

        btnYes.setOnClickListener {
            dialog.dismiss()
            showSavePopup(groups) // Show save dialog if the user selects "Yes"
        }

        btnNo.setOnClickListener {
            dialog.dismiss()
            requireView().findNavController().navigate(R.id.action_groupDisplayFragment_to_squadShuffle)
        }

        dialog.setContentView(view)
        dialog.show()
    }

}