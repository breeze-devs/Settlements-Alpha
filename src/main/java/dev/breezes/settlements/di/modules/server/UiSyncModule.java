package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import dev.breezes.settlements.application.ui.dayplan.DayPlanSessionValidator;
import dev.breezes.settlements.application.ui.dayplan.DayPlanSnapshotPublisher;
import dev.breezes.settlements.application.ui.sync.UiServerChannelDefinition;
import dev.breezes.settlements.di.UiChannelKey;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;

@Module
public abstract class UiSyncModule {

    @Provides
    @IntoMap
    @UiChannelKey(UiChannel.DAY_PLAN)
    static UiServerChannelDefinition dayPlan(DayPlanSessionValidator validator,
                                             DayPlanSnapshotPublisher publisher) {
        return UiServerChannelDefinition.builder()
                .channel(UiChannel.DAY_PLAN)
                .defaultUnavailableReasonKey("ui.settlements.dayplan.unavailable")
                .validator(validator)
                .snapshotPublisher(publisher)
                .build();
    }

}
