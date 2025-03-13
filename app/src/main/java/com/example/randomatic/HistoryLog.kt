package com.example.randomatic

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.example.randomatic.databinding.FragmentHistoryLogBinding

class HistoryLog : Fragment() {

    private  lateinit var binding : FragmentHistoryLogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHistoryLogBinding.inflate(inflater, container, false)

        binding.soloSpinContainer.setOnClickListener{
            it.findNavController().navigate(R.id.action_historyLog_to_soloSpinHistoryList)
        }
        binding.backArrow.setOnClickListener {
            it.findNavController().navigate(R.id.action_historyLog_to_homePage)
        }

        binding.squadShuffleContainer.setOnClickListener {
            it.findNavController().navigate(R.id.action_historyLog_to_squadShuffleList)
        }

        return binding.root
    }



}