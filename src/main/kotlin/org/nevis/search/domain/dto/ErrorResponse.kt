package org.nevis.search.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Error response model")
data class ErrorResponse(
    @Schema(
        description = "HTTP status code",
        example = "400",
        required = true
    )
    val status: Int,
    
    @Schema(
        description = "Error message describing what went wrong",
        example = "Invalid request data",
        required = true
    )
    val message: String,
    
    @Schema(
        description = "Detailed error information or exception type",
        example = "IllegalArgumentException: Invalid UUID format",
        required = false
    )
    val details: String? = null,
    
    @Schema(
        description = "Timestamp when the error occurred",
        example = "2024-01-15T10:30:00",
        required = false
    )
    val timestamp: String? = null
)

