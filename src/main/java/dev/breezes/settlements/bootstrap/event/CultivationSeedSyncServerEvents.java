package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.farming.CultivationCropDefinition;
import dev.breezes.settlements.domain.farming.CultivationCropRegistry;
import dev.breezes.settlements.infrastructure.network.features.farming.packet.ClientBoundCultivationSeedSetPacket;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Keeps {@link dev.breezes.settlements.infrastructure.minecraft.data.farming.crops.CultivationSeedSetClientProjection}
 * in agreement with the server's crop registry.
 */
@ServerSide
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CultivationSeedSyncServerEvents {

    private final CultivationCropRegistry cultivationCropRegistry;

    @SubscribeEvent
    public void onDatapackSync(@Nonnull OnDatapackSyncEvent event) {
        List<ResourceLocation> seedItemIds = this.cultivationCropRegistry.all().stream()
                .map(CultivationCropDefinition::seedItem)
                .toList();

        ClientBoundCultivationSeedSetPacket packet = new ClientBoundCultivationSeedSetPacket(seedItemIds);
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, packet));
    }

}
