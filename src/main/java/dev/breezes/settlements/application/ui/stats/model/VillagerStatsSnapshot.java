package dev.breezes.settlements.application.ui.stats.model;

import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.domain.genetics.GeneType;
import lombok.Builder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
        @Nullable String activeBehaviorStage,
        @Nullable String activeBehaviorIconId,
        @Nonnull SchedulePhase schedulePhase,
        int reputation,
        float hunger
) {

    public VillagerStatsSnapshot {
        if (geneValues.length != GeneType.VALUES.length) {
            throw new IllegalArgumentException("geneValues must have exactly " + GeneType.VALUES.length + " entries");
        }
        geneValues = geneValues.clone();
    }

    @Override
    public double[] geneValues() {
        return geneValues.clone();
    }

}
