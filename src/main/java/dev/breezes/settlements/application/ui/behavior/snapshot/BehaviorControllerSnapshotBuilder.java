package dev.breezes.settlements.application.ui.behavior.snapshot;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.ai.behavior.model.BehaviorStatus;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class BehaviorControllerSnapshotBuilder {

    private static final BehaviorControllerSnapshotBuilder INSTANCE = new BehaviorControllerSnapshotBuilder();
    private static final Comparator<BehaviorRowSnapshot> BEHAVIOR_COMPARATOR =
            Comparator.comparingInt(BehaviorRowSnapshot::priority)
                    .reversed()
                    .thenComparingInt(BehaviorRowSnapshot::uiBehaviorIndex)
                    .thenComparing(BehaviorRowSnapshot::behaviorId);

    // TODO: migrate this?
    private static final List<SchedulePhase> SCHEDULE_PHASE_UI_ORDER = List.of(
            SchedulePhase.WORK,
            SchedulePhase.MEET,
            SchedulePhase.IDLE,
            SchedulePhase.REST,
            SchedulePhase.UNKNOWN
    );

    public static BehaviorControllerSnapshotBuilder getInstance() {
        return INSTANCE;
    }

    public BehaviorControllerSnapshot build(@Nonnull BaseVillager villager, long gameTime) {
        List<BehaviorRowSnapshot> rows = new ArrayList<>();
        for (BehaviorBinding binding : villager.getTrackedCustomBehaviors()) {
            rows.add(this.buildRow(villager, binding));
        }
        rows.sort(BEHAVIOR_COMPARATOR);

        Activity activity = villager.getBrain().getActiveNonCoreActivity().orElse(Activity.IDLE);
        return BehaviorControllerSnapshot.builder()
                .gameTime(gameTime)
                .villagerEntityId(villager.getId())
                .villagerName(villager.getName().getString())
                .scheduleBucket(mapSchedulePhase(activity))
                .rawActivityKey(activity.getName())
                .rows(rows)
                .build();
    }

    private BehaviorRowSnapshot buildRow(@Nonnull BaseVillager villager, @Nonnull BehaviorBinding binding) {
        IBehavior<BaseVillager> behavior = binding.behavior();
        if (!(behavior instanceof IBehaviorInfoProvider infoProvider)) {
            // TODO: we should make stateMachineBehavior default so it implements this
            throw new IllegalStateException("Behavior '%s' must implement IBehaviorInfoProvider".formatted(behavior.getClass().getName()));
        }

        BehaviorDescriptor descriptor = infoProvider.getBehaviorDescriptor();
        BehaviorRuntimeInformation runtimeInformation = infoProvider.getBehaviorRuntimeInformation(villager);

        boolean running = behavior.getStatus() == BehaviorStatus.RUNNING;
        return BehaviorRowSnapshot.builder()
                .behaviorId(behavior.getClass().getSimpleName())
                .displayNameKey(descriptor.displayNameKey())
                .displaySuffix(descriptor.displaySuffix())
                .iconItemId(descriptor.iconItemId())
                .priority(binding.priority())
                .uiBehaviorIndex(binding.uiBehaviorIndex())
                .registeredSchedules(mapRegisteredSchedules(binding.registeredActivities()))
                .running(running)
                .currentStageLabel(runtimeInformation.currentStageLabel())
                .cooldownRemainingTicks(runtimeInformation.cooldownRemainingTicks())
                .preconditionSummary(runtimeInformation.preconditionSummary())
                .build();
    }

    private static List<SchedulePhase> mapRegisteredSchedules(@Nonnull Set<Activity> activities) {
        List<SchedulePhase> mapped = activities.stream()
                .map(BehaviorControllerSnapshotBuilder::mapSchedulePhase)
                .distinct()
                .toList();

        return SCHEDULE_PHASE_UI_ORDER.stream()
                .filter(mapped::contains)
                .toList();
    }

    // TODO: this could be a mapper class?
    private static SchedulePhase mapSchedulePhase(@Nonnull Activity activity) {
        if (activity == Activity.REST) {
            return SchedulePhase.REST;
        }
        if (activity == Activity.WORK) {
            return SchedulePhase.WORK;
        }
        if (activity == Activity.MEET) {
            return SchedulePhase.MEET;
        }
        if (activity == Activity.IDLE || activity == Activity.PLAY) {
            return SchedulePhase.IDLE;
        }
        return SchedulePhase.UNKNOWN;
    }

}
