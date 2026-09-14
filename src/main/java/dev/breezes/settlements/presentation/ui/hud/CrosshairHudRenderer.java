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
 * Owns the HUD region below the crosshair and renders one provider's content at a time.
 */
@ClientSide
@ClientScope
public final class CrosshairHudRenderer implements LayeredDraw.Layer, ClientSessionResettable {

    /**
     * Vertical gap, in pixels, between the crosshair center and the headline below it.
     */
    private static final int CROSSHAIR_GAP = 22;

    /**
     * Extra pixels between lines to keep individual details visually separate.
     */
    private static final int LINE_LEADING = 3;

    private static final float FULLY_OPAQUE = 1.0F;

    /**
     * Text alpha floor; see platform standard P12.
     */
    private static final int MIN_HONORED_ALPHA_BYTE = 4;

    private final List<ModeHudProvider> modeProviders;
    private final List<LookTargetHudProvider> lookTargetProviders;
    private final List<HeldItemHudProvider> heldItemProviders;
    private final DwellRevealTracker lookTargetDwellTracker = new DwellRevealTracker();

    // Retained so content can fade out after the target is lost
    @Nullable
    private HudContent lastLookTargetContent;

    @Inject
    CrosshairHudRenderer(Set<ModeHudProvider> modeProviders,
                         Set<LookTargetHudProvider> lookTargetProviders,
                         Set<HeldItemHudProvider> heldItemProviders) {
        this.modeProviders = HudPrecedence.orderedByPriority(modeProviders);
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
        if (modeProviders.isEmpty() && lookTargetProviders.isEmpty() && heldItemProviders.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (ClientSurfaceSuppression.isHudSuppressed(minecraft)) {
            return;
        }

        // See platform standard P9 for ray-trace absence and miss handling
        HitResult.Type hitType = minecraft.hitResult == null ? HitResult.Type.MISS : minecraft.hitResult.getType();
        HudFrame frame = buildFrame(minecraft, deltaTracker, hitType);
        resolveContent(frame).ifPresent(resolved -> draw(guiGraphics, minecraft.font, resolved.content(), resolved.alpha()));
    }

    private static HudFrame buildFrame(@Nonnull Minecraft minecraft, @Nonnull DeltaTracker deltaTracker, @Nonnull HitResult.Type hitType) {
        return HudFrame.builder()
                .localPlayer(minecraft.player)
                .mainHandStack(minecraft.player.getMainHandItem())
                .blockHit(hitType == HitResult.Type.BLOCK ? (BlockHitResult) minecraft.hitResult : null)
                .aimedAtEntity(minecraft.crosshairPickEntity)
                .partialTick(deltaTracker.getGameTimeDeltaPartialTick(false))
                .build();
    }

    /**
     * Target identity scoped to a level so equal positions or entity IDs in different levels remain distinct.
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
            // Keep retained keys stable even if the hit position is mutable
            return blockHit != null ? new BlockTarget(level, blockHit.getBlockPos().immutable()) : null;
        }

        record BlockTarget(@Nonnull Level level, @Nonnull BlockPos pos) implements AimTargetKey {
        }

        record EntityTarget(@Nonnull Level level, int entityId) implements AimTargetKey {
        }

    }

    private record ResolvedHudContent(@Nonnull HudContent content, float alpha) {
    }

    /**
     * Resolves mode content first, then visible look-target content, then held-item content.
     */
    private Optional<ResolvedHudContent> resolveContent(@Nonnull HudFrame frame) {
        // Mode and held-item content need no dwell because selecting them is deliberate
        Optional<HudContent> mode = HudPrecedence.firstPresent(modeProviders, provider -> provider.contentFor(frame));
        if (mode.isPresent()) {
            // Keep the look-target fade advancing while mode content hides it
            lookTargetDwellTracker.observe(null);
            return Optional.of(new ResolvedHudContent(mode.get(), FULLY_OPAQUE));
        }

        Optional<HudContent> claimed = HudPrecedence.firstPresent(lookTargetProviders,
                provider -> provider.contentFor(frame));

        // Unclaimed targets must not renew the reveal and keep the previous target's content visible
        DwellReveal reveal = lookTargetDwellTracker.observe(claimed.isPresent() ? AimTargetKey.of(frame) : null);

        // A new target must earn its reveal before replacing content that is still fading
        if (reveal.revealed()) {
            claimed.ifPresent(content -> lastLookTargetContent = content);
        }

        // Retained content needs no separate expiry; the reveal controls whether it is visible
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
     * Scales a packed ARGB color's alpha by a fraction from zero to one, preserving RGB.
     * Clamps to {@link #MIN_HONORED_ALPHA_BYTE}; callers must skip drawing fully hidden content.
     */
    @VisibleForTesting
    static int withAlpha(int argb, float alphaFraction) {
        int scaled = Math.round(((argb >>> 24) & 0xFF) * alphaFraction);
        int alpha = Math.max(MIN_HONORED_ALPHA_BYTE, scaled);
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

}
