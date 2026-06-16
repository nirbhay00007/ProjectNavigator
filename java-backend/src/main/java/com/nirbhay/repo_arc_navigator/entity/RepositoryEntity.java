package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single analysed repository.
 * One row per repoId — created/updated when the Node.js pipeline
 * calls POST /persist/repository.
 */
@Entity
@Table(name = "repositories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepositoryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;           // repoId sent by Node.js (e.g. UUID or slug)

    /** Owner — set when Node.js forwards the JWT userId. Null for legacy/unowned rows. */
    @Column(name = "user_id")
    private String userId;

    @Column(name = "repo_label", nullable = false)
    private String repoLabel;    // Human-readable name shown in UI

    @Column(name = "repo_path", columnDefinition = "TEXT")
    private String repoPath;     // Absolute local path (null for remote-only)

    @Column(name = "repo_url", columnDefinition = "TEXT")
    private String repoUrl;      // Git remote URL

    @Column(name = "language")
    private String language;     // Detected primary language

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "file_count")
    private int fileCount;

    @Column(name = "duration_ms")
    private long durationMs;
}
