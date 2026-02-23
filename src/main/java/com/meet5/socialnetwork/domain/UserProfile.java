package com.meet5.socialnetwork.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class UserProfile {
    private final Long id;
    private final String name;
    private final int age;
    private final Instant createdAt;
    private final Map<String, String> attributes;
    private final boolean fraud;

    private UserProfile(
            Long id,
            String name,
            int age,
            Instant createdAt,
            Map<String, String> attributes,
            boolean fraud
    ) {
        validate(name, age);
        validateAttributes(attributes);
        this.id = id;
        this.name = name.trim();
        this.age = age;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.attributes = Collections.unmodifiableMap(Map.copyOf(attributes == null ? Map.of() : attributes));
        this.fraud = fraud;
    }

    public static UserProfile newUser(String name, int age, Map<String, String> attributes) {
        return new UserProfile(null, name, age, Instant.now(), attributes, false);
    }

    public static UserProfile existing(
            Long id,
            String name,
            int age,
            Instant createdAt,
            Map<String, String> attributes,
            boolean fraud
    ) {
        Objects.requireNonNull(id, "id must not be null");
        return new UserProfile(id, name, age, createdAt, attributes, fraud);
    }

    private static void validate(String name, int age) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (age < 13 || age > 120) {
            throw new IllegalArgumentException("age must be between 13 and 120");
        }
    }

    private static void validateAttributes(Map<String, String> attributes) {
        if (attributes == null) {
            return;
        }
        if (attributes.size() > 50) {
            throw new IllegalArgumentException("attributes cannot contain more than 50 entries");
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("attribute key cannot be empty");
            }
            if (key.length() > 40) {
                throw new IllegalArgumentException("attribute key length cannot exceed 40");
            }
            if (value != null && value.length() > 255) {
                throw new IllegalArgumentException("attribute value length cannot exceed 255");
            }
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public boolean isFraud() {
        return fraud;
    }

    public Optional<String> getAttribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }
}
