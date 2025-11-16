package org.nevis.search.controller

import org.nevis.search.domain.dto.client.CreateClientRequest
import org.nevis.search.domain.dto.client.ClientResponse
import org.nevis.search.domain.dto.document.CreateDocumentRequest
import org.nevis.search.domain.dto.document.DocumentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.domain.entities.Client
import org.nevis.search.domain.entities.Document
import org.nevis.search.service.IndexingService
import org.nevis.search.service.SummarizingService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
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
        description = "Create a new client"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Client created successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid client data"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error creating client"
            )
        ]
    )
    fun createClient(
        @RequestBody request: CreateClientRequest
    ): ResponseEntity<ClientResponse> {
        return try {
            var newClient = Client(
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
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping(
        value = ["/{id}/documents"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Create document for client",
        description = "Create a new document associated with a client"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Document created successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid document data"
            ),
            ApiResponse(
                responseCode = "404",
                description = "Client not found"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error creating document"
            )
        ]
    )
    fun createDocument(
        @PathVariable id: String,
        @RequestBody request: CreateDocumentRequest
    ): ResponseEntity<DocumentResponse> {
        return try {
            var newDocument = Document(
                title = request.title,
                content = request.content,
                clientId = UUID.fromString(id)
            )
            indexingService.indexDocument(newDocument)
            summarizingService.summarizeDocumentAsync(newDocument.id)
            val response = DocumentResponse(
                id = newDocument.id.toString(),
                clientId = newDocument.clientId.toString(),
                title = newDocument.title,
                content = newDocument.content
            )
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}