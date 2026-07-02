package dev.breezes.settlements.domain.fishing;

import lombok.Builder;
import lombok.Value;
import net.minecraft.resources.ResourceLocation;

@Value
@Builder
public class FishCatchEntry {

    ResourceLocation entityId;

    ResourceLocation itemId;

    double weight;

}
