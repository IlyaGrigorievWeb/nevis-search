package org.nevis.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.service.IndexingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reindex")
@Tag(name = "Deprecated Search API")
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
                description = "All clients reindexed successfully"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error during reindexing"
            )
        ]
    )
    fun reindexClients(): ResponseEntity<String> {
        indexingService.reindexAllClients()
        return ResponseEntity.ok("Clients reindexed successfully")
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
                description = "All documents reindexed successfully"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Internal server error during reindexing"
            )
        ]
    )
    fun reindexDocuments(): ResponseEntity<String> {
        indexingService.reindexAllDocuments()
        return ResponseEntity.ok("Documents reindexed successfully")
    }
}

