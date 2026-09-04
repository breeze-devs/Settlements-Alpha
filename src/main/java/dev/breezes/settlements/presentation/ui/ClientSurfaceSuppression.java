package dev.breezes.settlements.presentation.ui;

import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;

/**
 * The client states in which a mod surface does not draw, one rule per family of surface.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientSurfaceSuppression {

    public static boolean isHudSuppressed(@Nonnull Minecraft minecraft) {
        return isSuppressedForEverySurface(minecraft)
                || minecraft.screen != null
                || minecraft.player.isSpectator();
    }

    /**
     * Whether any villager's chat bubbles may draw this frame.
     */
    public static boolean isChatBubblesSuppressed(@Nonnull Minecraft minecraft) {
        return !GeneralConfig.chatBubblesEnabled
                || isSuppressedForEverySurface(minecraft);
    }

    /**
     * The floor all surface obeys.
     */
    private static boolean isSuppressedForEverySurface(@Nonnull Minecraft minecraft) {
        return minecraft.options.hideGui
                || minecraft.player == null
                || minecraft.level == null;
    }

}
