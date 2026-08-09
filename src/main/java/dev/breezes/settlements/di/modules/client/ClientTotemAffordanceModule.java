package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.infrastructure.rendering.highlight.EntityHighlightProvider;
import dev.breezes.settlements.infrastructure.rendering.highlight.TotemHighlightProvider;
import dev.breezes.settlements.presentation.ui.hud.HeldItemHudProvider;
import dev.breezes.settlements.presentation.ui.hud.TotemHeldItemHudProvider;

@Module
public abstract class ClientTotemAffordanceModule {

    @Binds
    @IntoSet
    abstract EntityHighlightProvider totemHighlightProvider(TotemHighlightProvider implementation);

    @Binds
    @IntoSet
    abstract HeldItemHudProvider totemHeldItemHudProvider(TotemHeldItemHudProvider implementation);

}
