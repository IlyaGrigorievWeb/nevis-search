package org.nevis.search.domain.dto.document

import java.time.LocalDateTime

data class DocumentResponse(
    val id: String,
    val client_id: String,
    val title: String,
    val content: String,
    val created_at: LocalDateTime
)