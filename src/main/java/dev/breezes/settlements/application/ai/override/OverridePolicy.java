package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.domain.ai.override.OverridePrecedence;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Declares when to request an override and which activities permit its execution.
 * <p>
 * Implementations must be stateless and side-effect-free: inspecting a trigger must not consume
 * it or mutate world, villager, or registry state.
 */
public interface OverridePolicy {

    /**
     * This policy's fixed precedence. Must be unique among registered policies.
     */
    OverridePrecedence precedence();

    /**
     * Whether this policy permits its override to start or remain active during the given activity.
     *
     * @param activity active non-core activity, or null when none is active
     */
    boolean isAdmissibleDuring(@Nullable Activity activity);

    /**
     * Identifies an override to request for the villager's current state.
     * Returning a request does not guarantee admission or startup.
     *
     * @param level    current server level
     * @param villager villager to evaluate
     * @return the requested override, or empty if the trigger is not satisfied
     */
    Optional<OverrideRequest> evaluate(@Nonnull ServerLevel level, @Nonnull BaseVillager villager);

}
