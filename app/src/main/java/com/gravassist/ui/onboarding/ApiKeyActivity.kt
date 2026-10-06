package com.gravassist.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gravassist.MainActivity
import com.gravassist.databinding.ActivityApiKeyBinding
import com.gravassist.utils.PreferenceManager

class ApiKeyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiKeyBinding
    private lateinit var prefManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApiKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PreferenceManager(this)

        binding.btnSaveKey.setOnClickListener {
            val key = binding.etApiKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter an API key", Toast.LENGTH_SHORT).show()
            } else {
                prefManager.saveApiKey(key)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}
