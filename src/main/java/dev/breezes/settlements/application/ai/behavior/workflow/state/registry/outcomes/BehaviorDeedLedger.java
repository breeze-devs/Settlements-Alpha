package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.outcomes;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ordered deed facts recorded during a single behavior run.
 * <p>
 * Behaviors record the primary deed plus any qualitatively distinct secondary deeds here. The
 * ledger only accumulates facts — deciding which of them become world events belongs to whatever
 * reads it, so a behavior never has to know the fan-out policy in force.
 * <p>
 * TODO: nothing reads the ledger today. The deed vocabulary is deliberately kept recorded while
 *  the emission payload is redesigned, so the behaviors do not have to be revisited.
 */
@CustomLog
public final class BehaviorDeedLedger implements BehaviorState {

    public static final int MAX_ENTRIES = 3;

    private final List<BehaviorOutcome> entries = new ArrayList<>(1);

    public Optional<BehaviorOutcome> primary() {
        return this.entries.isEmpty() ? Optional.empty() : Optional.of(this.entries.get(0));
    }

    public BehaviorOutcome declarePrimary(@Nonnull BehaviorOutcome primary) {
        if (this.entries.isEmpty()) {
            this.entries.add(primary);
        } else {
            this.entries.set(0, primary);
        }
        return primary;
    }

    public BehaviorOutcome addSecondary(@Nonnull BehaviorOutcome secondary) {
        if (this.entries.size() >= MAX_ENTRIES) {
            log.behaviorWarn("Behavior deed ledger reached cap of {}; dropping secondary deed {}",
                    MAX_ENTRIES, secondary.getDeedType());
            return secondary;
        }

        this.entries.add(secondary);
        return secondary;
    }

    public List<BehaviorOutcome> entriesView() {
        return List.copyOf(this.entries);
    }

    @Override
    public void reset() {
        this.entries.clear();
    }

}
