package com.meet5.socialnetwork.service;

import com.meet5.socialnetwork.config.AppConfig;
import com.meet5.socialnetwork.domain.UserProfile;
import com.meet5.socialnetwork.exception.BadRequestException;
import com.meet5.socialnetwork.exception.ConflictException;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionServiceTest {

    @Mock
    private InteractionRepository interactionRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FraudDetectionService fraudDetectionService;

    private InteractionService interactionService;

    @BeforeEach
    void setup() {
        AppConfig.FraudProperties props = new AppConfig.FraudProperties(100, 10, false);
        Clock clock = Clock.fixed(Instant.parse("2026-02-21T10:00:00Z"), ZoneOffset.UTC);
        interactionService = new InteractionService(
                interactionRepository,
                userService,
                userRepository,
                fraudDetectionService,
                props,
                clock
        );
    }

    @Test
    void preventsSelfLike() {
        assertThrows(BadRequestException.class, () -> interactionService.like(7L, 7L));
    }

    @Test
    void rejectsDuplicateLike() {
        UserProfile actor = UserProfile.existing(7L, "actor", 30, Instant.parse("2026-02-21T09:50:00Z"), Map.of(), false);
        when(userRepository.existingIds(Set.of(7L, 8L))).thenReturn(Set.of(7L, 8L));
        when(userService.getOrThrow(7L)).thenReturn(actor);
        when(interactionRepository.insertLikeIfNotExists(eq(7L), eq(8L), any())).thenReturn(false);

        assertThrows(ConflictException.class, () -> interactionService.like(7L, 8L));
        verify(fraudDetectionService, never()).evaluateAfterInteraction(any());
    }

    @Test
    void rejectsWhenTargetUserMissing() {
        when(userRepository.existingIds(Set.of(7L, 8L))).thenReturn(Set.of(7L));

        assertThrows(BadRequestException.class, () -> interactionService.visit(7L, 8L));
    }

    @Test
    void recordsLikeAndTriggersFraudEvaluation() {
        UserProfile actor = UserProfile.existing(7L, "actor", 30, Instant.parse("2026-02-21T09:50:00Z"), Map.of(), false);
        when(userRepository.existingIds(Set.of(7L, 8L))).thenReturn(Set.of(7L, 8L));
        when(userService.getOrThrow(7L)).thenReturn(actor);
        when(interactionRepository.insertLikeIfNotExists(eq(7L), eq(8L), any())).thenReturn(true);

        interactionService.like(7L, 8L);

        verify(interactionRepository).insertLikeIfNotExists(eq(7L), eq(8L), any());
        verify(fraudDetectionService).evaluateAfterInteraction(actor);
    }
}
