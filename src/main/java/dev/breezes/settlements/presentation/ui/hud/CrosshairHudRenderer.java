package dev.breezes.settlements.presentation.ui.hud;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.presentation.ui.ClientSurfaceSuppression;
import dev.breezes.settlements.presentation.ui.framework.UITheme;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.stylistic.VisibleForTesting;
import dev.breezes.settlements.shared.util.DwellReveal;
import dev.breezes.settlements.shared.util.DwellRevealTracker;
import jakarta.inject.Inject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The single owner of the region below the crosshair, registered as its only {@link LayeredDraw.Layer}:
 * builds one {@link HudFrame} per frame, resolves it against every {@link LookTargetHudProvider} and
 * {@link HeldItemHudProvider}, and paints whichever content wins. A surface that wants the region adds a
 * provider rather than claiming a layer slot of its own.
 */
@ClientSide
@ClientScope
public final class CrosshairHudRenderer implements LayeredDraw.Layer, ClientSessionResettable {

    /**
     * Vertical gap, in pixels, between the crosshair center and the headline below it.
     */
    private static final int CROSSHAIR_GAP = 22;

    /**
     * Extra pixels between lines, on top of the font's own line height. That height alone leaves no gap,
     * packing the lines into one block a player reads as a paragraph rather than as separate facts.
     */
    private static final int LINE_LEADING = 3;

    private static final float FULLY_OPAQUE = 1.0F;

    /**
     * The lowest alpha byte the font renderer honors; anything under it is drawn fully opaque instead.
     * See the platform standard, P12 — the tail of a fade is the frame that trips it.
     */
    private static final int MIN_HONORED_ALPHA_BYTE = 4;

    private final List<LookTargetHudProvider> lookTargetProviders;
    private final List<HeldItemHudProvider> heldItemProviders;
    private final DwellRevealTracker lookTargetDwellTracker = new DwellRevealTracker();

    // Retained so a fading frame still has content to paint once the target itself is gone
    @Nullable
    private HudContent lastLookTargetContent;

    @Inject
    CrosshairHudRenderer(Set<LookTargetHudProvider> lookTargetProviders, Set<HeldItemHudProvider> heldItemProviders) {
        this.lookTargetProviders = HudPrecedence.orderedByPriority(lookTargetProviders);
        this.heldItemProviders = HudPrecedence.orderedByPriority(heldItemProviders);
    }

    @Override
    public void onClientSessionEnded() {
        lookTargetDwellTracker.reset();
        lastLookTargetContent = null;
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, @Nonnull DeltaTracker deltaTracker) {
        if (lookTargetProviders.isEmpty() && heldItemProviders.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isHudSuppressed(minecraft)) {
            return;
        }

        // hitResult is only assigned during a tick, so it is still null on the frames between a level
        // loading and its first tick. That is the same "no target" the layer already handles as a miss.
        HitResult.Type hitType = minecraft.hitResult == null ? HitResult.Type.MISS : minecraft.hitResult.getType();
        HudFrame frame = buildFrame(minecraft, deltaTracker, hitType);
        resolveContent(frame).ifPresent(resolved -> draw(guiGraphics, minecraft.font, resolved.content(), resolved.alpha()));
    }

    private static HudFrame buildFrame(@Nonnull Minecraft minecraft, @Nonnull DeltaTracker deltaTracker, @Nonnull HitResult.Type hitType) {
        return HudFrame.builder()
                .localPlayer(minecraft.player)
                .mainHandStack(minecraft.player.getMainHandItem())
                .blockHit(isBlockHit(hitType) ? (BlockHitResult) minecraft.hitResult : null)
                .aimedAtEntity(minecraft.crosshairPickEntity)
                .partialTick(deltaTracker.getGameTimeDeltaPartialTick(false))
                .build();
    }

    /**
     * Whether the hit type names an actual block rather than a miss. A miss is reported as a
     * BlockHitResult too, so an instanceof check alone cannot tell the two apart.
     */
    @VisibleForTesting
    static boolean isBlockHit(@Nonnull HitResult.Type hitType) {
        return hitType == HitResult.Type.BLOCK;
    }

