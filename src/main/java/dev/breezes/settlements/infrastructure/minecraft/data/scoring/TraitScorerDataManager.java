package dev.breezes.settlements.infrastructure.minecraft.data.scoring;

import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.scoring.ConfiguredTraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerConfig;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerConfigCodec;
import dev.breezes.settlements.domain.generation.scoring.TraitScorerRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.framework.CodecJsonDataManager;
import jakarta.inject.Inject;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

public class TraitScorerDataManager extends CodecJsonDataManager<TraitScorerConfig> implements TraitScorerRegistry {

    private static final String DIRECTORY_PATH = "settlements/traits/scoring";

    private Map<TraitId, TraitScorer> rawScorersByTrait = Map.of();
    private Map<TraitId, TraitScorer> activeScorersByTrait = Map.of();

    @Inject
    public TraitScorerDataManager() {
        super(DIRECTORY_PATH, TraitScorerConfigCodec.CODEC);
    }

    @Override
    protected String label() {
        return "trait scorer config";
    }

    @Override
    protected void onReloaded(@Nonnull Map<ResourceLocation, TraitScorerConfig> values) {
        // A later file overrides an earlier one that configures the same trait
        Map<TraitId, TraitScorer> merged = new LinkedHashMap<>();
        for (TraitScorerConfig config : values.values()) {
            merged.put(config.trait(), new ConfiguredTraitScorer(config));
        }

        Map<TraitId, TraitScorer> immutable = Map.copyOf(merged);
        this.rawScorersByTrait = immutable;
        this.activeScorersByTrait = immutable;
    }

    @Override
    public Map<TraitId, TraitScorer> allScorers() {
        return this.activeScorersByTrait;
    }

    public Map<TraitId, TraitScorer> rawScorers() {
        return this.rawScorersByTrait;
    }

    public void replaceActiveScorers(@Nonnull Map<TraitId, TraitScorer> validatedScorers) {
        this.activeScorersByTrait = Map.copyOf(validatedScorers);
    }

}
