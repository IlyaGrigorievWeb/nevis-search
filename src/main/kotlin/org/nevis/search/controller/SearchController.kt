package org.nevis.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.domain.dto.ErrorResponse
import org.nevis.search.domain.dto.search.BasesSearchResultItem
import org.nevis.search.domain.dto.search.SearchRequest
import org.nevis.search.service.HybridSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/search")
@Tag(name = "Search", description = "Search API")
class SearchController(
    private val searchService: HybridSearchService
) {

    @GetMapping(
        value = [""],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(
        summary = "Search for clients and documents",
        description = "Performs a hybrid search across clients and documents using semantic vector search and full-text search. " +
                "Returns a list of matching clients and documents sorted by relevance score."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Search completed successfully",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = ArraySchema(schema = Schema(implementation = BasesSearchResultItem::class))
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid search query - query parameter is blank or empty",
                content = [Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = ErrorResponse::class),
                    examples = [
                        ExampleObject(
                            name = "Blank query",
                            value = """{"status": 400, "message": "Invalid search query (query is blank or empty)", "details": "Query parameter 'q' cannot be blank"}"""
                        ),
                        ExampleObject(
                            name = "Empty query",
                            value = """{"status": 400, "message": "Invalid search query (query is blank or empty)", "details": "Query parameter 'q' is required"}"""
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
                            value = """{"status": 500, "message": "Error performing search", "details": "Failed to generate embedding: OpenAI API error"}"""
                        ),
                        ExampleObject(
                            name = "Database error",
                            value = """{"status": 500, "message": "Error performing search", "details": "Database connection failed"}"""
                        ),
                        ExampleObject(
                            name = "Unexpected error",
                            value = """{"status": 500, "message": "Error performing search", "details": "Unexpected error occurred during search"}"""
                        )
                    ]
                )]
            )
        ]
    )
    fun search(
        @Parameter(
            description = "Search query string",
            example = "John Doe",
            required = true
        )
        @RequestParam("q") query: String,
    ): ResponseEntity<Any> {
        if (query.isBlank()) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.BAD_REQUEST.value(),
                message = "Invalid search query (query is blank or empty)",
                details = "Query parameter 'q' cannot be blank",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
        }

        return try {
            val searchRequest = SearchRequest(
                query = query,
                limit = 5,
            )
            val results = searchService.search(searchRequest)

            ResponseEntity.ok(results)
        } catch (e: Exception) {
            val errorResponse = ErrorResponse(
                status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message = "Error performing search",
                details = "${e.javaClass.simpleName}: ${e.message}",
                timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
        }
    }
}