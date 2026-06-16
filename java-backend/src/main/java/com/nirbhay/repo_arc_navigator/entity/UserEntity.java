package com.nirbhay.repo_arc_navigator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an authenticated user (created on first OAuth login).
 * The user record is NEVER deleted — only graph data is cleaned up on token expiry.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;  // UUID as string

    @Column(name = "email", nullable = false, unique = true, columnDefinition = "TEXT")
    private String email;

    @Column(name = "username", columnDefinition = "TEXT")
    private String username;

    /** OAuth provider: "google" or "github" */
    @Column(name = "provider", length = 20)
    private String provider;

    /**
     * Set to NOW() + 30min on every login.
     * The CleanupService deletes all graph data for users where token_expires_at < NOW().
     * Reset to NULL after cleanup so the job doesn't re-run for the same user.
     */
    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (id == null) id = UUID.randomUUID().toString();
    }
}
