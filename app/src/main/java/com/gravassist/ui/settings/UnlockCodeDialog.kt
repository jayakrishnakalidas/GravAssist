package com.gravassist.ui.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.gravassist.R
import com.gravassist.databinding.DialogUnlockCodeBinding

class UnlockCodeDialog(
    private val onUnlocked: () -> Unit
) : DialogFragment() {

    companion object {
        const val PASSCODE = "6660_0D_F"
    }

    private var _binding: DialogUnlockCodeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUnlockCodeBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(requireContext(), R.style.Theme_GravAssist)
            .setView(binding.root)
            .setPositiveButton(getString(R.string.unlock), null)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveBtn.setOnClickListener {
                val inputCode = binding.etPasscode.text.toString().trim()
                if (inputCode == PASSCODE) {
                    onUnlocked()
                    dismiss()
                } else {
                    binding.tvPasscodeError.visibility = View.VISIBLE
                }
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
