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
class OpenAIService(
    @Value("\${openai.api-key}") private val apiKey: String,
    @Value("\${openai.embedding-model}") private val model: String = "text-embedding-3-small",
    @Value("\${openai.summarization-model:gpt-4o-mini}") private val summarizationModel: String = "gpt-4o-mini"
) {
    private val client = OkHttpClient()
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

    @Serializable
    data class ChatCompletionRequest(
        val model: String,
        val messages: List<Message>
    )

    @Serializable
    data class Message(
        val role: String,
        val content: String
    )

    @Serializable
    data class ChatCompletionResponse(
        val id: String,
        @SerialName("object")
        val obj: String,
        val created: Long,
        val model: String,
        val choices: List<Choice>,
        val usage: ChatUsage
    )

    @Serializable
    data class Choice(
        val index: Int,
        val message: Message,
        @SerialName("finish_reason")
        val finishReason: String
    )

    @Serializable
    data class ChatUsage(
        @SerialName("prompt_tokens")
        val promptTokens: Int,
        @SerialName("completion_tokens")
        val completionTokens: Int,
        @SerialName("total_tokens")
        val totalTokens: Int
    )

    fun summarizeText(text: String): String {
        val requestBody = ChatCompletionRequest(
            model = summarizationModel,
            messages = listOf(
                Message(
                    role = "developer",
                    content =cheapSummarizingPropmpt
                ),
                Message(
                    role = "user",
                    content = text
                )
            )
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("OpenAI API error: ${response.code} - ${response.body?.string()}")
            }

            val completionResponse = json.decodeFromString<ChatCompletionResponse>(response.body?.string()!!)
            return completionResponse.choices.firstOrNull()?.message?.content
                ?: throw RuntimeException("No summary generated from OpenAI API")
        }
    }
    private val cheapSummarizingPropmpt = "You are an analyst at a financial consulting that summarizes text concisely and accurately"
    private val summarizingPropmpt =  """
                            You are an analyst at a financial consulting firm. Your goal is to create a clear and accurate summary of the document provided by the user.

                            Summary Requirements:

                            Identify and clearly state the main topic — what the document is about and what issue, transaction, or situation it describes.

                            Highlight and preserve all important numerical details and factual data, including:

                            transaction amounts (e.g., “transaction value: $3 million”),

                            percentages, rates, KPIs, deadlines, dates, financial metrics,

                            any figures that influence the business or financial context.

                            Explain key terms or concepts if they are relevant for understanding the document (e.g., type of transaction, financial instruments, corporate structure).

                            Use a structured format:

                            Brief Summary

                            Key Figures and Metrics

                            Terms and Definitions

                            Notes or Risks (if mentioned in the document)

                            Do not add information that is not present in the document. Base your summary strictly on the provided content.

                            Response Format:

                            1. Executive Summary (3–5 sentences)

                            2. Key Numbers & Metrics (bullet list)

                            3. Terms & Definitions (if applicable)

                            4. Notes/Risks (if applicable)
                        """.trimIndent()
}

