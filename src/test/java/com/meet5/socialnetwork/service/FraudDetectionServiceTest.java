package com.meet5.socialnetwork.service;

import com.meet5.socialnetwork.config.AppConfig;
import com.meet5.socialnetwork.domain.UserProfile;
import com.meet5.socialnetwork.repository.InteractionRepository;
import com.meet5.socialnetwork.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;
    @Mock
    private UserRepository userRepository;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setup() {
        AppConfig.FraudProperties props = new AppConfig.FraudProperties(100, 10, false);
        Clock clock = Clock.fixed(Instant.parse("2026-02-21T10:05:00Z"), ZoneOffset.UTC);
        fraudDetectionService = new FraudDetectionService(interactionRepository, userRepository, props, clock);
    }

    @Test
    void marksUserWhenThresholdReachedWithinWindow() {
        UserProfile actor = UserProfile.existing(
                42L,
                "actor",
                24,
                Instant.parse("2026-02-21T10:00:00Z"),
                Map.of(),
                false
        );
        when(interactionRepository.countDistinctVisitedUsersInWindow(eq(42L), any(), any())).thenReturn(100);
        when(interactionRepository.countDistinctLikedUsersInWindow(eq(42L), any(), any())).thenReturn(100);

        fraudDetectionService.evaluateAfterInteraction(actor);

        verify(userRepository).markFraud(eq(42L), eq(Instant.parse("2026-02-21T10:05:00Z")));
    }

    @Test
    void doesNotMarkWhenBelowThreshold() {
        UserProfile actor = UserProfile.existing(
                42L,
                "actor",
                24,
                Instant.parse("2026-02-21T10:00:00Z"),
                Map.of(),
                false
        );
        when(interactionRepository.countDistinctVisitedUsersInWindow(eq(42L), any(), any())).thenReturn(99);

        fraudDetectionService.evaluateAfterInteraction(actor);

        verify(userRepository, never()).markFraud(anyLong(), any());
    }

    @Test
    void doesNotMarkWhenOutsideWindow() {
        AppConfig.FraudProperties props = new AppConfig.FraudProperties(100, 10, false);
        Clock clock = Clock.fixed(Instant.parse("2026-02-21T10:11:01Z"), ZoneOffset.UTC);
        FraudDetectionService service = new FraudDetectionService(interactionRepository, userRepository, props, clock);
        UserProfile actor = UserProfile.existing(
                42L,
                "actor",
                24,
                Instant.parse("2026-02-21T10:00:00Z"),
                Map.of(),
                false
        );

        service.evaluateAfterInteraction(actor);

        verify(interactionRepository, never()).countDistinctVisitedUsersInWindow(anyLong(), any(), any());
        verify(interactionRepository, never()).countDistinctLikedUsersInWindow(anyLong(), any(), any());
        verify(userRepository, never()).markFraud(anyLong(), any());
    }
}
