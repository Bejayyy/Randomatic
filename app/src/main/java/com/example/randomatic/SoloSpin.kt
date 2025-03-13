package com.example.randomatic

import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentSoloSpinBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class SoloSpin : Fragment() {

    private lateinit var binding: FragmentSoloSpinBinding
    private val namesList = mutableListOf<String>()
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private val sectionNames = mutableListOf<String>()
    private val sectionIds = mutableListOf<String?>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSoloSpinBinding.inflate(inflater, container, false)

        binding.backArrow.setOnClickListener {
            it.findNavController().navigate(R.id.action_soloSpin_to_homePage)
        }

        populateSections()
        setupSpinner()
        setupButtons()
        updateStartButtonState()
        updateNamesListView()

        return binding.root
    }

    private fun populateSections() {
        val uid = user?.uid ?: return
        firestore.collection("users").document(uid).collection("sections")
            .get()
            .addOnSuccessListener { querySnapshot ->
                sectionNames.clear()
                sectionIds.clear()
                querySnapshot.documents.forEach { document ->
                    sectionNames.add(document.getString("section_name") ?: "Unknown Section")
                    sectionIds.add(document.id)
                }
            }
            .addOnFailureListener {
                showToast("Error fetching sections")
            }
    }

    private fun setupSpinner() {
        binding.spinnerLayout.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), binding.spinnerIcon)
            sectionNames.forEachIndexed { index, section ->
                popupMenu.menu.add(0, index, 0, section)
            }
            popupMenu.setOnMenuItemClickListener { menuItem ->
                val position = menuItem.itemId
                binding.spinnerText.text = sectionNames[position]
                sectionIds[position]?.let { fetchStudentsForSection(it) }
                true
            }
            popupMenu.show()
        }
    }

    private fun fetchStudentsForSection(sectionId: String) {
        val uid = user?.uid ?: return
        firestore.collection("users").document(uid).collection("sections")
            .document(sectionId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                namesList.clear()
                namesList.addAll(documentSnapshot.get("students") as? List<String> ?: emptyList())
                updateListView()
            }
            .addOnFailureListener {
                showToast("Error fetching students")
            }
    }

    private fun setupButtons() {
        binding.addNameButton.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            if (name.isNotEmpty()) {
                namesList.add(name)
                updateListView()
                binding.editTextName.text.clear()
            } else {
                showToast("Please enter a name")
            }
        }

        binding.startButton.setOnClickListener {
            if (namesList.isNotEmpty()) {
                showSpinningPopup()
            } else {
                showToast("No names to select from!")
            }
        }

        binding.spinTitleInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateStartButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateListView() {
        binding.listViewNames.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, namesList)
        updateStartButtonState()
    }

    private fun updateStartButtonState() {
        binding.startButton.isEnabled = binding.spinTitleInput.text.toString().trim().isNotEmpty() && namesList.isNotEmpty()
    }

    private fun showSpinningPopup() {
        val inflater = LayoutInflater.from(requireContext())
        val popupView = inflater.inflate(R.layout.popup_spinning, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            animationStyle = R.style.SpinningPopupAnimation
            width = (resources.displayMetrics.widthPixels * 0.9).toInt() // 90% of screen width
        }

        // Find views from the popup layout
        val spinningTextView = popupView.findViewById<TextView>(R.id.spinningTextView)
        val spinTitleTextView = popupView.findViewById<TextView>(R.id.spinTitleTextView)
        val stopSpinButton = popupView.findViewById<Button>(R.id.stopSpinButton)

        // Result controls
        val spinningControlsLayout = popupView.findViewById<LinearLayout>(R.id.spinningControlsLayout)
        val resultControlsLayout = popupView.findViewById<LinearLayout>(R.id.resultControlsLayout)
        val resultMessageTextView = popupView.findViewById<TextView>(R.id.resultMessageTextView)
        val removeNameButton = popupView.findViewById<Button>(R.id.removeNameButton)
        val keepNameButton = popupView.findViewById<Button>(R.id.keepNameButton)
        val respinButton = popupView.findViewById<Button>(R.id.respinButton)
        val closeButton = popupView.findViewById<Button>(R.id.closeButton)

        // Set the spin title
        spinTitleTextView.text = binding.spinTitleInput.text.toString().trim()

        // Show popup
        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0)

        val spinningNames = namesList.toMutableList() // Make a copy for the current session

        fun startSpinning() {
            // Reset UI for new spin
            spinningControlsLayout.visibility = View.VISIBLE
            resultControlsLayout.visibility = View.GONE

            if (spinningNames.isEmpty()) {
                spinningTextView.text = "No names left!"
                spinningControlsLayout.visibility = View.GONE
                resultControlsLayout.visibility = View.VISIBLE
                resultMessageTextView.text = "No names left to spin!"
                removeNameButton.visibility = View.GONE
                keepNameButton.visibility = View.GONE
                return
            }

            val handler = Handler()
            var isSpinning = true
            val spinDuration = 5000L // Maximum spin duration (if user doesn't stop manually)
            val updateInterval = 80L // Faster updates for smoother animation
            var elapsedTime = 0L
            var currentPosition = 0
            var spinSpeed = updateInterval // Start fast

            // Create a carousel effect with slowing down animation
            val spinRunnable = object : Runnable {
                override fun run() {
                    if (isSpinning) {
                        // Update text with the next name in the list
                        currentPosition = (currentPosition + 1) % spinningNames.size
                        val currentName = spinningNames[currentPosition]

                        // Apply a quick scale animation for each name change
                        spinningTextView.apply {
                            animate()
                                .scaleX(0.8f)
                                .scaleY(0.8f)
                                .setDuration(spinSpeed / 2)
                                .withEndAction {
                                    text = currentName
                                    animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(spinSpeed / 2)
                                        .start()
                                }
                                .start()
                        }

                        // Gradually slow down if we're approaching the max duration
                        elapsedTime += updateInterval
                        if (elapsedTime > spinDuration * 0.7) {
                            // Increase the interval to simulate slowing down
                            spinSpeed = (updateInterval * (1 + (elapsedTime - spinDuration * 0.7) / (spinDuration * 0.3) * 5)).toLong()
                        }

                        // Check if max duration reached
                        if (elapsedTime >= spinDuration) {
                            finalizeSelection(spinningTextView, popupWindow, spinningNames, spinningControlsLayout, resultControlsLayout, resultMessageTextView)
                            isSpinning = false
                        } else {
                            handler.postDelayed(this, spinSpeed)
                        }
                    }
                }
            }

            // Start spinning
            handler.post(spinRunnable)

            // Stop button logic
            stopSpinButton.setOnClickListener {
                if (isSpinning) {
                    isSpinning = false
                    handler.removeCallbacks(spinRunnable)

                    // Run a slowing down animation before showing final result
                    slowDownAndStop(spinningTextView, handler, popupWindow, spinningNames, spinningControlsLayout, resultControlsLayout, resultMessageTextView)
                }
            }
        }

        // Setup result buttons
        removeNameButton.setOnClickListener {
            val selectedName = spinningTextView.text.toString()
            spinningNames.remove(selectedName)
            // Also remove from the original list so it updates in the UI
            namesList.remove(selectedName)
            updateListView()
            startSpinning()
        }

        keepNameButton.setOnClickListener {
            startSpinning()
        }

        respinButton.setOnClickListener {
            startSpinning()
        }

        closeButton.setOnClickListener {
            popupWindow.dismiss()
        }

        // Start initial spinning
        startSpinning()
    }


    private fun slowDownAndStop(
        spinningTextView: TextView,
        handler: Handler,
        popupWindow: PopupWindow,
        spinningNames: MutableList<String>,
        spinningControlsLayout: View,
        resultControlsLayout: View,
        resultMessageTextView: TextView
    ) {
        var stoppingSpeed = 100L
        var stepsRemaining = 10 // Number of slowing steps
        var currentIndex = spinningNames.indexOf(spinningTextView.text.toString())
        if (currentIndex < 0) currentIndex = 0

        val slowDownRunnable = object : Runnable {
            override fun run() {
                if (stepsRemaining > 0) {
                    // Move to next name
                    currentIndex = (currentIndex + 1) % spinningNames.size
                    val nextName = spinningNames[currentIndex]

                    // Apply bounce effect
                    spinningTextView.apply {
                        animate()
                            .scaleX(0.9f)
                            .scaleY(0.9f)
                            .setDuration(stoppingSpeed / 2)
                            .withEndAction {
                                text = nextName
                                animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(stoppingSpeed / 2)
                                    .start()
                            }
                            .start()
                    }

                    // Increase interval for next iteration
                    stoppingSpeed += 50
                    stepsRemaining--
                    handler.postDelayed(this, stoppingSpeed)
                } else {
                    // Final selection
                    finalizeSelection(spinningTextView, popupWindow, spinningNames, spinningControlsLayout, resultControlsLayout, resultMessageTextView)
                }
            }
        }

        handler.post(slowDownRunnable)
    }

    private fun finalizeSelection(
        spinningTextView: TextView,
        popupWindow: PopupWindow,
        spinningNames: MutableList<String>,
        spinningControlsLayout: View,
        resultControlsLayout: View,
        resultMessageTextView: TextView
    ) {
        val selectedName = spinningTextView.text.toString()

        // Highlight selection with a pulse animation
        spinningTextView.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(300)
            .withEndAction {
                spinningTextView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start()
            }
            .start()

        // Save to Firestore
        saveSpinResultToFirestore(selectedName)

        // Show result controls
        spinningControlsLayout.visibility = View.GONE
        resultControlsLayout.visibility = View.VISIBLE
        resultMessageTextView.text = "Selected: $selectedName\nWould you like to keep this name for the next spin?"

        // Show toast but don't dismiss
        showToast("Selected: $selectedName")
    }
    private fun saveSpinResultToFirestore(selectedName: String) {
        val uid = user?.uid ?: return
        val spinTitle = binding.spinTitleInput.text.toString().trim()
        if (spinTitle.isEmpty()) {
            showToast("Please enter a spin title")
            return
        }
        val spinData = mapOf(
            "title" to spinTitle,
            "selectedName" to selectedName,
            "category" to "SoloSpin",
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("users").document(uid).collection("spinHistory")
            .add(spinData)
            .addOnSuccessListener { showToast("Spin history saved!") }
            .addOnFailureListener { showToast("Failed to save spin history") }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun updateNamesListView() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, namesList)
        binding.listViewNames.adapter = adapter

        // Ensure visibility is properly toggled
        if (namesList.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.listViewNames.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.listViewNames.visibility = View.VISIBLE
        }
    }

}