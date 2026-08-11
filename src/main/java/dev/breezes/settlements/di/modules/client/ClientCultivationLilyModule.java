package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.presentation.ui.hud.CultivationLilyLookTargetHudProvider;
import dev.breezes.settlements.presentation.ui.hud.LookTargetHudProvider;

@Module
public abstract class ClientCultivationLilyModule {

    @Binds
    @IntoSet
    abstract LookTargetHudProvider cultivationLilyLookTargetHudProvider(CultivationLilyLookTargetHudProvider implementation);

}
