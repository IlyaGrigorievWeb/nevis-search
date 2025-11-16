package org.nevis.search.domain.dto.document

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Document information response")
data class DocumentResponse(
    @Schema(description = "Unique identifier of the document", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    
    @Schema(description = "Unique identifier of the client who owns this document", example = "123e4567-e89b-12d3-a456-426614174000")
    val clientId: String,
    
    @Schema(description = "Title of the document", example = "Passport Copy")
    val title: String,
    
    @Schema(description = "Content of the document", example = "This is the document content...")
    val content: String
)