package org.nevis.search.service

import org.nevis.search.domain.entities.Client
import org.nevis.search.domain.entities.Document
import org.nevis.search.repository.ClientRepository
import org.nevis.search.repository.DocumentRepository
import org.springframework.stereotype.Service

@Service
class IndexingService(
    private val clientRepository: ClientRepository,
    private val documentRepository: DocumentRepository,
    private val embeddingService: OpenAIEmbeddingService
) {

    /**
     * Index client - NO EMBEDDING NEEDED
     * Clients use trigram + fulltext indexes only
     */
    fun indexClient(client: Client): Client {
        // Just save - trigram indexes work automatically on name/company fields
        return clientRepository.save(client)
    }

    /**
     * Index document - REQUIRES EMBEDDING
     * Documents use semantic/vector search
     */
    fun indexDocument(document: Document): Document {
        val searchableText = "${document.title} ${document.content}"
        val embedding = embeddingService.generateEmbedding(searchableText)
        val documentWithEmbedding = document.copy(embedding = embedding)

        return documentRepository.save(documentWithEmbedding)
    }

    fun reindexAllDocuments() {
        val documents = documentRepository.findAll()
        documents.forEach { document ->
            indexDocument(document)
        }
    }

    fun reindexAllClients() {
        val clients = clientRepository.findAll()
        clients.forEach { client ->
            indexClient(client)
        }
    }
}

