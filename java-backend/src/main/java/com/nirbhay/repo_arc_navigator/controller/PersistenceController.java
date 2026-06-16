package com.nirbhay.repo_arc_navigator.controller;

import com.nirbhay.repo_arc_navigator.entity.*;
import com.nirbhay.repo_arc_navigator.service.PersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PersistenceController — REST API consumed exclusively by the Node.js AI backend.
 * All endpoints live under /persist/** so they are clearly separated from the
 * existing /repo/** Java AST endpoints.
 *
 * userId is extracted from the JWT via Spring Security context.
 * If no JWT is present (legacy/anonymous call), userId is null.
 */
@Slf4j
@RestController
@RequestMapping("/persist")
@RequiredArgsConstructor
public class PersistenceController {

    private final PersistenceService svc;

    /** Extract userId from the validated JWT (set by JwtAuthFilter). */
    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return null;
    }

    // ── Repository ────────────────────────────────────────────────────────────

    @PostMapping("/repository")
    public ResponseEntity<RepositoryEntity> upsertRepository(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(svc.upsertRepository(body, currentUserId()));
    }

    @GetMapping("/repository/{repoId}")
    public ResponseEntity<?> getRepository(@PathVariable String repoId) {
        return svc.getRepository(repoId)
                  .map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/repositories")
    public ResponseEntity<List<RepositoryEntity>> getAllRepositories() {
        return ResponseEntity.ok(svc.getAllRepositories(currentUserId()));
    }

    @DeleteMapping("/repository/{repoId}")
    public ResponseEntity<Map<String, String>> deleteRepository(@PathVariable String repoId) {
        svc.deleteRepo(repoId);
        return ResponseEntity.ok(Map.of("status", "deleted", "repoId", repoId));
    }

    // ── File Nodes ────────────────────────────────────────────────────────────

    @PostMapping("/file-nodes")
    public ResponseEntity<Map<String, Object>> upsertFileNodes(@RequestBody Map<String, Object> body) {
        String repoId = (String) body.get("repoId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) body.get("nodes");
        int count = svc.upsertFileNodes(repoId, nodes);
        return ResponseEntity.ok(Map.of("saved", count, "repoId", repoId));
    }

    @GetMapping("/file-nodes/{repoId}")
    public ResponseEntity<List<FileNodeEntity>> getFileNodes(@PathVariable String repoId) {
        return ResponseEntity.ok(svc.getFileNodes(repoId));
    }

    // ── Dependency Edges ─────────────────────────────────────────────────────

    @PostMapping("/edges")
    public ResponseEntity<Map<String, Object>> upsertEdges(@RequestBody Map<String, Object> body) {
        String repoId = (String) body.get("repoId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> edges = (List<Map<String, Object>>) body.get("edges");
        int count = svc.upsertEdges(repoId, edges);
        return ResponseEntity.ok(Map.of("saved", count, "repoId", repoId));
    }

    @GetMapping("/edges/{repoId}")
    public ResponseEntity<List<DependencyEdgeEntity>> getEdges(@PathVariable String repoId) {
        return ResponseEntity.ok(svc.getEdges(repoId));
    }

    // ── Vector Embeddings ─────────────────────────────────────────────────────

    @PostMapping("/vectors")
    public ResponseEntity<Map<String, Object>> upsertVectors(@RequestBody Map<String, Object> body) {
        String repoId = (String) body.get("repoId");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> docs = (List<Map<String, Object>>) body.get("docs");
        int count = svc.upsertVectors(repoId, docs);
        return ResponseEntity.ok(Map.of("saved", count, "repoId", repoId));
    }

    @PostMapping("/vectors/upsert-one")
    public ResponseEntity<Map<String, String>> upsertSingleVector(@RequestBody Map<String, Object> body) {
        String repoId = (String) body.get("repoId");
        @SuppressWarnings("unchecked")
        Map<String, Object> doc = (Map<String, Object>) body.get("doc");
        svc.upsertSingleVector(repoId, doc);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/vectors/{repoId}")
    public ResponseEntity<List<VectorEmbeddingEntity>> getVectors(@PathVariable String repoId) {
        return ResponseEntity.ok(svc.getVectors(repoId));
    }

    // ── File Summary Cache ────────────────────────────────────────────────────

    @GetMapping("/cache/{cacheKey}")
    public ResponseEntity<?> getCachedSummary(@PathVariable String cacheKey) {
        String decoded = cacheKey.replace("%3A%3A", "::");
        Optional<FileSummaryCacheEntity> cached = svc.getCachedSummary(decoded);
        return cached.map(c -> ResponseEntity.ok(Map.of("summaryJson", c.getSummaryJson())))
                     .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/cache")
    public ResponseEntity<Map<String, String>> setCachedSummary(@RequestBody Map<String, Object> body) {
        svc.setCachedSummary(
            (String) body.get("filePath"),
            (String) body.get("contentHash"),
            (String) body.get("summaryJson")
        );
        return ResponseEntity.ok(Map.of("status", "cached"));
    }

    // ── Health ────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "persistence-layer"));
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        // Session ends client-side by discarding the JWT.
        // Graph data is intentionally preserved so the user can resume
        // work on their next login. To delete a repo, use DELETE /persist/repository/{repoId}.
        return ResponseEntity.ok(Map.of("status", "logged_out", "message", "Session ended — data preserved"));
    }
}
