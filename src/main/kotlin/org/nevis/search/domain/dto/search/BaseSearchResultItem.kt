package org.nevis.search.domain.dto.search

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Base interface for search result items")
sealed interface BasesSearchResultItem {
    val id: UUID
    val score: Double
}