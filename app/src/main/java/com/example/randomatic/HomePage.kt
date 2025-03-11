package com.example.randomatic

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentHomePageBinding
import com.example.randomatic.databinding.FragmentLoginBinding


class HomePage : Fragment() {
    private lateinit var binding: FragmentHomePageBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentHomePageBinding.inflate(inflater, container, false)

        binding.soloSpinContainer.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_soloSpin)
        }

        binding.squadShuffleButton.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_squadShuffle)
        }
        binding.soloSpinButton.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_soloSpin)
        }

        binding.squadShuffleContainer.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_squadShuffle)
        }

        binding.listManagerContainer.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_listManager)
        }

        binding.historyLogContainer.setOnClickListener{
            it.findNavController().navigate(R.id.action_homePage_to_historyLog)
        }

        return binding.root
    }
}
