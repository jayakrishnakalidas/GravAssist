package com.gravassist.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gravassist.databinding.FragmentProfileBinding
import com.gravassist.utils.PreferenceManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefManager = PreferenceManager(requireContext())
        val rawKey = prefManager.getApiKey()

        val maskedKey = if (rawKey.length > 8) {
            "Key: ${rawKey.substring(0, 4)}••••••••${rawKey.substring(rawKey.length - 4)}"
        } else {
            "Key: ••••••••"
        }
        binding.tvMaskedKey.text = maskedKey
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
