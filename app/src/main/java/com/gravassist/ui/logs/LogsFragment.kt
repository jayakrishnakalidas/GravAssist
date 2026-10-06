package com.gravassist.ui.logs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gravassist.databinding.FragmentLogsBinding
import com.gravassist.services.LoggerService

class LogsFragment : Fragment() {

    private var _binding: FragmentLogsBinding? = null
    private val binding get() = _binding!!

    private lateinit var logAdapter: LogAdapter
    private val logListener = {
        activity?.runOnUiThread {
            logAdapter.updateLogs(LoggerService.getLogs())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        logAdapter = LogAdapter()
        binding.rvLogs.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogs.adapter = logAdapter

        logAdapter.updateLogs(LoggerService.getLogs())
        LoggerService.addListener(logListener)

        binding.btnClearLogs.setOnClickListener {
            LoggerService.clearLogs()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LoggerService.removeListener(logListener)
        _binding = null
    }
}
