package dev.breezes.settlements.bootstrap.registry.activities;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ActivityRegistry {

    public static final DeferredRegister<Activity> REGISTRY =
            DeferredRegister.create(Registries.ACTIVITY, SettlementsMod.MOD_ID);

    // Create activity eagerly to be used in schedule
    // The following is an example (delete this after having a concrete activity)
//    public static final Activity PLAN_ACTIVITY_INSTANCE = new Activity("plan");
//
//    public static final Supplier<Activity> SETTLEMENTS_PLAN = REGISTRY.register(
//            "plan",
//            () -> PLAN_ACTIVITY_INSTANCE);

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
