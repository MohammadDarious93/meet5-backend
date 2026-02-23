package com.meet5.socialnetwork.dto;

import java.time.Instant;

public record InteractionResponse(
        String status,
        long actorId,
        long targetId,
        Instant createdAt
) {
}
