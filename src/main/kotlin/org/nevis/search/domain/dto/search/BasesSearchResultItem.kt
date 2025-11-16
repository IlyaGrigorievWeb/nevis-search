package org.nevis.search.domain.dto.search

import java.util.UUID

sealed interface BasesSearchResultItem {
    val id: UUID
    val score: Double
}

data class ClientResult(
    override val id: UUID,
    override val score: Double,
    val email: String,
    val firstName: String,
    val lastName: String,
    val countryOfResidence: String?
) : BasesSearchResultItem

data class DocumentResult(
    override val id: UUID,
    override val score: Double,
    val clientId: UUID,
    val title: String,
    val content: String,
    val summary: String? = null,
) : BasesSearchResultItem