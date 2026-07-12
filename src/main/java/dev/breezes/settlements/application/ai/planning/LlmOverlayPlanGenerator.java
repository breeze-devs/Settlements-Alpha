package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.inference.plan.PlanSelection;
import dev.breezes.settlements.application.ai.inference.plan.VillagerPlanResult;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PinnedSelection;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.planning.PlanIntent;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns an LLM-authored {@link VillagerPlanResult} into a {@link DayPlan}.
 * <p>
 * This class owns the {@link VillagerPlanResult} → {@link PlanIntent} mapping —
 * every stage of composition (frame, meals, band derivation, gap-fill) lives in the composer, so
 * this generator produces output that satisfies the same {@link DayPlan} validation the heuristic
 * does, by construction.
 * <p>
 * The mapping populates one {@link PlanIntent} entry per band the composer currently derives for
 * this context (via {@link DayPlanComposer#bands}), defaulting to an empty list for any band the
 * model didn't address. This is load-bearing, not cosmetic: it is what makes an entirely empty
 * {@link VillagerPlanResult} distinguishable from {@link PlanIntent#empty()} — "the model considered
 * every band and chose nothing" (a legitimately sparse day) must still route through THIS class's
 * fill policy, never fall back to the heuristic's own pool.
 * <p>
 * A selection carrying {@code at} is split off into a {@link PinnedSelection} instead of the
 * ordinary per-band fill list — the pinned pass (the composer's anchor stage) consumes it; only a
 * failed/demoted placement re-adds it to fill, and that re-add happens composer-side, not here.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class LlmOverlayPlanGenerator {

    private final DayPlanComposer composer;

    public DayPlan generate(PlanGenerationContext context, VillagerPlanResult result) {
        List<PlanBand> bands = this.composer.bands(context);
        return this.composer.compose(context, toIntent(bands, result));
    }

    @VisibleForTesting
    static PlanIntent toIntent(List<PlanBand> bands, VillagerPlanResult result) {
        Map<String, List<BehaviorKey>> selections = new LinkedHashMap<>();
        List<PinnedSelection> pins = new ArrayList<>();

        for (PlanBand band : bands) {
            List<PlanSelection> bandSelections = result.getSelections().getOrDefault(band.id(), List.of());
            List<BehaviorKey> selectedKeysInOrder = new ArrayList<>();

            for (PlanSelection selection : bandSelections) {
                if (selection == null || StringUtils.isBlank(selection.getId())) {
                    continue;
                }
                BehaviorKey key = BehaviorKey.of(selection.getId());

                if (selection.getAt() != null) {
                    int civilTick = civilTickOf(selection.getAt());
                    if (civilTick >= 0) {
                        pins.add(new PinnedSelection(key, civilTick, key.equals(BehaviorKey.EAT_FOOD)));
                    }
                    // A malformed `at` value drops the pin silently
                    continue;
                }

                selectedKeysInOrder.add(key);
            }

            // distinct() keeps first occurrence and drops later repeats, matching the "ordered,
            // de-duped" contract the packer's rank map already assumes (see OrderedPreferenceSelectionStrategy).
            // Unknown ids are NOT filtered here — they simply never match a pool entry once the
            // composer restricts the band's pool to its available menu, so they drop out silently
            // downstream without needing a lookup against the catalog at mapping time.
            selections.put(band.id(), selectedKeysInOrder.stream().distinct().toList());
        }

        return new PlanIntent(selections, pins);
    }

    /**
     * Resolves an {@code at} bucket name (e.g. {@code "AT_18_00"}) to its civil tick, or -1 if the
     * name is not a real {@link TimeOfDay} constant.
     */
    private static int civilTickOf(String at) {
        try {
            return TimeOfDay.valueOf(at).getCivilTick();
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }

}
