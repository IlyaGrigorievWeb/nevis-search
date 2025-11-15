package org.nevis.search.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.nevis.search.domain.dto.search.SearchRequest
import org.nevis.search.service.HybridSearchService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
        summary = "Search",
        description = "Search for clients and documents using a query string"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Search completed successfully",
                content = [Content(mediaType = MediaType.APPLICATION_JSON_VALUE)]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid search query"
            ),
            ApiResponse(
                responseCode = "500",
                description = "Error performing search"
            )
        ]
    )
    fun search(
        @RequestParam("q") query: String,
        @RequestParam("generateSummary", required = false, defaultValue = "false") generateSummary: Boolean
    ): ResponseEntity<List<*>> {
        if (query.isBlank()) {
            return ResponseEntity.badRequest().build()
        }

        return try {
            var searchRequest = SearchRequest(
                query = query,
                limit = 5,
                generateSummary = generateSummary
            )
            val results = searchService.search(searchRequest, searchRequest.generateSummary)

            ResponseEntity.ok(results)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}