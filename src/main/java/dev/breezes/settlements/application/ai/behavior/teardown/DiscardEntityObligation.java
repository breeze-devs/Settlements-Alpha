package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Obligation to discard a spawned display entity (or any world entity) by UUID.
 * <p>
 * Canonical use-case: the temporary {@code TransformedBlockDisplay} entities
 * spawned by {@code CutStoneBehavior} during the cutting animation.
 * <p>
 * {@code spawnPos} is the block position where the entity was spawned, used by
 * reconciliation to distinguish "entity gone" from "chunk not loaded yet".
 */
public record DiscardEntityObligation(@Nonnull UUID entityId,
                                      @Nonnull BlockPos spawnPos) implements TeardownObligation {

    @Override
    public BlockPos targetPos() {
        return this.spawnPos;
    }

    @Override
    public boolean stillValid(@Nonnull ServerLevel level) {
        if (!level.isLoaded(this.spawnPos)) {
            return false;
        }

        Entity entity = level.getEntity(this.entityId);
        return entity != null && !entity.isRemoved();
    }

    @Override
    public void discharge(@Nonnull ServerLevel level) {
        Entity entity = level.getEntity(this.entityId);
        if (entity == null || entity.isRemoved()) {
            return;
        }

        entity.discard();
    }

    @Override
    public boolean durable() {
        return true;
    }

    @Override
    public String describe() {
        return "discard entity " + this.entityId + " at " + this.spawnPos.toShortString();
    }

}
