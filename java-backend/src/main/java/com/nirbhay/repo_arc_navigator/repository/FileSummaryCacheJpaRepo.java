package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.FileSummaryCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileSummaryCacheJpaRepo extends JpaRepository<FileSummaryCacheEntity, String> {
    // findById(cacheKey) — inherited from JpaRepository
    // save(entity)       — inherited from JpaRepository
}
