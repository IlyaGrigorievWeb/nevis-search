package org.nevis.search.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.Array
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "documents")
data class Document(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 500)
    val title: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String = "",

    //Meta data

    @Column(columnDefinition = "vector(1536)")
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    val embedding: FloatArray? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "client_id", nullable = false, columnDefinition = "UUID")
    val clientId: UUID? = null,

    @Column(columnDefinition = "TEXT")
    val summary: String? = null,
)

