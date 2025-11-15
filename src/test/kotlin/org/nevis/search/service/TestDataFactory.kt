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
     * Creates a mock fulltext result array as returned by ClientRepository.fullTextSearch
     * Format: [id, email, firstName, lastName, countryOfResidence, rank]
     */
    fun createFulltextResult(
        id: UUID,
        email: String = "${id.toString().take(8)}@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        country: String = "USA",
        rank: Double = 1.0
    ): Array<Any> {
        return arrayOf(
            id.toString(),
            email,
            firstName,
            lastName,
            rank,
            country
        )
    }

    /**
     * Creates a mock vector similarity result array as returned by DocumentRepository.findByVectorSimilarity
     * Format: [id, clientId, title, content, similarity]
     */
    fun createDocumentResult(
        id: UUID,
        clientId: UUID,
        title: String = "Test Document",
        content: String = "Test content",
        similarity: Double = 0.5
    ): Array<Any> {
        return arrayOf(
            id.toString(),
            clientId.toString(),
            title,
            content,
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
     * Helper to calculate expected merged client score
     */
    fun calculateMergedClientScore(
        trigramScore: Double?,
        fulltextScore: Double?
    ): Double {
        return when {
            trigramScore != null && fulltextScore != null -> {
                // Both found: trigram * 0.8 + normalized(fulltext) * 0.2
                (trigramScore * 0.8) + (normalizeFullTextScore(fulltextScore) * 0.2)
            }
            trigramScore != null -> {
                // Only trigram: score * 0.8
                trigramScore * 0.8
            }
            fulltextScore != null -> {
                // Only fulltext: normalized(score) * 0.5
                normalizeFullTextScore(fulltextScore) * 0.5
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

    // TODO problem with rank scoring in tests cause of filtering by FILTERING_SCORE
    fun convertScoreToRank(score: Double): Double {
        return score / (1 - score)
    }
    
}

