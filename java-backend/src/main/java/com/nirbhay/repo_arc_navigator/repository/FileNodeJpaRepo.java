package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.FileNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FileNodeJpaRepo extends JpaRepository<FileNodeEntity, String> {

    /** Fetch all file nodes belonging to a specific repository. */
    List<FileNodeEntity> findByRepoId(String repoId);

    /** Delete all file nodes for a repository (used before re-ingestion). */
    @Modifying
    @Transactional
    @Query("DELETE FROM FileNodeEntity f WHERE f.repoId = :repoId")
    void deleteByRepoId(@Param("repoId") String repoId);
}
