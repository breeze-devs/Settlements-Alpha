package dev.breezes.settlements.infrastructure.minecraft.navigation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;

/**
 * Installs {@link SettlementsWalkNodeEvaluator} in place of vanilla's {@link
 * net.minecraft.world.level.pathfinder.WalkNodeEvaluator} so fence-gate-aware pathing is available
 * to anything using this navigation. Otherwise identical to {@link GroundPathNavigation}.
 */
public class SettlementsGroundPathNavigation extends GroundPathNavigation {

    public SettlementsGroundPathNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new SettlementsWalkNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
    }

}
