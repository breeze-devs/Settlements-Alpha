package dev.breezes.settlements.di.modules.client;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.application.ui.sync.UiClientChannelDefinition;
import dev.breezes.settlements.di.UiChannelKey;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;
import dev.breezes.settlements.presentation.ui.dayplan.DayPlanScreenOpener;

@Module
public abstract class UiSyncClientModule {

    @Provides
    @IntoMap
    @UiChannelKey(UiChannel.DAY_PLAN)
    static UiClientChannelDefinition dayPlan(DayPlanScreenOpener screenOpener) {
        return UiClientChannelDefinition.builder()
                .channel(UiChannel.DAY_PLAN)
                .screenOpener(screenOpener)
                .build();
    }

}
