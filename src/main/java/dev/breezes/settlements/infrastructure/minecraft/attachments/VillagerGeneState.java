package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.genetics.GeneType;

import javax.annotation.Nonnull;

public record VillagerGeneState(@Nonnull GeneType type,
                                double value) {
}
