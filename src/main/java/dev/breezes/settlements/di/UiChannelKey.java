package dev.breezes.settlements.di;

import dagger.MapKey;
import dev.breezes.settlements.infrastructure.network.features.ui.sync.UiChannel;

@MapKey
public @interface UiChannelKey {

    UiChannel value();

}
