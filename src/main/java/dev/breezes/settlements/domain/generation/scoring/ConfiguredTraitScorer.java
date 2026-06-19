package dev.breezes.settlements.domain.generation.scoring;

import dev.breezes.settlements.domain.common.BiomeId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.model.survey.SiteReport;
import dev.breezes.settlements.domain.generation.model.survey.WaterFeatureType;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import java.util.Map;

@AllArgsConstructor
public class ConfiguredTraitScorer implements TraitScorer {

    private final TraitScorerConfig config;

    public TraitScorerConfig config() {
        return this.config;
    }

    @Override
    public float score(@Nonnull SiteReport report) {
        for (ResourceTag vetoTag : this.config.vetoTags()) {
            if (report.resourceTags().contains(vetoTag)) {
                return 0.0f;
            }
        }

        for (ResourceTag requiredTag : this.config.requiredTags()) {
            if (!report.resourceTags().contains(requiredTag)) {
                return 0.0f;
            }
        }

        float score = this.config.baseScore();

        for (Map.Entry<ResourceTag, Float> entry : this.config.resourceTagWeights().entrySet()) {
            score += report.resourceDensities().getOrDefault(entry.getKey(), 0.0f) * entry.getValue();
        }

        for (Map.Entry<WaterFeatureType, Float> entry : this.config.waterFeatureWeights().entrySet()) {
            if (report.waterFeatureTypes().contains(entry.getKey())) {
                score += entry.getValue();
            }
        }

        for (Map.Entry<BiomeId, Float> entry : this.config.biomeWeights().entrySet()) {
            score += report.biomeDistribution().getOrDefault(entry.getKey(), 0.0f) * entry.getValue();
        }

        score += normalizedElevationDelta(report) * this.config.elevationDeltaWeight();
        return Math.clamp(score, 0.0f, 1.0f);
    }

    private float normalizedElevationDelta(@Nonnull SiteReport report) {
        int elevationDelta = report.elevation().max() - report.elevation().min();

        float normalization = this.config.elevationDeltaNormalization();
        if (normalization <= 0.0f) {
            return 0.0f;
        }

        return Math.clamp(elevationDelta / normalization, 0.0f, 1.0f);
    }

}
