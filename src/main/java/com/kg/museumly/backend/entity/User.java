package com.kg.museumly.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Why each piece:
 *
 * @Entity tells JPA: this class maps to a database table. @Table(name = "users") says which one —
 * without it, JPA would guess the table name from the class name (User → maybe user), so being explicit avoids mismatches.
 * @Id marks the primary key field. Unlike auto-increment integer IDs, JPA won’t generate this value
 * for you — your SQL default (gen_random_uuid()) does that, at the database level, on insert. So no @GeneratedValue here.
 * @Column(name = "google_sub", ...) — Java convention is camelCase (googleSub), SQL convention
 * is snake_case (google_sub). JPA doesn’t guess this mapping reliably across all setups, so being explicit avoids surprises.
 * I didn’t add it to email or id since those names are already identical in both conventions.
 * Instant createdAt — maps to timestamptz. Instant is Java’s “a single point in time, UTC,
 * no timezone ambiguity” type, which matches your timestamptz decision.
 * (LocalDateTime would be the wrong choice here — it has no timezone concept at all.)
 * The no-arg protected User() constructor — JPA/Hibernate builds objects via
 * reflection without calling your constructor, but it still requires some
 * constructor to exist. protected (not public) signals “this is for the framework,
 * not for you to call” while still satisfying that requirement.
 * The real constructor plus only getters, no setters — this makes the entity
 * effectively immutable after creation. You don’t have an “update user” use case yet,
 * so there’s nothing to set. If that changes later, you add setters for exactly the fields that need to change, not preemptively.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(nullable = false)
    private String email;

    @Column(name = "created_at" , nullable = false)
    private Instant createdAt;

    // jpa requires it.
    protected User(){

    }

    public User(UUID id, String googleSub, String email, Instant createdAt) {
        this.id = id;
        this.googleSub = googleSub;
        this.email = email;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
