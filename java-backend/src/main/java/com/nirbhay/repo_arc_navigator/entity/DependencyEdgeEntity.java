package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;

/**
 * Represents a directed import/dependency edge between two file nodes.
 * Mirrors the Edge interface from graphStore.ts.
 */
@Entity
@Table(name = "dependency_edges", indexes = {
    @Index(name = "idx_edges_repo_id",  columnList = "repo_id"),
    @Index(name = "idx_edges_source",   columnList = "source"),
    @Index(name = "idx_edges_target",   columnList = "target")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyEdgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    /** Absolute path of the importing file (source of the edge). */
    @Column(name = "source", columnDefinition = "TEXT", nullable = false)
    private String source;

    /** Absolute path of the imported file (target of the edge). */
    @Column(name = "target", columnDefinition = "TEXT", nullable = false)
    private String target;
}
