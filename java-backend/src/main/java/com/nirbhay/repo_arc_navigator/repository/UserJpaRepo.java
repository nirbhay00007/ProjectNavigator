package com.nirbhay.repo_arc_navigator.repository;

import com.nirbhay.repo_arc_navigator.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepo extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findByEmail(String email);

    /** Used by CleanupService to find all users whose session has expired */
    List<UserEntity> findAllByTokenExpiresAtBefore(Instant now);

    /** Check if a user already exists (avoid duplicate inserts on re-login) */
    boolean existsByEmail(String email);
}
