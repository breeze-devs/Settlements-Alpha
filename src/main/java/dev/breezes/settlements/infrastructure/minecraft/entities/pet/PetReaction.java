package dev.breezes.settlements.infrastructure.minecraft.entities.pet;

import dev.breezes.settlements.domain.animal.PetSquish;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import lombok.Builder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * Reusable "petting" reaction shared by Settlements animals: a transient squash-and-stretch, a species
 * sound, and a few hearts, rate-limited so click-spam will be throttled.
 */
public class PetReaction {

    /**
     * Custom entity-event id broadcast to clients to start the squish. Chosen well above the range of
     * vanilla {@code Entity} event ids so it never collides with a vanilla reaction on any animal.
     */
    public static final byte SQUISH_EVENT_ID = 64;

    /**
     * Sentinel {@link #squishStartTick} value meaning no squish is currently playing.
     */
    private static final int NO_SQUISH = Integer.MIN_VALUE;

    private static final int HEART_COUNT = 3;
    private static final float SOUND_VOLUME = 0.6F;
    private static final float SOUND_PITCH = 1.0F;

    private final ClockTicks cooldown;
    private final SoundEvent sound;

    /**
     * Server-only game-tick before which no new reaction may fire.
     */
    private long nextAllowedGameTime;

    /**
     * Client-only tick at which the current squish began; {@link #NO_SQUISH} when idle.
     */
    private int squishStartTick = NO_SQUISH;

    @Builder
    private PetReaction(@Nonnull ClockTicks cooldown, @Nonnull SoundEvent sound) {
        this.cooldown = cooldown;
        this.sound = sound;
    }

    /**
     * Fires the reaction from the given animal. Server-authoritative: emits the sound, hearts, and squish
     * broadcast. No-op client-side or while still on cooldown.
     *
     * @return whether the reaction actually fired
     */
    public boolean trigger(@Nonnull LivingEntity animal) {
        Level level = animal.level();
        if (level.isClientSide()) {
            return false;
        }

        long now = level.getGameTime();
        if (now < this.nextAllowedGameTime) {
            return false;
        }
        this.nextAllowedGameTime = now + this.cooldown.getTicksAsInt();

        Location location = Location.fromEntity(animal, false).add(0, 0.1, 0, false);
        location.displayParticles(ParticleTypes.HEART, HEART_COUNT, 0.3, 0.5, 0.3, 0.01);
        location.playSound(this.sound, SOUND_VOLUME, SOUND_PITCH, SoundSource.NEUTRAL);
        level.broadcastEntityEvent(animal, SQUISH_EVENT_ID);
        return true;
    }

    /**
     * Client hook for {@code Entity.handleEntityEvent}: starts the squish when {@code id} is ours.
     *
     * @return whether the event was consumed, so the caller can skip its {@code super} call
     */
    public boolean onEntityEvent(byte id, int tickCount) {
        if (id != SQUISH_EVENT_ID) {
            return false;
        }
        this.squishStartTick = tickCount;
        return true;
    }

    /**
     * Client render hook: the per-axis squish scale for the current frame, or {@link PetSquish#IDENTITY}
     * when no reaction is playing.
     */
    public PetSquish.Factors squishFactors(int tickCount, float partialTick) {
        if (this.squishStartTick == NO_SQUISH) {
            return PetSquish.IDENTITY;
        }
        return PetSquish.factorsAt((tickCount - this.squishStartTick) + partialTick);
    }

}
