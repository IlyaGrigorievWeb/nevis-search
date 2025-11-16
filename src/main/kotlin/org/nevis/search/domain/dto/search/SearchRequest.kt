package org.nevis.search.domain.dto.search

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Search request parameters")
data class SearchRequest(
    @Schema(description = "Search query string", example = "John Doe", required = true)
    val query: String,
    
    @Schema(description = "Maximum number of results to return", example = "10", defaultValue = "10")
    val limit: Int = 10,
)