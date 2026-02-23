package com.meet5.socialnetwork.dto;

import java.time.Instant;
import java.util.Map;

public record UserResponse(
        long id,
        String name,
        int age,
        boolean fraud,
        Instant createdAt,
        Map<String, String> attributes
) {
}
