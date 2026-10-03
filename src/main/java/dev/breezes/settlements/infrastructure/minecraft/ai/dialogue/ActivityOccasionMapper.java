package dev.breezes.settlements.infrastructure.minecraft.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.bootstrap.registry.activities.ActivityRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;

/**
 * Maps brain activities into the Minecraft-free dialogue occasion vocabulary.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityOccasionMapper {

    public static Occasion map(@Nonnull Activity activity) {
        if (activity == Activity.WORK) {
            return Occasion.WORK;
        }
        if (activity == Activity.MEET) {
            return Occasion.MEET;
        }
        if (activity == Activity.REST) {
            return Occasion.REST;
        }
        if (activity == Activity.PANIC) {
            return Occasion.PANIC;
        }
        if (activity == ActivityRegistry.COMBAT) {
            return Occasion.COMBAT;
        }
        if (activity == Activity.PRE_RAID) {
            return Occasion.PRE_RAID;
        }
        if (activity == ActivityRegistry.RAID_HIDE) {
            // The occasion is the live wave itself; sheltering is only how the villager answers it
            return Occasion.RAID;
        }
        if (activity == ActivityRegistry.RAID_CELEBRATE) {
            return Occasion.RAID_CELEBRATE;
        }
        if (activity == Activity.HIDE) {
            return Occasion.HIDE;
        }
        return Occasion.IDLE;
    }

}
