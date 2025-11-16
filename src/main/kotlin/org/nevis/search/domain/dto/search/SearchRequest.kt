package org.nevis.search.domain.dto.search

data class SearchRequest(
    val query: String,
    val limit: Int = 10,
    val vectorWeight: Double = 0.7,
    val fulltextWeight: Double = 0.3,
    val generateSummary: Boolean = false
)