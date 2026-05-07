package dev.breezes.settlements.application.ui.sync;

import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UiClientChannelDefinition {

    private final UiChannel channel;
    private final UiScreenOpener screenOpener;

}
