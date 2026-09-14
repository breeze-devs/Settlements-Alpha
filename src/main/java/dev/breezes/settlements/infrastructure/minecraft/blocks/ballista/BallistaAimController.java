package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.infrastructure.network.features.ballista.packet.ServerBoundBallistaAimPacket;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Captures the player's input events related to aiming and sends them to the server.
 * Session ends when the player exits the aim mode.
 */
@ClientSide
@ClientScope
public final class BallistaAimController implements ClientSessionResettable {

    private final BallistaAimGuideRenderer guideRenderer;
    private final BallistaAimMode<AimTarget> mode = new BallistaAimMode<>();

    @Inject
    BallistaAimController(@Nonnull BallistaAimGuideRenderer guideRenderer) {
        this.guideRenderer = guideRenderer;
    }

    @Override
    public void onClientSessionEnded() {
        this.mode.end();
    }

    /**
     * Claims the interaction event consumed by aim mode before vanilla dispatches it.
     */
    public void onInteractionKeyMappingTriggered(@Nonnull InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            if (this.mode.shouldSuppressOffHandUse()) {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        AimTarget target = player.isSpectator() ? null : targetBallista(minecraft, minecraft.level);
        BallistaAimMode.UseResponse response = this.mode.mainHandUse(target, player.getMainHandItem().isEmpty(),
                minecraft.options.keyShift.isDown());
        if (response == BallistaAimMode.UseResponse.PASS_THROUGH) {
            return;
        }

        event.setCanceled(true);
        event.setSwingHand(response != BallistaAimMode.UseResponse.HELD);
    }

    /**
     * While aiming, the scroll wheel turns the machine and never reaches the hotbar.
     */
    public void onMouseScrolling(@Nonnull InputEvent.MouseScrollingEvent event) {
        AimTarget target = this.mode.aimed();
        if (target == null) {
            return;
        }

        // Cancel the scroll event so hotbar doesn't move
        event.setCanceled(true);

        BallistaBlockEntity ballista = this.aimedBallista();
        if (ballista == null) {
            return;
        }

        BallistaAim aim = ballista.getAim();
        BallistaAim adjusted = this.mode.scrolled(aim, event.getScrollDeltaY());
        if (adjusted == aim) {
            return;
        }

        ballista.predictIntent(adjusted.getIntentYaw(), adjusted.getIntentPitch());
        PacketDistributor.sendToServer(new ServerBoundBallistaAimPacket(target.pos(), adjusted.getIntentYaw(), adjusted.getIntentPitch()));

        this.guideRenderer.adjusted(target.level(), target.pos());
    }

    public void onKey(@Nonnull InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.onPress(minecraft, mapping -> mapping.matches(event.getKey(), event.getScanCode()));
    }

    public void onMouseButton(@Nonnull InputEvent.MouseButton.Post event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        this.onPress(minecraft, mapping -> mapping.matchesMouse(event.getButton()));
    }

    private void onPress(@Nonnull Minecraft minecraft, @Nonnull Predicate<KeyMapping> pressed) {
        // A press within a screen is ignored
        if (minecraft.screen != null) {
            return;
        }

        if (pressed.test(minecraft.options.keyUse)) {
            this.mode.usePressed();
        }
        if (pressed.test(minecraft.options.keyShift)) {
            this.mode.sneakPressed();
        }
    }

    /**
     * Ends aim mode once the player can no longer operate the aimed machine.
     */
    public void onClientTick() {
        AimTarget target = this.mode.aimed();
        if (target == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null
                || minecraft.level != target.level()
                || minecraft.screen != null
                || !player.isAlive()
                || player.isSpectator()
                || !(target.level().getBlockEntity(target.pos()) instanceof BallistaBlockEntity)
                || BallistaBlockEntity.isOutsideOperatingRange(player.getEyePosition(), target.pos())) {
            this.mode.end();
        }
    }

    /**
     * The aimed ballista as this client holds it, or null when not aiming.
     */
    @Nullable
    public BallistaBlockEntity aimedBallista() {
        AimTarget target = this.mode.aimed();
        if (target == null || Minecraft.getInstance().level != target.level()) {
            return null;
        }

        return target.level().getBlockEntity(target.pos()) instanceof BallistaBlockEntity ballista ? ballista : null;
    }

    public BallistaAimMode.Axis selectedAxis() {
        return this.mode.axis();
    }

    /**
     * The ballista under the crosshair.
     */
    @Nullable
    private static AimTarget targetBallista(@Nonnull Minecraft minecraft, @Nonnull Level level) {
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        BlockPos pos = ((BlockHitResult) hit).getBlockPos().immutable();
        return level.getBlockEntity(pos) instanceof BallistaBlockEntity ? new AimTarget(level, pos) : null;
    }

    /**
     * A machine by its level and position.
     */
    private record AimTarget(@Nonnull Level level, @Nonnull BlockPos pos) {
    }

}
