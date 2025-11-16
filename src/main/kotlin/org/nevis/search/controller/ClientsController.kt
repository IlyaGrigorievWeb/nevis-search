package org.nevis.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.domain.dto.ErrorResponse
import org.nevis.search.domain.dto.client.ClientResponse
import org.nevis.search.domain.dto.client.CreateClientRequest
import org.nevis.search.domain.dto.document.CreateDocumentRequest
import org.nevis.search.domain.dto.document.DocumentResponse
import org.nevis.search.domain.entities.Client
import org.nevis.search.domain.entities.Document
import org.nevis.search.service.IndexingService
import org.nevis.search.service.SummarizingService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@RestController
@RequestMapping("/clients")
@Tag(name = "Clients", description = "Client management API")
class ClientsController(
    private val indexingService: IndexingService,
    private val summarizingService: SummarizingService
) {

    @PostMapping(
        value = [""],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Create client",
        description = "Create a new client. The client will be indexed for search using trigram-based fuzzy matching."
    )
    @SwaggerRequestBody(
        description = "Client creation request",
        required = true,
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = CreateClientRequest::class)
        )]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Client created successfully",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ClientResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid client data - missing required fields or invalid format",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Missing required field",
                            value = """{"status": 400, "message": "Invalid client data", "details": "Email is required"}"""
                        ),
                        ExampleObject(
                            name = "Invalid email format",
                            value = """{"status": 400, "message": "Invalid client data", "details": "Invalid email format"}"""
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error - database connection issue or unexpected error occurred",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Database error",
                            value = """{"status": 500, "message": "Error creating client", "details": "Database connection failed"}"""
                        ),
                        ExampleObject(
                            name = "Unexpected error",
                            value = """{"status": 500, "message": "Error creating client", "details": "Unexpected error occurred"}"""
                        )
                    ]
                )]
            )
        ]
    )
    fun createClient(
        @RequestBody request: CreateClientRequest
    ): ResponseEntity<Any> {
        return try {
            val newClient = Client(
                first_name = request.first_name,
                last_name = request.last_name,
                email = request.email,
                countryOfResidence = request.countryOfResidence,
                createdAt = LocalDateTime.now()
            )

            indexingService.indexClient(newClient)

            val response = ClientResponse(
                id = newClient.id.toString(),
                first_name = newClient.first_name,
                last_name = newClient.last_name,
                email = request.email,
                countryOfResidence = request.countryOfResidence
            )
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                message = "Invalid client data",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error creating client",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }

    @PostMapping(
        value = ["/{id}/documents"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Create document for client",
        description = "Create a new document associated with a client. The document will be indexed for semantic search using vector embeddings, and a summary will be generated asynchronously."
    )
    @SwaggerRequestBody(
        description = "Document creation request",
        required = true,
        content = [Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = Schema(implementation = CreateDocumentRequest::class)
        )]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Document created successfully",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = DocumentResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid document data or invalid client ID format",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Invalid UUID format",
                            value = """{"status": 400, "message": "Invalid document data or invalid client ID format", "details": "IllegalArgumentException: Invalid UUID string: invalid-id"}"""
                        ),
                        ExampleObject(
                            name = "Missing required field",
                            value = """{"status": 400, "message": "Invalid document data or invalid client ID format", "details": "Title is required"}"""
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Client not found - the specified client ID does not exist",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Client not found",
                            value = """{"status": 404, "message": "Client not found", "details": "Client with ID 123e4567-e89b-12d3-a456-426614174000 does not exist"}"""
                        )
                    ]
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error - database error, OpenAI API error, or unexpected error occurred",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "OpenAI API error",
                            value = """{"status": 500, "message": "Error creating document", "details": "Failed to generate embedding: OpenAI API error"}"""
                        ),
                        ExampleObject(
                            name = "Database error",
                            value = """{"status": 500, "message": "Error creating document", "details": "Database connection failed"}"""
                        )
                    ]
                )]
            )
        ]
    )
    fun createDocument(
        @Parameter(
            description = "Client unique identifier (UUID)",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true
        )
        @PathVariable id: String,
        @RequestBody request: CreateDocumentRequest
    ): ResponseEntity<Any> {
        return try {
            val document = Document(
                title = request.title,
                content = request.content,
                clientId = UUID.fromString(id)
            )
            val savedDocument = indexingService.indexDocument(document)
            summarizingService.summarizeDocumentAsync(savedDocument.id)
            val response = DocumentResponse(
                id = savedDocument.id.toString(),
                clientId = savedDocument.clientId.toString(),
                title = savedDocument.title,
                content = savedDocument.content
            )
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                message = "Invalid document data or invalid client ID format",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error creating document",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }
}