package dev.breezes.settlements.application.ai.behavior.usecases.villager.ballista;

import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaFiringSolution;
import dev.breezes.settlements.domain.ballista.BallistaLaunchPoint;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.projectiles.BallistaBolt;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Whether a ballista has a shot at a target.
 * <p>
 * The answer depends only on the ballista and the target, so it is the same for whoever using it.
 */
public final class BallistaShotChecks {

    // Blocks between machine and hostile change without any report, so every answer needs a maximum age. The
    // hostile's own movement is not tracked either; one window of it is accepted
    private static final ClockTicks WINDOW = ClockTicks.of(5);

    private final Reference2ObjectOpenHashMap<ResourceKey<Level>, Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ShotCheck>>> checks =
            new Reference2ObjectOpenHashMap<>();

    private long window = Long.MIN_VALUE;

    /**
     * Whether a shot from the machine can hit the hostile, with nothing in the way.
     */
    boolean canHit(@Nonnull ServerLevel level, @Nonnull BlockPos machinePos, @Nonnull LivingEntity hostile) {
        ShotCheck check = this.checkFor(level, machinePos, hostile);
        if (check.solvable == null) {
            Vec3 toAimPoint = BallistaCombatOption.aimPointOf(hostile).subtract(BallistaCombatOption.floorCenterOf(machinePos));
            check.solvable = BallistaFiringSolution.solve(toAimPoint, Vec3.ZERO, BallistaBolt.LAUNCH_SPEED_BLOCKS_PER_TICK).isPresent();
        }
        return check.solvable && this.hasLineOfSight(level, machinePos, hostile, check);
    }

    /**
     * Whether the machine's muzzle sees the hostile's aim point now, cast afresh whatever the window holds. The answer
     * replaces the window's, so the villagers asking after it share it.
     */
    boolean refreshLineOfSight(@Nonnull ServerLevel level, @Nonnull BlockPos machinePos, @Nonnull LivingEntity hostile) {
        ShotCheck check = this.checkFor(level, machinePos, hostile);
        check.inSight = traceLineOfSight(level, machinePos, hostile);
        return check.inSight;
    }

    private boolean hasLineOfSight(@Nonnull ServerLevel level,
                                   @Nonnull BlockPos machinePos,
                                   @Nonnull LivingEntity hostile,
                                   @Nonnull ShotCheck check) {
        if (check.inSight == null) {
            check.inSight = traceLineOfSight(level, machinePos, hostile);
        }
        return check.inSight;
    }

    private ShotCheck checkFor(@Nonnull ServerLevel level, @Nonnull BlockPos machinePos, @Nonnull LivingEntity hostile) {
        long now = level.getGameTime() / WINDOW.getTicks();
        if (now != this.window) {
            // Dropped whole rather than entry by entry, so no stale answer outlives its window and nothing needs a sweep
            this.checks.clear();
            this.window = now;
        }

        Long2ObjectOpenHashMap<Int2ObjectOpenHashMap<ShotCheck>> byMachine =
                this.checks.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>());

        Int2ObjectOpenHashMap<ShotCheck> byHostile = byMachine.get(machinePos.asLong());
        if (byHostile == null) {
            byHostile = new Int2ObjectOpenHashMap<>();
            byMachine.put(machinePos.asLong(), byHostile);
        }

        ShotCheck check = byHostile.get(hostile.getId());
        if (check == null) {
            check = new ShotCheck();
            byHostile.put(hostile.getId(), check);
        }

        return check;
    }

    /**
     * Tests line of sight from the muzzle at zero pitch to the hostile's aim point. Fluids do not block the ray. A ray
     * whose bounding box reaches an unloaded chunk counts as blocked, so the check never loads a chunk.
     */
    private static boolean traceLineOfSight(@Nonnull ServerLevel level, @Nonnull BlockPos machinePos, @Nonnull LivingEntity hostile) {
        Vec3 floorCenter = BallistaCombatOption.floorCenterOf(machinePos);
        Vec3 aimPoint = BallistaCombatOption.aimPointOf(hostile);

        // Start at the muzzle to avoid hitting the ballista's own block or cover below the barrel
        float heading = BallistaLaunchPoint.headingToward(aimPoint.subtract(floorCenter));
        Vec3 muzzle = floorCenter.add(BallistaLaunchPoint.tipOffset(BallistaAim.facing(heading, 0.0F)));

        // A block read in an unloaded chunk would load it on the server thread, so any unloaded chunk in the ray's box
        // counts as blocking
        if (!level.hasChunksAt(Mth.floor(Math.min(muzzle.x, aimPoint.x)), Mth.floor(Math.min(muzzle.z, aimPoint.z)),
                Mth.floor(Math.max(muzzle.x, aimPoint.x)), Mth.floor(Math.max(muzzle.z, aimPoint.z)))) {
            return false;
        }

        ClipContext ray = new ClipContext(muzzle, aimPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty());
        return level.clip(ray).getType() == HitResult.Type.MISS;
    }

    /**
     * One machine's answers about one hostile within the current window; null until asked.
     */
    private static final class ShotCheck {

        @Nullable
        private Boolean solvable;

        @Nullable
        private Boolean inSight;

    }

}
