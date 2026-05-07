package dev.breezes.settlements.application.ui.sync;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

@ClientSide
public interface UiScreenOpener {

    void openScreen(long sessionId, int villagerEntityId);

}
