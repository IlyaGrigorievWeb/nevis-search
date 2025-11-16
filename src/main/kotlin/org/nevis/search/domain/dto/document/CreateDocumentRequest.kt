package org.nevis.search.domain.dto.document

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to create a new document")
data class CreateDocumentRequest(
    @Schema(description = "Title of the document", example = "Passport Copy", required = true)
    val title: String,
    
    @Schema(description = "Content of the document", example = "This is the document content...", required = true)
    val content: String
)