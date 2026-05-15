package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.domain.time.GameTicks;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.EnumMap;
import java.util.Map;

import static dev.breezes.settlements.domain.time.TimeOfDay.TICKS_PER_DAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WakeTickResolverTest {

    private final WakeTickResolver resolver = new WakeTickResolver();

    @ParameterizedTest
    @CsvSource({
            "0.0, WORK_DAY, 6900",
            "0.5, WORK_DAY, 6400",
            "1.0, WORK_DAY, 5900",
            "0.5, REST_DAY, 7400"
    })
    void resolveWakeTick_appliesConstitutionAndRestDayOffset(double constitution,
                                                             PlanDayType dayType,
                                                             int expectedWakeTick) {
        // Arrange
        ScheduleProfile profile = ScheduleProfile.builder()
                .profession(VillagerProfessionKey.FARMER)
                .defaultWakeTick(6_400)
                .defaultSleepTick(13_000)
                .workStartTick(7_000)
                .workEndTick(12_000)
                .scheduleDescription("test")
                .build();
        GeneticsProfile genetics = genetics(constitution);

        // Act
        int wakeTick = this.resolver.resolveWakeTick(profile, genetics, dayType);

        // Assert
        assertEquals(expectedWakeTick, wakeTick);
    }

    @ParameterizedTest
    @CsvSource({
            "0.0, 0",
            "1.0, 23000"
    })
    void resolveWakeTick_wrapsIntoSingleDay(double constitution, int expectedWakeTick) {
        // Arrange
        ScheduleProfile profile = ScheduleProfile.builder()
                .profession(VillagerProfessionKey.FARMER)
                .defaultWakeTick(TICKS_PER_DAY - GameTicks.minutes(30).getTicksAsInt())
                .defaultSleepTick(13_000)
                .workStartTick(7_000)
                .workEndTick(12_000)
                .scheduleDescription("test")
                .build();

        // Act
        int wakeTick = this.resolver.resolveWakeTick(profile, genetics(constitution), PlanDayType.WORK_DAY);

        // Assert
        assertEquals(expectedWakeTick, wakeTick);
    }

    private static GeneticsProfile genetics(double constitution) {
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType geneType : GeneType.VALUES) {
            genes.put(geneType, new Gene(0.5));
        }
        genes.put(GeneType.CONSTITUTION, new Gene(constitution));
        return new GeneticsProfile(genes);
    }

}
