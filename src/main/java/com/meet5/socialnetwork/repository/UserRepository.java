package com.meet5.socialnetwork.repository;

import com.meet5.socialnetwork.domain.UserProfile;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    UserProfile create(UserProfile userProfile);

    Optional<UserProfile> findById(long id);

    int batchInsert(List<UserProfile> users, int batchSize);

    void markFraud(long userId, Instant flaggedAt);

    Collection<Long> existingIds(Collection<Long> ids);
}
