# NevisSearch

A hybrid search application built with Spring Boot and Kotlin that provides intelligent search capabilities across multiple entity types using different search strategies optimized for each entity.

## Running the Application

### Prerequisites

- Docker and Docker Compose installed
- OpenAI API key

### Setup Instructions

1. **Clone the repository** (if not already done):
   ```bash
   git clone <repository-url>
   cd NevisSearch
   ```

2. **Set OpenAI API Key**:

   Before starting the application, you need to set your OpenAI API key in the `docker-compose.yml` file:

   Open `docker-compose.yml` and replace the `${OPENAI_API_KEY}` or set environment variable:
   ```yaml
   environment:
     OPENAI_API_KEY: ${OPENAI_API_KEY}
   ```

3. **Change database connection settings (optional)**:

   Note: By default, docker-compose contains development values for an empty containerized database. For testing with
   prefilled data, use secure credentials.
    
   In the `docker-compose.yml` file, you can configure the following environment variables for the Spring Boot
   application:
   ```yaml
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/searchdb
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
   ```

4. **Start the application**:
   ```bash
   docker-compose up --build
   ```

   This will:
    - Build the Spring Boot application
    - Start PostgreSQL with pgvector and pg_trgm extensions
    - Initialize the database schema from `database-schema.sql`
    - Start the application on port 8080

5. **Verify the application is running**:
    - API: http://localhost:8080
    - Swagger UI: http://localhost:8080/swagger-ui.html
    - API Docs: http://localhost:8080/api-docs

### Example Search Queries and Responses

This section demonstrates the hybrid search capabilities with real examples showing how the system handles different types of queries across clients and documents.

#### Example 1: Fuzzy Client Matching

**Query:** `Fast Logistic`

This query demonstrates the trigram-based fuzzy matching for client searches. The system finds clients even when the query contains typos or partial matches.

**Request:**
```http
GET /search?q=Fast%20Logistic
```

**Response:**
```json
{
  "results": [
    {
      "type": "client",
      "score": 0.21052631735801697,
      "data": {
        "id": 1,
        "firstName": "Elena",
        "lastName": "Smith",
        "email": "elenas.support@fastlogicstics.io"
      }
    }
  ]
}
```

**Explanation:** The system matched the query "Fast Logistic" to a client with email "elenas.support@fastlogicstics.io" using trigram similarity. This showcases how the hybrid search handles:
- Partial company name matching in email addresses
- Typos and variations (e.g., "Fast Logistic" vs "fastlogicstics")
- Email domain similarity detection

---

#### Example 2: Semantic Document Search

**Query:** `Moving to new space`

This query demonstrates semantic/vector search capabilities for documents. The system finds conceptually related documents even when they don't contain the exact query terms.

**Request:**
```http
GET /search?q=Moving%20to%20new%20space
```

**Response:**
```json
{
  "results": [
    {
      "type": "document",
      "score": 0.37431060961611656,
      "data": {
        "id": 5,
        "title": "Commercial Offer",
        "content": "FastLogistics proposes a full-service transportation package for relocating office equipment belonging to the client, Orion DataWorks LLC, from their current address at 1440 Ridgehaven Park, Suite 220..."
      }
    }
  ]
}
```

**Explanation:** The document was matched semantically using vector embeddings. Notice that:
- The document content doesn't contain the exact phrase "Moving to new space"
- The system understood the conceptual relationship between "moving" and "relocating office equipment"
- The vector embedding captures semantic similarity, allowing for synonym and context-based matching



### Database Schema

The database schema is automatically initialized when the PostgreSQL container starts. The `database-schema.sql` file is mounted to `/docker-entrypoint-initdb.d/` and executed automatically.

The schema includes:
- `clients` table with trigram and full-text indexes
- `documents` table with vector embedding column and IVFFlat index
- Required PostgreSQL extensions (`vector`, `pg_trgm`)

### API Endpoints

#### Search
- **GET** `/search?q={query}`
    - Performs hybrid search across clients and documents
    - Returns unified, ranked results

#### Clients
- **GET** `/clients` - List all clients
- **POST** `/clients` - Create a new client
- **GET** `/clients/{id}` - Get client by ID

#### Documents
- **GET** `/documents` - List all documents
- **POST** `/documents` - Create a new document (automatically generates embedding)
- **GET** `/documents/{id}` - Get document by ID

#### Reindexing
- **POST** `/reindex/clients` - Reindex all clients
- **POST** `/reindex/documents` - Reindex all documents (regenerates embeddings)


### Development

To run the application locally without Docker:

1. **Start PostgreSQL** with pgvector extension:
   ```bash
   docker run -d \
     --name nevissearch-postgres \
     -e POSTGRES_DB=searchdb \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     pgvector/pgvector:pg16
   ```

2. **Initialize the database schema**:
   ```bash
   psql -h localhost -U postgres -d searchdb -f database-schema.sql
   ```

3. **Set environment variable**:
   ```bash
   export OPENAI_API_KEY="your-openai-api-key-here"
   ```

4. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

### Configuration

Application configuration is in `src/main/resources/application.yml`:
- Database connection settings
- OpenAI API configuration
- SpringDoc OpenAPI settings

For local development, override settings in `application-local.yml`.

## Architecture

### Technology Stack

- **Backend Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.25
- **Database**: PostgreSQL 16 with extensions:
  - `pgvector`: Vector similarity search for semantic/document search
  - `pg_trgm`: Trigram similarity for fuzzy text matching
- **Embedding Service**: OpenAI API (text-embedding-3-small model)
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Containerization**: Docker & Docker Compose

### System Components

```
┌─────────────────┐
│  REST API       │  (SearchController, ClientsController, ReindexController)
│  (Spring Boot)  │
└────────┬────────┘
         │
         ├─────────────────┬─────────────────┐
         │                 │                 │
┌────────▼────────┐ ┌──────▼──────┐ ┌───────▼────────┐
│ HybridSearch    │ │ Indexing    │ │ OpenAI         │
│ Service         │ │ Service     │ │ Embedding      │
│                 │ │             │ │ Service        │
└────────┬────────┘ └──────┬──────┘ └────────────────┘
         │                 │
         ├─────────────────┼─────────────────┐
         │                 │                 │
┌────────▼────────┐ ┌──────▼──────┐ ┌───────▼────────┐
│ Client          │ │ Document    │ │ PostgreSQL     │
│ Repository      │ │ Repository  │ │ (pgvector +    │
│                 │ │             │ │  pg_trgm)      │
└─────────────────┘ └─────────────┘ └────────────────┘
```

### Key Features

- **Hybrid Search**: Combines multiple search strategies optimized per entity type
- **Semantic Search**: Vector embeddings for conceptual document search
- **Fuzzy Matching**: Trigram similarity for handling typos and name variations
- **Full-Text Search**: PostgreSQL full-text search for exact keyword matches
- **Unified Results**: Merges and ranks results from different search strategies

## Search Strategies per Entity

### 1. Client Search Strategy

**Approach**: Trigram-based fuzzy matching + Full-text search

**Best For**: 
- Name variations and typos
- Partial email matches
- Company name searches

**Implementation**:
- **Primary Strategy (80% weight)**: Trigram similarity search
  - Uses PostgreSQL `pg_trgm` extension
  - Searches across `first_name`, `last_name`, and `email` fields
  - Threshold: 0.3 for name similarity, 0.1 for email similarity
  - Example: "nikita insurance" matches "Nikita Petrov @ Alliance Insurance"

- **Secondary Strategy (20% weight)**: Full-text search
  - Uses PostgreSQL `to_tsvector` and `plainto_tsquery`
  - Catches exact email and keyword matches
  - Normalized scores merged with trigram results

**Indexes**:
- GIN index on trigram operations for `first_name`, `last_name`, `email`
- GIN index on full-text search vector

**Scoring**:
- Results with score > 0.3 are included
- Merged scores: `(trigram_score * 0.8) + (fulltext_score * 0.2)`

### 2. Document Search Strategy

**Approach**: Pure vector/semantic search

**Best For**:
- Conceptual queries
- Synonym matching
- Context understanding
- Example: "address proof" finds documents mentioning "utility bill"

**Implementation**:
- Uses OpenAI `text-embedding-3-small` model to generate 1536-dimensional embeddings
- Stores embeddings in PostgreSQL `vector(1536)` column
- Uses cosine similarity (`<=>`) operator for vector comparison
- IVFFlat index for efficient vector similarity search

**Indexing**:
- When a document is indexed, embeddings are generated from `title + content`
- Embeddings are stored in the `embedding` column
- IVFFlat index with 100 lists for fast approximate nearest neighbor search

**Scoring**:
- Similarity score: `1 - (embedding <=> query_embedding)`
- Results with score > 0.3 are included

### Result Merging

The `HybridSearchService` combines results from both entity types:
1. Executes entity-specific searches in parallel
2. Filters results by score threshold (0.3)
3. Sorts all results by score (descending)
4. Returns top N results based on request limit

## Project Structure

```
NevisSearch/
├── src/
│   ├── main/
│   │   ├── kotlin/org/nevis/search/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── domain/
│   │   │   │   ├── dto/         # Data transfer objects
│   │   │   │   └── entities/    # JPA entities
│   │   │   ├── repository/      # Data access layer
│   │   │   └── service/         # Business logic
│   │   └── resources/
│   │       └── application.yml  # Application configuration
│   └── test/                    # Unit tests
├── database-schema.sql          # Database initialization script
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Application Docker image
└── build.gradle.kts            # Gradle build configuration
```

## Notes

- The application uses a filtering score threshold of 0.3 to exclude low-relevance results
- Document embeddings are generated automatically when documents are created or reindexed
- Client search does not require embeddings, using only PostgreSQL native search capabilities
- The hybrid search service merges results from different strategies and ranks them by relevance

