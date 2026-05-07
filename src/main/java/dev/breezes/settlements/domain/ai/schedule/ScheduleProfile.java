package dev.breezes.settlements.domain.ai.schedule;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.time.TimeOfDay;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Per-profession schedule tendencies expressed as game-tick offsets within a single
 * Minecraft day (0–24 000 ticks, where tick 0 = 06:00 AM). See {@link TimeOfDay} for
 * named time constants.
 */
@Builder
public record ScheduleProfile(
        VillagerProfessionKey profession,
        int defaultWakeTick,
        int defaultSleepTick,
        int workStartTick,
        int workEndTick,
        String scheduleDescription
) {

    private static final Map<VillagerProfessionKey, ScheduleProfile> DEFAULT_PROFILES = Map.ofEntries(
            Map.entry(VillagerProfessionKey.ARMORER, standardHours(VillagerProfessionKey.ARMORER)),
            Map.entry(VillagerProfessionKey.BUTCHER, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.BUTCHER)
                    .defaultWakeTick(TimeOfDay.AT_06_00.getTick())
                    .workStartTick(TimeOfDay.AT_06_30.getTick())
                    .workEndTick(TimeOfDay.AT_15_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_20_30.getTick())
                    .scheduleDescription("Early for fresh preparation")
                    .build()),
            Map.entry(VillagerProfessionKey.CARTOGRAPHER, standardHours(VillagerProfessionKey.CARTOGRAPHER)),
            Map.entry(VillagerProfessionKey.CLERIC, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.CLERIC)
                    .defaultWakeTick(TimeOfDay.AT_06_00.getTick())
                    .workStartTick(TimeOfDay.AT_06_30.getTick())
                    .workEndTick(TimeOfDay.AT_18_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_21_00.getTick())
                    .scheduleDescription("Dedicated to duty")
                    .build()),
            Map.entry(VillagerProfessionKey.FARMER, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.FARMER)
                    .defaultWakeTick(TimeOfDay.AT_05_00.getTick())
                    .workStartTick(TimeOfDay.AT_05_30.getTick())
                    .workEndTick(TimeOfDay.AT_14_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_20_00.getTick())
                    .scheduleDescription("Early riser, finishes by afternoon")
                    .build()),
            Map.entry(VillagerProfessionKey.FISHERMAN, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.FISHERMAN)
                    .defaultWakeTick(TimeOfDay.AT_04_30.getTick())
                    .workStartTick(TimeOfDay.AT_05_00.getTick())
                    .workEndTick(TimeOfDay.AT_12_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_19_30.getTick())
                    .scheduleDescription("Very early riser, works at dawn")
                    .build()),
            Map.entry(VillagerProfessionKey.FLETCHER, standardHours(VillagerProfessionKey.FLETCHER)),
            Map.entry(VillagerProfessionKey.LEATHERWORKER, standardHours(VillagerProfessionKey.LEATHERWORKER)),
            Map.entry(VillagerProfessionKey.LIBRARIAN, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.LIBRARIAN)
                    .defaultWakeTick(TimeOfDay.AT_07_00.getTick())
                    .workStartTick(TimeOfDay.AT_08_00.getTick())
                    .workEndTick(TimeOfDay.AT_17_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_22_00.getTick())
                    .scheduleDescription("Late riser, stays up reading")
                    .build()),
            Map.entry(VillagerProfessionKey.MASON, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.MASON)
                    .defaultWakeTick(TimeOfDay.AT_06_30.getTick())
                    .workStartTick(TimeOfDay.AT_07_00.getTick())
                    .workEndTick(TimeOfDay.AT_16_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_21_00.getTick())
                    .scheduleDescription("Solid working hours")
                    .build()),
            Map.entry(VillagerProfessionKey.NITWIT, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.NITWIT)
                    .defaultWakeTick(TimeOfDay.AT_09_00.getTick())
                    .workStartTick(TimeOfDay.AT_10_00.getTick())
                    .workEndTick(TimeOfDay.AT_10_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_23_00.getTick())
                    .scheduleDescription("Sleeps in, no work schedule")
                    .build()),
            Map.entry(VillagerProfessionKey.NONE, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.NONE)
                    .defaultWakeTick(TimeOfDay.AT_06_30.getTick())
                    .workStartTick(TimeOfDay.AT_09_00.getTick())
                    .workEndTick(TimeOfDay.AT_09_30.getTick())
                    .defaultSleepTick(TimeOfDay.AT_21_00.getTick())
                    .scheduleDescription("Community-oriented day with broad social availability")
                    .build()),
            Map.entry(VillagerProfessionKey.SHEPHERD, ScheduleProfile.builder()
                    .profession(VillagerProfessionKey.SHEPHERD)
                    .defaultWakeTick(TimeOfDay.AT_05_30.getTick())
                    .workStartTick(TimeOfDay.AT_06_00.getTick())
                    .workEndTick(TimeOfDay.AT_15_00.getTick())
                    .defaultSleepTick(TimeOfDay.AT_20_00.getTick())
                    .scheduleDescription("Early for animal care")
                    .build()),
            Map.entry(VillagerProfessionKey.TOOLSMITH, standardHours(VillagerProfessionKey.TOOLSMITH)),
            Map.entry(VillagerProfessionKey.WEAPONSMITH, standardHours(VillagerProfessionKey.WEAPONSMITH))
    );

    private static final List<ScheduleProfile> DEFAULT_PROFILE_LIST = List.copyOf(DEFAULT_PROFILES.values());

    public ScheduleProfile {
        validateTick(defaultWakeTick, "defaultWakeTick");
        validateTick(defaultSleepTick, "defaultSleepTick");
        validateTick(workStartTick, "workStartTick");
        validateTick(workEndTick, "workEndTick");
    }

    public static ScheduleProfile defaultFor(VillagerProfessionKey profession) {
        return DEFAULT_PROFILES.getOrDefault(profession, standardHours(profession));
    }

    public static List<ScheduleProfile> defaultProfiles() {
        return DEFAULT_PROFILE_LIST;
    }

    private static ScheduleProfile standardHours(VillagerProfessionKey profession) {
        return ScheduleProfile.builder()
                .profession(profession)
                .defaultWakeTick(TimeOfDay.AT_06_30.getTick())
                .workStartTick(TimeOfDay.AT_07_00.getTick())
                .workEndTick(TimeOfDay.AT_16_00.getTick())
                .defaultSleepTick(TimeOfDay.AT_21_00.getTick())
                .scheduleDescription("Standard working hours")
                .build();
    }

    private static void validateTick(int tick, String fieldName) {
        if (!TimeOfDay.isValidTick(tick)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 23999");
        }
    }

}
