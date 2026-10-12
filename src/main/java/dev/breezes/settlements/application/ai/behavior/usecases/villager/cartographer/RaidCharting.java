package dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer;

import dev.breezes.settlements.application.ai.brain.ActivityArbiter;
import dev.breezes.settlements.domain.time.ClockTicks;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Which raid a cartographer charts, and which of its raiders still need charting.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RaidCharting {

    /**
     * How long a charted raider glows.
     */
    static final ClockTicks GLOW_DURATION = ClockTicks.seconds(60);

    /**
     * The glow a raider must have left to count as charted.
     * <p>
     * It must stay below GLOW_DURATION.
     */
    private static final ClockTicks RENEWAL_THRESHOLD = ClockTicks.seconds(10);

    /**
     * The raid with a wave in progress at the cartographer's position, by the test {@link ActivityArbiter#isRaidWaveLive} applies.
     * It reads the raid as it is now, so it can disagree for a moment with an activity derived earlier.
     *
     * @return the raid, or empty when there is no raid there or it has no wave in progress
     */
    public static Optional<Raid> raidInWave(@Nonnull ServerLevel level, @Nonnull Entity cartographer) {
        Raid raid = level.getRaidAt(cartographer.blockPosition());
        return ActivityArbiter.isRaidWaveLive(raid) ? Optional.of(raid) : Optional.empty();
    }

    /**
     * Whether a raid member can be selected as a target to be revealed.
     */
    public static boolean isChartable(@Nonnull Raider raider, @Nonnull Level level) {
        if (!raider.isAlive() || raider.level() != level || raider.hasGlowingTag()) {
            return false;
        }

        MobEffectInstance glow = raider.getEffect(MobEffects.GLOWING);
        return glow == null || (!glow.isInfiniteDuration() && glow.getDuration() < RENEWAL_THRESHOLD.getTicks());
    }

}
