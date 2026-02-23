package com.meet5.socialnetwork.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VisitRequest(
        @NotNull @Positive Long visitorId,
        @NotNull @Positive Long visitedId
) {
}
