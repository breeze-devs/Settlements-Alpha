package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.planning.IWakeTickResolver;
import dev.breezes.settlements.domain.ai.schedule.IWeekCycleProvider;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.ai.schedule.ScheduleProfile;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.world.WorldCalendar;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Resolves a villager's wake tick from its profession schedule, the target day's
 * {@link PlanDayType}, and its stable chronotype seed.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class VillagerWakeScheduler {

    private final IWeekCycleProvider weekCycleProvider;
    private final IWakeTickResolver wakeTickResolver;

    /**
     * Resolves the absolute {@code dayTime} a villager wakes at within {@code calendarDay}.
     */
    public long wakeAbsoluteTickFor(@Nonnull BaseVillager villager, long calendarDay) {
        int wakeTickInMcDay = this.wakeTickFor(villager, calendarDay);
        return WorldCalendar.absoluteTickFor(calendarDay, wakeTickInMcDay);
    }

    /**
     * Resolves the next wake tick strictly after {@code currentWakeAtAbsoluteTick}: today's wake
     * (relative to {@code dayTime}'s calendar day) if it hasn't already passed that reference,
     * else tomorrow's. Used to anchor async successor-plan generation, whether submitted by the
     * runner itself or by the overnight sweep on the runner's behalf.
     */
    public long nextWakeAtAbsoluteTick(@Nonnull BaseVillager villager, long dayTime, long currentWakeAtAbsoluteTick) {
        long currentCalendarDay = WorldCalendar.calendarDayOf(dayTime);
        long todayWake = this.wakeAbsoluteTickFor(villager, currentCalendarDay);
        if (todayWake > currentWakeAtAbsoluteTick) {
            return todayWake;
        }
        return this.wakeAbsoluteTickFor(villager, currentCalendarDay + 1);
    }

    private int wakeTickFor(@Nonnull BaseVillager villager, long calendarDay) {
        VillagerProfessionKey professionKey = villager.getProfession();
        ScheduleProfile scheduleProfile = ScheduleProfile.defaultFor(professionKey);
        PlanDayType dayType = this.weekCycleProvider.getDayType(calendarDay);
        return this.wakeTickResolver.resolveWakeTick(scheduleProfile, dayType, PlanGenerationContextFactory.chronotypeSeedFor(villager));
    }

}
