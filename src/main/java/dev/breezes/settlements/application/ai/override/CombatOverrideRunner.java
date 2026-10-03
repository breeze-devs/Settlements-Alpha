package dev.breezes.settlements.application.ai.override;

import dev.breezes.settlements.application.ai.threat.CombatOption;
import dev.breezes.settlements.application.ai.threat.CombatSelection;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.override.OverrideTier;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.domain.ai.perception.SensedEntity;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * Runs a villager's fight as successive combat actions, one at a time, each launched from the villager's current
 * combat selection.
 * <p>
 * The runner remains active between combat actions, keeping movement and interaction reserved so ordinary behavior
 * cannot resume mid-fight.
 */
@CustomLog
final class CombatOverrideRunner implements OverrideRunner {

    /**
     * Bounds one combat action, so a wedged action is replaced instead of holding the fight.
     */
    private static final ClockTicks ACTION_MAX_DURATION = ClockTicks.seconds(30);

    // Villagers can keep talking while they fight, so SOCIAL and COGNITION stay free
    private static final Set<BehaviorChannel> OCCUPIED_CHANNELS =
            Set.of(BehaviorChannel.MOVEMENT, BehaviorChannel.INTERACTION);

    private final OverrideTier tier;

    @Nullable
    private IBehavior<BaseVillager> action;

    /**
     * The option that created the running action; null exactly when no action runs.
     */
    @Nullable
    private CombatOption actionOption;

    private int actionElapsedTicks;

    /**
     * The hostile this runner last pointed the villager's look target at; null when it set none.
     */
    @Nullable
    private LivingEntity facedHostile;

    private boolean ended;

    CombatOverrideRunner(@Nonnull OverrideTier tier) {
        this.tier = tier;
    }

    @Override
    public void start(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Do nothing
    }

    /**
     * Ends execution once the verdict leaves COMBAT or an action throws.
     * An action past its time ceiling is replaced, not ended.
     */
    @Override
    public void tick(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (this.ended) {
            return;
        }

        CombatSelection selection = villager.getSettlementsBrain().threats().selection();
        if (selection == null) {
            this.ended = true;
            return;
        }

        if (this.action != null) {
            if (!this.shouldReplaceAction(villager, selection)) {
                this.tickAction(level, villager);
                return;
            }
            // Discharged in full before anything replaces it, so two actions never hold the villager at once
            this.stopAction(level, villager);
        }

        LivingEntity target = resolveTarget(level, villager, selection);
        if (target == null) {
            this.lookAtNearestAwareHostile(villager);
            return;
        }

        this.launchAction(level, villager, selection.option(), target);
    }

    @Override
    public void stop(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        if (this.action != null) {
            this.stopAction(level, villager);
        }

        if (this.facedHostile != null) {
            // The facing is this runner's own, so it must not outlive the fight
            villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            this.facedHostile = null;
        }
    }

    @Override
    public boolean isComplete() {
        return this.ended;
    }

    @Override
    public OverrideTier tier() {
        return this.tier;
    }

    @Override
    public Set<BehaviorChannel> occupiedChannels() {
        return OCCUPIED_CHANNELS;
    }

    @Override
    public String diagnosticId() {
        return "combat";
    }

    private boolean shouldReplaceAction(@Nonnull BaseVillager villager, @Nonnull CombatSelection selection) {
        if (this.action.getStatus() == BehaviorStatus.STOPPED) {
            return true;
        }
        // Only the assessment's approved option may act, so a changed selection retires the running action
        if (selection.option() != this.actionOption) {
            return true;
        }
        if (this.actionElapsedTicks >= ACTION_MAX_DURATION.getTicksAsInt()) {
            log.behaviorWarn("Combat action exceeded max duration ({} ticks) for villager {}; replacing it",
                    ACTION_MAX_DURATION.getTicksAsInt(), villager.getUUID());
            return true;
        }
        return false;
    }

    private void tickAction(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        this.actionElapsedTicks += 1;
        try {
            this.action.tick(1, level, villager);
        } catch (Exception e) {
            log.behaviorError("Combat action threw during tick for villager {}; ending combat execution", villager.getUUID(), e);
            // Signal completion so a failed tick still follows the normal stop() cleanup contract.
            this.ended = true;
        }
    }

    private void launchAction(@Nonnull ServerLevel level,
                              @Nonnull BaseVillager villager,
                              @Nonnull CombatOption option,
                              @Nonnull LivingEntity target) {
        // TODO: an option that acquires a resource at launch, such as a ballista lease, must invalidate the selection
        //  and trigger reassessment when acquisition fails, rather than relaunch a stale choice. Eggs are conjured, so
        //  no registered option acquires anything.
        IBehavior<BaseVillager> next;
        try {
            next = option.createBehavior(target);
            next.start(level, villager);
        } catch (Exception e) {
            log.behaviorError("Combat action threw during start for villager {}; ending combat execution", villager.getUUID(), e);
            this.ended = true;
            return;
        }

        // The action now directs the villager's gaze itself
        this.facedHostile = null;
        this.action = next;
        this.actionOption = option;
        this.actionElapsedTicks = 0;
    }

    private void stopAction(@Nonnull ServerLevel level, @Nonnull BaseVillager villager) {
        // Avoid repeating teardown after natural completion
        if (this.action.getStatus() != BehaviorStatus.STOPPED) {
            this.action.stop(level, villager);
        }
        this.action = null;
        this.actionOption = null;
    }

    /**
     * Get the selection's target if it is alive and within reach; otherwise null.
     */
    @Nullable
    private static LivingEntity resolveTarget(@Nonnull ServerLevel level,
                                              @Nonnull BaseVillager villager,
                                              @Nonnull CombatSelection selection) {
        UUID targetId = selection.targetId();
        if (targetId == null) {
            return null;
        }

        // The selection is only as fresh as the last assessment, so its target may have died or moved off since
        if (!(level.getEntity(targetId) instanceof LivingEntity target) || !target.isAlive()) {
            return null;
        }

        double reach = selection.option().reachBlocks();
        return villager.distanceToSqr(target) <= reach * reach ? target : null;
    }

    private void lookAtNearestAwareHostile(@Nonnull BaseVillager villager) {
        PerceivedEntities hostiles = villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_HOSTILES)
                .orElse(PerceivedEntities.empty());
        // Nearest first, so the first live entry is the nearest live hostile
        for (SensedEntity sensed : hostiles.entities()) {
            LivingEntity hostile = sensed.entity();
            if (!hostile.isAlive()) {
                continue;
            }

            Brain<Villager> brain = villager.getBrain();
            // Set only when the hostile changes or the look target is gone, so holding a facing allocates nothing
            if (hostile != this.facedHostile || !brain.hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
                brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(hostile, true));
                this.facedHostile = hostile;
            }
            return;
        }
    }

}
