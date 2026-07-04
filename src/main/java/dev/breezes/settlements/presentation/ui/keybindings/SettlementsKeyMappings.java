package dev.breezes.settlements.presentation.ui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.KeyMapping;

/**
 * Central registry of all key mappings for the Settlements mod.
 */
@ClientSide
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsKeyMappings {

    public static final String KEY_CATEGORY = "mod.name";

    public static final KeyMapping OPEN_VILLAGER_STATS = new KeyMapping(
            "key.settlements.open_villager_stats",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    public static final KeyMapping OPEN_DAY_PLAN = new KeyMapping(
            "key.settlements.open_day_plan",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

}
