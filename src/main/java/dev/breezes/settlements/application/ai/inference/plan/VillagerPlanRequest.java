package dev.breezes.settlements.application.ai.inference.plan;

import dev.breezes.settlements.application.ai.inference.monologue.EpisodicEntryDTO;
import dev.breezes.settlements.application.ai.inference.monologue.PersonaBundle;
import dev.breezes.settlements.application.ai.inference.monologue.Snapshot;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PLAN request payload for one villager sent to SIS.
 * <p>
 * Reuses {@link PersonaBundle}, {@link Snapshot}, and {@link EpisodicEntryDTO} from the MONOLOGUE
 * package rather than duplicating them — same villager identity tokens ground both capabilities.
 * <p>
 * snapshot is required by SIS even when empty and must serialize as {"sites":{}}, exactly like
 * the MONOLOGUE request.
 */
@Builder
@Getter
public final class VillagerPlanRequest {

    private final UUID villagerId;
    private final PersonaBundle persona;

    /**
     * Spatial snapshot of the villager's sensed environment. Required by SIS.
     */
    private final Snapshot snapshot;

    /**
     * Structured episodic memory entries derived from the villager's knowledge store.
     */
    @Singular("episodic")
    private final List<EpisodicEntryDTO> episodic;

    /**
     * {@link dev.breezes.settlements.domain.ai.schedule.PlanDayType#name()} as a raw wire string —
     * SIS validates the value; the mod does not interpret it beyond passing it through.
     */
    private final String dayType;

    /**
     * The villager's closed set of selectable behaviors for today.
     */
    @Singular
    private final List<BehaviorOptionDTO> options;

    /**
     * Coarse windows to order the option selections within.
     */
    @Singular
    private final List<PlanWindowDTO> windows;

    /**
     * Currency name (e.g. "emeralds") to amount held. A plain map rather than a first-class field
     * per currency, since the currency set is expected to grow independently of this DTO's shape.
     */
    private final Map<String, Integer> wallet;

    private final float hunger;

    /**
     * Item registry id (e.g. {@code minecraft:wheat}) to held count across the villager's backpack.
     * Uncapped — SIS phrases inventory magnitude in prose rather than reasoning over exact numeric
     * ranges, so no threshold is pre-applied here the way demand/surplus counts are.
     */
    private final Map<String, Integer> inventory;

    /**
     * Unmet economic demands, priority-sorted (descending) by the caller. See {@link DemandDTO} —
     * shortfall/priority are mod-resolved decisions, not raw counts for the model to re-derive.
     */
    @Singular
    private final List<DemandDTO> demands;

    /**
     * Economic surplus available to trade or discard. See {@link SurplusDTO} — sellable/dumpable
     * are mod-resolved decisions, not raw stock counts.
     */
    @Singular("surplus")
    private final List<SurplusDTO> surplus;

}
