package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Represents a single file/module node in the dependency graph.
 * Mirrors NodeMetadata from graphStore.ts.
 *
 * Arrays (keyExports, patterns, etc.) are stored as JSON text in PostgreSQL TEXT columns.
 * The PersistenceService serialises/deserialises them via Jackson.
 */
@Entity
@Table(name = "file_nodes", indexes = {
    @Index(name = "idx_file_nodes_repo_id", columnList = "repo_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileNodeEntity {

    /** Absolute file path — used as the primary key (stable, unique per file). */
    @Id
    @Column(name = "id", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String id;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "repo_label")
    private String repoLabel;

    @Column(name = "repo_path", columnDefinition = "TEXT")
    private String repoPath;

    @Column(name = "repo_url", columnDefinition = "TEXT")
    private String repoUrl;

    // ── AI-generated fields ────────────────────────────────────────────────────

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "responsibility", columnDefinition = "TEXT")
    private String responsibility;

    @Column(name = "is_entry_point")
    private boolean isEntryPoint;

    /** JSON array of exported symbol names, e.g. ["AuthService","login","logout"] */
    @Column(name = "key_exports", columnDefinition = "TEXT")
    private String keyExports;

    /** JSON array of internal function/method calls */
    @Column(name = "internal_calls", columnDefinition = "TEXT")
    private String internalCalls;

    /** low | medium | high */
    @Column(name = "complexity")
    private String complexity;

    /** presentation | business_logic | data_access | infrastructure | utility | config | unknown */
    @Column(name = "layer")
    private String layer;

    /** low | medium | high */
    @Column(name = "risk_category")
    private String riskCategory;

    /** clean | acceptable | needs_refactor */
    @Column(name = "code_quality")
    private String codeQuality;

    /** JSON array of design pattern names */
    @Column(name = "patterns", columnDefinition = "TEXT")
    private String patterns;

    /** JSON array of external dependency names (npm packages / Maven coords) */
    @Column(name = "external_deps", columnDefinition = "TEXT")
    private String externalDeps;

    // ── Graph-computed metrics ─────────────────────────────────────────────────

    @Column(name = "commit_churn")
    private int commitChurn;

    @Column(name = "fan_in")
    private int fanIn;

    @Column(name = "fan_out")
    private int fanOut;

    @Column(name = "is_orphan")
    private boolean isOrphan;
}