    /**
     * Identifies what the crosshair is aimed at this frame, or null for nothing. Level identity is part of
     * the key because dimension travel can put an unrelated target at the same position or entity id. The
     * position is copied because the key outlives its frame, and a block ray trace may hand back a cursor
     * its next call moves.
     */
    private sealed interface AimTargetKey permits AimTargetKey.BlockTarget, AimTargetKey.EntityTarget {

        @Nullable
        static AimTargetKey of(@Nonnull HudFrame frame) {
            Level level = frame.localPlayer().level();
            Entity aimedAtEntity = frame.aimedAtEntity();
            if (aimedAtEntity != null) {
                return new EntityTarget(level, aimedAtEntity.getId());
            }

            BlockHitResult blockHit = frame.blockHit();
            return blockHit != null ? new BlockTarget(level, blockHit.getBlockPos().immutable()) : null;
        }

        record BlockTarget(@Nonnull Level level, @Nonnull BlockPos pos) implements AimTargetKey {
        }

        record EntityTarget(@Nonnull Level level, int entityId) implements AimTargetKey {
        }

    }

    /**
     * The content to paint together with the opacity to paint it at.
     */
    private record ResolvedHudContent(@Nonnull HudContent content, float alpha) {
    }

    /**
     * The look-target layer wins whenever it has something to say; held-item content is the fallback.
     * <p>
     * Held-item content has no dwell of its own, because holding an item is already a deliberate act where
     * a crosshair sweeping across candidates is not.
     */
    private Optional<ResolvedHudContent> resolveContent(@Nonnull HudFrame frame) {
        Optional<HudContent> claimed = HudPrecedence.firstPresent(lookTargetProviders,
                provider -> provider.contentFor(frame));

        // Only a claimed target is offered to the dwell. Offering whatever the crosshair rests on lets a
        // patch of dirt serve its own dwell and win the reveal away from the lily being read, cutting that
        // content off mid-fade because the dirt has nothing to replace it with.
        DwellReveal reveal = lookTargetDwellTracker.observe(claimed.isPresent() ? AimTargetKey.of(frame) : null);

        // A claim alone must not replace what is retained, or a lily the crosshair merely crossed would
        // paint at the opacity the previous one built up, arriving near-full without serving a dwell.
        if (reveal.revealed()) {
            claimed.ifPresent(content -> lastLookTargetContent = content);
        }

        // Content that has finished fading is left retained rather than cleared: alpha alone decides
        // whether it draws, and the next earned reveal overwrites it.
        if (lastLookTargetContent != null && reveal.paints()) {
            return Optional.of(new ResolvedHudContent(lastLookTargetContent, reveal.alpha()));
        }

        return HudPrecedence.firstPresent(heldItemProviders, provider -> provider.contentFor(frame))
                .map(content -> new ResolvedHudContent(content, FULLY_OPAQUE));
    }

    private void draw(@Nonnull GuiGraphics guiGraphics, @Nonnull Font font, @Nonnull HudContent content, float alpha) {
        int centerX = guiGraphics.guiWidth() / 2;
        int y = guiGraphics.guiHeight() / 2 + CROSSHAIR_GAP;

        guiGraphics.drawCenteredString(font, content.headline().copy().withStyle(ChatFormatting.BOLD), centerX, y,
                withAlpha(content.accentColor(), alpha));

        for (Component detail : content.details()) {
            y += font.lineHeight + LINE_LEADING;
            guiGraphics.drawCenteredString(font, detail, centerX, y, withAlpha(UITheme.DEFAULT.subtleTextColor(), alpha));
        }
    }

    /**
     * Scales a packed ARGB color's alpha channel by the given fraction, leaving RGB untouched. Never
     * returns an alpha the font renderer would misread; see {@link #MIN_HONORED_ALPHA_BYTE}.
     */
    @VisibleForTesting
    static int withAlpha(int argb, float alphaFraction) {
        int scaled = Math.round(((argb >>> 24) & 0xFF) * alphaFraction);
        int alpha = Math.max(MIN_HONORED_ALPHA_BYTE, scaled);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

}
