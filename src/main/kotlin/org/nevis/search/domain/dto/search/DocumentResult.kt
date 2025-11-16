package org.nevis.search.domain.dto.search

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Document search result")
data class DocumentResult(
    @Schema(description = "Unique identifier of the document", example = "123e4567-e89b-12d3-a456-426614174000")
    override val id: UUID,
    
    @Schema(description = "Relevance score (0.0-1.0)", example = "0.92")
    override val score: Double,
    
    @Schema(description = "Unique identifier of the client who owns this document", example = "123e4567-e89b-12d3-a456-426614174000")
    val clientId: UUID,
    
    @Schema(description = "Document title", example = "Passport Copy")
    val title: String,
    
    @Schema(description = "Document content (truncated to 200 characters)", example = "This is the document content...")
    val content: String,
    
    @Schema(description = "Document summary (if available)", example = "A summary of the document content")
    val summary: String? = null,
) : BasesSearchResultItem

