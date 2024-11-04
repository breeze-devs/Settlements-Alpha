package dev.breezes.settlements.client;

import dev.breezes.settlements.configurations.GeneralConfig;

import javax.annotation.Nonnull;

public class ClientExecutor {

    public static void runOnClient(@Nonnull Runnable runnable) {
        if (isClientEnabled()) {
            // TODO: implement
        }
        throw new RuntimeException("Not implemented");
    }

    private static boolean isClientEnabled() {
        return GeneralConfig.clientEnabled;
    }

}
