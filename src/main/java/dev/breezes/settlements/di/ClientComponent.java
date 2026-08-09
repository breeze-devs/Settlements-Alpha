package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientAttachmentModule;
import dev.breezes.settlements.di.modules.client.ClientLifecycleModule;
import dev.breezes.settlements.di.modules.client.ClientNetworkModule;
import dev.breezes.settlements.di.modules.client.ClientTotemAffordanceModule;
import dev.breezes.settlements.di.modules.client.UiSyncClientModule;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.SlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketReceiver;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.infrastructure.rendering.highlight.EntityHighlightRenderer;
import dev.breezes.settlements.presentation.ui.hud.HeldItemHudRenderer;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;

import java.util.Set;

@ClientScope
@Subcomponent(modules = {
        ClientAttachmentModule.class,
        ClientLifecycleModule.class,
        ClientNetworkModule.class,
        ClientTotemAffordanceModule.class,
        UiSyncClientModule.class,
})
public interface ClientComponent {

    ClientSidePacketReceiver clientSidePacketReceiver();

    UiClientState uiClientState();

    SettlementDebugOverlayState settlementDebugOverlayState();

    Set<AttachmentProvider> attachmentProviders();

    SlotAnchorRegistry slotAnchorRegistry();

    SocketRegistry socketRegistry();

    AttachmentDisplayProfileRegistry attachmentDisplayProfileRegistry();

    EntityHighlightRenderer entityHighlightRenderer();

    HeldItemHudRenderer heldItemHudRenderer();

    Set<ClientSessionResettable> clientSessionResettables();

    ClientSessionComponent.Factory clientSessionComponentFactory();

    @Subcomponent.Factory
    interface Factory {
        ClientComponent create();
    }

}
