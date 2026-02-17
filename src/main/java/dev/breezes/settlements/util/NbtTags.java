package dev.breezes.settlements.util;

import dev.breezes.settlements.SettlementsMod;

public class NbtTags {

    public static String of(String tag) {
        return "%s:%s".formatted(SettlementsMod.MOD_ID, tag);
    }

}
