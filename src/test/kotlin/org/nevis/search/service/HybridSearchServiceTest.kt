package org.nevis.search.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.nevis.search.domain.dto.search.ClientResult
import org.nevis.search.domain.dto.search.DocumentResult
import org.nevis.search.domain.dto.search.SearchRequest
import org.nevis.search.repository.ClientRepository
import org.nevis.search.repository.DocumentRepository
import org.nevis.search.service.TestDataFactory.convertScoreToRank
import java.util.UUID

//Ranking tests
@ExtendWith(MockitoExtension::class)
class HybridSearchServiceTest {

    @Mock
    private lateinit var clientRepository: ClientRepository

    @Mock
    private lateinit var documentRepository: DocumentRepository

    @Mock
    private lateinit var embeddingService: OpenAIService

    @InjectMocks
    private lateinit var hybridSearchService: HybridSearchService

    private fun setupMocks(
        trigramResults: List<Array<Any>> = emptyList(),
        fulltextResults: List<Array<Any>> = emptyList(),
        documentResults: List<Array<Any>> = emptyList(),
        embedding: FloatArray = TestDataFactory.createDefaultEmbedding()
    ) {
        whenever(clientRepository.findByTrigramSimilarity(any(), any()))
            .thenReturn(trigramResults)
        whenever(clientRepository.fullTextSearch(any(), any()))
            .thenReturn(fulltextResults)
        whenever(documentRepository.findByVectorSimilarity(any(), any()))
            .thenReturn(documentResults)
        whenever(embeddingService.generateEmbedding(any()))
            .thenReturn(embedding)
    }

    @Test
    fun `test search merges client results by score correctly`() {
        // Given
        val clientId1 = UUID.randomUUID()
        val clientId2 = UUID.randomUUID()
        val clientId3 = UUID.randomUUID()

        val trigramResult1 = TestDataFactory.createTrigramResult(
            clientId1, "john.doe@example.com", "John", "Doe", "USA", 0.85
        )
        val fulltextResult1 = TestDataFactory.createFulltextResult(
            clientId1, "john.doe@example.com", "John", "Doe", "USA", convertScoreToRank(0.75) // TODO problem with rank scoring
        )
        val trigramResult2 = TestDataFactory.createTrigramResult(
            clientId2, "jane.smith@example.com", "Jane", "Smith", "UK", 0.70
        )
        val fulltextResult3 = TestDataFactory.createFulltextResult(
            clientId3, "johnny.walker@example.com", "Johnny", "Walker", "Canada", convertScoreToRank(0.65)  // TODO problem with rank scoring
        )

        setupMocks(
            trigramResults = listOf(trigramResult1, trigramResult2),
            fulltextResults = listOf(fulltextResult1, fulltextResult3)
        )

        // When
        val results = hybridSearchService.search(SearchRequest("john doe", 10))

        // Then
        assertEquals(3, results.size)

        val client1 = results.find { it.id == clientId1 } as ClientResult
        assertEquals("john.doe@example.com", client1.email)
        assertEquals(TestDataFactory.calculateMergedClientScore(0.85, 0.75), client1.score, 0.001)

        val client2 = results.find { it.id == clientId2 } as ClientResult
        assertEquals("jane.smith@example.com", client2.email)
        assertEquals(TestDataFactory.calculateMergedClientScore(0.70, null), client2.score, 0.001)

        val client3 = results.find { it.id == clientId3 } as ClientResult
        assertEquals("johnny.walker@example.com", client3.email)
        assertEquals(TestDataFactory.calculateMergedClientScore(null, 0.65), client3.score, 0.001)
    }

