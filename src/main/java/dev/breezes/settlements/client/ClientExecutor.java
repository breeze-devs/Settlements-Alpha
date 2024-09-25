package dev.breezes.settlements.client;

import dev.breezes.settlements.configurations.GeneralConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nonnull;

public class ClientExecutor {

    public static void runOnClient(@Nonnull Runnable runnable) {
        if (isClientEnabled()) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> runnable);
        }
    }

    private static boolean isClientEnabled() {
        return GeneralConfig.clientEnabled;
    }

}
