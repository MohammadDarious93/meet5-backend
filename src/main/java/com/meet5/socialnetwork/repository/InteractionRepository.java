package com.meet5.socialnetwork.repository;

import com.meet5.socialnetwork.domain.ProfileVisitorView;

import java.time.Instant;
import java.util.List;

/**
 * Persistence contract for visits/likes reads and writes used by interaction logic.
 */
public interface InteractionRepository {
    void insertVisit(long visitorId, long visitedId, Instant createdAt);

    boolean insertLikeIfNotExists(long likerId, long likedId, Instant createdAt);

    List<ProfileVisitorView> findVisitorsByUserId(long userId, int limit, int offset);

    /** Counts distinct visited users by actor within a bounded time window. */
    int countDistinctVisitedUsersInWindow(long visitorId, Instant windowStart, Instant windowEnd);

    /** Counts distinct liked users by actor within a bounded time window. */
    int countDistinctLikedUsersInWindow(long likerId, Instant windowStart, Instant windowEnd);
}
