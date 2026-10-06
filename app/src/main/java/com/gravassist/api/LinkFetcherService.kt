package com.gravassist.api

import com.gravassist.services.LoggerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object LinkFetcherService {
    private const val PHP_ENDPOINT = "http://aicontroller.lovestoblog.com/ai_link.php"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun fetchCloudflareLink(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(PHP_ENDPOINT)
                .header("User-Agent", "Mozilla/5.0 GravAssist-AndroidApp")
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string()?.trim()

            if (response.isSuccessful && !responseText.isNullOrEmpty()) {
                val formattedUrl = if (!responseText.startsWith("http://") && !responseText.startsWith("https://")) {
                    "https://$responseText"
                } else {
                    responseText
                }
                LoggerService.log("LinkFetcher", "Fetched Tunnel: $formattedUrl")
                formattedUrl
            } else {
                LoggerService.log("LinkFetcher", "Failed to fetch link: HTTP ${response.code}", "FAILED")
                null
            }
        } catch (e: Exception) {
            LoggerService.log("LinkFetcher", "Error fetching link: ${e.localizedMessage}", "FAILED")
            null
        }
    }
}
