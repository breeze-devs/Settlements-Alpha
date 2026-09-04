package dev.breezes.settlements.infrastructure.rendering.debug.tuning;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.presentation.ui.ClientSurfaceSuppression;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;

import javax.annotation.Nonnull;

/**
 * Paints the debug tuning board as a corner panel: every group, every knob, and which row the keys will act on.
 */
@ClientSide
@ClientScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DebugTuningHudRenderer implements LayeredDraw.Layer {

    private static final String LEGEND = "Tuning  ↑↓ row  ←→ adjust (shift=x10)  R reset (shift=all)  P print";

    private static final int MARGIN = 4;
    private static final int PADDING = 4;
    private static final int LINE_LEADING = 2;

    private static final String SELECTED_PREFIX = "> ";
    private static final String UNSELECTED_PREFIX = "  ";

    private final DebugTuningBoard board;

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, @Nonnull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isHudSuppressed(minecraft) || !this.board.isVisible()) {
            return;
        }

        Font font = minecraft.font;
        int lineHeight = font.lineHeight + LINE_LEADING;
        int rowCount = this.rowCount();
        int panelWidth = this.widestRow(font) + PADDING * 2;
        int panelHeight = rowCount * lineHeight + PADDING * 2;

        guiGraphics.fill(MARGIN, MARGIN, MARGIN + panelWidth, MARGIN + panelHeight,
                UITheme.DEFAULT.overlayColor());

        int x = MARGIN + PADDING;
        int y = MARGIN + PADDING;
        guiGraphics.drawString(font, LEGEND, x, y, UITheme.DEFAULT.subtleTextColor());
        y += lineHeight;

        for (DebugTuningGroup group : this.board.groups()) {
            y += lineHeight;
            guiGraphics.drawString(font, "[" + group.getName() + "]", x, y, UITheme.DEFAULT.warningColor());
            for (DebugTuningKnob knob : group.getKnobs()) {
                y += lineHeight;
                guiGraphics.drawString(font, rowText(knob, this.board.isSelected(knob)), x, y, rowColor(knob));
            }
        }
    }

    private static String rowText(@Nonnull DebugTuningKnob knob, boolean selected) {
        return (selected ? SELECTED_PREFIX : UNSELECTED_PREFIX)
                + knob.getName() + " = " + DebugTuningBoard.format(knob.getValue());
    }

    /**
     * A moved knob is colored apart from an untouched one.
     */
    private static int rowColor(@Nonnull DebugTuningKnob knob) {
        return knob.isMoved() ? UITheme.DEFAULT.successColor() : UITheme.DEFAULT.textColor();
    }

    /**
     * Row count including the legend and each group's heading.
     */
    private int rowCount() {
        int groupCount = this.board.groups().size();
        int knobCount = this.board.groups().stream().mapToInt(group -> group.getKnobs().size()).sum();
        return 1 + groupCount * 2 + knobCount;
    }

    private int widestRow(@Nonnull Font font) {
        int widest = font.width(LEGEND);
        for (DebugTuningGroup group : this.board.groups()) {
            widest = Math.max(widest, font.width("[" + group.getName() + "]"));
            for (DebugTuningKnob knob : group.getKnobs()) {
                widest = Math.max(widest, font.width(rowText(knob, true)));
            }
        }
        return widest;
    }

}
