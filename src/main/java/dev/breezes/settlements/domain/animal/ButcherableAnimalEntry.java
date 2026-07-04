package dev.breezes.settlements.domain.animal;

import lombok.Builder;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

@Builder
public record ButcherableAnimalEntry(
        ResourceLocation entityId,
        int minimumKeepCount,
        @Nullable String speciesNoun
) {

    public ButcherableAnimalEntry {
        if (minimumKeepCount < 0) {
            throw new IllegalArgumentException("minimumKeepCount must be >= 0, got " + minimumKeepCount);
        }
    }

}
