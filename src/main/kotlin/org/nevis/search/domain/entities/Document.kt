package org.nevis.search.domain.entities

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, columnDefinition = "UUID")
    val client_id: UUID? = null,

    @Column(nullable = false, length = 500)
    val title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String = "",

    //Meta data

    @Column(columnDefinition = "vector(1536)")
    val embedding: FloatArray? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

