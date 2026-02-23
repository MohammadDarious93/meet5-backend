package com.meet5.socialnetwork.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record CreateUserRequest(
        @NotBlank String name,
        @Min(13) @Max(120) int age,
        Map<String, String> attributes
) {
}
