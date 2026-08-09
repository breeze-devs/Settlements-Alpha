package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

/**
 * Renders whatever the first contributing {@link HeldItemHudProvider} declares for the player's currently
 * held item, centered below the crosshair.
 */
@ClientSide
@ClientScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class HeldItemHudRenderer implements LayeredDraw.Layer {

    /**
     * Vertical gap, in pixels, between the crosshair center and the headline below it.
     */
    private static final int CROSSHAIR_GAP = 22;

    private final Set<HeldItemHudProvider> providers;

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, @Nonnull DeltaTracker deltaTracker) {
        if (providers.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        // Suppress GUI for non-relevant scenarios
        if (minecraft.options.hideGui
                || minecraft.screen != null
                || minecraft.player == null
                || minecraft.level == null
                || minecraft.player.isSpectator()) {
            return;
        }

        resolveContent(minecraft.player).ifPresent(content -> draw(guiGraphics, minecraft.font, content));
    }

    private Optional<HeldItemHudContent> resolveContent(@Nonnull Player player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return Optional.empty();
        }

        for (HeldItemHudProvider provider : providers) {
            Optional<HeldItemHudContent> content = provider.contentFor(stack);
            if (content.isPresent()) {
                return content;
            }
        }

        return Optional.empty();
    }

    private void draw(@Nonnull GuiGraphics guiGraphics, @Nonnull Font font, @Nonnull HeldItemHudContent content) {
        int centerX = guiGraphics.guiWidth() / 2;
        int y = guiGraphics.guiHeight() / 2 + CROSSHAIR_GAP;

        guiGraphics.drawCenteredString(font, content.headline().copy().withStyle(ChatFormatting.BOLD), centerX, y, content.accentColor());

        for (Component detail : content.details()) {
            y += font.lineHeight;
            guiGraphics.drawCenteredString(font, detail, centerX, y, UITheme.DEFAULT.subtleTextColor());
        }
    }

}
