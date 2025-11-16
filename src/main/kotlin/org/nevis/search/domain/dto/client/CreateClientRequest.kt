package org.nevis.search.domain.dto.client

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to create a new client")
data class CreateClientRequest(
    @Schema(description = "First name of the client", example = "John", required = true)
    val first_name: String,
    
    @Schema(description = "Last name of the client", example = "Doe", required = true)
    val last_name: String,
    
    @Schema(description = "Email address of the client", example = "john.doe@example.com", required = true)
    val email: String,
    
    @Schema(description = "Country of residence", example = "USA", required = false)
    val countryOfResidence: String?
)