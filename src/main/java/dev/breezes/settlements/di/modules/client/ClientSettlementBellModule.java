package dev.breezes.settlements.di.modules.client;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.presentation.ui.hud.LookTargetHudProvider;
import dev.breezes.settlements.presentation.ui.hud.SettlementBellLookTargetHudProvider;

@Module
public abstract class ClientSettlementBellModule {

    @Binds
    @IntoSet
    abstract LookTargetHudProvider settlementBellLookTargetHudProvider(SettlementBellLookTargetHudProvider implementation);

}
