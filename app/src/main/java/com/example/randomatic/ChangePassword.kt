package com.example.randomatic

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.randomatic.databinding.FragmentChangePasswordBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class ChangePassword : Fragment() {

    private var _binding: FragmentChangePasswordBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val currentUser: FirebaseUser? by lazy { auth.currentUser }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout using view binding
        _binding = FragmentChangePasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        setupPasswordValidation()
    }

    private fun setupListeners() {
        // Back button click listener
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Cancel button click listener
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Save button click listener
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                changePassword()
            }
        }
    }

    private fun setupPasswordValidation() {
        // Add text change listener to new password field
        binding.newPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePasswordStrength(s.toString())
            }
        })

        // Add text change listener to confirm password field
        binding.confirmPasswordInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
            }
        })
    }

    private fun validatePasswordStrength(password: String) {
        val hasMinLength = password.length >= 8
        val hasNumber = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (!hasMinLength || !hasNumber || !hasSpecial) {
            binding.newPasswordLayout.error = "Password must meet requirements"
        } else {
            binding.newPasswordLayout.error = null
        }
    }

    private fun validatePasswordMatch() {
        val newPassword = binding.newPasswordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
        } else {
            binding.confirmPasswordLayout.error = null
        }
    }

    private fun validateInputs(): Boolean {
        val currentPassword = binding.currentPasswordInput.text.toString()
        val newPassword = binding.newPasswordInput.text.toString()
        val confirmPassword = binding.confirmPasswordInput.text.toString()

        // Check if fields are empty
        if (currentPassword.isEmpty()) {
            binding.currentPasswordLayout.error = "Current password is required"
            return false
        }

        if (newPassword.isEmpty()) {
            binding.newPasswordLayout.error = "New password is required"
            return false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.error = "Please confirm your new password"
            return false
        }

        // Check password strength
        val hasMinLength = newPassword.length >= 8
        val hasNumber = newPassword.any { it.isDigit() }
        val hasSpecial = newPassword.any { !it.isLetterOrDigit() }

        if (!hasMinLength || !hasNumber || !hasSpecial) {
            binding.newPasswordLayout.error = "Password must be at least 8 characters with a number and special character"
            return false
        }

        // Check if passwords match
        if (newPassword != confirmPassword) {
            binding.confirmPasswordLayout.error = "Passwords do not match"
            return false
        }

        // Check if new password is different from current
        if (currentPassword == newPassword) {
            binding.newPasswordLayout.error = "New password must be different from current password"
            return false
        }

        return true
    }

    private fun changePassword() {
        if (currentUser == null) {
            showMessage("You must be logged in to change your password")
            return
        }

        val currentPassword = binding.currentPasswordInput.text.toString()
        val newPassword = binding.newPasswordInput.text.toString()

        // Show loading state
        setLoadingState(true)

        // Re-authenticate user before changing password
        val credential = EmailAuthProvider.getCredential(currentUser!!.email!!, currentPassword)

        currentUser!!.reauthenticate(credential)
            .addOnSuccessListener {
                // Authentication successful, now change password
                currentUser!!.updatePassword(newPassword)
                    .addOnSuccessListener {
                        setLoadingState(false)
                        showMessage("Password updated successfully")
                        // Navigate back after short delay
                        binding.root.postDelayed({
                            findNavController().navigateUp()
                        }, 1500)
                    }
                    .addOnFailureListener { exception ->
                        setLoadingState(false)
                        showMessage("Failed to update password: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                setLoadingState(false)
                binding.currentPasswordLayout.error = "Current password is incorrect"
                showMessage("Authentication failed: ${exception.message}")
            }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.saveButton.isEnabled = false
            binding.saveButton.text = "Updating..."
            binding.cancelButton.isEnabled = false
        } else {
            binding.saveButton.isEnabled = true
            binding.saveButton.text = "Save Changes"
            binding.cancelButton.isEnabled = true
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

