package org.nevis.search.service

import java.util.UUID

object TestDataFactory {

    /**
     * Creates a mock trigram result array as returned by ClientRepository.findByTrigramSimilarity
     * Format: [id, email, firstName, lastName, countryOfResidence, similarity]
     */
    fun createTrigramResult(
        id: UUID,
        email: String = "${id.toString().take(8)}@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        country: String = "USA",
        similarity: Double = 0.5
    ): Array<Any> {
        return arrayOf(
            id.toString(),
            email,
            firstName,
            lastName,
            country,
            similarity
        )
    }

    /**
     * Creates a mock fulltext result array as returned by DocumentRepository.fullTextSearch
     * Format: [id, clientId, title, content, summary, rank]
     */
    fun createDocumentFulltextResult(
        id: UUID,
        clientId: UUID,
        title: String = "Test Document",
        content: String = "Test content",
        summary: String = "",
        rank: Double = 1.0
    ): Array<Any> {
        return arrayOf(
            id.toString(),
            clientId.toString(),
            title,
            content,
            summary,
            rank
        )
    }

    /**
     * Creates a mock vector similarity result array as returned by DocumentRepository.findByVectorSimilarity
     * Format: [id, clientId, title, content, summary, similarity]
     */
    fun createDocumentVectorResult(
        id: UUID,
        clientId: UUID,
        title: String = "Test Document",
        content: String = "Test content",
        summary: String = "",
        similarity: Double = 0.5
    ): Array<Any> {
        return arrayOf(
            id.toString(),
            clientId.toString(),
            title,
            content,
            summary,
            similarity
        )
    }

    /**
     * Creates a default embedding vector (1536 dimensions, all zeros)
     */
    fun createDefaultEmbedding(): FloatArray {
        return FloatArray(1536) { 0.0f }
    }

    /**
     * Helper to calculate expected merged score (vector + fulltext)
     */
    fun calculateMergedScore(
        vectorScore: Double?,
        fulltextRank: Double?
    ): Double {
        return when {
            vectorScore != null && fulltextRank != null -> {
                // Both found: vector * 0.8 + normalized(fulltext) * 0.2
                (vectorScore * 0.8) + (normalizeFullTextScore(fulltextRank) * 0.2)
            }
            vectorScore != null -> {
                // Only vector: score * 0.8
                vectorScore * 0.8
            }
            fulltextRank != null -> {
                // Only fulltext: normalized(score) * 0.5
                normalizeFullTextScore(fulltextRank) * 0.5
            }
            else -> 0.0
        }
    }

    /**
     * Normalize fulltext score (replicates HybridSearchService logic)
     */
    private fun normalizeFullTextScore(score: Double): Double {
        return (score / (score + 1.0)).coerceIn(0.0, 1.0)
    }
}

