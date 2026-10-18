package com.kg.museumly.backend.repository;

import com.kg.museumly.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * extends JpaRepository<User, UUID> — User is the entity, UUID is the type of its @Id field.
 * By extending this interface (no implementation body needed),
 * Spring Data JPA generates a working implementation at startup
 * via a dynamic proxy. You get save(), findById(), findAll(), deleteById(), etc. for free — none of that is code you write.
 *
 * findByGoogleSub(String googleSub) — this one method you do declare
 * yourself, with no body, because Spring Data parses the method name itself:
 * “find by googleSub” tells it to generate SELECT * FROM users WHERE google_sub = ?.
 * This works because the method name matches the field name on the entity (googleSub),
 * not the column name (google_sub) — Spring Data looks at your Java field names,
 * not your SQL column names, then uses the @Column mapping to translate.
 *
 *  Why this specific method, and not findByEmail: this is the lookup your sign-in flow needs.
 * When POST /auth/google runs, you’ll have a sub claim from the verified Google token,
 * and the question is “does a user with this sub already exist, or do I need to create one.”
 * google_sub is your unique identity key (we discussed why on the migration), so that’s the lookup — not email.
 *
 * Optional<User>, not User — because the user might not exist yet (first sign-in).
 * Optional forces whoever calls this to explicitly handle the “not found, I must create one” branch,
 * rather than risking a silent null.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * SELECT * FROM users WHERE google_sub = ?
     */
    Optional<User> findByGoogleSub(String googleSub);
}
