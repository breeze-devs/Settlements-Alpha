package dev.breezes.settlements.infrastructure.network.features.ui.stats.handler;

import dev.breezes.settlements.presentation.ui.stats.VillagerStatsScreen;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.function.Consumer;

@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
final class VillagerStatsSnapshotClientDispatcher {

    private final UiClientState uiClientState;

    void apply(long sessionId,
               @Nonnull String snapshotName,
               @Nonnull Consumer<VillagerStatsScreen> screenApplicator) {
        if (!this.uiClientState.recordSnapshotReceived(sessionId)) {
            log.debug("Ignoring stale {} sessionId={}", snapshotName, sessionId);
            return;
        }

        Screen activeScreen = Minecraft.getInstance().screen;
        if (!(activeScreen instanceof VillagerStatsScreen statsScreen)) {
            log.debug("Cannot apply {} because client does not have a villager stats screen", snapshotName);
            return;
        }

        if (statsScreen.getSessionId() != sessionId) {
            log.debug("Cannot apply {} because session ID mismatch: expected {}, got {}",
                    snapshotName, sessionId, statsScreen.getSessionId());
            return;
        }

        screenApplicator.accept(statsScreen);
    }

}
