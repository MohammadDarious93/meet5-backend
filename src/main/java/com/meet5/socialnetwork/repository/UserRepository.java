package com.meet5.socialnetwork.repository;

import com.meet5.socialnetwork.domain.UserProfile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persistence contract for user lifecycle, identity lookup, and fraud state updates.
 */
public interface UserRepository {
    UserProfile create(UserProfile userProfile);

    Optional<UserProfile> findById(long id);

    int batchInsert(List<UserProfile> users, int batchSize);

    /** Marks a user as fraud at a specific evaluation timestamp. */
    void markFraud(long userId, Instant flaggedAt);

    Collection<Long> existingIds(Collection<Long> ids);
}
