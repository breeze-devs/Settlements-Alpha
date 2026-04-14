package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.presentation.ui.framework.BaseElement;
import dev.breezes.settlements.presentation.ui.framework.Bounds;
import dev.breezes.settlements.presentation.ui.framework.Insets;
import dev.breezes.settlements.presentation.ui.framework.SizeConstraint;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import javax.annotation.Nonnull;

/**
 * A UI element that renders a hunger bar with label and percentage text.
 * <p>
 * Layout: {@code "Hunger: [===bar===] 75%"}
 * <p>
 * Label width and percentage width are cached and only recalculated when
 * {@link #setHunger(float)} is called (on snapshot update).
 */
@ClientSide
class HungerBarElement extends BaseElement {

    private static final String HUNGER_KEY = "ui.settlements.stats.hunger";
    private static final int BAR_BACKGROUND_COLOR = 0xFF333333;
    private static final int BAR_FILL_COLOR = 0xFF8B5A2B;
    private static final int LABEL_BAR_GAP = 3;
    private static final int BAR_Y_OFFSET = 1;
    private static final int BAR_HEIGHT = 7;

    private final UITheme theme;
    private final Component hungerLabel;
    private final int hungerLabelWidth;

    // Cached render state — updated only on snapshot change, not per frame
    private float currentHunger;
    private String hungerPercentText;
    private int hungerPercentWidth;

    HungerBarElement(@Nonnull UITheme theme, float initialHunger) {
        super(SizeConstraint.FILL, SizeConstraint.fixed(9), Insets.NONE);

        this.theme = theme;
        this.hungerLabel = Component.translatable(HUNGER_KEY);

        Font font = Minecraft.getInstance().font;
        this.hungerLabelWidth = font.width(this.hungerLabel);

        updateHungerCache(initialHunger, font);
    }

    void setHunger(float hunger) {
        if (this.currentHunger == hunger) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        updateHungerCache(hunger, font);
    }

    private void updateHungerCache(float hunger, @Nonnull Font font) {
        this.currentHunger = hunger;
        this.hungerPercentText = Math.round(hunger * 100) + "%";
        this.hungerPercentWidth = font.width(this.hungerPercentText);
    }

    @Override
    public void measure(int availableWidth, int availableHeight) {
        int w = resolveSize(widthConstraint, availableWidth, availableWidth);
        int h = resolveSize(heightConstraint, availableHeight, 9);
        setMeasuredSize(w, h);
    }

    @Override
    public void render(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        Bounds b = bounds();
        float ratio = Mth.clamp(currentHunger, 0, 1);

        // Hunger label on the left
        graphics.drawString(font, hungerLabel, b.x(), b.y(), theme.subtleTextColor(), false);

        // Percentage on the right
        int percentX = b.right() - hungerPercentWidth;
        graphics.drawString(font, hungerPercentText, percentX, b.y(), theme.textColor(), false);

        // Bar fills the space between label and percentage
        int barX = b.x() + hungerLabelWidth + LABEL_BAR_GAP;
        int barEndX = percentX - LABEL_BAR_GAP;
        int barWidth = barEndX - barX;
        if (barWidth > 2) {
            int barY = b.y() + BAR_Y_OFFSET;
            graphics.fill(barX, barY, barEndX, barY + BAR_HEIGHT, BAR_BACKGROUND_COLOR);
            int filledWidth = Math.round(barWidth * ratio);
            if (filledWidth > 0) {
                graphics.fill(barX, barY, barX + filledWidth, barY + BAR_HEIGHT, BAR_FILL_COLOR);
            }
        }
    }

}
