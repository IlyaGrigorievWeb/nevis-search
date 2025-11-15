package org.nevis.search.domain.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "clients")
data class Client(
    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false)
    val email: String = "",

    @Column(nullable = false)
    val first_name: String= "",

    @Column(nullable = false)
    val last_name: String= "",

    @Column(nullable = true)
    val countryOfResidence: String? = null,

    //Meta data

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)


