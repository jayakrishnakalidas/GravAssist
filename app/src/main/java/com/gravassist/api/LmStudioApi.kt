package com.gravassist.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface LmStudioApi {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>

    @GET("v1/models")
    suspend fun getModels(
        @Header("Authorization") authHeader: String
    ): Response<ModelsResponse>
}
