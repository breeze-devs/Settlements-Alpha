package dev.breezes.settlements.application.ui.sync;

import dev.breezes.settlements.application.ui.sync.session.UiSession;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nonnull;

public interface UiSnapshotPublisher {

    void onSessionOpened(@Nonnull UiSession session,
                         @Nonnull ServerPlayer player,
                         @Nonnull BaseVillager villager,
                         long gameTime,
                         long dayTime);

    void tick(@Nonnull UiSession session,
              @Nonnull ServerPlayer player,
              @Nonnull BaseVillager villager,
              long gameTime,
              long dayTime);

    default void onSessionClosed(@Nonnull UiSession session) {
    }

}
