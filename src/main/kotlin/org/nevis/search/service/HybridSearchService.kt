package org.nevis.search.service

import org.nevis.search.domain.dto.search.BasesSearchResultItem
import org.nevis.search.domain.dto.search.ClientResult
import org.nevis.search.domain.dto.search.DocumentResult
import org.nevis.search.domain.dto.search.SearchRequest
import org.nevis.search.repository.ClientRepository
import org.nevis.search.repository.DocumentRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HybridSearchService(
    private val clientRepository: ClientRepository,
    private val documentRepository: DocumentRepository,
    private val embeddingService: OpenAIEmbeddingService
) {
    companion object {
        const val FILTERING_SCORE = 0.3;  //TODO move to config
    }

    fun search(request: SearchRequest): List<BasesSearchResultItem> {
        val results = mutableListOf<BasesSearchResultItem>()

        // Entity-specific search strategies
        //TODO maybe add strategy pattern and cre wrappers for custom behaviour of search

        //Clients
        results.addAll(searchClients(request.query, request.limit))

        //Documents
        results.addAll(searchDocuments(request.query, request.limit))

        //Collect most relevant
        return results
            .filter { it.score > FILTERING_SCORE }
            .sortedByDescending { it.score }
            .take(request.limit)
    }

    /**
     * CLIENT SEARCH STRATEGY: Trigram-based fuzzy matching + Full-text
     * Best for: name variations, typos, company names
     * Example: "nikita insurance" matches "Nikita Petrov @ Alliance Insurance"
     */
    private fun searchClients(query: String, limit: Int): List<ClientResult> {
        // Use trigram similarity for fuzzy name/company matching
        val trigramResults = clientRepository.findByTrigramSimilarity(query, limit)
            .map { parseClientTrigramResult(it) }

        //TODO fulltext search is overkill for expected user search
        // Complement with full-text search for exact email/keyword matches
        val fulltextResults = try {
            clientRepository.fullTextSearch(query, limit)
                .map { parseClientFulltextResult(it) }
        } catch (e: Exception) {
            emptyList()
        }

        // Merge results: prioritize trigram (better for names) but boost exact matches
        return mergeClientResults(trigramResults, fulltextResults)
    }

    /**
     * DOCUMENT SEARCH STRATEGY: Pure vector/semantic search
     * Best for: conceptual queries, synonyms, context understanding
     * Example: "address proof" finds documents mentioning "utility bill"
     */
    private fun searchDocuments(query: String, limit: Int): List<DocumentResult> {
        val queryEmbedding = embeddingService.generateEmbedding(query)
        val embeddingString = "[${queryEmbedding.joinToString(",")}]"

        return documentRepository.findByVectorSimilarity(embeddingString, limit)
            .map { parseDocumentVectorResult(it) }
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

    private fun parseClientFulltextResult(row: Array<Any>): ClientResult {
        val id = UUID.fromString(row[0].toString())
        val rank = (row[4] as Number).toDouble()

        return ClientResult(
            id = id,
            score = normalizeFullTextScore(rank),

            email = row[1].toString(),
            firstName = row[2].toString(),
            lastName = row[3].toString(),
            countryOfResidence = row[5].toString()
        )
    }

    private fun parseDocumentVectorResult(row: Array<Any>): DocumentResult {
        val content = row[3].toString()
        val rank = (row[4] as Number).toDouble()

        return DocumentResult(
            id = UUID.fromString(row[0].toString()),
            score = rank,

            clientId = UUID.fromString(row[1].toString()),
            title = row[2].toString(),
            content = content.take(200) + if (content.length > 200) "..." else ""
        )
    }

    /**
     * Merge client results with strategy:
     * - 80% weight to trigram (better for name matching)
     * - 20% weight to fulltext (catches exact email matches)
     */
    private fun mergeClientResults(
        trigramResults: List<ClientResult>,
        fulltextResults: List<ClientResult>
    ): List<ClientResult> {
        val resultMap = mutableMapOf<UUID, ClientResult>()

        // Add trigram results with higher weight (primary strategy)
        trigramResults.forEach { result ->
            resultMap[result.id] = result.copy(score = result.score * 0.8)
        }

        // Boost with fulltext matches (secondary strategy)
        fulltextResults.forEach { result ->
            val existing = resultMap[result.id]
            if (existing != null) {
                // If already found by trigram, boost the score
                resultMap[result.id] = existing.copy(
                    score = existing.score + (result.score * 0.2)
                )
            } else {
                // Only fulltext match (e.g., exact email match)
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

