package dev.breezes.settlements.presentation.ui.keybindings;

import com.mojang.blaze3d.platform.InputConstants;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

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
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );

    public static final KeyMapping OPEN_DAY_PLAN = new KeyMapping(
            "key.settlements.open_day_plan",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            KEY_CATEGORY
    );

    public static final KeyMapping OPEN_DEBUG_POSE_OVERLAY = new KeyMapping(
            "key.settlements.open_debug_pose_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            KEY_CATEGORY
    );

}
