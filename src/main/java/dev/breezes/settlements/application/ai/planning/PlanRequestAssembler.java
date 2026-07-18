package dev.breezes.settlements.application.ai.planning;

import dev.breezes.settlements.application.ai.inference.monologue.EpisodicEntryAssembler;
import dev.breezes.settlements.application.ai.inference.monologue.EpisodicEntryDTO;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.application.ai.inference.monologue.PersonaBundleAssembler;
import dev.breezes.settlements.application.ai.inference.monologue.SnapshotAssembler;
import dev.breezes.settlements.application.ai.inference.plan.BehaviorOptionDTO;
import dev.breezes.settlements.application.ai.inference.plan.DemandDTO;
import dev.breezes.settlements.application.ai.inference.plan.PlanBatchRequest;
import dev.breezes.settlements.application.ai.inference.plan.PlanWindowDTO;
import dev.breezes.settlements.application.ai.inference.plan.SurplusDTO;
import dev.breezes.settlements.application.ai.inference.plan.VillagerPlanRequest;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.supply.ActiveSupply;
import dev.breezes.settlements.application.economy.supply.SupplyEvaluator;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.WeightedBehavior;
import dev.breezes.settlements.domain.ai.planning.PlanGenerationContext;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.inventory.BackpackEntry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds {@link VillagerPlanRequest}/{@link PlanBatchRequest} — the LLM day-planning (PLAN)
 * request — from live villager state.
 * <p>
 * Mirrors {@link MonologueRequestAssembler}: persona/snapshot/episodic grounding is delegated
 * verbatim to the same three assemblers the MONOLOGUE capability uses, so both capabilities
 * ground a villager identically. Unlike MONOLOGUE, the caller supplies the
 * {@link PlanGenerationContext} rather than this class building one — the request's windows and
 * option menu must describe the SAME context {@link PlanGenerationContextFactory} produced for
 * the overlay generator, or SIS's selections would be scored against a plan the mod never
 * actually assembles.
 * <p>
 * Everything here runs on the SERVER THREAD: snapshot and episodic reads are not off-thread safe
 * (see {@link SnapshotAssembler} / {@link EpisodicEntryAssembler}), same rule as MONOLOGUE.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PlanRequestAssembler {

    private final PersonaBundleAssembler personaBundleAssembler;
    private final SnapshotAssembler snapshotAssembler;
    private final EpisodicEntryAssembler episodicEntryAssembler;
    private final DemandEvaluator demandEvaluator;
    private final SupplyEvaluator supplyEvaluator;
    private final VillagerWallet villagerWallet;
    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final DayPlanComposer composer;

    /**
     * Pairs a villager with the {@link PlanGenerationContext} the caller already built (via
     * {@link PlanGenerationContextFactory}) for that same villager. Taking the context as input,
     * rather than deriving one here, guarantees the request's windows/options match what the
     * overlay will later pack.
     */
    public record VillagerContext(BaseVillager villager, PlanGenerationContext context) {
    }

    /**
     * Assembles a multi-villager {@link PlanBatchRequest} covering every supplied pairing, so one
     * batch bounds the round trip to SIS regardless of village size.
     */
    public PlanBatchRequest assemble(@Nonnull List<VillagerContext> villagersWithContexts) {
        PlanBatchRequest.PlanBatchRequestBuilder builder = PlanBatchRequest.builder();
        for (VillagerContext entry : villagersWithContexts) {
            builder.villager(this.buildVillagerRequest(entry.villager(), entry.context()));
        }
        return builder.build();
    }

    /**
     * Assembles a single-villager {@link VillagerPlanRequest}. {@code context} must have been
     * built for {@code villager} — this method never re-derives one.
     */
    public VillagerPlanRequest buildVillagerRequest(@Nonnull BaseVillager villager, @Nonnull PlanGenerationContext context) {
        VillagerPlanRequest.VillagerPlanRequestBuilder requestBuilder = VillagerPlanRequest.builder()
                .villagerId(villager.getUUID())
                .persona(this.personaBundleAssembler.assemble(villager))
                .snapshot(this.snapshotAssembler.assemble(villager))
                .dayType(context.dayType().name())
                .wallet(Map.of("emeralds", this.villagerWallet.getBalance(villager)))
                .hunger(villager.getHunger())
                .inventory(this.buildInventory(villager));

        List<EpisodicEntryDTO> episodic = this.episodicEntryAssembler.assemble(
                villager.getUUID(), villager.getKnowledgeStore(), villager.level().getGameTime());
        episodic.forEach(requestBuilder::episodic);

        this.buildOptions(context).forEach(requestBuilder::option);
        this.buildWindows(context).forEach(requestBuilder::window);
        this.buildDemands(villager).forEach(requestBuilder::demand);
        this.buildSurplus(villager).forEach(requestBuilder::surplus);

        return requestBuilder.build();
    }

    /**
     * Builds the flat option menu from the context's already-resolved pool.
     * <p>
     * On a rest day (or for a NITWIT, who is hard-routed through the rest-day generator
     * regardless of {@link PlanDayType}) the menu is restricted to behaviors the rest-day policy
     * would actually weight above zero — the mod frames the rest day by only offering
     * rest-appropriate behaviors, so a farmer is never offered "harvest ore" on a day off.
     */
    private List<BehaviorOptionDTO> buildOptions(PlanGenerationContext context) {
        boolean restrictToRestDayMenu = context.dayType() == PlanDayType.REST_DAY
                || context.profession().equals(VillagerProfessionKey.NITWIT);

        List<BehaviorOptionDTO> options = new ArrayList<>();
        for (WeightedBehavior behavior : context.availableBehaviors()) {
            if (restrictToRestDayMenu
                    && PlannerPolicy.restDayMultiplier(behavior.descriptor(), context.restDayPolicy()) <= 0.0D) {
                continue;
            }

            options.add(BehaviorOptionDTO.builder()
                    .id(behavior.key().id())
                    .description(behavior.descriptor().getDescription())
                    .category(behavior.descriptor().getCategory().name())
                    .intensity(behavior.descriptor().getIntensity().name())
                    .estimatedMinutes(behavior.descriptor().getEstimatedDuration().getAsGameMinutes())
                    .hasOpportunity(!context.behaviorsLackingOpportunity().contains(behavior.key()))
                    .build());
        }
        return options;
    }

    /**
     * Mirrors {@link DayPlanComposer#bands} into wire windows — the SAME band objects the composer
     * will later pack, not a re-derived copy. Window bounds are already civil-space ticks
     * (0 = midnight), so the ids and tick ranges the overlay later packs into are exactly what SIS
     * is shown here.
     */
    private List<PlanWindowDTO> buildWindows(PlanGenerationContext context) {
        List<PlanWindowDTO> windows = new ArrayList<>();
        for (PlanBand band : this.composer.bands(context)) {
            windows.add(PlanWindowDTO.builder()
                    .id(band.id())
                    .startTick(band.startCivil())
                    .endTick(band.endCivil())
                    .build());
        }
        return windows;
    }

    private List<DemandDTO> buildDemands(BaseVillager villager) {
        List<DemandDTO> demands = new ArrayList<>();
        // DemandEvaluator already returns its list priority-sorted (descending); preserving that
        // order here is what makes the wire order the priority order per DemandDTO's contract.
        for (ActiveDemand demand : this.demandEvaluator.resolve(villager)) {
            demands.add(DemandDTO.builder()
                    .token(matchToken(demand.match()))
                    .shortfall(demand.desiredCount())
                    .priority(demand.priority())
                    .build());
        }
        return demands;
    }

    /**
     * Merges the dumpable side (from {@link SupplyEvaluator}) and the sellable side (derived from
     * the villager's own trade catalog offers) into one {@link SurplusDTO} per token, so SIS sees
     * a single economic picture per item rather than two independently-sourced lists that might
     * both mention the same token.
     */
    private List<SurplusDTO> buildSurplus(BaseVillager villager) {
        Map<String, Integer> dumpableByToken = new LinkedHashMap<>();
        for (ActiveSupply supply : this.supplyEvaluator.resolve(villager)) {
            dumpableByToken.merge(matchToken(supply.match()), supply.dumpableCount(), Integer::sum);
        }

        Map<String, Integer> sellableByToken = new LinkedHashMap<>();
        for (OfferEntry offer : this.tradeCatalogRegistry.offersFor(villager.getProfession())) {
            int held = villager.getSettlementsInventory().countMatching(offer.match());
            int sellable = Math.max(held - offer.surplusThreshold(), 0);
            sellableByToken.merge(matchToken(offer.match()), sellable, Integer::sum);
        }

        Set<String> tokens = new LinkedHashSet<>(dumpableByToken.keySet());
        tokens.addAll(sellableByToken.keySet());

        List<SurplusDTO> surplus = new ArrayList<>();
        for (String token : tokens) {
            int dumpable = dumpableByToken.getOrDefault(token, 0);
            int sellable = sellableByToken.getOrDefault(token, 0);
            // Drop all-zero entries: a token can reach this map via one side (e.g. a trade offer
            // whose current stock is still under its surplus threshold) without carrying anything
            // actionable to report.
            if (sellable > 0 || dumpable > 0) {
                surplus.add(SurplusDTO.builder().token(token).sellable(sellable).dumpable(dumpable).build());
            }
        }
        return surplus;
    }

    private Map<String, Integer> buildInventory(BaseVillager villager) {
        Map<String, Integer> inventory = new LinkedHashMap<>();
        for (BackpackEntry entry : villager.getSettlementsInventory().entries()) {
            String key = BuiltInRegistries.ITEM.getKey(entry.representative().getItem()).toString();
            inventory.merge(key, entry.count(), Integer::sum);
        }
        return inventory;
    }

    /**
     * Renders an {@link ItemMatch} to its wire token form. Deliberately NOT
     * {@link ItemMatch#asDebugString()}, which prepends {@code #} to a tag's location — the PLAN
     * wire contract carries tag tokens bare (e.g. {@code c:foods}, not {@code #c:foods}).
     */
    private static String matchToken(ItemMatch match) {
        return switch (match) {
            case ItemMatch.ItemRef itemRef -> itemRef.id().toString();
            case ItemMatch.TagRef tagRef -> tagRef.tag().location().toString();
        };
    }

}
