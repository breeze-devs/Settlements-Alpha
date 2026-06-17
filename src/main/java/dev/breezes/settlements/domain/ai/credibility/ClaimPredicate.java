package dev.breezes.settlements.domain.ai.credibility;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * A machine-checkable predicate that Investigate evaluates on arrival at the tip location.
 * <p>
 * Keeping this as a functional interface lets every gossip-worthy observation supply its
 * own compact closure (e.g. "≥1 ripe melon within 8 blocks of pos") without ceremony.
 * Resolution is mechanical: the behavior scans the world, calls {@link #test}, and records
 * CONFIRMED or REFUTED — no prose interpretation required.
 * <p>
 * Invariants:
 * <ul>
 *   <li>Implementations must be pure world-read-only; no state mutation.</li>
 *   <li>The radius field is advisory; the behavior navigates to within {@code sensorRadius}
 *       of {@code origin} before calling this, so the test always sees the relevant area.</li>
 * </ul>
 */
@FunctionalInterface
public interface ClaimPredicate {

    /**
     * Tests whether the claimed condition is currently true in the world.
     *
     * @param level  the server level to query
     * @param origin the position the hearsay tip refers to
     * @return true when the claim is confirmed, false when it is refuted
     */
    boolean test(ServerLevel level, Vec3 origin);

}
