package com.gravassist.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.gravassist.services.LoggerService

sealed class ParsedResult {
    data class ActionIntent(
        val actionName: String,
        val params: JsonObject?,
        val spokenResponse: String
    ) : ParsedResult()

    data class ChatIntent(
        val spokenResponse: String
    ) : ParsedResult()
}

object IntentParser {
    private val gson = Gson()

    fun parse(rawResponse: String): ParsedResult {
        return try {
            val cleaned = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val json = gson.fromJson(cleaned, JsonObject::class.java)
            val type = json.get("type")?.asString ?: "chat"
            val spokenResponse = json.get("spoken_response")?.asString ?: "Done."

            if (type == "action") {
                val actionName = json.get("action")?.asString ?: ""
                val params = json.getAsJsonObject("params")
                LoggerService.log("IntentParser", "Parsed action: $actionName")
                ParsedResult.ActionIntent(actionName, params, spokenResponse)
            } else {
                ParsedResult.ChatIntent(spokenResponse)
            }
        } catch (e: Exception) {
            LoggerService.log("IntentParser", "Raw response parse fallback: ${e.localizedMessage}", "WARNING")
            ParsedResult.ChatIntent(rawResponse)
        }
    }
}
