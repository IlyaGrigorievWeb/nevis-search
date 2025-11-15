package org.nevis.search.service

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Service
class OpenAIEmbeddingService(
    @Value("\${openai.api-key}") private val apiKey: String,
    @Value("\${openai.embedding-model}") private val model: String = "text-embedding-3-small"
) {
    private val client = OkHttpClient()
    private val objectMapper = ObjectMapper()
    private val jsonMediaType = "application/json".toMediaType()
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class EmbeddingRequest(
        val model: String,
        val input: String,
        val encoding_format: String
    )

    @Serializable
    data class EmbeddingResponse(
        @SerialName("object")
        val obj: String,

        @SerialName("data")
        val data: List<EmbeddingData>,

        @SerialName("model")
        val model: String,

        @SerialName("usage")
        val usage: Usage
    )

    @Serializable
    data class EmbeddingData(
        @SerialName("object")
        val obj: String,

        @SerialName("index")
        val index: Int,

        @SerialName("embedding")
        val embedding: List<Double>
    )

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens")
        val prompt_tokens: Int,

        @SerialName("total_tokens")
        val total_tokens: Int
    )

    fun generateEmbedding(text: String): FloatArray {
        val requestBody = EmbeddingRequest(
            model = model,
            input = text.take(8000),  // Limit input size
            encoding_format = "float"
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/embeddings")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("OpenAI API error: ${response.code}")
            }

            val embeddingResponse = json.decodeFromString<EmbeddingResponse>(response.body?.string()!!)

            return embeddingResponse.data.first().embedding
                .map { it.toFloat() }
                .toFloatArray()
        }
    }

    fun generateEmbeddingBatch(texts: List<String>): List<FloatArray> {
        return texts.map { generateEmbedding(it) }
    }
}

