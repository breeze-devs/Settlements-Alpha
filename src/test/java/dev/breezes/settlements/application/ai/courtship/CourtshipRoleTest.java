package dev.breezes.settlements.application.ai.courtship;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CourtshipRoleTest {

    @Test
    void of_lowerUuidIsPresenter() {
        // Arrange — choose UUIDs with a known ordering
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Act
        CourtshipRole lowerRole = CourtshipRole.of(lower, higher);
        CourtshipRole higherRole = CourtshipRole.of(higher, lower);

        // Assert — exactly one PRESENTER, one RECEIVER
        assertEquals(CourtshipRole.PRESENTER, lowerRole);
        assertEquals(CourtshipRole.RECEIVER, higherRole);
    }

    @Test
    void of_strictLessThan_equalUuidsNotPresenter() {
        // Arrange — equal UUIDs should not yield PRESENTER (strict < 0 comparison)
        UUID same = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        // Act
        CourtshipRole role = CourtshipRole.of(same, same);

        // Assert — equal compareTo = 0 falls through to RECEIVER
        assertEquals(CourtshipRole.RECEIVER, role);
    }

    @Test
    void of_rolesAreSymmetric() {
        // Arrange
        UUID a = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID b = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // Act
        CourtshipRole aRole = CourtshipRole.of(a, b);
        CourtshipRole bRole = CourtshipRole.of(b, a);

        // Assert — roles must be different (one each)
        assertNotEquals(aRole, bRole);
    }

}
