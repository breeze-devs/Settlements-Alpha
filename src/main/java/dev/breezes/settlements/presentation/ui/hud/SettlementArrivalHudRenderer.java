package dev.breezes.settlements.presentation.ui.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.breezes.settlements.bootstrap.registry.sounds.SoundEventRegistry;
import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementSummary;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.ClientBoundSettlementArrivalPacket;
import dev.breezes.settlements.infrastructure.network.features.arrival.packet.SettlementAnnouncementType;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Owner of the arrival region: the title at the top center, in the upper third and below any boss bars.
 */
@ClientSide
@ClientScope
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementArrivalHudRenderer implements LayeredDraw.Layer {

    private static final float NAME_SCALE = 2.2F;

    private static final String DISCOVERY_KEY = "ui.settlements.arrival.discovery";
    private static final String FOUNDED_KEY = "ui.settlements.arrival.founded";
    private static final String RENAMED_KEY = "ui.settlements.arrival.renamed";
    private static final String FORMERLY_KEY = "ui.settlements.arrival.formerly";
    private static final String LEAVING_KEY = "ui.settlements.arrival.leaving";
    private static final String DISBANDED_KEY = "ui.settlements.arrival.disbanded";

    /**
     * Where the title's top sits as a share of the screen height when no boss bar reaches lower.
     */
    private static final float TITLE_TOP_SHARE = 0.125F;
    private static final int BOSS_BAR_GAP = 4;
    private static final int TITLE_SIDE_MARGIN = 8;
    private static final int LINE_GAP = 5;

    /**
     * Extra rule length beyond each side of the rendered name, in GUI pixels.
     */
    private static final int RULE_OVERHANG = 24;

    /**
     * Fraction of the rule's width held at peak brightness.
     */
    private static final float RULE_BRIGHT_CENTER_SHARE = 0.4F;
    private static final int RULE_RGB = 0xFFFFFF;
    private static final int RULE_PEAK_ALPHA = 0xC0;
    private static final int RULE_HEIGHT = 2;

    /**
     * Display time the rule takes to grow from the center out to its full width.
     */
    private static final long RULE_GROW_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(1));

    /**
     * Display time a heading takes to slide into place from the right while fading in.
     */
    private static final long HEADING_SLIDE_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(0.4));

    /**
     * Distance right of its place a heading starts sliding from, in GUI pixels.
     */
    private static final float HEADING_SLIDE_DISTANCE = 12.0F;

    /**
     * Display time the line under the rule takes to float up into place while fading in.
     */
    private static final long DETAIL_FLOAT_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(0.5));

    /**
     * Distance below its place the line under the rule starts floating up from, in GUI pixels.
     */
    private static final float DETAIL_FLOAT_DISTANCE = 6.0F;

    /**
     * Offset of the discovery jingle's first piano note from the start of the sound, after its opening bell.
     */
    private static final long JINGLE_FIRST_NOTE_MILLIS = ClientMonotonicClock.millisIn(ClockTicks.seconds(0.35));

    private static final float UI_SOUND_PITCH = 1.0F;
    private static final float UI_SOUND_VOLUME = 0.3F;

    private final SettlementAnnouncementQueue queue;

    /**
     * The lowest point the boss bars drawn this frame reach, or zero when none has drawn.
     */
    private int bossBarsBottom;

    /**
     * Records the space a boss bar drawn this frame takes, for the title to sit below it.
     */
    public void onBossEventProgress(@Nonnull CustomizeGuiOverlayEvent.BossEventProgress event) {
        // The overlay steps down by the increment for the next bar and writes that bar's name one line above it, so
        // this bar's space ends where the next bar's name would start
        int bottom = event.getY() + event.getIncrement() - Minecraft.getInstance().font.lineHeight;
        this.bossBarsBottom = Math.max(this.bossBarsBottom, bottom);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, @Nonnull DeltaTracker deltaTracker) {
        // Measured afresh every frame, so a boss bar that has ended stops holding the title down
        int bossBarsBottom = this.bossBarsBottom;
        this.bossBarsBottom = 0;

        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isHudSuppressed(minecraft)) {
            return;
        }

        SettlementAnnouncementQueue.Showing title = this.queue.title(minecraft.level.dimension(), ClientMonotonicClock.nowMillis());
        if (title != null) {
            playOnFirstFrame(minecraft, title);
            drawTitle(guiGraphics, minecraft.font, title, bossBarsBottom);
        }
    }

    private static void playOnFirstFrame(@Nonnull Minecraft minecraft, @Nonnull SettlementAnnouncementQueue.Showing showing) {
        if (!showing.started()) {
            return;
        }

        SoundEvent sound = sound(showing.arrival().announcementType());
        if (sound != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, UI_SOUND_PITCH, UI_SOUND_VOLUME));
        }
    }

    @Nullable
    private static SoundEvent sound(@Nonnull SettlementAnnouncementType announcementType) {
        return switch (announcementType) {
            case DISCOVERY, FOUNDED -> SoundEventRegistry.SETTLEMENT_DISCOVERY.get();
            case RENAMED, ENTRY -> SoundEventRegistry.SETTLEMENT_ENTER.get();
            case EXIT, DISBANDED -> null;
        };
    }

    private static void drawTitle(@Nonnull GuiGraphics guiGraphics,
                                  @Nonnull Font font,
                                  @Nonnull SettlementAnnouncementQueue.Showing title,
                                  int bossBarsBottom) {
        ClientBoundSettlementArrivalPacket arrival = title.arrival();
        SettlementAnnouncementType announcementType = arrival.announcementType();
        // Leaving and ruin are dimmer than every other title
        boolean dim = announcementType == SettlementAnnouncementType.EXIT
                || announcementType == SettlementAnnouncementType.DISBANDED;
        int baseColor = dim ? UITheme.DEFAULT.subtleTextColor() : UITheme.DEFAULT.textColor();
        int textColor = HudTextColor.withAlpha(baseColor, title.opacity());
        float centerX = guiGraphics.guiWidth() / 2.0F;
        int maxWidth = guiGraphics.guiWidth() - TITLE_SIDE_MARGIN * 2;
        float top = Math.max(guiGraphics.guiHeight() * TITLE_TOP_SHARE, bossBarsBottom + BOSS_BAR_GAP);

        // Held back until the jingle reaches its melody, so the heading and the line under the rule arrive with the
        // first note
        long entranceDelayMillis = sound(announcementType) == SoundEventRegistry.SETTLEMENT_DISCOVERY.get()
                ? JINGLE_FIRST_NOTE_MILLIS
                : 0L;
        long entranceMillis = title.elapsedMillis() - entranceDelayMillis;

        Component heading = heading(announcementType);
        float headingProgress = progress(entranceMillis, HEADING_SLIDE_MILLIS);
        if (heading != null && headingProgress > 0.0F) {
            float slide = HEADING_SLIDE_DISTANCE * (1.0F - easeOut(headingProgress));
            int headingColor = HudTextColor.withAlpha(baseColor, title.opacity() * headingProgress);
            drawCentered(guiGraphics, font, heading, centerX + slide, top, 1.0F, maxWidth, headingColor);
        }
        // A title without a heading leaves its line empty, so the name never jumps when one title takes another's place
        top += font.lineHeight + LINE_GAP;

        Component name = Component.literal(arrival.name());
        int nameWidth = font.width(name);
        float nameScale = fittedScale(nameWidth, NAME_SCALE, maxWidth);
        top = drawCentered(guiGraphics, font, name, centerX, top, nameScale, maxWidth, textColor) + LINE_GAP;

        float ruleFullHalfWidth = Math.min(maxWidth, Math.round(nameWidth * nameScale) + RULE_OVERHANG * 2) / 2.0F;
        float ruleHalfWidth = ruleFullHalfWidth * easeOut(progress(title.elapsedMillis(), RULE_GROW_MILLIS));
        int ruleRgb = dim ? UITheme.DEFAULT.subtleTextColor() & 0x00FFFFFF : RULE_RGB;
        drawRule(guiGraphics, centerX - ruleHalfWidth, centerX + ruleHalfWidth, Math.round(top), ruleRgb,
                title.opacity());
        top += RULE_HEIGHT + LINE_GAP;

        Component detail = detail(arrival);
        float detailProgress = progress(entranceMillis, DETAIL_FLOAT_MILLIS);
        if (detail != null && detailProgress > 0.0F) {
            float rise = DETAIL_FLOAT_DISTANCE * (1.0F - easeOut(detailProgress));
            int detailColor = HudTextColor.withAlpha(baseColor, title.opacity() * detailProgress);
            drawCentered(guiGraphics, font, detail, centerX, top + rise, 1.0F, maxWidth, detailColor);
        }
    }

    @Nullable
    private static Component heading(@Nonnull SettlementAnnouncementType announcementType) {
        return switch (announcementType) {
            case DISCOVERY -> Component.translatable(DISCOVERY_KEY).withStyle(ChatFormatting.GREEN);
            case FOUNDED -> Component.translatable(FOUNDED_KEY).withStyle(ChatFormatting.GREEN);
            case RENAMED -> Component.translatable(RENAMED_KEY);
            case EXIT -> Component.translatable(LEAVING_KEY);
            case ENTRY, DISBANDED -> null;
        };
    }

    @Nullable
    private static Component detail(@Nonnull ClientBoundSettlementArrivalPacket arrival) {
        SettlementSummary.Fading status = arrival.status();
        return switch (arrival.announcementType()) {
            case DISCOVERY, ENTRY -> status == null ? null : SettlementStatusLine.of(status);
            case RENAMED -> Component.translatable(FORMERLY_KEY, arrival.previousName())
                    .withColor(UITheme.DEFAULT.subtleTextColor());
            case DISBANDED -> Component.translatable(DISBANDED_KEY).withColor(UITheme.DEFAULT.errorColor());
            // A settlement just founded is healthy, and its founding is the news
            case FOUNDED -> null;
            // The settlement is behind the player, so how it fares is no longer news
            case EXIT -> null;
        };
    }

    /**
     * Draws a rule in the given color with a flat bright center and linear fades toward both ends, scaled by opacity.
     */
    private static void drawRule(@Nonnull GuiGraphics guiGraphics,
                                 float left, float right,
                                 int top,
                                 int rgb,
                                 float opacity) {
        float centerX = (left + right) / 2.0F;
        float brightHalfWidth = (right - left) / 2.0F * RULE_BRIGHT_CENTER_SHARE;
        int bright = (Math.round(RULE_PEAK_ALPHA * opacity) << 24) | rgb;
        int clear = rgb;

        Matrix4f pose = guiGraphics.pose().last().pose();
        VertexConsumer consumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        addHorizontalGradient(consumer, pose, left, centerX - brightHalfWidth, top, clear, bright);
        addHorizontalGradient(consumer, pose, centerX - brightHalfWidth, centerX + brightHalfWidth, top, bright, bright);
        addHorizontalGradient(consumer, pose, centerX + brightHalfWidth, right, top, bright, clear);
        // GuiGraphics' own fills flush after every quad; submitting these three together costs one flush
        guiGraphics.flush();
    }

    /**
     * Adds a quad one rule tall whose color blends from leftColor at its left edge to rightColor at its right edge.
     */
    private static void addHorizontalGradient(@Nonnull VertexConsumer consumer,
                                              @Nonnull Matrix4f pose,
                                              float left, float right,
                                              int top,
                                              int leftColor, int rightColor) {
        int bottom = top + RULE_HEIGHT;
        consumer.addVertex(pose, left, top, 0.0F).setColor(leftColor);
        consumer.addVertex(pose, left, bottom, 0.0F).setColor(leftColor);
        consumer.addVertex(pose, right, bottom, 0.0F).setColor(rightColor);
        consumer.addVertex(pose, right, top, 0.0F).setColor(rightColor);
    }

    /**
     * Returns how far an animation of the given duration has come after elapsedMillis: zero until it starts, and one
     * once it has ended.
     */
    private static float progress(long elapsedMillis, long durationMillis) {
        return Mth.clamp((float) elapsedMillis / durationMillis, 0.0F, 1.0F);
    }

    /**
     * Eases linear progress out, so motion starts fast and settles gently into place.
     */
    private static float easeOut(float progress) {
        float remaining = 1.0F - progress;
        return 1.0F - remaining * remaining * remaining;
    }

    /**
     * Draws a line centered on centerX, at the given scale or smaller so that it fits maxWidth.
     *
     * @return the top of the line below it
     */
    private static float drawCentered(@Nonnull GuiGraphics guiGraphics,
                                      @Nonnull Font font,
                                      @Nonnull Component line,
                                      float centerX,
                                      float top,
                                      float scale,
                                      int maxWidth,
                                      int color) {
        int width = font.width(line);
        float fitted = fittedScale(width, scale, maxWidth);
        drawScaled(guiGraphics, font, line, centerX - width * fitted / 2.0F, top, fitted, color);
        return top + font.lineHeight * fitted;
    }

    /**
     * The scale nearest to scale at which a line of the given width fits maxWidth and each pixel of Minecraft's bitmap
     * font covers a whole number of screen pixels, so that its glyphs stay sharp at every GUI scale.
     */
    private static float fittedScale(int width, float scale, int maxWidth) {
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        double aligned = Math.max(1, Math.round(scale * guiScale)) / guiScale;
        if (width * aligned <= maxWidth) {
            return (float) aligned;
        }

        double fitted = Math.floor((double) maxWidth / width * guiScale) / guiScale;
        // A line too long for one screen pixel per glyph pixel keeps its exact fit, blurred rather than cut off
        return fitted > 0 ? (float) fitted : (float) maxWidth / width;
    }

    private static void drawScaled(@Nonnull GuiGraphics guiGraphics,
                                   @Nonnull Font font,
                                   @Nonnull Component line,
                                   float left,
                                   float top,
                                   float scale,
                                   int color) {
        // Whole screen pixels rather than whole GUI pixels: the fitted scale already makes every glyph pixel span whole
        // screen pixels, so the glyphs stay sharp, while a line sliding into place moves one screen pixel at a time
        double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        float snappedLeft = (float) (Math.round(left * guiScale) / guiScale);
        float snappedTop = (float) (Math.round(top * guiScale) / guiScale);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(snappedLeft, snappedTop, 0.0F);
        pose.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, line, 0, 0, color, true);
        pose.popPose();
    }

}
