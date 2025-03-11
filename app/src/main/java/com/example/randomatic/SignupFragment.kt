package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {
    private lateinit var binding: FragmentSignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = inflater.inflate(R.layout.fragment_signup, container, false)

        // Initialize Firebase Auth & Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val signUpButton = binding.findViewById<View>(R.id.SignupsignInButton)
        val usernameInput = binding.findViewById<EditText>(R.id.SignupusernameInput)
        val passwordInput = binding.findViewById<EditText>(R.id.SignuppasswordInput)
        val confirmPasswordInput = binding.findViewById<EditText>(R.id.SignuppasswordInputconfirmpassword)
        val signInLink = binding.findViewById<View>(R.id.signInLink)

        signInLink.setOnClickListener {
            it.findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        signUpButton.setOnClickListener {
            val email = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (validateInputs(email, password, confirmPassword, usernameInput, passwordInput, confirmPasswordInput)) {
                // Create user with email and password
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
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
        confirmPassword: String,
        usernameInput: EditText,
        passwordInput: EditText,
        confirmPasswordInput: EditText
    ): Boolean {
        if (email.isEmpty()) {
            usernameInput.error = "Email is required"
            return false
        }
        if (password.isEmpty()) {
            passwordInput.error = "Password is required"
            return false
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.error = "Please confirm your password"
            return false
        }
        if (password != confirmPassword) {
            confirmPasswordInput.error = "Passwords do not match"
            return false
        }
        return true
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
