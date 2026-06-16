package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.VectorEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VectorEmbeddingJpaRepo extends JpaRepository<VectorEmbeddingEntity, UUID> {

    /** Fetch all vectors for a repo (loaded into Node.js RAM for cosine similarity). */
    List<VectorEmbeddingEntity> findByRepoId(String repoId);

    /** Find existing embedding for a specific file (for upsert logic). */
    Optional<VectorEmbeddingEntity> findByFileNodeId(String fileNodeId);

    /** Delete all vectors for a repository (used before re-ingestion). */
    @Modifying
    @Transactional
    @Query("DELETE FROM VectorEmbeddingEntity v WHERE v.repoId = :repoId")
    void deleteByRepoId(@Param("repoId") String repoId);

    /** Delete vector for a single file (used during incremental sync). */
    @Modifying
    @Transactional
    @Query("DELETE FROM VectorEmbeddingEntity v WHERE v.fileNodeId = :fileNodeId")
    void deleteByFileNodeId(@Param("fileNodeId") String fileNodeId);
}
