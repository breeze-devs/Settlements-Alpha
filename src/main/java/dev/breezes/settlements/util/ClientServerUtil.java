package dev.breezes.settlements.util;

import dev.breezes.settlements.entities.villager.BaseVillager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;
import java.util.Optional;

public class ClientServerUtil {

    public static void runOnClient(@Nonnull Runnable runnable) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> runnable);
    }

    public static Optional<BaseVillager> getClientSideVillager(@Nonnull BaseVillager serverSideVillager) {
        return Optional.ofNullable(Minecraft.getInstance().level)
                .map(clientLevel -> clientLevel.getEntity(serverSideVillager.getId()))
                .filter(entity -> entity instanceof BaseVillager)
                .map(BaseVillager.class::cast);
    }

}
