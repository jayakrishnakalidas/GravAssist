package com.gravassist.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.gravassist.R
import com.gravassist.api.ApiClient
import com.gravassist.databinding.FragmentSettingsBinding
import com.gravassist.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefManager: PreferenceManager
    private var isUnlocked = false
    private val modelsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PreferenceManager(requireContext())
        binding.etSettingsApiKey.setText(prefManager.getApiKey())

        binding.btnUpdateApiKey.setOnClickListener {
            val key = binding.etSettingsApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                prefManager.saveApiKey(key)
                Toast.makeText(requireContext(), "API Key updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnUnlockModel.setOnClickListener {
            val dialog = UnlockCodeDialog {
                unlockModelSelector()
            }
            dialog.show(childFragmentManager, "UnlockDialog")
        }

        checkConnectionStatus()
    }

    private fun checkConnectionStatus() {
        val cachedUrl = prefManager.getCachedTunnelUrl()
        if (cachedUrl.isEmpty()) {
            binding.tvCloudEndpoint.text = "No Cloudflare link found yet"
            binding.tvPingStatus.text = "Offline"
            binding.viewStatusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_dot_red)
            return
        }

        binding.tvCloudEndpoint.text = "Endpoint: $cachedUrl"
        binding.tvPingStatus.text = "Pinging..."

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(cachedUrl)
                val response = withContext(Dispatchers.IO) {
                    api.getModels("Bearer ${prefManager.getApiKey()}")
                }

                if (response.isSuccessful && response.body() != null) {
                    binding.tvPingStatus.text = "Online (Connected)"
                    binding.viewStatusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_dot_green)

                    // Store models list
                    response.body()?.data?.map { it.id }?.let { fetchedModels ->
                        modelsList.clear()
                        modelsList.addAll(fetchedModels)
                        if (isUnlocked) {
                            populateModelSpinner()
                        }
                    }
                } else {
                    binding.tvPingStatus.text = "Offline (HTTP ${response.code()})"
                    binding.viewStatusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_dot_red)
                }
            } catch (e: Exception) {
                binding.tvPingStatus.text = "Offline (${e.localizedMessage})"
                binding.viewStatusDot.background = ContextCompat.getDrawable(requireContext(), R.drawable.shape_dot_red)
            }
        }
    }

    private fun unlockModelSelector() {
        isUnlocked = true
        binding.spinnerModels.isEnabled = true
        binding.tvLockStatusHint.text = getString(R.string.model_unlocked)
        binding.tvLockStatusHint.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_green))
        binding.btnUnlockModel.visibility = View.GONE

        Toast.makeText(requireContext(), getString(R.string.model_unlocked), Toast.LENGTH_SHORT).show()

        if (modelsList.isNotEmpty()) {
            populateModelSpinner()
        } else {
            checkConnectionStatus()
        }
    }

    private fun populateModelSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modelsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerModels.adapter = adapter

        val currentSelected = prefManager.getSelectedModel()
        val index = modelsList.indexOf(currentSelected)
        if (index >= 0) {
            binding.spinnerModels.setSelection(index)
        }

        binding.spinnerModels.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = modelsList[position]
                prefManager.saveSelectedModel(selected)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
