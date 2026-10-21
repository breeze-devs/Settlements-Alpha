package dev.breezes.settlements.shared.logging;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Publication point for the route that logger handles consult on every call.
 * <p>
 * Installing a policy redirects every handle connected to the bridge, including handles created before the install.
 * A bridge accepts one policy in its lifetime: the policy is resolved once per game run, and changing it requires a restart.
 */
final class LoggingBridge {

    /**
     * The bridge connecting every handle LoggerFactory creates.
     * It follows LoggingPolicy.DEFAULT until a policy is installed.
     */
    static final LoggingBridge SHARED = new LoggingBridge(LogRoute.of(LoggingPolicy.DEFAULT));

    private volatile LogRoute route;

    // Written once, under this bridge's lock.
    @Nullable
    private LoggingPolicy installedPolicy;

    LoggingBridge(@Nonnull LogRoute initial) {
        this.route = initial;
    }

    /**
     * Returns the route installed most recently.
     */
    LogRoute route() {
        return this.route;
    }

    /**
     * Publishes the route that follows the given policy, which every later call through a connected handle consults.
     *
     * @throws IllegalStateException if a policy is already installed; the installed policy stays in force
     */
    synchronized void installPolicy(@Nonnull LoggingPolicy policy) {
        if (this.installedPolicy != null) {
            throw new IllegalStateException("A logging policy is installed once per game run: %s is in force, so %s was rejected"
                    .formatted(this.installedPolicy, policy));
        }

        LogRoute installed = LogRoute.of(policy);
        this.installedPolicy = policy;
        this.route = installed;
    }

}
