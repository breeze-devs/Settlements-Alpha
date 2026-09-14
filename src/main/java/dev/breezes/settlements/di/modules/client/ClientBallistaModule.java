package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.presentation.ui.hud.BallistaAimHudProvider;
import dev.breezes.settlements.presentation.ui.hud.BallistaLookTargetHudProvider;
import dev.breezes.settlements.presentation.ui.hud.LookTargetHudProvider;
import dev.breezes.settlements.presentation.ui.hud.ModeHudProvider;

@Module
public abstract class ClientBallistaModule {

    @Binds
    @IntoSet
    abstract LookTargetHudProvider ballistaLookTargetHudProvider(BallistaLookTargetHudProvider implementation);

    @Binds
    @IntoSet
    abstract ModeHudProvider ballistaAimHudProvider(BallistaAimHudProvider implementation);

}
