package com.meet5.socialnetwork.controller;

import com.meet5.socialnetwork.dto.BulkInsertResult;
import com.meet5.socialnetwork.dto.BulkUserInsertRequest;
import com.meet5.socialnetwork.dto.CreateUserRequest;
import com.meet5.socialnetwork.dto.UserResponse;
import com.meet5.socialnetwork.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
/**
 * Exposes user creation APIs, including single and bulk onboarding flows.
 */
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    /** Creates one user profile and returns its persisted representation. */
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    /** Inserts a batch of users using repository-level JDBC batching. */
    public BulkInsertResult bulkInsert(@Valid @RequestBody BulkUserInsertRequest request) {
        return userService.bulkInsert(request.users());
    }
}
