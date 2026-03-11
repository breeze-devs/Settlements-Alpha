package dev.breezes.settlements.infrastructure.network.core.annotations;

import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the {@link ServerBoundPacket} type handled by the annotated server-side packet handler class.
 * <p>
 * This annotation is processed at mod startup to discover packets and build packet-to-handler bindings automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HandleServerPacket {

    /**
     * The server-bound packet type that the annotated handler processes.
     *
     * @return the {@link ServerBoundPacket} class bound to the annotated handler
     */
    Class<? extends ServerBoundPacket> value();

}
