package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.ClientComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.infrastructure.rendering.debug.DevTooling;
import dev.breezes.settlements.infrastructure.rendering.debug.tuning.DebugTuningBoard;
import dev.breezes.settlements.presentation.ui.keybindings.SettlementsKeyMappings;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Drives the debug tuning board from the keyboard while its overlay is open. Only the overlay toggle is a bound
 * key mapping; the adjustment keys are read raw and are live only while the overlay is showing.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DebugTuningClientGameEvents {

    /**
     * Multiplier applied while shift is held, so one board can cross both the range where a value is being
     * searched for and the range where it is being settled.
     */
    private static final int COARSE_STEPS = 10;

    private static final int FINE_STEPS = 1;

    @SubscribeEvent
    public static void onClientTick(@Nonnull ClientTickEvent.Post event) {
        DebugTuningBoard board = debugTuningBoard();
        if (board == null) {
            return;
        }

        while (SettlementsKeyMappings.TOGGLE_DEBUG_TUNING_OVERLAY.consumeClick()) {
            board.toggleVisible();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(@Nonnull InputEvent.Key event) {
        DebugTuningBoard board = debugTuningBoard();
        if (board == null || !board.isVisible()) {
            return;
        }

        if (event.getAction() != GLFW.GLFW_PRESS && event.getAction() != GLFW.GLFW_REPEAT) {
            return;
        }

        boolean coarse = (event.getModifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        int steps = coarse ? COARSE_STEPS : FINE_STEPS;

        switch (event.getKey()) {
            case GLFW.GLFW_KEY_UP -> board.moveSelection(-1);
            case GLFW.GLFW_KEY_DOWN -> board.moveSelection(1);
            case GLFW.GLFW_KEY_LEFT -> board.nudgeSelected(-steps);
            case GLFW.GLFW_KEY_RIGHT -> board.nudgeSelected(steps);
            case GLFW.GLFW_KEY_R -> resetKnobs(board, coarse);
            case GLFW.GLFW_KEY_P -> printSnapshot(board);
            default -> {
                // Every other key belongs to whatever else is handling it
            }
        }
    }

    private static void resetKnobs(@Nonnull DebugTuningBoard board, boolean all) {
        if (all) {
            board.resetAll();
            return;
        }
        board.resetSelected();
    }

    private static void printSnapshot(@Nonnull DebugTuningBoard board) {
        Minecraft minecraft = Minecraft.getInstance();
        board.snapshotLines()
                .forEach(line -> minecraft.gui.getChat().addMessage(Component.literal(line)));
    }

    /**
     * The board, or null when the debug tuning facility is not installed in this build or the client graph has
     * not been built yet.
     */
    @Nullable
    private static DebugTuningBoard debugTuningBoard() {
        if (!DevTooling.isEnabled()) {
            return null;
        }

        ClientComponent clientComponent = SettlementsDagger.clientOrNull();
        return clientComponent == null ? null : clientComponent.debugTuningBoard();
    }

}
