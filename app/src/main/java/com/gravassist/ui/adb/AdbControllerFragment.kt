package com.gravassist.ui.adb

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gravassist.R
import com.gravassist.databinding.FragmentAdbBinding
import com.gravassist.services.AdbAccessibilityService
import com.gravassist.utils.PreferenceManager

class AdbControllerFragment : Fragment() {

    private var _binding: FragmentAdbBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdbBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())

        binding.switchAdbController.isChecked = prefManager.isAdbEnabled()

        binding.switchAdbController.setOnCheckedChangeListener { _, isChecked ->
            prefManager.setAdbEnabled(isChecked)
        }

        binding.btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        updateAccessibilityStatus()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }

    private fun updateAccessibilityStatus() {
        if (AdbAccessibilityService.isRunning) {
            binding.tvAccessibilityServiceState.text = "Accessibility Service: Active (Enabled)"
            binding.tvAccessibilityServiceState.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
        } else {
            binding.tvAccessibilityServiceState.text = "Accessibility Service: Disabled"
            binding.tvAccessibilityServiceState.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_red))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
