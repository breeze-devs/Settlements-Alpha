package dev.breezes.settlements.util;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtil {

    public static ResourceLocation mod(String path) {
        return ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, path);
    }

}
