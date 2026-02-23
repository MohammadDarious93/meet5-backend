package com.meet5.socialnetwork.service;

import com.meet5.socialnetwork.config.AppConfig;
import com.meet5.socialnetwork.domain.ProfileVisitorView;
import com.meet5.socialnetwork.domain.UserProfile;
import com.meet5.socialnetwork.dto.InteractionResponse;
import com.meet5.socialnetwork.exception.BadRequestException;
import com.meet5.socialnetwork.exception.ConflictException;
import com.meet5.socialnetwork.repository.InteractionRepository;
import com.meet5.socialnetwork.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AppConfig.FraudProperties fraudProperties;
    private final Clock clock;

    public InteractionService(
            InteractionRepository interactionRepository,
            UserService userService,
            UserRepository userRepository,
            FraudDetectionService fraudDetectionService,
            AppConfig.FraudProperties fraudProperties,
            Clock clock
    ) {
        this.interactionRepository = interactionRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.fraudDetectionService = fraudDetectionService;
        this.fraudProperties = fraudProperties;
        this.clock = clock;
    }

    @Transactional
    public InteractionResponse visit(long visitorId, long visitedId) {
        if (visitorId == visitedId && !fraudProperties.enableSelfVisit()) {
            throw new BadRequestException("users cannot visit themselves");
        }
        ensureUsersExist(visitorId, visitedId);

        UserProfile visitor = userService.getOrThrow(visitorId);
        if (visitor.isFraud()) {
            throw new ConflictException("fraud user cannot create interactions");
        }

        Instant now = Instant.now(clock);
        interactionRepository.insertVisit(visitorId, visitedId, now);
        fraudDetectionService.evaluateAfterInteraction(visitor);
        return new InteractionResponse("VISIT_RECORDED", visitorId, visitedId, now);
    }

    @Transactional
    public InteractionResponse like(long likerId, long likedId) {
        if (likerId == likedId) {
            throw new BadRequestException("users cannot like themselves");
        }
        ensureUsersExist(likerId, likedId);

        UserProfile liker = userService.getOrThrow(likerId);
        if (liker.isFraud()) {
            throw new ConflictException("fraud user cannot create interactions");
        }

        Instant now = Instant.now(clock);
        boolean inserted = interactionRepository.insertLikeIfNotExists(likerId, likedId, now);
        if (!inserted) {
            throw new ConflictException("duplicate like is not allowed");
        }
        fraudDetectionService.evaluateAfterInteraction(liker);
        return new InteractionResponse("LIKE_RECORDED", likerId, likedId, now);
    }

    @Transactional(readOnly = true)
    public List<ProfileVisitorView> getVisitors(long userId, int limit, int offset) {
        if (limit < 1 || limit > 500) {
            throw new BadRequestException("limit must be between 1 and 500");
        }
        if (offset < 0) {
            throw new BadRequestException("offset cannot be negative");
        }
        userService.getOrThrow(userId);
        return interactionRepository.findVisitorsByUserId(userId, limit, offset);
    }

    private void ensureUsersExist(long actorId, long targetId) {
        Set<Long> ids = Set.of(actorId, targetId);
        if (userRepository.existingIds(ids).size() != 2) {
            throw new BadRequestException("actor or target user does not exist");
        }
    }
}
