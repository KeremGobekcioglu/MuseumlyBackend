package com.kg.museumly.backend.service;

import com.kg.museumly.backend.entity.User;
import com.kg.museumly.backend.exception.ApiException;
import com.kg.museumly.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * @Service — same idea as @Entity/@Bean before: it tells Spring “manage this as a bean,”
 * so it can be injected elsewhere (into the controller, later).
 *
 * Constructor injection (private final UserRepository userRepository, set via constructor) —
 * this is how UserService gets a working UserRepository without calling new UserRepository() itself.
 * Spring sees UserService needs a UserRepository in its constructor, finds the proxy bean it generated earlier,
 * and passes it in automatically. final because once constructed, this dependency never changes.
 *
 * findByGoogleSub(googleSub).orElseGet(() -> ...) — this is Optional’s way of saying “if present, use it;
 * if empty, run this fallback and use that instead.” orElseGet (not orElse) is important here: orElse would always
 * construct a new User(...) eagerly, even when one was found, just to throw it away. orElseGet only runs the lambda
 * when actually needed.
 *
 * UUID.randomUUID() in Java, not the database default — this is worth noticing: your SQL has DEFAULT gen_random_uuid(),
 * but since we pass an explicit id into the User constructor, Postgres’s default never fires; it only applies when a column
 * is omitted from the INSERT. Generating it here in Java is fine and common, but it’s the kind of inconsistency worth being
 * aware of — the DB default is effectively dead code right now. Not a bug, just something to know.
 *
 * No UserDto used yet — this method returns the entity, not the DTO. That’s intentional:
 * converting entity → DTO is the controller’s job, not the service’s, so the service stays reusable for non-HTTP callers too.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository)
    {
        this.userRepository = userRepository;
    }

    /**
     * So the dividing line is: the service produces the full, true internal object.
     * The controller decides what subset of that is safe to hand to the outside world
     * and shapes it into a DTO right before returning the HTTP response. That’s the concrete meaning
     * of “controllers return DTOs, never entities” — the conversion happens at the controller boundary, not earlier.
     */
    public User findOrCreateByGoogleSub(String googleSub, String email) {
        return userRepository.findByGoogleSub(googleSub)
                .orElseGet(() -> {
                    User newUser = new User(UUID.randomUUID(), googleSub, email, Instant.now());
                    return userRepository.save(newUser);
                });
    }

    public User getById(UUID id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User no longer exists");
        }
        return user.get();
    }
}
