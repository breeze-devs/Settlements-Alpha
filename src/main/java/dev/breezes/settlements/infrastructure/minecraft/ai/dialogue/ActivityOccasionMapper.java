package dev.breezes.settlements.infrastructure.minecraft.ai.dialogue;

import dev.breezes.settlements.application.ai.dialogue.Occasion;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;

/**
 * Maps vanilla brain activities into the Minecraft-free dialogue occasion vocabulary.
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
        if (activity == Activity.PRE_RAID) {
            return Occasion.PRE_RAID;
        }
        if (activity == Activity.RAID) {
            return Occasion.RAID;
        }
        if (activity == Activity.HIDE) {
            return Occasion.HIDE;
        }
        return Occasion.IDLE;
    }

}
