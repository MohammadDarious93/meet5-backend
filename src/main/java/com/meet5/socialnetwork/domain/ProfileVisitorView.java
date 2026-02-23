package com.meet5.socialnetwork.domain;

import java.time.Instant;

public record ProfileVisitorView(
        long visitorUserId,
        String visitorName,
        int visitorAge,
        Instant visitedAt
) {
}
