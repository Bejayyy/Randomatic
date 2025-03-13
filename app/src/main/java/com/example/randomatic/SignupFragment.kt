package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI elements
    private lateinit var signUpButton: Button
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signInLink: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_signup, container, false)

        // Initialize Firebase Auth & Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        signUpButton = binding.findViewById(R.id.SignupsignInButton)
        usernameInput = binding.findViewById(R.id.SignupusernameInput)
        passwordInput = binding.findViewById(R.id.SignuppasswordInput)
        confirmPasswordInput = binding.findViewById(R.id.SignuppasswordInputconfirmpassword)
        signInLink = binding.findViewById(R.id.signInLink)

        signInLink.setOnClickListener {
            it.findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        signUpButton.setOnClickListener {
            val email = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(email, password, confirmPassword)) {
                // Set loading state before creating user
                setLoadingState(true)

                // Create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        // Reset loading state after sign-up completes
                        setLoadingState(false)

                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val uid = user?.uid
                            saveUserData(uid, email)  // Save user data in Firestore
                            Toast.makeText(requireContext(), "Sign-up successful", Toast.LENGTH_SHORT).show()
                            it.findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
                        } else {
                            Toast.makeText(requireContext(), "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return binding
    }

    private fun validateInputs(
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        // Email validation
        if (email.isEmpty()) {
            usernameInput.error = "Email is required"
            return false
        }

        // Password validation
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }

        // Password strength validation
        if (password.length < 8) {
            passwordInput.error = "Password must be at least 8 characters"
            return false
        }

        // Check for at least one number
        if (!password.any { it.isDigit() }) {
            passwordInput.error = "Password must include at least one number"
            return false
        }

        // Check for at least one special character
        if (!password.any { !it.isLetterOrDigit() }) {
            passwordInput.error = "Password must include at least one special character"
            return false
        }

        // Confirm password validation
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your password"
            return false
        }

        // Password matching validation
        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            return false
        }

        return true
    }

    /**
     * Sets the loading state for the sign-up button
     * @param isLoading Whether the sign-up is in progress
     */
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Disable button and change text to show loading
            signUpButton.isEnabled = false
            signUpButton.text = "Creating Account..."

            // Disable input fields and links
            usernameInput.isEnabled = false
            passwordInput.isEnabled = false
            confirmPasswordInput.isEnabled = false
            signInLink.isEnabled = false
        } else {
            // Re-enable button and restore original text
            signUpButton.isEnabled = true
            signUpButton.text = "Sign Up"

            // Re-enable input fields and links
            usernameInput.isEnabled = true
            passwordInput.isEnabled = true
            confirmPasswordInput.isEnabled = true
            signInLink.isEnabled = true
        }
    }

    private fun saveUserData(uid: String?, email: String) {
        if (uid == null) return

        // Firestore reference
        val userRef = firestore.collection("users").document(uid)

        // User data
        val user = User(email)

        userRef.set(user)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "User data saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save user data", Toast.LENGTH_SHORT).show()
            }
    }

    // Data class for the user
    data class User(val email: String)
}

