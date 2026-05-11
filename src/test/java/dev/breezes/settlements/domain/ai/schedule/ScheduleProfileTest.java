package dev.breezes.settlements.domain.ai.schedule;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.TimeOfDay;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleProfileTest {

    @Test
    void defaultProfiles_existForAllVanillaProfessionConstants() {
        Set<VillagerProfessionKey> professions = Set.of(
                VillagerProfessionKey.NONE,
                VillagerProfessionKey.ARMORER,
                VillagerProfessionKey.BUTCHER,
                VillagerProfessionKey.CARTOGRAPHER,
                VillagerProfessionKey.CLERIC,
                VillagerProfessionKey.FARMER,
                VillagerProfessionKey.FISHERMAN,
                VillagerProfessionKey.FLETCHER,
                VillagerProfessionKey.LEATHERWORKER,
                VillagerProfessionKey.LIBRARIAN,
                VillagerProfessionKey.MASON,
                VillagerProfessionKey.NITWIT,
                VillagerProfessionKey.SHEPHERD,
                VillagerProfessionKey.TOOLSMITH,
                VillagerProfessionKey.WEAPONSMITH
        );

        for (VillagerProfessionKey profession : professions) {
            assertEquals(profession, ScheduleProfile.defaultFor(profession).profession());
        }
    }

    @Test
    void defaultProfileTicks_areInValidRange() {
        List<ScheduleProfile> profiles = ScheduleProfile.defaultProfiles();

        for (ScheduleProfile profile : profiles) {
            assertValidTick(profile.defaultWakeTick());
            assertValidTick(profile.defaultSleepTick());
            assertValidTick(profile.workStartTick());
            assertValidTick(profile.workEndTick());
        }
    }

    @Test
    void builder_rejectsNegativeTick() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleProfile.builder()
                .profession(VillagerProfessionKey.FARMER)
                .defaultWakeTick(-1)
                .workStartTick(1_000)
                .workEndTick(8_000)
                .defaultSleepTick(14_000)
                .scheduleDescription("invalid")
                .build());
    }

    @Test
    void builder_rejectsWrappedDayTick() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleProfile.builder()
                .profession(VillagerProfessionKey.FARMER)
                .defaultWakeTick(24_000)
                .workStartTick(1_000)
                .workEndTick(8_000)
                .defaultSleepTick(14_000)
                .scheduleDescription("invalid")
                .build());
    }


    private static void assertValidTick(int tick) {
        assertTrue(tick >= 0 && tick < TimeOfDay.TICKS_PER_DAY);
    }

}
