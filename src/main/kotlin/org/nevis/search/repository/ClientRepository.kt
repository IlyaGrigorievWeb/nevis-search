package org.nevis.search.repository

import org.nevis.search.domain.entities.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ClientRepository : JpaRepository<Client, UUID> {
    /**
     * Trigram similarity search - best for fuzzy email/first_name/last_name matching
     */
    @Query(
        value = """
            SELECT id, email, first_name, last_name, country_of_residence,
                   GREATEST(
                       similarity(first_name || last_name, :query),
                       similarity(email, :query)
                   ) as score
            FROM clients
            WHERE similarity(first_name || last_name, :query) > 0.3
               OR similarity(email, :query) > 0.1
            ORDER BY score DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findByTrigramSimilarity(
        @Param("query") query: String,
        @Param("limit") limit: Int
    ): List<Array<Any>>

    /**
     * Full-text search - catches exact email and keyword matches
     */
    @Query(
        value = """
            SELECT id, email, first_name, last_name, country_of_residence
                   ts_rank(to_tsvector('english', first_name || ' ' || last_name || ' ' || email), 
                          plainto_tsquery('english', :query)) as rank
            FROM clients
            WHERE to_tsvector('english', first_name || ' ' || last_name || ' ' || email) 
                  @@ plainto_tsquery('english', :query)
            ORDER BY rank DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun fullTextSearch(
        @Param("query") query: String,
        @Param("limit") limit: Int
    ): List<Array<Any>>
}

