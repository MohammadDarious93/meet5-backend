package com.meet5.socialnetwork.repository;

import com.meet5.socialnetwork.domain.ProfileVisitorView;

import java.time.Instant;
import java.util.List;

public interface InteractionRepository {
    void insertVisit(long visitorId, long visitedId, Instant createdAt);

    boolean insertLikeIfNotExists(long likerId, long likedId, Instant createdAt);

    List<ProfileVisitorView> findVisitorsByUserId(long userId, int limit, int offset);

    int countDistinctVisitedUsersInWindow(long visitorId, Instant windowStart, Instant windowEnd);

    int countDistinctLikedUsersInWindow(long likerId, Instant windowStart, Instant windowEnd);
}
