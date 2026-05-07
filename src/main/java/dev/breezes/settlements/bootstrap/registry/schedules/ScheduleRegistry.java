package dev.breezes.settlements.bootstrap.registry.schedules;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.time.TimeOfDay;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.entity.schedule.ScheduleBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ScheduleRegistry {

    public static final DeferredRegister<Schedule> REGISTRY =
            DeferredRegister.create(Registries.SCHEDULE, SettlementsMod.MOD_ID);

    public static final Supplier<Schedule> SETTLEMENTS_SCHEDULE = REGISTRY.register(
            "settlements_schedule",
            () -> new ScheduleBuilder(new Schedule())
                    .changeActivityAt(TimeOfDay.AT_12_00.getTick(), Activity.IDLE)
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
