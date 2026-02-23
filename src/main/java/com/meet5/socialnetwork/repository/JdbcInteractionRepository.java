package com.meet5.socialnetwork.repository;

import com.meet5.socialnetwork.domain.ProfileVisitorView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class JdbcInteractionRepository implements InteractionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertVisit(long visitorId, long visitedId, Instant createdAt) {
        String sql = "INSERT INTO profile_visits(visitor_user_id, visited_user_id, created_at) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, visitorId, visitedId, Timestamp.from(createdAt));
    }

    @Override
    public boolean insertLikeIfNotExists(long likerId, long likedId, Instant createdAt) {
        String sql = "INSERT INTO profile_likes(liker_user_id, liked_user_id, created_at) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(sql, likerId, likedId, Timestamp.from(createdAt));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public List<ProfileVisitorView> findVisitorsByUserId(long userId, int limit, int offset) {
        String sql = """
                SELECT pv.visitor_user_id, u.name, u.age, pv.created_at
                FROM profile_visits pv
                JOIN users u ON u.id = pv.visitor_user_id
                WHERE pv.visited_user_id = ?
                ORDER BY pv.created_at DESC, pv.id DESC
                LIMIT ? OFFSET ?
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ProfileVisitorView(
                rs.getLong("visitor_user_id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getTimestamp("created_at").toInstant()
        ), userId, limit, offset);
    }

    @Override
    public int countDistinctVisitedUsersInWindow(long visitorId, Instant windowStart, Instant windowEnd) {
        String sql = """
                SELECT COUNT(DISTINCT visited_user_id)
                FROM profile_visits
                WHERE visitor_user_id = ?
                  AND created_at >= ?
                  AND created_at <= ?
                """;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                visitorId,
                Timestamp.from(windowStart),
                Timestamp.from(windowEnd)
        );
        return count == null ? 0 : count;
    }

    @Override
    public int countDistinctLikedUsersInWindow(long likerId, Instant windowStart, Instant windowEnd) {
        String sql = """
                SELECT COUNT(DISTINCT liked_user_id)
                FROM profile_likes
                WHERE liker_user_id = ?
                  AND created_at >= ?
                  AND created_at <= ?
                """;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                Integer.class,
                likerId,
                Timestamp.from(windowStart),
                Timestamp.from(windowEnd)
        );
        return count == null ? 0 : count;
    }
}
