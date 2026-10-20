package dev.breezes.settlements.domain.generation.model.survey;

import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;

public record SurveyBounds(
        BoundingRegion buildArea,
        BoundingRegion sampleArea,
        int padding
) {

    /**
     * How far past a settlement's build area its survey samples the land, in blocks.
     */
    private static final int SETTLEMENT_PADDING = 30;

    public SurveyBounds {
        if (padding < 0) {
            throw new IllegalArgumentException("padding must be >= 0");
        }
    }

    public static SurveyBounds fromBuildArea(BoundingRegion buildArea, int padding) {
        return new SurveyBounds(buildArea, buildArea.expandedBy(padding), padding);
    }

    /**
     * Returns the area surveyed for a settlement centered on the given block column. Every settlement, generated or
     * founded and whatever its tier, is surveyed over an area of this size, so the scores of any two sites compare.
     */
    public static SurveyBounds aroundSettlementAt(int centerX, int centerZ) {
        int radius = ScaleTier.VILLAGE.areaRadius();
        BlockPosition center = new BlockPosition(centerX, 0, centerZ);
        return fromBuildArea(new BoundingRegion(
                center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius)
        ), SETTLEMENT_PADDING);
    }

}
