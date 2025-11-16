-- Database Schema (run manually before starting app)

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    country_of_residence VARCHAR(255)
);

-- Trigram indexes for fuzzy matching
CREATE INDEX clients_first_name_trgm_idx ON clients USING gin(first_name gin_trgm_ops);
CREATE INDEX clients_last_name_trgm_idx ON clients USING gin(last_name gin_trgm_ops);
CREATE INDEX clients_email_trgm_idx ON clients USING gin(email gin_trgm_ops);

CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    summary TEXT
);

ALTER TABLE documents ADD COLUMN IF NOT EXISTS client_id UUID;

-- Add foreign key constraint to the clients table
DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'fk_documents_client_id'
              AND conrelid = 'documents'::regclass
        ) THEN
            ALTER TABLE documents
                ADD CONSTRAINT fk_documents_client_id
                    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE;
        END IF;
    END $$;

-- Vector similarity indexes
CREATE INDEX ON documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Full-text search indexes for documents
CREATE INDEX documents_search_idx ON documents
    USING gin (to_tsvector('english', title || ' ' || content));

