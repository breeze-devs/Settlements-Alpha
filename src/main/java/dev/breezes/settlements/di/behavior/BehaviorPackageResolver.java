package dev.breezes.settlements.di.behavior;

import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.application.ui.behavior.snapshot.BehaviorBinding;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.behavior.adapter.DefaultBehaviorAdapter;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ServerScope
public class BehaviorPackageResolver {

    public record ResolvedBehaviors(
            @Nonnull List<Pair<? extends BehaviorControl<? super Villager>, Integer>> choiceBehaviors,
            @Nonnull List<BehaviorBinding> trackedBindings
    ) {
    }

    private final Map<VillagerProfessionKey, List<BehaviorRegistration>> professionBehaviors;

    @Inject
    public BehaviorPackageResolver(@Nonnull Set<BehaviorRegistration> registrations) {
        // Group once at component creation time so villager brain registration does not
        // repeatedly re-scan the entire behavior catalog on every entity spawn.
        this.professionBehaviors = registrations.stream()
                .sorted(Comparator
                        .comparing((BehaviorRegistration r) -> r.profession().id())
                        .thenComparing(r -> String.valueOf(r.activity()))
                        .thenComparingInt(BehaviorRegistration::priority)
                        .thenComparingInt(BehaviorRegistration::weight))
                .collect(Collectors.groupingBy(BehaviorRegistration::profession));
    }

    public ResolvedBehaviors resolve(@Nonnull VillagerProfessionKey villagerProfessionKey, @Nonnull Activity activity) {
        List<BehaviorRegistration> registrations = this.professionBehaviors.getOrDefault(villagerProfessionKey, List.of());
        List<Pair<? extends BehaviorControl<? super Villager>, Integer>> choiceBehaviors = new ArrayList<>();
        List<BehaviorBinding> trackedBindings = new ArrayList<>();

        for (BehaviorRegistration registration : registrations) {
            if (!registration.activity().equals(activity)) {
                continue;
            }

            // Create the shared behavior instance between brain and UI tracking.
            // The brain ticks this instance; the snapshot builder reads the live state from it.
            IBehavior<BaseVillager> behavior = registration.behaviorFactory().get();
            choiceBehaviors.add(Pair.of(DefaultBehaviorAdapter.adapt(behavior), registration.weight()));
            trackedBindings.add(BehaviorBinding.builder()
                    .behavior(behavior)
                    .priority(registration.priority())
                    .uiBehaviorIndex(-1)
                    .registeredActivities(Set.of(registration.activity()))
                    .build());
        }

        return new ResolvedBehaviors(choiceBehaviors, trackedBindings);
    }

    /**
     * All registered behaviors across all professions.
     * Intended for diagnostics and future LLM agent use.
     */
    @Nonnull
    public List<BehaviorRegistration> allRegistered() {
        return this.professionBehaviors.values().stream()
                .flatMap(List::stream)
                .toList();
    }

}
