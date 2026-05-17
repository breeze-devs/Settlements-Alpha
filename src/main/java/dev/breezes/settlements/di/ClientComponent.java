package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientAttachmentModule;
import dev.breezes.settlements.di.modules.client.ClientNetworkModule;
import dev.breezes.settlements.di.modules.client.UiSyncClientModule;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.SlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketReceiver;
import dev.breezes.settlements.infrastructure.rendering.animation.debug.DebugPoseOverride;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;

import java.util.Set;

@ClientScope
@Subcomponent(modules = {
        ClientAttachmentModule.class,
        ClientNetworkModule.class,
        UiSyncClientModule.class,
})
public interface ClientComponent {

    ClientSidePacketReceiver clientSidePacketReceiver();

    UiClientState uiClientState();

    SettlementDebugOverlayState settlementDebugOverlayState();

    DebugPoseOverride debugPoseOverride();

    Set<AttachmentProvider> attachmentProviders();

    SlotAnchorRegistry slotAnchorRegistry();

    SocketRegistry socketRegistry();

    AttachmentDisplayProfileRegistry attachmentDisplayProfileRegistry();

    ClientSessionComponent.Factory clientSessionComponentFactory();

    @Subcomponent.Factory
    interface Factory {
        ClientComponent create();
    }

}
