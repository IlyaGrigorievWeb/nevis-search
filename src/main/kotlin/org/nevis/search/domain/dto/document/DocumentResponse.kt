package org.nevis.search.domain.dto.document

import java.time.LocalDateTime

data class DocumentResponse(
    val id: String,
    val clientId: String,
    val title: String,
    val content: String
)