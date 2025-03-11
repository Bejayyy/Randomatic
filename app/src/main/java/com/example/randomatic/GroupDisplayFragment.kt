package com.example.randomatic

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.randomatic.databinding.FragmentGroupDisplayBinding
import com.google.android.material.card.MaterialCardView

class GroupDisplayFragment : Fragment() {

    private lateinit var binding: FragmentGroupDisplayBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGroupDisplayBinding.inflate(inflater, container, false)

        // Retrieve the groups argument from NavArgs as Serializable
        val groups = arguments?.getSerializable("groups") as? List<List<String>>

        groups?.let {
            // Set the "Grouped in teams of" text dynamically if needed
            binding.textViewTeamCount.text = it.size.toString()

            // Clear the previous rows in GridLayout
            binding.gridLayoutGroups.removeAllViews()

            // Add each group to the GridLayout
            it.forEachIndexed { index, group ->
                // Create a MaterialCardView to hold the group
                val groupCard = MaterialCardView(requireContext()).apply {
                    radius = resources.getDimension(R.dimen.card_corner_radius) // You'll need to define this in dimens.xml (8dp recommended)
                    cardElevation = resources.getDimension(R.dimen.card_elevation) // You'll need to define this in dimens.xml (2dp recommended)
                    setCardBackgroundColor(Color.WHITE)
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = GridLayout.LayoutParams.WRAP_CONTENT
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        setMargins(16, 16, 16, 16)
                    }
                    // Add stroke to card
                    strokeWidth = 1
                    strokeColor = ContextCompat.getColor(context, R.color.colorGroup6) // orange stroke
                }

                // Create a LinearLayout to organize the content
                val groupLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Create a TextView for the group header
                val groupTitle = TextView(requireContext()).apply {
                    text = "Group ${index + 1}"
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    setPadding(16, 12, 16, 12)
                    setBackgroundColor(getGroupColor(index))
                    gravity = View.TEXT_ALIGNMENT_CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    // Add elevation to title for depth effect
                    elevation = resources.getDimension(R.dimen.text_elevation) // You'll need to define this in dimens.xml (1dp recommended)
                }

                // Add group title to the layout
                groupLayout.addView(groupTitle)

                // Create container for members with some padding
                val membersContainer = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(8, 8, 8, 8)
                }

                // Add each group member to the container with improved styling
                group.forEachIndexed { memberIndex, member ->
                    val memberCard = CardView(requireContext()).apply {
                        radius = resources.getDimension(R.dimen.member_card_radius) // You'll need to define this in dimens.xml (4dp recommended)
                        cardElevation = resources.getDimension(R.dimen.member_card_elevation) // You'll need to define this in dimens.xml (1dp recommended)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(8, if (memberIndex == 0) 0 else 8, 8, 8)
                        }
                    }

                    val memberView = TextView(requireContext()).apply {
                        text = member
                        textSize = 16f
                        setTextColor(Color.BLACK)
                        setPadding(16, 12, 16, 12)
                        background = if (memberIndex % 2 == 0) {
                            // Even rows get a light background
                            ContextCompat.getDrawable(requireContext(), R.drawable.member_even_bg)
                        } else {
                            // Odd rows get a slightly different background
                            ContextCompat.getDrawable(requireContext(), R.drawable.member_odd_bg)
                        }
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                    }

                    memberCard.addView(memberView)
                    membersContainer.addView(memberCard)
                }

                // Add the members container to the group layout
                groupLayout.addView(membersContainer)

                // Add the group layout to the card
                groupCard.addView(groupLayout)

                // Add the card to the GridLayout
                binding.gridLayoutGroups.addView(groupCard)
            }
        } ?: run {
            binding.textViewGroups.text = "No groups to display."
        }

        return binding.root
    }

    // Function to get different colors for the groups
    private fun getGroupColor(index: Int): Int {
        val colors = listOf(
            R.color.colorGroup1,
            R.color.colorGroup2,
            R.color.colorGroup3,
            R.color.colorGroup4,
            R.color.colorGroup5,
            R.color.colorGroup6  // Orange color you specified
        )

        // Using the firstcardBackgroundColor (#24A7A1) as a potential primary accent
        // for special cases like the first group or highlighted elements
        if (index == 0) {
            return resources.getColor(R.color.firstcardBackgroundColor, null)
        }

        return resources.getColor(colors[index % colors.size], null)
    }
}