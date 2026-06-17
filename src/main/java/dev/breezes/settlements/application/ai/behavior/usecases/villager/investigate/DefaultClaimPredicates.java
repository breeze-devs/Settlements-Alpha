package dev.breezes.settlements.application.ai.behavior.usecases.villager.investigate;

import dev.breezes.settlements.domain.ai.credibility.ClaimPredicate;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Default {@link ClaimPredicate} implementations keyed by {@link ObservationType}.
 * <p>
 * These predicates represent the minimal machine-checkable claim for each observation
 * category. They intentionally use broad heuristics — "is there any non-trivial block
 * at the position" rather than "is there a ripe melon specifically" — because the origin
 * metadata does not carry enough type information to drive a precise lookup.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class DefaultClaimPredicates {

    /**
     * Radius (in blocks) used for the block-presence scan
     */
    private static final double SCAN_RADIUS = 8.0;

    /**
     * Returns the best-fit default predicate for the given observation type.
     * Falls back to {@link #alwaysConfirm()} for types that cannot produce a
     * meaningful world check (SOCIAL, GOSSIP_RECEIVED, ENVIRONMENT) rather than
     * returning null — a confirmed result is less harmful than a false refutation.
     */
    public static ClaimPredicate forObservationType(ObservationType type) {
        return switch (type) {
            case RESOURCE -> resourcePresent();
            case THREAT -> hostilesPresent();
            // Always confirm events that cannot be mechanically verified
            case SOCIAL, GOSSIP_RECEIVED, TASK_COMPLETION, TASK_FAILURE, ENVIRONMENT -> alwaysConfirm();
        };
    }

    /**
     * Checks whether any 'non-trivial' block exists within the scan radius of the
     * tip position.
     * <p>
     * This is intentionally broad: the predicate should confirm if something
     * worth investigating is there, not whether the exact original resource is still present.
     * A false confirmation is far cheaper than wrongly blacklisting a reliable source because
     * the precise crop type was not stored in the gossip payload.
     */
    public static ClaimPredicate resourcePresent() {
        return (level, origin) -> {
            // TODO: is this the right way to do this? Having an interesting block is kinda... wack; revisit later
            AABB scanBox = AABB.ofSize(origin, SCAN_RADIUS * 2, SCAN_RADIUS * 2, SCAN_RADIUS * 2);
            return level.getBlockStates(scanBox).anyMatch(state ->
                    !state.isAir()
                            && state.getBlock() != Blocks.WATER
                            && state.getBlock() != Blocks.LAVA
                            && state.getBlock() != Blocks.GRASS_BLOCK
                            && state.getBlock() != Blocks.DIRT
                            && state.getBlock() != Blocks.STONE
                            && state.getBlock() != Blocks.SAND
                            && state.getBlock() != Blocks.GRAVEL);
        };
    }

    /**
     * Checks whether any hostile monster is near the tip position via a simple AABB scan.
     */
    public static ClaimPredicate hostilesPresent() {
        return (level, origin) -> {
            AABB scanBox = AABB.ofSize(origin, SCAN_RADIUS * 2, SCAN_RADIUS * 2, SCAN_RADIUS * 2);
            return !level.getEntitiesOfClass(Monster.class, scanBox).isEmpty();
        };
    }

    /**
     * Always confirms — used for claims that cannot be mechanically verified or where a
     * false negative would unjustly penalize an honest source.
     */
    public static ClaimPredicate alwaysConfirm() {
        return (level, origin) -> true;
    }

}
