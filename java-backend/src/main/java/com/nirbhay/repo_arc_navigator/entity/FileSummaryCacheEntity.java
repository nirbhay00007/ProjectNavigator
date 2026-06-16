package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * Cache table for LLM-generated file summaries.
 *
 * Key: "{filePath}::{sha256-of-file-content}"
 * Value: the full FileSummary JSON blob produced by Ollama/Gemini
 *
 * This prevents re-summarising files whose content hasn't changed,
 * even across server restarts — replacing the previous .dev-clash/cache.json file.
 */
@Entity
@Table(name = "file_summary_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileSummaryCacheEntity {

    /**
     * Composite cache key: "{filePath}::{contentHash}"
     * Identical to the key used in the old persistentStore.ts cache.json format
     * so the Node.js layer needs no changes to key generation logic.
     */
    @Id
    @Column(name = "cache_key", columnDefinition = "TEXT", nullable = false)
    private String cacheKey;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    /** SHA-256 hash of the file content at the time of summarisation. */
    @Column(name = "content_hash")
    private String contentHash;

    /**
     * Full FileSummary serialised as JSON.
     * e.g. { "summary": "...", "responsibility": "...", "keyExports": [...], ... }
     */
    @Column(name = "summary_json", columnDefinition = "TEXT", nullable = false)
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
