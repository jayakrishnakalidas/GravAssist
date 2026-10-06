package com.gravassist.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private var currentBaseUrl: String? = null
    private var apiService: LmStudioApi? = null

    fun getService(baseUrl: String): LmStudioApi {
        val sanitizedUrl = if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl
        if (apiService == null || currentBaseUrl != sanitizedUrl) {
            currentBaseUrl = sanitizedUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(sanitizedUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(LmStudioApi::class.java)
        }
        return apiService!!
    }
}
