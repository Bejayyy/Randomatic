package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentSquadShuffleBinding

class SquadShuffle : Fragment() {

    private lateinit var binding: FragmentSquadShuffleBinding
    private val namesList = mutableListOf<String>()  // Mutable list to hold names

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSquadShuffleBinding.inflate(inflater, container, false)

        // Back navigation
        binding.backArrow.setOnClickListener {
            it.findNavController().navigate(R.id.action_squadShuffle_to_homePage)
        }

        // Handle adding names when 'Enter' is pressed
        binding.editTextName.setOnEditorActionListener { _, _, _ ->
            val name = binding.editTextName.text.toString()
            if (name.isNotBlank()) {
                namesList.add(name)  // Add name to the list
                updateNamesListView()  // Update the ListView with new name
                binding.editTextName.text.clear()  // Clear the text field after adding
            }
            true
        }

        // Handle add button click
        binding.editTextName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_add, 0)
        binding.editTextName.setOnClickListener {
            val name = binding.editTextName.text.toString()
            if (name.isNotBlank()) {
                namesList.add(name)
                updateNamesListView()
                binding.editTextName.text.clear()
            }
        }

        // Handle start button click to divide names into groups
        binding.startButton.setOnClickListener {
            val numGroups =
                binding.editTextGroups.text.toString().toIntOrNull() ?: return@setOnClickListener
            val numMembers = binding.editTextMembers.text.toString().toIntOrNull()

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

    private fun updateNamesListView() {
        // Update the ListView with the latest names
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, namesList)
        binding.listViewNames.adapter = adapter

        // Show the "Empty List" view if the list is empty
        if (namesList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.emptyView.visibility = View.GONE
        }
    }

    private fun divideNamesIntoGroups(numGroups: Int, numMembers: Int?): List<List<String>> {
        val shuffledNames = namesList.shuffled() // Shuffle the names to randomize group assignment
        val groupSize = numMembers ?: (shuffledNames.size / numGroups)
        val groups = MutableList(numGroups) { mutableListOf<String>() }

        var index = 0
        for (name in shuffledNames) {
            groups[index % numGroups].add(name)  // Distribute names randomly
            index++
        }

        return groups
    }
}




