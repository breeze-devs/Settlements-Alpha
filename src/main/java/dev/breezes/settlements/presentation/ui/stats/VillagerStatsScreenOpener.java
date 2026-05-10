package dev.breezes.settlements.presentation.ui.stats;

import dev.breezes.settlements.application.ui.sync.UiScreenOpener;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;

import javax.inject.Inject;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerStatsScreenOpener implements UiScreenOpener {

    private final UiClientState uiClientState;

    @Override
    public void openScreen(long sessionId, int villagerEntityId) {
        Minecraft.getInstance().setScreen(new VillagerStatsScreen(sessionId, this.uiClientState, villagerEntityId));
    }

}
