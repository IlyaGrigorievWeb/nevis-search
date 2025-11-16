package org.nevis.search.service

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.*
import org.nevis.search.repository.DocumentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SummarizingService(
    private val documentRepository: DocumentRepository,
    private val openAIService: OpenAIService
) {
    private val logger = LoggerFactory.getLogger(SummarizingService::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @PreDestroy
    fun cleanup() {
        coroutineScope.cancel()
    }

    /**
     * Asynchronously generate and update document summary by documentId
     * This is a suspend function that can be called from coroutine context
     */
    suspend fun summarizeDocument(documentId: UUID): String = withContext(Dispatchers.IO) {
        try {
            val document = documentRepository.findById(documentId).orElse(null)
                ?: throw IllegalArgumentException("Document with id $documentId not found")

            // Generate summary using OpenAI
            val textToSummarize = "${document.title}\n\n${document.content}"
            val summary = openAIService.summarizeText(textToSummarize)

            // Update document with summary
            val updatedDocument = document.copy(summary = summary)
            documentRepository.save(updatedDocument)

            logger.info("Successfully generated and saved summary for document $documentId")
            summary
        } catch (e: Exception) {
            logger.error("Error generating summary for document $documentId", e)
            throw e
        }
    }

    /**
     * Launch async summarization task without blocking
     * This can be called from non-suspend contexts
     */
    fun summarizeDocumentAsync(documentId: UUID) {
        coroutineScope.launch {
            try {
                summarizeDocument(documentId)
            } catch (e: Exception) {
                logger.error("Failed to summarize document $documentId asynchronously", e)
            }
        }
    }
}

