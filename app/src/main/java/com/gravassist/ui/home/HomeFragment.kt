package com.gravassist.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gravassist.actions.AlarmAction
import com.gravassist.actions.AppLaunchAction
import com.gravassist.actions.CallAction
import com.gravassist.actions.TorchAction
import com.gravassist.actions.VolumeAction
import com.gravassist.ai.IntentParser
import com.gravassist.ai.ParsedResult
import com.gravassist.ai.SystemPrompts
import com.gravassist.api.ApiClient
import com.gravassist.api.ChatCompletionRequest
import com.gravassist.api.ChatMessageDto
import com.gravassist.databinding.FragmentHomeBinding
import com.gravassist.services.AdbAccessibilityService
import com.gravassist.services.LoggerService
import com.gravassist.utils.PreferenceManager
import com.gravassist.voice.SpeechInput
import com.gravassist.voice.TextToSpeechEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var prefManager: PreferenceManager
    private lateinit var ttsEngine: TextToSpeechEngine
    private var speechInput: SpeechInput? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefManager = PreferenceManager(requireContext())
        ttsEngine = TextToSpeechEngine(requireContext())

        chatAdapter = ChatAdapter()
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = chatAdapter

        // Initial welcome message
        chatAdapter.addMessage(ChatMessage("ai", "Hello! I am GravAssist. How can I help you today?"))

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessage.setText("")
            }
        }

        speechInput = SpeechInput(requireContext(), onResult = { text ->
            binding.tvVoiceStatus.text = "Recognized: $text"
            sendMessage(text)
        }, onError = { err ->
            binding.tvVoiceStatus.text = err
        })

        binding.btnMic.setOnClickListener {
            binding.tvVoiceStatus.text = "Listening..."
            speechInput?.startListening(requireContext())
        }
    }

    private fun sendMessage(userText: String) {
        chatAdapter.addMessage(ChatMessage("user", userText))
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)

        val tunnelUrl = prefManager.getCachedTunnelUrl()
        val apiKey = prefManager.getApiKey()

        if (tunnelUrl.isEmpty()) {
            val errorMsg = "Cloudflare Tunnel URL not ready yet."
            chatAdapter.addMessage(ChatMessage("ai", errorMsg))
            ttsEngine.speak(errorMsg)
            return
        }

        lifecycleScope.launch {
            try {
                val api = ApiClient.getService(tunnelUrl)
                val messages = listOf(
                    ChatMessageDto("system", SystemPrompts.ASSISTANT_SYSTEM_PROMPT),
                    ChatMessageDto("user", userText)
                )
                val model = prefManager.getSelectedModel()
                val request = ChatCompletionRequest(model = model, messages = messages)

                val response = withContext(Dispatchers.IO) {
                    api.createChatCompletion("Bearer $apiKey", request)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val rawAiText = body.choices.firstOrNull()?.message?.content ?: ""

                    // Record Token Usage
                    body.usage?.let { u ->
                        prefManager.recordUsage(u.promptTokens, u.completionTokens)
                    }

                    // Parse Intent
                    val result = IntentParser.parse(rawAiText)
                    handleParsedResult(result)
                } else {
                    val errorStr = "Error from server: HTTP ${response.code()}"
                    chatAdapter.addMessage(ChatMessage("ai", errorStr))
                    ttsEngine.speak(errorStr)
                }
            } catch (e: Exception) {
                val errorStr = "Network connection error: ${e.localizedMessage}"
                chatAdapter.addMessage(ChatMessage("ai", errorStr))
                ttsEngine.speak("Unable to connect to AI server.")
            }
        }
    }

    private fun handleParsedResult(result: ParsedResult) {
        when (result) {
            is ParsedResult.ActionIntent -> {
                executeAction(result.actionName, result.params)
                chatAdapter.addMessage(ChatMessage("ai", result.spokenResponse))
                ttsEngine.speak(result.spokenResponse)
            }
            is ParsedResult.ChatIntent -> {
                chatAdapter.addMessage(ChatMessage("ai", result.spokenResponse))
                ttsEngine.speak(result.spokenResponse)
            }
        }
        binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
    }

    private fun executeAction(actionName: String, params: com.google.gson.JsonObject?) {
        val ctx = requireContext()
        when (actionName.lowercase()) {
            "turn_torch" -> {
                val state = params?.get("state")?.asString ?: "on"
                TorchAction.execute(ctx, state.equals("on", ignoreCase = true))
            }
            "set_alarm" -> {
                val hour = params?.get("hour")?.asInt ?: 7
                val minute = params?.get("minute")?.asInt ?: 0
                val label = params?.get("label")?.asString ?: "Alarm"
                AlarmAction.setAlarm(ctx, hour, minute, label)
            }
            "set_timer" -> {
                val seconds = params?.get("seconds")?.asInt ?: 300
                val label = params?.get("label")?.asString ?: "Timer"
                AlarmAction.setTimer(ctx, seconds, label)
            }
            "set_volume" -> {
                val level = params?.get("level")?.asString ?: "up"
                VolumeAction.adjustVolume(ctx, level)
            }
            "make_call" -> {
                val phone = params?.get("phone_number")?.asString ?: ""
                if (phone.isNotEmpty()) CallAction.makeCall(ctx, phone)
            }
            "send_sms" -> {
                val phone = params?.get("phone_number")?.asString ?: ""
                val msg = params?.get("message")?.asString ?: ""
                if (phone.isNotEmpty()) CallAction.sendSms(ctx, phone, msg)
            }
            "open_app" -> {
                val app = params?.get("app_name")?.asString ?: ""
                if (app.isNotEmpty()) AppLaunchAction.launchApp(ctx, app)
            }
            "adb_touch" -> {
                val x = params?.get("x")?.asFloat ?: 500f
                val y = params?.get("y")?.asFloat ?: 1000f
                AdbAccessibilityService.instance?.performClick(x, y)
            }
            "adb_input" -> {
                val text = params?.get("text")?.asString ?: ""
                if (text.isNotEmpty()) AdbAccessibilityService.instance?.inputText(text)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechInput?.destroy()
        ttsEngine.shutdown()
        _binding = null
    }
}
