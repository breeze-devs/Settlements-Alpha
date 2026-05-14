package dev.breezes.settlements.di;

import lombok.CustomLog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Centralized bridge into the Dagger graph during the migration away from
 * ad-hoc singletons.
 * <p>
 * We keep exactly one static access point so bootstrap-owned objects can reach
 * the application graph without preserving the current service-locator sprawl.
 */
@CustomLog
public final class SettlementsDagger {

    @Nullable
    private static SettlementsComponent component;
    @Nullable
    private static ServerComponent serverComponent;
    @Nullable
    private static ClientComponent clientComponent;
    @Nullable
    private static ClientSessionComponent clientSessionComponent;

    public static void initialize(@Nonnull SettlementsComponent settlementsComponent) {
        if (component != null) {
            throw new IllegalStateException("SettlementsComponent has already been initialized");
        }

        component = settlementsComponent;
    }

    public static void initializeServer(@Nonnull ServerComponent settlementsServerComponent) {
        if (serverComponent != null) {
            log.warn("Overwriting previously initialized ServerComponent (clearServer may have been missed)");
        }

        serverComponent = settlementsServerComponent;
    }

    public static void initializeClient(@Nonnull ClientComponent settlementsClientComponent) {
        if (clientComponent != null) {
            throw new IllegalStateException("ClientComponent has already been initialized");
        }

        clientComponent = settlementsClientComponent;
    }

    public static void initializeClientSession(@Nonnull ClientSessionComponent settlementsClientSessionComponent) {
        if (clientSessionComponent != null) {
            log.warn("Overwriting previously initialized ClientSessionComponent (clearClientSession may have been missed)");
        }

        clientSessionComponent = settlementsClientSessionComponent;
    }

    @Nonnull
    public static SettlementsComponent component() {
        if (component == null) {
            throw new IllegalStateException("SettlementsComponent has not been initialized yet");
        }

        return component;
    }

    @Nonnull
    public static ServerComponent serverOrThrow() {
        if (serverComponent == null) {
            throw new IllegalStateException("ServerComponent has not been initialized yet");
        }

        return serverComponent;
    }

    @Nullable
    public static ServerComponent serverOrNull() {
        return serverComponent;
    }

    @Nonnull
    public static ClientComponent client() {
        if (clientComponent == null) {
            throw new IllegalStateException("ClientComponent has not been initialized yet");
        }

        return clientComponent;
    }

    @Nonnull
    public static ClientSessionComponent clientSessionOrThrow() {
        if (clientSessionComponent == null) {
            throw new IllegalStateException("ClientSessionComponent has not been initialized yet");
        }

        return clientSessionComponent;
    }

    @Nullable
    public static ClientSessionComponent clientSessionOrNull() {
        return clientSessionComponent;
    }

    public static void clearServer() {
        serverComponent = null;
    }

    public static void clearClientSession() {
        clientSessionComponent = null;
    }

}
