package dev.breezes.settlements.presentation.client;

import dev.breezes.settlements.infrastructure.config.annotations.GeneralConfig;

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
