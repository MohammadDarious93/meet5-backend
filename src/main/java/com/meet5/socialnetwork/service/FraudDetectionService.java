package com.meet5.socialnetwork.service;

import com.meet5.socialnetwork.config.AppConfig;
import com.meet5.socialnetwork.domain.UserProfile;
import com.meet5.socialnetwork.repository.InteractionRepository;
import com.meet5.socialnetwork.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class FraudDetectionService {

    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final AppConfig.FraudProperties fraudProperties;
    private final Clock clock;

    public FraudDetectionService(
            InteractionRepository interactionRepository,
            UserRepository userRepository,
            AppConfig.FraudProperties fraudProperties,
            Clock clock
    ) {
        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
        this.fraudProperties = fraudProperties;
        this.clock = clock;
    }

    public void evaluateAfterInteraction(UserProfile actor) {
        if (actor.isFraud()) {
            return;
        }
        Instant now = Instant.now(clock);
        Instant userWindowEnd = actor.getCreatedAt().plusSeconds((long) fraudProperties.windowMinutes() * 60);
        if (now.isAfter(userWindowEnd)) {
            return;
        }

        int visitedDistinct = interactionRepository.countDistinctVisitedUsersInWindow(
                actor.getId(),
                actor.getCreatedAt(),
                userWindowEnd
        );
        if (visitedDistinct < fraudProperties.threshold()) {
            return;
        }

        int likedDistinct = interactionRepository.countDistinctLikedUsersInWindow(
                actor.getId(),
                actor.getCreatedAt(),
                userWindowEnd
        );
        if (likedDistinct < fraudProperties.threshold()) {
            return;
        }

        userRepository.markFraud(actor.getId(), now);
    }
}
