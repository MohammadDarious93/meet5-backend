package com.meet5.socialnetwork.service;

import com.meet5.socialnetwork.domain.UserProfile;
import com.meet5.socialnetwork.dto.BulkInsertResult;
import com.meet5.socialnetwork.dto.CreateUserRequest;
import com.meet5.socialnetwork.dto.UserResponse;
import com.meet5.socialnetwork.exception.BadRequestException;
import com.meet5.socialnetwork.exception.NotFoundException;
import com.meet5.socialnetwork.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
/**
 * Coordinates user lifecycle operations and maps domain objects to API DTOs.
 */
public class UserService {

    private static final int BATCH_SIZE = 500;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    /** Creates and persists a validated user profile. */
    public UserResponse create(CreateUserRequest request) {
        UserProfile saved = userRepository.create(UserProfile.newUser(request.name(), request.age(), request.attributes()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    /** Loads a user or fails with a not-found business exception. */
    public UserProfile getOrThrow(long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("user not found: " + userId));
    }

    @Transactional
    /** Performs high-throughput user insertion using fixed-size JDBC batches. */
    public BulkInsertResult bulkInsert(List<CreateUserRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("users payload cannot be empty");
        }
        List<UserProfile> users = requests.stream()
                .map(req -> UserProfile.newUser(req.name(), req.age(), req.attributes()))
                .toList();
        int inserted = userRepository.batchInsert(users, BATCH_SIZE);
        return new BulkInsertResult(requests.size(), inserted);
    }

    public UserResponse toResponse(UserProfile userProfile) {
        return new UserResponse(
                userProfile.getId(),
                userProfile.getName(),
                userProfile.getAge(),
                userProfile.isFraud(),
                userProfile.getCreatedAt(),
                userProfile.getAttributes()
        );
    }
}
