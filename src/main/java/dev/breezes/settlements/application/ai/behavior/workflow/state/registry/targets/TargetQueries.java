package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorContext;
import dev.breezes.settlements.application.ai.behavior.workflow.state.registry.BehaviorStateType;
import dev.breezes.settlements.domain.ai.brain.ISettlementsBrainEntity;
import dev.breezes.settlements.domain.ai.conditions.ICondition;
import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public final class TargetQueries {

    public static <T extends ISettlementsBrainEntity> Optional<Targetable> firstMatching(@Nonnull BehaviorContext<T> context,
                                                                                         @Nonnull ICondition<Targetable> condition) {
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .map(targetState -> targetState.match(condition))
                .flatMap(Stream::findFirst);
    }

    public static <T extends ISettlementsBrainEntity, E extends Entity> Optional<E> firstEntity(@Nonnull BehaviorContext<T> context,
                                                                                                @Nonnull EntityType<E> type,
                                                                                                @Nonnull Class<E> castTo) {
        ICondition<Targetable> matchesType = target -> target != null
                && target.getType() == TargetableType.ENTITY
                && target.getAsEntity().getType() == type;

        return firstMatching(context, matchesType)
                .map(Targetable::getAsEntity)
                .filter(castTo::isInstance)
                .map(castTo::cast);
    }

    /**
     * Returns the first {@link TargetableType#BLOCK} target as a {@link PhysicalBlock}, or empty if
     * the target state is missing or the first target is not a block.
     * <p>
     * Used by bespoke impact-keyframe steps that need the captured block snapshot (state + location).
     */
    public static <T extends ISettlementsBrainEntity> Optional<PhysicalBlock> firstBlock(@Nonnull BehaviorContext<T> context) {
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .filter(target -> target.getType() == TargetableType.BLOCK)
                .map(Targetable::getAsBlock);
    }

    /**
     * Convenience over {@link #firstBlock(BehaviorContext)} returning just the {@link BlockPos} —
     * matches the most common call-site shape inside harvest impact keyframes.
     */
    public static <T extends ISettlementsBrainEntity> Optional<BlockPos> firstBlockPos(@Nonnull BehaviorContext<T> context) {
        return firstBlock(context)
                .map(block -> block.getLocation(false).toBlockPos());
    }

    /**
     * Returns the location of the first (active) target, whether it is a block or an entity.
     * Used by the behavior framework to point the entity's head at whatever it is currently acting on.
     */
    public static <T extends ISettlementsBrainEntity> Optional<Location> firstTargetLocation(@Nonnull BehaviorContext<T> context) {
        return context.getState(BehaviorStateType.TARGET, TargetState.class)
                .flatMap(TargetState::getFirst)
                .map(Targetable::getLocation);
    }

}
