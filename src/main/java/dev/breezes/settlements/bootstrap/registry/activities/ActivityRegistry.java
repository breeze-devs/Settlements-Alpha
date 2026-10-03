package dev.breezes.settlements.bootstrap.registry.activities;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.schedule.Activity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ActivityRegistry {

    public static final DeferredRegister<Activity> REGISTRY =
            DeferredRegister.create(Registries.ACTIVITY, SettlementsMod.MOD_ID);

    /**
     * Fighting a threat, during a raid or not.
     */
    public static final Activity COMBAT = new Activity(SettlementsMod.MOD_ID + ":combat");

    /**
     * Sheltering during a live raid wave.
     */
    public static final Activity RAID_HIDE = new Activity(SettlementsMod.MOD_ID + ":raid_hide");

    /**
     * Celebrating a raid the village has won.
     */
    public static final Activity RAID_CELEBRATE = new Activity(SettlementsMod.MOD_ID + ":raid_celebrate");

    static {
        // Registered as the eagerly created instances above, so they can be read as constants before any registry lookup
        REGISTRY.register("combat", () -> COMBAT);
        REGISTRY.register("raid_hide", () -> RAID_HIDE);
        REGISTRY.register("raid_celebrate", () -> RAID_CELEBRATE);
    }

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
