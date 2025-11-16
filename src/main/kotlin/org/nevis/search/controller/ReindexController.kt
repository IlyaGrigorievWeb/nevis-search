package org.nevis.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.domain.dto.ErrorResponse
import org.nevis.search.service.IndexingService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/reindex")
@Tag(name = "Reindex", description = "Reindexing API for regenerating embeddings")
class ReindexController(
    private val indexingService: IndexingService
) {

    @PostMapping("/reindex/clients")
    @Operation(
        summary = "Reindex all clients",
        description = "Regenerates embeddings for all existing clients in the database. " +
                "This operation may take some time depending on the number of clients."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "All clients reindexed successfully",
                content = [Content(
                    mediaType = "text/plain",
                    schema = Schema(implementation = String::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error - database error, OpenAI API error, or unexpected error occurred during reindexing",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Database error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Database connection failed"}"""
                        ),
                        ExampleObject(
                            name = "OpenAI API error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Failed to generate embeddings: OpenAI API error"}"""
                        ),
                        ExampleObject(
                            name = "Unexpected error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Unexpected error occurred while reindexing clients"}"""
                        )
                    ]
                )]
            )
        ]
    )
    fun reindexClients(): ResponseEntity<Any> {
        return try {
            indexingService.reindexAllClients()
            ResponseEntity.ok("Clients reindexed successfully")
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Internal server error during reindexing",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }

    @PostMapping("/reindex/documents")
    @Operation(
        summary = "Reindex all documents",
        description = "Regenerates embeddings for all existing documents in the database. " +
                "This operation may take some time depending on the number of documents."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "All documents reindexed successfully",
                content = [Content(
                    mediaType = "text/plain",
                    schema = Schema(implementation = String::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error - database error, OpenAI API error, or unexpected error occurred during reindexing",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Database error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Database connection failed"}"""
                        ),
                        ExampleObject(
                            name = "OpenAI API error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Failed to generate embeddings: OpenAI API error"}"""
                        ),
                        ExampleObject(
                            name = "Unexpected error",
                            value = """{"status": 500, "message": "Internal server error during reindexing", "details": "Unexpected error occurred while reindexing documents"}"""
                        )
                    ]
                )]
            )
        ]
    )
    fun reindexDocuments(): ResponseEntity<Any> {
        return try {
            indexingService.reindexAllDocuments()
            ResponseEntity.ok("Documents reindexed successfully")
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Internal server error during reindexing",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }
}

