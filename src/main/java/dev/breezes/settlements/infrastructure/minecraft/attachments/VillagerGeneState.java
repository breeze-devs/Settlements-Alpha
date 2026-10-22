package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.genetics.GeneType;

public record VillagerGeneState(GeneType type,
                                double value) {
}
