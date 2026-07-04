package dev.breezes.settlements.application.ui.stats.model;

import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.personality.OriginType;
import lombok.Builder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Builder
public record VillagerStatsSnapshot(
        long gameTime,
        int villagerEntityId,
        @Nullable String villagerName,
        @Nonnull String professionKey,
        int expertiseLevel,
        float currentHealth,
        float maxHealth,
        @Nonnull double[] geneValues,
        @Nullable BlockPos homePos,
        @Nullable BlockPos workstationPos,
        @Nullable String activeBehaviorNameKey,
        @Nullable String activeBehaviorIconId,
        @Nonnull SchedulePhase schedulePhase,
        int reputation,
        float hunger,
        int walletBalance,
        @Nonnull List<String> adjectives,
        @Nullable String characterSketch,
        @Nullable OriginType origin
) {

    public VillagerStatsSnapshot {
        if (geneValues.length != GeneType.VALUES.length) {
            throw new IllegalArgumentException("geneValues must have exactly " + GeneType.VALUES.length + " entries");
        }
        geneValues = geneValues.clone();
        // Lombok's @Builder leaves unset fields null rather than applying record defaults,
        // so an un-set adjectives list would otherwise NPE the codec's varint-length write.
        adjectives = adjectives == null ? List.of() : List.copyOf(adjectives);
    }

    @Override
    public double[] geneValues() {
        return geneValues.clone();
    }

}
