CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    age INT NOT NULL,
    attributes_json JSON NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    is_fraud BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_marked_at TIMESTAMP(3) NULL,
    CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CHECK (age BETWEEN 13 AND 120)
);

CREATE TABLE profile_visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    visitor_user_id BIGINT NOT NULL,
    visited_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_visits_visitor FOREIGN KEY (visitor_user_id) REFERENCES users (id),
    CONSTRAINT fk_visits_visited FOREIGN KEY (visited_user_id) REFERENCES users (id),
    CHECK (visitor_user_id <> visited_user_id)
);

CREATE TABLE profile_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    liker_user_id BIGINT NOT NULL,
    liked_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_likes_liker FOREIGN KEY (liker_user_id) REFERENCES users (id),
    CONSTRAINT fk_likes_liked FOREIGN KEY (liked_user_id) REFERENCES users (id),
    CONSTRAINT uq_liker_liked UNIQUE (liker_user_id, liked_user_id),
    CHECK (liker_user_id <> liked_user_id)
);

CREATE INDEX idx_visits_visited_created_id
    ON profile_visits (visited_user_id, created_at DESC, id DESC);
CREATE INDEX idx_visits_visitor_created
    ON profile_visits (visitor_user_id, created_at);

CREATE INDEX idx_likes_liked_created_id
    ON profile_likes (liked_user_id, created_at DESC, id DESC);
CREATE INDEX idx_likes_liker_created
    ON profile_likes (liker_user_id, created_at);

CREATE INDEX idx_users_created ON users (created_at);
CREATE INDEX idx_users_fraud ON users (is_fraud);
