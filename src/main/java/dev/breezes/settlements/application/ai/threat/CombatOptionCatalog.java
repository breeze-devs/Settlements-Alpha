package dev.breezes.settlements.application.ai.threat;

import dev.breezes.settlements.di.ServerScope;
import jakarta.inject.Inject;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every registered {@link CombatOption}, sorted by declared order.
 */
@ServerScope
public final class CombatOptionCatalog {

    private final List<CombatOption> orderedOptions;

    /**
     * @throws IllegalArgumentException if two options declare the same order
     */
    @Inject
    CombatOptionCatalog(@Nonnull Set<CombatOption> options) {
        List<CombatOption> ordered = options.stream()
                .sorted(Comparator.comparingInt(CombatOption::order))
                .toList();

        Set<Integer> seenOrders = new HashSet<>();
        for (CombatOption option : ordered) {
            Validate.isTrue(seenOrders.add(option.order()),
                    "Duplicate CombatOption order %s declared by more than one CombatOption", option.order());
        }

        this.orderedOptions = ordered;
    }

    /**
     * Every registered option, sorted by declared order.
     */
    public List<CombatOption> orderedOptions() {
        return this.orderedOptions;
    }

}
