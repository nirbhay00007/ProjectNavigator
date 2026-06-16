package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RepositoryJpaRepo extends JpaRepository<RepositoryEntity, String> {

    /** Fetch all repos belonging to a specific user */
    List<RepositoryEntity> findAllByUserId(String userId);

    /** Get just the IDs — used by CleanupService to cascade-delete graph data */
    @Query("SELECT r.id FROM RepositoryEntity r WHERE r.userId = :userId")
    List<String> findIdsByUserId(@Param("userId") String userId);

    /** Delete all repository rows for a user (called after graph data is removed) */
    @Modifying
    @Transactional
    @Query("DELETE FROM RepositoryEntity r WHERE r.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
