package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.di.ClientSessionResettable;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationSeedSetClientProjection;
import dev.breezes.settlements.infrastructure.rendering.debug.SettlementDebugOverlayState;
import dev.breezes.settlements.infrastructure.rendering.highlight.TotemHighlightProvider;
import dev.breezes.settlements.infrastructure.rendering.zone.CultivationLilyZoneRevealRenderer;
import dev.breezes.settlements.presentation.ui.hud.CrosshairHudRenderer;
import dev.breezes.settlements.presentation.ui.sync.UiClientState;

@Module
public abstract class ClientLifecycleModule {

    @Binds
    @IntoSet
    abstract ClientSessionResettable uiClientState(UiClientState implementation);

    @Binds
    @IntoSet
    abstract ClientSessionResettable settlementDebugOverlayState(SettlementDebugOverlayState implementation);

    @Binds
    @IntoSet
    abstract ClientSessionResettable totemHighlightProvider(TotemHighlightProvider implementation);

    @Binds
    @IntoSet
    abstract ClientSessionResettable crosshairHudRenderer(CrosshairHudRenderer implementation);

    @Binds
    @IntoSet
    abstract ClientSessionResettable cultivationSeedSetClientProjection(CultivationSeedSetClientProjection implementation);

    @Binds
    @IntoSet
    abstract ClientSessionResettable cultivationLilyZoneRevealRenderer(CultivationLilyZoneRevealRenderer implementation);

}
