package com.nirbhay.repo_arc_navigator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nirbhay.repo_arc_navigator.entity.*;
import com.nirbhay.repo_arc_navigator.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * PersistenceService — the single point of truth for reading/writing all
 * graph, vector, and cache data to PostgreSQL.
 *
 * All methods that deal with user-owned data now accept a userId parameter
 * extracted from the validated JWT by PersistenceController.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final RepositoryJpaRepo    repositoryRepo;
    private final FileNodeJpaRepo      fileNodeRepo;
    private final DependencyEdgeJpaRepo edgeRepo;
    private final VectorEmbeddingJpaRepo vectorRepo;
    private final FileSummaryCacheJpaRepo cacheRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ────────────────────────────────────────────────────────────────────────────
    // Repository CRUD
    // ────────────────────────────────────────────────────────────────────────────

    public RepositoryEntity upsertRepository(Map<String, Object> body, String userId) {
        String id = (String) body.get("id");
        RepositoryEntity entity = repositoryRepo.findById(id).orElse(new RepositoryEntity());
        entity.setId(id);
        entity.setUserId(userId);
        entity.setRepoLabel(str(body, "repoLabel"));
        entity.setRepoPath(str(body, "repoPath"));
        entity.setRepoUrl(str(body, "repoUrl"));
        entity.setLanguage(str(body, "language"));
        entity.setAnalyzedAt(Instant.now());
        entity.setFileCount(intVal(body, "fileCount"));
        entity.setDurationMs(longVal(body, "durationMs"));
        return repositoryRepo.save(entity);
    }

    public Optional<RepositoryEntity> getRepository(String repoId) {
        return repositoryRepo.findById(repoId);
    }

    /** Returns only repos owned by the given user */
    public List<RepositoryEntity> getAllRepositories(String userId) {
        if (userId == null) return repositoryRepo.findAll();
        return repositoryRepo.findAllByUserId(userId);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // File Nodes — batch upsert
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public int upsertFileNodes(String repoId, List<Map<String, Object>> nodes) {
        fileNodeRepo.deleteByRepoId(repoId);
        List<FileNodeEntity> entities = nodes.stream().map(n -> FileNodeEntity.builder()
                .id(str(n, "id"))
                .repoId(repoId)
                .repoLabel(str(n, "repoLabel"))
                .repoPath(str(n, "repoPath"))
                .repoUrl(str(n, "repoUrl"))
                .summary(str(n, "summary"))
                .responsibility(str(n, "responsibility"))
                .isEntryPoint(boolVal(n, "isEntryPoint"))
                .keyExports(toJson(n.get("keyExports")))
                .internalCalls(toJson(n.get("internalCalls")))
                .complexity(str(n, "complexity"))
                .layer(str(n, "layer"))
                .riskCategory(str(n, "riskCategory"))
                .codeQuality(str(n, "codeQuality"))
                .patterns(toJson(n.get("patterns")))
                .externalDeps(toJson(n.get("externalDeps")))
                .commitChurn(intVal(n, "commitChurn"))
                .fanIn(intVal(n, "fanIn"))
                .fanOut(intVal(n, "fanOut"))
                .isOrphan(boolVal(n, "isOrphan"))
                .build())
            .toList();
        fileNodeRepo.saveAll(entities);
        log.info("[Persistence] Saved {} file nodes for repo {}", entities.size(), repoId);
        return entities.size();
    }

    public List<FileNodeEntity> getFileNodes(String repoId) {
        return fileNodeRepo.findByRepoId(repoId);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Dependency Edges — batch upsert
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public int upsertEdges(String repoId, List<Map<String, Object>> edges) {
        edgeRepo.deleteByRepoId(repoId);
        List<DependencyEdgeEntity> entities = edges.stream().map(e -> DependencyEdgeEntity.builder()
                .repoId(repoId)
                .source(str(e, "source"))
                .target(str(e, "target"))
                .build())
            .toList();
        edgeRepo.saveAll(entities);
        log.info("[Persistence] Saved {} edges for repo {}", entities.size(), repoId);
        return entities.size();
    }

    public List<DependencyEdgeEntity> getEdges(String repoId) {
        return edgeRepo.findByRepoId(repoId);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Vector Embeddings — batch upsert
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public int upsertVectors(String repoId, List<Map<String, Object>> docs) {
        vectorRepo.deleteByRepoId(repoId);
        List<VectorEmbeddingEntity> entities = docs.stream().map(d -> VectorEmbeddingEntity.builder()
                .fileNodeId(str(d, "filePath"))
                .repoId(repoId)
                .compositeText(str(d, "compositeText"))
                .vectorJson(toJson(d.get("vector")))
                .fileBasename(str(d, "fileBasename"))
                .summary(str(d, "summary"))
                .responsibility(str(d, "responsibility"))
                .keyExports(toJson(d.get("keyExports")))
                .internalCalls(toJson(d.get("internalCalls")))
                .patterns(toJson(d.get("patterns")))
                .complexity(str(d, "complexity"))
                .isEntryPoint(boolVal(d, "isEntryPoint"))
                .build())
            .toList();
        vectorRepo.saveAll(entities);
        log.info("[Persistence] Saved {} vectors for repo {}", entities.size(), repoId);
        return entities.size();
    }

    @Transactional
    public void upsertSingleVector(String repoId, Map<String, Object> doc) {
        String fileNodeId = str(doc, "filePath");
        vectorRepo.deleteByFileNodeId(fileNodeId);
        VectorEmbeddingEntity entity = VectorEmbeddingEntity.builder()
                .fileNodeId(fileNodeId)
                .repoId(repoId)
                .compositeText(str(doc, "compositeText"))
                .vectorJson(toJson(doc.get("vector")))
                .fileBasename(str(doc, "fileBasename"))
                .summary(str(doc, "summary"))
                .responsibility(str(doc, "responsibility"))
                .keyExports(toJson(doc.get("keyExports")))
                .internalCalls(toJson(doc.get("internalCalls")))
                .patterns(toJson(doc.get("patterns")))
                .complexity(str(doc, "complexity"))
                .isEntryPoint(boolVal(doc, "isEntryPoint"))
                .build();
        vectorRepo.save(entity);
    }

    public List<VectorEmbeddingEntity> getVectors(String repoId) {
        return vectorRepo.findByRepoId(repoId);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // File Summary Cache
    // ────────────────────────────────────────────────────────────────────────────

    public Optional<FileSummaryCacheEntity> getCachedSummary(String cacheKey) {
        return cacheRepo.findById(cacheKey);
    }

    public void setCachedSummary(String filePath, String contentHash, String summaryJson) {
        String key = filePath + "::" + contentHash;
        FileSummaryCacheEntity entity = FileSummaryCacheEntity.builder()
                .cacheKey(key)
                .filePath(filePath)
                .contentHash(contentHash)
                .summaryJson(summaryJson)
                .createdAt(Instant.now())
                .build();
        try {
            cacheRepo.save(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.debug("[Persistence] Cache key already exists (concurrent insert): {}", key);
        }
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Full Repo Deletion
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteRepo(String repoId) {
        vectorRepo.deleteByRepoId(repoId);
        edgeRepo.deleteByRepoId(repoId);
        fileNodeRepo.deleteByRepoId(repoId);
        repositoryRepo.deleteById(repoId);
        log.info("[Persistence] Deleted all data for repo {}", repoId);
    }

    @Transactional
    public void deleteAllUserData(String userId) {
        if (userId == null) return;
        List<String> repoIds = repositoryRepo.findIdsByUserId(userId);
        for (String repoId : repoIds) {
            deleteRepo(repoId);
        }
        log.info("[Persistence] Deleted all data for user {}", userId);
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Helper utilities
    // ────────────────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int intVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) { try { return Integer.parseInt(s); } catch (Exception ignored) {} }
        return 0;
    }

    private long longVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) { try { return Long.parseLong(s); } catch (Exception ignored) {} }
        return 0L;
    }

    private boolean boolVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return false;
    }

    private String toJson(Object value) {
        if (value == null) return "[]";
        if (value instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
