package com.kg.museumly.backend.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Why a record here: Java records are immutable data carriers —
 * you get a constructor, getters (called id(), email(), createdAt(), not getId()),
 * equals/hashCode/toString, all generated, for a class whose entire job is “hold these fields,
 * nothing else.” A DTO is exactly that job, so it’s a good fit.
 * 
 * Notice googleSub is deliberately not here — the client never needs Google’s
 * internal subject ID; it’s purely a server-side lookup key.
 * @param id
 * @param email
 * @param createdAt
 */
public record UserDto(
    UUID id,
    String email,
    Instant createdAt
){}
