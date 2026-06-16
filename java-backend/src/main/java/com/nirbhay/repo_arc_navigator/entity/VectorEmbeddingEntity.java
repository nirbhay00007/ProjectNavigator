package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

/**
 * Stores the embedding vector for a file node.
 *
 * The vector is a float array (768 dimensions for nomic-embed-text,
 * 1536 for Gemini) serialised as a JSON text string.
 * Cosine similarity search is performed in the Node.js layer after
 * fetching all vectors for a repo.
 *
 * This is a 1-to-1 relationship with FileNodeEntity — one embedding per file.
 */
@Entity
@Table(name = "vector_embeddings", indexes = {
    @Index(name = "idx_vectors_file_node_id", columnList = "file_node_id"),
    @Index(name = "idx_vectors_repo_id",      columnList = "repo_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorEmbeddingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK to file_nodes.id (absolute file path). */
    @Column(name = "file_node_id", columnDefinition = "TEXT", nullable = false)
    private String fileNodeId;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    /**
     * The composite text that was embedded (kept for re-embedding / debugging).
     * e.g. "File: AuthService.ts\nPurpose: Handles JWT authentication..."
     */
    @Column(name = "composite_text", columnDefinition = "TEXT")
    private String compositeText;

    /**
     * JSON-serialised float array of embedding values.
     * e.g. "[0.123, -0.456, 0.789, ...]"
     * Deserialised by the Node.js backend for in-memory cosine similarity search.
     */
    @Column(name = "vector_json", columnDefinition = "TEXT", nullable = false)
    private String vectorJson;

    /** Basename of the file — stored for quick search result display. */
    @Column(name = "file_basename")
    private String fileBasename;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "responsibility", columnDefinition = "TEXT")
    private String responsibility;

    @Column(name = "key_exports", columnDefinition = "TEXT")
    private String keyExports;

    @Column(name = "internal_calls", columnDefinition = "TEXT")
    private String internalCalls;

    @Column(name = "patterns", columnDefinition = "TEXT")
    private String patterns;

    @Column(name = "complexity")
    private String complexity;

    @Column(name = "is_entry_point")
    private boolean isEntryPoint;
}
