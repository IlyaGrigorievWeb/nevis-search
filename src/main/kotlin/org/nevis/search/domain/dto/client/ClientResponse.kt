package org.nevis.search.domain.dto.client

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Client information response")
data class ClientResponse(
    @Schema(description = "Unique identifier of the client", example = "123e4567-e89b-12d3-a456-426614174000")
    val id: String,
    
    @Schema(description = "First name of the client", example = "John")
    val first_name: String,
    
    @Schema(description = "Last name of the client", example = "Doe")
    val last_name: String,
    
    @Schema(description = "Email address of the client", example = "john.doe@example.com")
    val email: String,
    
    @Schema(description = "Country of residence", example = "USA")
    val countryOfResidence: String?
)