package com.example.randomatic

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentHistoryLogBinding
import com.example.randomatic.databinding.FragmentSettingsBinding

class Settings : Fragment() {

    private  lateinit var binding : FragmentSettingsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingsBinding.inflate(inflater, container, false)

        binding.changePasswordCard.setOnClickListener{
            it.findNavController().navigate(R.id.action_settings_to_changePassword)
        }

        binding.LogoutLayout.setOnClickListener {
            it.findNavController().navigate(R.id.action_settings_to_loginFragment)
        }

        return binding.root
    }

}