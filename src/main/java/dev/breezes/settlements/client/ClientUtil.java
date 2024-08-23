package dev.breezes.settlements.client;

import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.client.Minecraft;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ClientUtil {

    public static Optional<BaseVillager> getClientSideVillager(@Nonnull BaseVillager serverSideVillager) {
        return Optional.ofNullable(Minecraft.getInstance().level)
                .map(clientLevel -> clientLevel.getEntity(serverSideVillager.getId()))
                .filter(entity -> entity instanceof BaseVillager)
                .map(BaseVillager.class::cast);
    }

}
