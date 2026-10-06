package com.gravassist.ui.google

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gravassist.databinding.FragmentGoogleBinding

class GoogleAuthFragment : Fragment() {

    private var _binding: FragmentGoogleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoogleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoogleSignin.setOnClickListener {
            Toast.makeText(requireContext(), "Google Sign-In flow initiated for Gmail, Calendar, Sheets, Drive, and Meet.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
