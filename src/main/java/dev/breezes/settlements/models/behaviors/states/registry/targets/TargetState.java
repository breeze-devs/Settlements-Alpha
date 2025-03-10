package dev.breezes.settlements.models.behaviors.states.registry.targets;

import dev.breezes.settlements.models.behaviors.states.BehaviorState;
import dev.breezes.settlements.models.conditions.ICondition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class TargetState implements BehaviorState {

    /**
     * List of targets, possibly ranked by priority
     * <p>
     * Must be mutable to allow for dynamic target selection
     */
    private List<Targetable> targets;

    public TargetState(@Nonnull List<Targetable> targets) {
        this.targets = new ArrayList<>(targets);
    }

    public static TargetState of(@Nonnull List<Targetable> targets) {
        return new TargetState(targets);
    }

    public static TargetState of(@Nonnull Targetable target) {
        return new TargetState(List.of(target));
    }

    public boolean hasTarget() {
        return !this.targets.isEmpty();
    }

    public Optional<Targetable> getFirst() {
        return this.targets.stream()
                .findFirst();
    }

    public void setTargets(@Nonnull List<Targetable> targets) {
        this.targets = new ArrayList<>(targets);
    }

    public void addTarget(@Nonnull Targetable target) {
        this.targets.add(target);
    }

    public Stream<Targetable> match(@Nonnull ICondition<Targetable> condition) {
        return this.targets.stream()
                .filter(condition);
    }

    @Override
    public void reset() {
        this.targets = new ArrayList<>();
    }

}
