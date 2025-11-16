package org.nevis.search.service

import org.nevis.search.domain.dto.search.BasesSearchResultItem
import org.nevis.search.domain.dto.search.ClientResult
import org.nevis.search.domain.dto.search.DocumentResult
import org.nevis.search.domain.dto.search.SearchRequest
import org.nevis.search.repository.ClientRepository
import org.nevis.search.repository.DocumentRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.text.take

@Service
class HybridSearchService(
    private val clientRepository: ClientRepository,
    private val documentRepository: DocumentRepository,
    private val embeddingService: OpenAIService,
    private val summarizingService: SummarizingService
) {
    fun search(request: SearchRequest): List<BasesSearchResultItem> {
        val results = mutableListOf<BasesSearchResultItem>()

        // Entity-specific search strategies
        //TODO maybe add strategy pattern and create wrappers for custom behaviour of search
        //TODO Idea: sort by score + createdAt -> relevant + recent

        //Clients
        results.addAll(searchClients(request.query, request.limit))

        //Documents
        val documents = searchDocuments(request.query, request.limit)

        // Asynchronously generate summaries for documents that don't have one
        //TODO remove filter, added for less count of queries to OPEN AI
        documents.filter { it.score > 0.5 }.forEach { documentResult ->
            if (documentResult.summary == null) {
                // Launch async summarization without blocking
                summarizingService.summarizeDocumentAsync(documentResult.id)
            }
        }

        results.addAll(documents)

        //Collect most relevant
        return results
            .sortedByDescending { it.score }
            .take(request.limit)
    }

    /**
     * CLIENT SEARCH STRATEGY: Trigram-based fuzzy matching
     * Best for: name variations, typos, company names
     * Example: "nikita insurance" matches "Nikita Petrov @ Alliance Insurance"
     */
    private fun searchClients(query: String, limit: Int): List<ClientResult> {
        // Use trigram similarity for fuzzy name/company matching
        return clientRepository.findByTrigramSimilarity(query, limit)
            .map { parseClientTrigramResult(it) }
    }

    /**
     * DOCUMENT SEARCH STRATEGY: Vector/semantic search + Full-text
     * Best for: conceptual queries, synonyms, context understanding + exact keyword matches
     * Example: "address proof" finds documents mentioning "utility bill" (vector) + exact matches (FTS)
     */
    private fun searchDocuments(query: String, limit: Int): List<DocumentResult> {
        val queryEmbedding = embeddingService.generateEmbedding(query)
        val embeddingString = "[${queryEmbedding.joinToString(",")}]"

        // Use vector similarity for semantic/conceptual matching
        val vectorResults = documentRepository.findByVectorSimilarity(embeddingString, limit)
            .map { parseDocumentVectorResult(it) }

        // Complement with full-text search for exact keyword matches
        val fulltextResults = try {
            documentRepository.fullTextSearch(query, limit)
                .map { parseDocumentFulltextResult(it) }
        } catch (e: Exception) {
            emptyList()
        }

        // Merge results: prioritize vector (better for semantic search) but boost exact matches
        return mergeDocumentResults(vectorResults, fulltextResults)
    }

    private fun parseClientTrigramResult(row: Array<Any>): ClientResult {
        val id = UUID.fromString(row[0].toString())
        val similarity = (row[5] as Number).toDouble()

        return ClientResult(
            id = id,
            score = similarity,
            email = row[1].toString(),
            firstName = row[2].toString(),
            lastName = row[3].toString(),
            countryOfResidence = row[4].toString()
        )
    }

    private fun parseDocumentVectorResult(row: Array<Any>): DocumentResult {
        val content = row[3].toString()
        val summary = row[4] as? String
        val rank = (row[5] as Number).toDouble()

        return DocumentResult(
            id = UUID.fromString(row[0].toString()),
            score = rank,
            clientId = UUID.fromString(row[1].toString()),
            title = row[2].toString(),
            content = content.take(200) + if (content.length > 200) "..." else "",
            summary = summary
        )
    }

    private fun parseDocumentFulltextResult(row: Array<Any>): DocumentResult {
        val content = row[3].toString()
        val summary = row[4] as? String
        val rank = (row[5] as Number).toDouble()

        return DocumentResult(
            id = UUID.fromString(row[0].toString()),
            score = normalizeFullTextScore(rank),
            clientId = UUID.fromString(row[1].toString()),
            title = row[2].toString(),
            content = content.take(200) + if (content.length > 200) "..." else "",
            summary = summary
        )
    }

    /**
     * Merge document results with strategy:
     * - 80% weight to vector (better for semantic/conceptual search)
     * - 20% weight to fulltext (catches exact keyword matches)
     */
    private fun mergeDocumentResults(
        vectorResults: List<DocumentResult>,
        fulltextResults: List<DocumentResult>
    ): List<DocumentResult> {
        val resultMap = mutableMapOf<UUID, DocumentResult>()

        // Add vector results with higher weight (primary strategy)
        vectorResults.forEach { result ->
            resultMap[result.id] = result.copy(score = result.score * 0.8)
        }

        // Boost with fulltext matches (secondary strategy)
        fulltextResults.forEach { result ->
            val existing = resultMap[result.id]
            if (existing != null) {
                // If already found by vector, boost the score
                resultMap[result.id] = existing.copy(
                    score = existing.score + (result.score * 0.2)
                )
            } else {
                // Only fulltext match (e.g., exact keyword match)
                resultMap[result.id] = result.copy(score = result.score * 0.5)
            }
        }

        return resultMap.values.toList()
    }

    private fun normalizeFullTextScore(score: Double): Double {
        // Normalize ts_rank scores to 0-1 range
        return (score / (score + 1.0)).coerceIn(0.0, 1.0)
    }
}

