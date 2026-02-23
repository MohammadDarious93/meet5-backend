package com.meet5.socialnetwork.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkUserInsertRequest(
        @NotEmpty List<@Valid CreateUserRequest> users
) {
}
