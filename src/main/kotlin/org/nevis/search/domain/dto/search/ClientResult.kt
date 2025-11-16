package org.nevis.search.domain.dto.search

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Client search result")
data class ClientResult(
    @Schema(description = "Unique identifier of the client", example = "123e4567-e89b-12d3-a456-426614174000")
    override val id: UUID,
    
    @Schema(description = "Relevance score (0.0-1.0)", example = "0.85")
    override val score: Double,
    
    @Schema(description = "Email address", example = "john.doe@example.com")
    val email: String,
    
    @Schema(description = "First name", example = "John")
    val firstName: String,
    
    @Schema(description = "Last name", example = "Doe")
    val lastName: String,
    
    @Schema(description = "Country of residence", example = "USA")
    val countryOfResidence: String?
) : BasesSearchResultItem

