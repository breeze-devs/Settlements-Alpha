package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.SettlementsMod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.resources.ResourceLocation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResourceLocationUtil {

    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, path);
    }

}
