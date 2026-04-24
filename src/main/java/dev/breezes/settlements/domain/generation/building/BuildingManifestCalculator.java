package dev.breezes.settlements.domain.generation.building;

import dev.breezes.settlements.domain.generation.model.building.BuildingDefinition;
import dev.breezes.settlements.domain.generation.model.building.BuildingManifest;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.profile.TraitSlot;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class BuildingManifestCalculator {

    private static final String TOWN_HALL_ID = "settlements:building_definitions/town_hall";
    private static final String TAVERN_ID = "settlements:building_definitions/tavern";
    private static final String WELL_ID = "settlements:building_definitions/well";
    private static final String MARKET_STALL_ID = "settlements:building_definitions/market_stall";
    private static final String HOUSE_ID = "settlements:building_definitions/house";

    private static final Comparator<BuildingDefinition> MANIFEST_ORDER =
            Comparator.<BuildingDefinition>comparingInt(definition -> definition.requiresResources().isEmpty() ? 1 : 0)
                    .thenComparing(Comparator.comparingInt(BuildingDefinition::placementPriority).reversed());

    private final BuildingRegistry registry;

    public BuildingManifestCalculator(@Nonnull BuildingRegistry registry) {
        this.registry = registry;
    }

    public BuildingManifest calculate(@Nonnull SettlementProfile profile, @Nonnull Random random) {
        List<BuildingDefinition> manifest = new ArrayList<>();

        this.addUniversalBuildings(profile.scaleTier(), manifest, random);
        this.addTraitBuildings(profile, manifest, random);
        this.addHouses(profile.scaleTier(), manifest, random);

        List<BuildingDefinition> sorted = manifest.stream()
                .sorted(MANIFEST_ORDER)
                .toList();
        return new BuildingManifest(sorted);
    }

    private void addUniversalBuildings(@Nonnull ScaleTier scaleTier,
                                       @Nonnull List<BuildingDefinition> manifest,
                                       @Nonnull Random random) {
        manifest.add(this.requiredDefinition(TOWN_HALL_ID));
        manifest.add(this.requiredDefinition(WELL_ID));

        if (scaleTier != ScaleTier.HAMLET) {
            manifest.add(this.requiredDefinition(TAVERN_ID));
        }

        int marketCount = switch (scaleTier) {
            case HAMLET -> 0;
            case VILLAGE -> random.nextInt(1, 3 + 1);
            case TOWN -> random.nextInt(2, 5 + 1);
        };
        this.addCopies(manifest, this.requiredDefinition(MARKET_STALL_ID), marketCount);
    }

    private void addTraitBuildings(@Nonnull SettlementProfile profile,
                                   @Nonnull List<BuildingDefinition> manifest,
                                   @Nonnull Random random) {
        for (TraitId trait : profile.allTraits()) {
            TraitSlot slot = profile.getTraitSlot(trait);
            if (slot == null) {
                continue;
            }

            int drawCount = switch (slot) {
                case PRIMARY -> random.nextInt(2, 4 + 1);
                case SECONDARY -> random.nextInt(1, 2 + 1);
                case FLAVOR -> random.nextInt(0, 1 + 1);
            };

            List<BuildingDefinition> eligible = this.registry.forTrait(trait).stream()
                    .filter(definition -> meetsMinimumRank(definition.minimumRank(), slot))
                    .filter(definition -> this.weightFor(definition, trait) > 0.0d)
                    .toList();

            manifest.addAll(this.drawWeighted(eligible, trait, drawCount, random));
        }
    }

    private void addHouses(@Nonnull ScaleTier scaleTier,
                           @Nonnull List<BuildingDefinition> manifest,
                           @Nonnull Random random) {
        int targetTotal = random.nextInt(scaleTier.minBuildings(), scaleTier.maxBuildings() + 1);
        int houseCount = Math.max(0, targetTotal - manifest.size());

        this.addCopies(manifest, this.requiredDefinition(HOUSE_ID), houseCount);
    }

    private List<BuildingDefinition> drawWeighted(@Nonnull List<BuildingDefinition> eligible,
                                                  @Nonnull TraitId trait,
                                                  int count,
                                                  @Nonnull Random random) {
        if (eligible.isEmpty() || count <= 0) {
            return List.of();
        }

        List<BuildingDefinition> result = new ArrayList<>(count);

        // Phase 1: prefer unique building picks
        int uniqueDraws = Math.min(count, eligible.size());
        List<BuildingDefinition> remaining = new ArrayList<>(eligible);
        while (!remaining.isEmpty() && result.size() < uniqueDraws) {
            Optional<BuildingDefinition> selected = this.drawOne(remaining, trait, random);
            if (selected.isEmpty()) {
                break;
            }
            result.add(selected.get());
            remaining.remove(selected.get());
        }

        // Phase 2: if we need more buildings than there are unique, allow duplicates from the full weighted pool
        while (result.size() < count) {
            Optional<BuildingDefinition> redraw = this.drawOne(eligible, trait, random);
            if (redraw.isEmpty()) {
                break;
            }
            result.add(redraw.get());
        }

        return result;
    }

    private Optional<BuildingDefinition> drawOne(@Nonnull List<BuildingDefinition> pool,
                                                 @Nonnull TraitId trait,
                                                 @Nonnull Random random) {
        double totalWeight = 0.0d;
        for (BuildingDefinition definition : pool) {
            totalWeight += this.weightFor(definition, trait);
        }
        if (totalWeight <= 0.0d) {
            return Optional.empty();
        }

        double roll = random.nextDouble(totalWeight);
        double cumulative = 0.0d;
        BuildingDefinition last = null;
        for (BuildingDefinition definition : pool) {
            last = definition;
            cumulative += this.weightFor(definition, trait);
            if (roll < cumulative) {
                return Optional.of(definition);
            }
        }
        return Optional.ofNullable(last);
    }

    private double weightFor(@Nonnull BuildingDefinition definition, @Nonnull TraitId trait) {
        Float affinity = definition.traitAffinities().get(trait);
        if (affinity == null) {
            return 0.0d;
        }
        return affinity * definition.placementPriority();
    }

    private BuildingDefinition requiredDefinition(@Nonnull String id) {
        return this.registry.byId(id)
                .orElseThrow(() -> new IllegalStateException("Missing required building definition: " + id));
    }

    private void addCopies(@Nonnull List<BuildingDefinition> manifest,
                           @Nonnull BuildingDefinition definition,
                           int count) {
        for (int i = 0; i < count; i++) {
            manifest.add(definition);
        }
    }

    private static boolean meetsMinimumRank(@Nonnull TraitSlot minimumRank, @Nonnull TraitSlot actualSlot) {
        return actualSlot.ordinal() <= minimumRank.ordinal();
    }

}
