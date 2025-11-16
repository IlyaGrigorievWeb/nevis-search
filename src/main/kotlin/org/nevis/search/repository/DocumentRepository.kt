package org.nevis.search.repository

import org.nevis.search.domain.entities.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DocumentRepository : JpaRepository<Document, UUID> {
    /**
     * Pure vector/semantic search for documents
     * Best for conceptual queries: "address proof" finds "utility bill"
     */
    @Query(
        value = """
            SELECT id, client_id, title, content, summary,
                   1 - (embedding <=> cast(:embedding as vector)) as similarity
            FROM documents
            WHERE embedding IS NOT NULL
            ORDER BY embedding <=> cast(:embedding as vector)
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findByVectorSimilarity(
        @Param("embedding") embedding: String,
        @Param("limit") limit: Int
    ): List<Array<Any>>
}

