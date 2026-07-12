package dev.breezes.settlements.infrastructure.minecraft.entities.villager;

import dev.breezes.settlements.domain.time.ClockTicks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Client animation for a villager restocking its vanilla player-trading offers
 */
public class RestockReaction {

    /**
     * Custom entity-event id broadcast to clients to start the spiral. Chosen above vanilla's
     * {@code Entity}/{@code Villager} event id range and above
     * {@link dev.breezes.settlements.infrastructure.minecraft.entities.pet.PetReaction#SQUISH_EVENT_ID}
     * (64) so the two custom reactions never collide.
     */
    public static final byte RESTOCK_EVENT_ID = 65;

    /**
     * Sentinel {@link #spiralStartTick} value meaning no spiral is currently playing. {@code tickCount}
     * can legitimately be small (or zero) right after an entity loads, so zero cannot double as "idle".
     */
    private static final int NO_SPIRAL = Integer.MIN_VALUE;

    /**
     * Points evenly spaced around the spiral's ring -- one full revolution.
     */
    private static final int RING_POINTS = 16;

    /**
     * Particles emitted per client tick; each one advances one ring index further around and up.
     */
    private static final int PARTICLES_PER_TICK = 3;

    /**
     * Vertical rise contributed by each ring-index step, so the ring climbs into a continuous spiral
     * instead of retracing the same circle every revolution.
     */
    private static final double DY_PER_INDEX = 0.03;

    /**
     * Ring radius around the villager's feet.
     */
    private static final double RADIUS = 0.7;

    /**
     * Total spiral lifetime, expressed as a real-world duration (~1 second) rather than a bare tick
     * count. The elapsed-tick comparison against it stays integer {@code tickCount} math.
     */
    private static final ClockTicks SPIRAL_DURATION = ClockTicks.seconds(1);

    /**
     * Client-only tick at which the current spiral began; {@link #NO_SPIRAL} when idle.
     */
    private int spiralStartTick = NO_SPIRAL;

    /**
     * Fires the reaction from the given villager. Server-authoritative: broadcasts the entity event so
     * every nearby client arms its own copy of the spiral. No-op client-side.
     */
    public void trigger(@Nonnull BaseVillager villager) {
        Level level = villager.level();
        if (level.isClientSide()) {
            return;
        }

        level.broadcastEntityEvent(villager, RESTOCK_EVENT_ID);
    }

    /**
     * Client hook for {@code Entity.handleEntityEvent}: arms the spiral when {@code id} is ours.
     *
     * @return whether the event was consumed, so the caller can skip its {@code super} call
     */
    public boolean onEntityEvent(byte id, int tickCount) {
        if (id != RESTOCK_EVENT_ID) {
            return false;
        }
        this.spiralStartTick = tickCount;
        return true;
    }

    /**
     * Client per-tick emitter: spawns this tick's {@link #PARTICLES_PER_TICK} spiral particles around the
     * villager's feet, then disarms once the spiral has run its course. Must be driven from {@code tick()}
     * (once per game tick) rather than a render method, which runs at framerate and would over-spawn.
     */
    public void tickClient(@Nonnull BaseVillager villager) {
        if (this.spiralStartTick == NO_SPIRAL) {
            return;
        }

        int elapsed = villager.tickCount - this.spiralStartTick;
        if (elapsed > SPIRAL_DURATION.getTicksAsInt()) {
            this.spiralStartTick = NO_SPIRAL;
            return;
        }

        Level level = villager.level();
        double feetX = villager.getX();
        double feetY = villager.getY();
        double feetZ = villager.getZ();
        for (SpiralOffset offset : spiralOffsetsForTick(elapsed)) {
            level.addParticle(ParticleTypes.COMPOSTER, feetX + offset.dx(), feetY + offset.dy(), feetZ + offset.dz(), 0, 0, 0);
        }
    }

    private static List<SpiralOffset> spiralOffsetsForTick(int elapsedTick) {
        List<SpiralOffset> offsets = new ArrayList<>(PARTICLES_PER_TICK);
        for (int particle = 0; particle < PARTICLES_PER_TICK; particle++) {
            int index = elapsedTick * PARTICLES_PER_TICK + particle;
            offsets.add(spiralOffsetAt(index));
        }
        return offsets;
    }

    private static SpiralOffset spiralOffsetAt(int index) {
        double angle = 2.0 * Math.PI * (index % RING_POINTS) / RING_POINTS;
        double dx = RADIUS * Math.cos(angle);
        double dz = RADIUS * Math.sin(angle);
        double dy = index * DY_PER_INDEX;
        return new SpiralOffset(dx, dy, dz);
    }

    /**
     * One particle's feet-relative offset, in world-space blocks.
     */
    record SpiralOffset(double dx, double dy, double dz) {
    }

}
