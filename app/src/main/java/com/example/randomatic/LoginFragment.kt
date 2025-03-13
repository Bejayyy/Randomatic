package com.example.randomatic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        //        if (auth.currentUser != null) {
        //            auth.signOut() // Force logout
        //        }


        binding.signInButton.setOnClickListener {
            val email = binding.usernameInput.text.toString().trim()
            val password = binding.passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                showToast("Email is required")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showToast("Password is required")
                return@setOnClickListener
            }

            // Set loading state before authentication
            setLoadingState(true)

            signIn(email, password)
        }

        binding.forgotPassword.setOnClickListener {
            it.findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
        binding.signUpLink.setOnClickListener{
            it.findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
        return binding.root
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                // Reset loading state after authentication completes
                setLoadingState(false)

                if (task.isSuccessful) {
                    val user = auth.currentUser
                    fetchUserData(user?.uid)
                    navigateToHomePage()
                } else {
                    showToast("Authentication failed. Please check your credentials.")
                }
            }
    }

    private fun fetchUserData(uid: String?) {
        if (uid == null) return

        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        showToast("Welcome back, ${user.email}")
                    }
                } else {
                    showToast("User data not found.")
                }
            }
            .addOnFailureListener {
                showToast("Failed to fetch user data.")
            }
    }

    /**
     * Sets the loading state for the sign-in button
     * @param isLoading Whether the authentication is in progress
     */
    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            // Disable button and change text to show loading
            binding.signInButton.isEnabled = false
            binding.signInButton.text = "Signing in..."

            // Optionally disable other interactive elements
            binding.usernameInput.isEnabled = false
            binding.passwordInput.isEnabled = false
            binding.signUpLink.isEnabled = false
        } else {
            // Re-enable button and restore original text
            binding.signInButton.isEnabled = true
            binding.signInButton.text = "Sign In"

            // Re-enable other interactive elements
            binding.usernameInput.isEnabled = true
            binding.passwordInput.isEnabled = true
            binding.signUpLink.isEnabled = true
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToHomePage() {
        view?.findNavController()?.navigate(R.id.action_loginFragment_to_homePage)
    }

    data class User(val email: String = "")
}

