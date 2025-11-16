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
import org.nevis.search.service.TestDataFactory.createDocumentFulltextResult
import org.nevis.search.service.TestDataFactory.createDocumentVectorResult
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

    @Mock
    private lateinit var summarizingService: SummarizingService

    @InjectMocks
    private lateinit var hybridSearchService: HybridSearchService

    private fun setupMocks(
        trigramResults: List<Array<Any>> = emptyList(),
        documentVectorResults: List<Array<Any>> = emptyList(),
        documentFulltextResults: List<Array<Any>> = emptyList(),
        embedding: FloatArray = TestDataFactory.createDefaultEmbedding()
    ) {
        whenever(clientRepository.findByTrigramSimilarity(any(), any()))
            .thenReturn(trigramResults)
        whenever(documentRepository.findByVectorSimilarity(any(), any()))
            .thenReturn(documentVectorResults)
        whenever(documentRepository.fullTextSearch(any(), any()))
            .thenReturn(documentFulltextResults)
        whenever(embeddingService.generateEmbedding(any()))
            .thenReturn(embedding)
    }

    @Test
    fun `test search merges document results by score correctly`() {
        // Given
        val clientId = UUID.randomUUID()
        val documentId1 = UUID.randomUUID()
        val documentId2 = UUID.randomUUID()
        val documentId3 = UUID.randomUUID()

        val vectorDoc1 = createDocumentVectorResult(
            documentId1, clientId, "Doc 1", "Content 1", similarity = 0.85
        )
        val fulltextDoc1 = createDocumentFulltextResult(
            documentId1, clientId, "Doc 1", "Content 1", rank = 0.75
        )
        val vectorDoc2 = createDocumentVectorResult(
            documentId2, clientId, "Doc 2", "Content 2", similarity = 0.70
        )
        val fulltextDoc3 = createDocumentFulltextResult(
            documentId3, clientId, "Doc 3", "Content 3", rank = 0.65
        )

        setupMocks(
            trigramResults = emptyList(),
            documentVectorResults = listOf(vectorDoc1, vectorDoc2),
            documentFulltextResults = listOf(fulltextDoc1, fulltextDoc3)
        )

        // When
        val results = hybridSearchService.search(SearchRequest("document query", 10))

        // Then
        assertEquals(3, results.size)
        val documents = results.filterIsInstance<DocumentResult>()
        assertEquals(3, documents.size)

        val doc1 = documents.find { it.id == documentId1 }!!
        assertEquals(
            TestDataFactory.calculateMergedScore(0.85, 0.75),
            doc1.score,
            0.001
        )

        val doc2 = documents.find { it.id == documentId2 }!!
        assertEquals(
            TestDataFactory.calculateMergedScore(0.70, null),
            doc2.score,
            0.001
        )

        val doc3 = documents.find { it.id == documentId3 }!!
        assertEquals(
            TestDataFactory.calculateMergedScore(null, 0.65),
            doc3.score,
            0.001
        )
    }

    @Test
    fun `test search returns all trigram client results`() {
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
        assertEquals(3, results.size)
        assertTrue(results.any { it.id == clientId1 })
        assertTrue(results.any { it.id == clientId2 })
        assertTrue(results.any { it.id == clientId3 })
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
        val doc1 = createDocumentVectorResult(
            documentId1, clientId, "Test Document 1", "This is test content for document 1", "", 0.75
        )
        val doc2 = createDocumentVectorResult(
            documentId2, clientId, "Test Document 2", "This is test content for document 2", "" , 0.50
        )

        setupMocks(
            trigramResults = listOf(clientResult),
            documentVectorResults = listOf(doc1, doc2)
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
        assertEquals(0.80, clients[0].score, 0.001)
        assertEquals(0.75 * 0.8, documents.find { it.id == documentId1 }!!.score, 0.001)
        assertEquals(0.50 * 0.8, documents.find { it.id == documentId2 }!!.score, 0.001)
    }

    @Test
    fun `test search handles document fulltext search exception gracefully`() {
        // Given
        val clientId = UUID.randomUUID()
        val documentId = UUID.randomUUID()
        val trigramResult = TestDataFactory.createTrigramResult(
            clientId, "client@example.com", "Test", "Client", "USA", 0.80
        )
        val docVector = createDocumentVectorResult(
            documentId, clientId, "Doc", "Content", similarity = 0.9
        )

        whenever(clientRepository.findByTrigramSimilarity(any(), any()))
            .thenReturn(listOf(trigramResult))
        whenever(documentRepository.findByVectorSimilarity(any(), any()))
            .thenReturn(listOf(docVector))
        whenever(documentRepository.fullTextSearch(any(), any()))
            .thenThrow(RuntimeException("Fulltext search failed"))
        whenever(embeddingService.generateEmbedding(any()))
            .thenReturn(TestDataFactory.createDefaultEmbedding())

        // When
        val results = hybridSearchService.search(SearchRequest("test query", 10))

        // Then
        assertEquals(2, results.size)
        assertTrue(results.any { it.id == clientId })
        assertTrue(results.any { it.id == documentId })
    }
}

