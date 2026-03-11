package dev.breezes.settlements.presentation.ui.behavior;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.AllArgsConstructor;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

// TODO: BLOCKING should this be here?
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class BehaviorControllerDebugKeyMappings {

    public static final String DEBUG_KEY_CATEGORY = "key.categories.settlements.debug";

    public static final KeyMapping OPEN_BEHAVIOR_CONTROLLER = new KeyMapping(
            "key.settlements.open_behavior_controller_debug",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            DEBUG_KEY_CATEGORY
    );

}
