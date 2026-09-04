package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;
import dev.breezes.settlements.domain.time.CivilTime;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.data.schedule.VillageDayTypeClientProjection;
import dev.breezes.settlements.presentation.ui.ClientSurfaceSuppression;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.util.ClientMonotonicClock;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Owner of the screen's top-right corner.
 */
@ClientSide
@ClientScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ClockHudRenderer implements LayeredDraw.Layer, ClientSessionResettable {

    /**
     * How often the painted lines are rebuilt. The finest reading on the panel is one in-game minute, which
     * lasts longer than this, so no minute can pass unshown while every frame in between reuses the work.
     */
    private static final ClockTicks REBUILD_INTERVAL = ClockTicks.seconds(0.5);

    private static final int MARGIN = 4;
    private static final int PADDING = 4;
    private static final int LINE_LEADING = 2;

    private static final String DAY_KEY = "ui.settlements.clock_hud.day";
    private static final String WORK_DAY_KEY = "ui.settlements.clock_hud.day_type.work";
    private static final String REST_DAY_KEY = "ui.settlements.clock_hud.day_type.rest";
    private static final String UNKNOWN_DAY_KEY = "ui.settlements.clock_hud.day_type.unknown";

    private final VillageDayTypeClientProjection dayTypeProjection;

    @Nullable
    private List<PanelLine> lines;
    private long nextRebuildAtMillis;

    @Override
    public void onClientSessionEnded() {
        this.lines = null;
        this.nextRebuildAtMillis = 0L;
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, @Nonnull DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isHudSuppressed(minecraft) || !isHoldingClock(minecraft.player)) {
            return;
        }

        this.rebuildIfDue(minecraft.level);
        if (this.lines != null) {
            draw(guiGraphics, minecraft.font, this.lines);
        }
    }

    private static boolean isHoldingClock(@Nonnull LocalPlayer player) {
        return player.getMainHandItem().is(Items.CLOCK) || player.getOffhandItem().is(Items.CLOCK);
    }

    private void rebuildIfDue(@Nonnull Level level) {
        long now = ClientMonotonicClock.nowMillis();
        if (this.lines != null && now < this.nextRebuildAtMillis) {
            return;
        }

        this.nextRebuildAtMillis = ClientMonotonicClock.deadlineFrom(REBUILD_INTERVAL);
        this.lines = this.buildLines(level);
    }

    private List<PanelLine> buildLines(@Nonnull Level level) {
        ClockHudReadout readout = ClockHudReadout.from(level.getDayTime());
        PlanDayType dayType = this.dayTypeProjection.dayTypeFor(readout.calendarDay()).orElse(null);
        String dayTypeKey = dayType == null ? UNKNOWN_DAY_KEY : switch (dayType) {
            case WORK_DAY -> WORK_DAY_KEY;
            case REST_DAY -> REST_DAY_KEY;
        };

        return List.of(
                new PanelLine(Component.literal(CivilTime.formatClock(readout.civilTick()))
                        .withStyle(ChatFormatting.BOLD), UITheme.DEFAULT.textColor()),
                new PanelLine(Component.translatable(DAY_KEY, readout.calendarDay()), UITheme.DEFAULT.subtleTextColor()),
                new PanelLine(Component.translatable(dayTypeKey), UITheme.DEFAULT.textColor()));
    }

    private static void draw(@Nonnull GuiGraphics guiGraphics, @Nonnull Font font, @Nonnull List<PanelLine> lines) {
        int lineHeight = font.lineHeight + LINE_LEADING;
        int contentWidth = lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(0);

        int panelRight = guiGraphics.guiWidth() - MARGIN;
        int panelLeft = panelRight - (contentWidth + PADDING * 2);
        int panelBottom = MARGIN + lines.size() * lineHeight - LINE_LEADING + PADDING * 2;
        guiGraphics.fill(panelLeft, MARGIN, panelRight, panelBottom, UITheme.DEFAULT.backgroundDimColor());

        int textRight = panelRight - PADDING;
        int y = MARGIN + PADDING;
        for (PanelLine line : lines) {
            guiGraphics.drawString(font, line.text(), textRight - font.width(line.text()), y, line.color());
            y += lineHeight;
        }
    }

    private record PanelLine(@Nonnull Component text, int color) {
    }

}