    @Test
    fun `test search filters results by score threshold`() {
        // Given
        val clientId1 = UUID.randomUUID()
        val clientId2 = UUID.randomUUID()
        val clientId3 = UUID.randomUUID()

        val highScore = TestDataFactory.createTrigramResult(
            clientId1, "high.score@example.com", "High", "Score", "USA", 0.80
        )
        val lowScore = TestDataFactory.createTrigramResult(
            clientId2, "low.score@example.com", "Low", "Score", "USA", 0.20
        )
        val mediumScore = TestDataFactory.createTrigramResult(
            clientId3, "medium.score@example.com", "Medium", "Score", "USA", 0.50
        )

        setupMocks(trigramResults = listOf(highScore, lowScore, mediumScore))

        // When
        val results = hybridSearchService.search(SearchRequest("test query", 10))

        // Then
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == clientId1 })
        assertTrue(results.any { it.id == clientId3 })
        assertFalse(results.any { it.id == clientId2 })
        results.forEach { assertTrue(it.score > 0.3) }
    }

    @Test
    fun `test search sorts results by score descending`() {
        // Given
        val clientId1 = UUID.randomUUID()
        val clientId2 = UUID.randomUUID()
        val clientId3 = UUID.randomUUID()

        val result1 = TestDataFactory.createTrigramResult(clientId1, "score1@example.com", "Score", "One", "USA", 0.50)
        val result2 = TestDataFactory.createTrigramResult(clientId2, "score2@example.com", "Score", "Two", "USA", 0.90)
        val result3 = TestDataFactory.createTrigramResult(clientId3, "score3@example.com", "Score", "Three", "USA", 0.60)

        setupMocks(trigramResults = listOf(result1, result2, result3))

        // When
        val results = hybridSearchService.search(SearchRequest("test query", 10))

        // Then
        assertEquals(3, results.size)
        assertEquals(clientId2, results[0].id)
        assertEquals(clientId3, results[1].id)
        assertEquals(clientId1, results[2].id)
        (0 until results.size - 1).forEach { i ->
            assertTrue(results[i].score >= results[i + 1].score)
        }
    }

    @Test
    fun `test search respects limit parameter`() {
        // Given
        val limit = 2
        val trigramResults = (1..5).map { index ->
            TestDataFactory.createTrigramResult(
                UUID.randomUUID(),
                "client$index@example.com",
                "Client",
                "Number$index",
                "USA",
                0.50 + (index * 0.1)
            )
        }

        setupMocks(trigramResults = trigramResults)

        // When
        val results = hybridSearchService.search(SearchRequest("test query", limit))

        // Then
        assertTrue(results.size <= limit)
    }

    @Test
    fun `test search merges client and document results correctly`() {
        // Given
        val clientId = UUID.randomUUID()
        val documentId1 = UUID.randomUUID()
        val documentId2 = UUID.randomUUID()

        val clientResult = TestDataFactory.createTrigramResult(
            clientId, "client@example.com", "Test", "Client", "USA", 0.80
        )
        val doc1 = TestDataFactory.createDocumentResult(
            documentId1, clientId, "Test Document 1", "This is test content for document 1", 0.75
        )
        val doc2 = TestDataFactory.createDocumentResult(
            documentId2, clientId, "Test Document 2", "This is test content for document 2", 0.50
        )

        setupMocks(
            trigramResults = listOf(clientResult),
            documentResults = listOf(doc1, doc2)
        )

        // When
        val results = hybridSearchService.search(SearchRequest("test query", 10))

        // Then
        assertEquals(3, results.size)
        val clients = results.filterIsInstance<ClientResult>()
        val documents = results.filterIsInstance<DocumentResult>()

        assertEquals(1, clients.size)
        assertEquals(2, documents.size)
        assertEquals(clientId, clients[0].id)
        assertEquals(0.80 * 0.8, clients[0].score, 0.001)
        assertEquals(0.75, documents.find { it.id == documentId1 }!!.score, 0.001)
        assertEquals(0.50, documents.find { it.id == documentId2 }!!.score, 0.001)
    }

    @Test
    fun `test search handles fulltext search exception gracefully`() {
        // Given
        val clientId = UUID.randomUUID()
        val trigramResult = TestDataFactory.createTrigramResult(
            clientId, "client@example.com", "Test", "Client", "USA", 0.80
        )

        whenever(clientRepository.findByTrigramSimilarity(any(), any()))
            .thenReturn(listOf(trigramResult))
        whenever(clientRepository.fullTextSearch(any(), any()))
            .thenThrow(RuntimeException("Fulltext search failed"))
        whenever(documentRepository.findByVectorSimilarity(any(), any()))
            .thenReturn(emptyList())
        whenever(embeddingService.generateEmbedding(any()))
            .thenReturn(TestDataFactory.createDefaultEmbedding())

        // When
        val results = hybridSearchService.search(SearchRequest("test query", 10))

        // Then
        assertEquals(1, results.size)
        assertEquals(clientId, results[0].id)
    }

    @Test
    fun `test search correctly merges overlapping client results with different scores`() {
        // Given
        val clientId = UUID.randomUUID()
        val trigramResult = TestDataFactory.createTrigramResult(
            clientId, "john.doe@example.com", "John", "Doe", "USA", 0.60
        )
        val fulltextResult = TestDataFactory.createFulltextResult(
            clientId, "john.doe@example.com", "John", "Doe", "USA", 0.80
        )

        setupMocks(
            trigramResults = listOf(trigramResult),
            fulltextResults = listOf(fulltextResult)
        )

        // When
        val results = hybridSearchService.search(SearchRequest("john", 10))

        // Then
        assertEquals(1, results.size)
        val result = results[0] as ClientResult
        assertEquals(clientId, result.id)
        assertEquals(TestDataFactory.calculateMergedClientScore(0.60, 0.80), result.score, 0.001)
    }
}

