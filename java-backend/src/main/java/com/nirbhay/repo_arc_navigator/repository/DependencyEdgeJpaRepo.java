package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.DependencyEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface DependencyEdgeJpaRepo extends JpaRepository<DependencyEdgeEntity, UUID> {

    /** Fetch all edges belonging to a specific repository. */
    List<DependencyEdgeEntity> findByRepoId(String repoId);

    /** Delete all edges for a repository (used before re-ingestion). */
    @Modifying
    @Transactional
    @Query("DELETE FROM DependencyEdgeEntity e WHERE e.repoId = :repoId")
    void deleteByRepoId(@Param("repoId") String repoId);
}
