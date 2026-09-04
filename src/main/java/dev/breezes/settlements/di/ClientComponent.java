package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientAttachmentModule;
import dev.breezes.settlements.di.modules.client.ClientCultivationLilyModule;
import dev.breezes.settlements.di.modules.client.ClientLifecycleModule;
import dev.breezes.settlements.di.modules.client.ClientNetworkModule;
import dev.breezes.settlements.di.modules.client.ClientTotemAffordanceModule;
import dev.breezes.settlements.di.modules.client.HudModule;
import dev.breezes.settlements.di.modules.client.UiSyncClientModule;
import dev.breezes.settlements.domain.attachment.AttachmentProvider;
import dev.breezes.settlements.domain.presentation.AttachmentDisplayProfileRegistry;
import dev.breezes.settlements.domain.presentation.SlotAnchorRegistry;
import dev.breezes.settlements.domain.presentation.SocketRegistry;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationSeedSetClientProjection;
import dev.breezes.settlements.infrastructure.minecraft.data.schedule.VillageDayTypeClientProjection;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketReceiver;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.infrastructure.rendering.debug.tuning.DebugTuningBoard;
import dev.breezes.settlements.infrastructure.rendering.debug.tuning.DebugTuningHudRenderer;
import dev.breezes.settlements.infrastructure.rendering.highlight.EntityHighlightRenderer;
import dev.breezes.settlements.infrastructure.rendering.zone.CultivationLilyPlacementPreviewRenderer;
import dev.breezes.settlements.infrastructure.rendering.zone.CultivationLilyZoneRevealRenderer;
import dev.breezes.settlements.presentation.ui.hud.ClockHudRenderer;
import dev.breezes.settlements.presentation.ui.hud.CrosshairHudRenderer;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;

import java.util.Set;

@ClientScope
@Subcomponent(modules = {
        ClientAttachmentModule.class,
        ClientCultivationLilyModule.class,
        ClientLifecycleModule.class,
        ClientNetworkModule.class,
        ClientTotemAffordanceModule.class,
        HudModule.class,
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

    CrosshairHudRenderer crosshairHudRenderer();

    ClockHudRenderer clockHudRenderer();

    VillageDayTypeClientProjection villageDayTypeClientProjection();

    DebugTuningBoard debugTuningBoard();

    DebugTuningHudRenderer debugTuningHudRenderer();

    CultivationLilyZoneRevealRenderer cultivationLilyZoneRevealRenderer();

    CultivationLilyPlacementPreviewRenderer cultivationLilyPlacementPreviewRenderer();

    CultivationSeedSetClientProjection cultivationSeedSetClientProjection();

    Set<ClientSessionResettable> clientSessionResettables();

    ClientSessionComponent.Factory clientSessionComponentFactory();

    @Subcomponent.Factory
    interface Factory {
        ClientComponent create();
    }

}
