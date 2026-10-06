package com.gravassist.ui.usage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gravassist.databinding.FragmentUsageBinding
import com.gravassist.utils.PreferenceManager

class UsageFragment : Fragment() {

    private var _binding: FragmentUsageBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PreferenceManager(requireContext())

        updateUI()

        binding.btnResetUsage.setOnClickListener {
            prefManager.resetUsage()
            updateUI()
            Toast.makeText(requireContext(), "Usage statistics reset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        val requests = prefManager.getRequestCount()
        val promptTokens = prefManager.getPromptTokens()
        val completionTokens = prefManager.getCompletionTokens()
        val totalTokens = promptTokens + completionTokens

        binding.tvTotalRequests.text = requests.toString()
        binding.tvTotalTokens.text = totalTokens.toString()
        binding.tvPromptTokens.text = "Prompt Tokens: $promptTokens"
        binding.tvCompletionTokens.text = "Completion Tokens: $completionTokens"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
