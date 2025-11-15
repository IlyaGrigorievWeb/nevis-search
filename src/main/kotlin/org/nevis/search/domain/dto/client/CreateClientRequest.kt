package org.nevis.search.domain.dto.client

data class CreateClientRequest(
    val first_name: String,
    val last_name: String,
    val email: String,
    val countryOfResidence: String?
)