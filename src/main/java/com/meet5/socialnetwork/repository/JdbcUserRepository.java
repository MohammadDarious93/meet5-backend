package com.meet5.socialnetwork.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meet5.socialnetwork.domain.UserProfile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
/**
 * JDBC implementation for user persistence, including batch ingestion and fraud flagging.
 */
public class JdbcUserRepository implements UserRepository {

    private static final TypeReference<Map<String, String>> ATTR_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcUserRepository(
            JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public UserProfile create(UserProfile userProfile) {
        String sql = "INSERT INTO users(name, age, created_at, is_fraud, attributes_json) VALUES(?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, userProfile.getName());
            ps.setInt(2, userProfile.getAge());
            ps.setObject(3, userProfile.getCreatedAt());
            ps.setBoolean(4, userProfile.isFraud());
            ps.setString(5, toJson(userProfile.getAttributes()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new DataIntegrityViolationException("failed to create user");
        }

        return UserProfile.existing(
                key.longValue(),
                userProfile.getName(),
                userProfile.getAge(),
                userProfile.getCreatedAt(),
                userProfile.getAttributes(),
                userProfile.isFraud()
        );
    }

    @Override
    public Optional<UserProfile> findById(long id) {
        String sql = """
                SELECT id, name, age, created_at, is_fraud, attributes_json
                FROM users
                WHERE id = ?
                """;
        List<UserProfile> result = jdbcTemplate.query(sql, (rs, rowNum) -> UserProfile.existing(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("age"),
                rs.getTimestamp("created_at").toInstant(),
                fromJson(rs.getString("attributes_json")),
                rs.getBoolean("is_fraud")
        ), id);
        return result.stream().findFirst();
    }

    @Override
    /** Inserts users in chunks to reduce round trips and improve throughput. */
    public int batchInsert(List<UserProfile> users, int batchSize) {
        String sql = "INSERT INTO users(name, age, created_at, is_fraud, attributes_json) VALUES(?, ?, ?, ?, ?)";
        int inserted = 0;
        for (int i = 0; i < users.size(); i += batchSize) {
            int end = Math.min(i + batchSize, users.size());
            List<UserProfile> chunk = users.subList(i, end);
            int[][] results = jdbcTemplate.batchUpdate(sql, chunk, chunk.size(), (ps, user) -> {
                ps.setString(1, user.getName());
                ps.setInt(2, user.getAge());
                ps.setObject(3, user.getCreatedAt());
                ps.setBoolean(4, user.isFraud());
                ps.setString(5, toJson(user.getAttributes()));
            });
            for (int[] batchResult : results) {
                inserted += batchResult.length;
            }
        }
        return inserted;
    }

    @Override
    public void markFraud(long userId, Instant flaggedAt) {
        String sql = """
                UPDATE users
                SET is_fraud = TRUE,
                    fraud_marked_at = ?
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, flaggedAt, userId);
    }

    @Override
    public Collection<Long> existingIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Set.of();
        }
        String sql = "SELECT id FROM users WHERE id IN (:ids)";
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        List<Long> matched = namedParameterJdbcTemplate.queryForList(sql, params, Long.class);
        return new HashSet<>(matched);
    }

    private String toJson(Map<String, String> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize attributes", e);
        }
    }

    private Map<String, String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, ATTR_TYPE);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
}
