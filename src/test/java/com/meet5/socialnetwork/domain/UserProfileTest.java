package com.meet5.socialnetwork.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserProfileTest {

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> UserProfile.newUser(" ", 20, Map.of()));
    }

    @Test
    void rejectsInvalidAge() {
        assertThrows(IllegalArgumentException.class, () -> UserProfile.newUser("john", 10, Map.of()));
    }

    @Test
    void storesAttributesAsImmutableMap() {
        UserProfile user = UserProfile.newUser("john", 20, Map.of("city", "berlin"));
        assertEquals("berlin", user.getAttribute("city").orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> user.getAttributes().put("x", "y"));
    }
}
