package dev.breezes.settlements.infrastructure.network.core.registry;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.infrastructure.network.core.ClientBoundPacket;
import dev.breezes.settlements.infrastructure.network.core.ClientSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.ServerBoundPacket;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleClientPacket;
import dev.breezes.settlements.infrastructure.network.core.annotations.HandleServerPacket;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.InvalidPacketRegistrationCrashReport;
import lombok.CustomLog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@CustomLog
public class PacketHandlerAnnotationProcessor {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            log.debug("PacketHandlerAnnotationProcessor already initialized");
            return;
        }

        log.info("Processing packet handler annotations");
        ModFileScanData scanner = ModList.get()
                .getModContainerById(SettlementsMod.MOD_ID)
                .orElseThrow()
                .getModInfo()
                .getOwningFile()
                .getFile()
                .getScanResult();

        Dist dist = FMLEnvironment.dist;

        // Only discover client packet handlers on the client
        Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> clientHandlers =
                dist == Dist.CLIENT ? discoverClientHandlers(scanner) : Map.of();
        // Always discover server packet handlers
        Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> serverHandlers = discoverServerHandlers(scanner);

        ClientPacketHandlerRegistry.initialize(clientHandlers);
        ServerPacketHandlerRegistry.initialize(serverHandlers);

        log.info("Initialized {} client-side packet handlers", clientHandlers.size());
        clientHandlers.forEach((packetClass, handler) ->
                log.debug("- {} -> {}", packetClass.getName(), handler.getClass().getName()));

        log.info("Initialized {} server-side packet handlers", serverHandlers.size());
        serverHandlers.forEach((packetClass, handler) ->
                log.debug("- {} -> {}", packetClass.getName(), handler.getClass().getName()));
    }

    private static Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> discoverClientHandlers(@Nonnull ModFileScanData scanner) {
        Set<? extends Class<?>> handlerClasses = scanner.getAnnotatedBy(HandleClientPacket.class, ElementType.TYPE)
                .map(PacketHandlerAnnotationProcessor::loadClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> bindings = new HashMap<>();
        for (Class<?> handlerClass : handlerClasses) {
            HandleClientPacket annotation = handlerClass.getAnnotation(HandleClientPacket.class);
            if (annotation == null) {
                crash("Missing @HandleClientPacket annotation on handler class: " + handlerClass.getName(), null);
                continue;
            }

            if (!ClientSidePacketHandler.class.isAssignableFrom(handlerClass)) {
                crash("@HandleClientPacket class does not implement ClientSidePacketHandler: " + handlerClass.getName(), null);
                continue;
            }

            Class<? extends ClientBoundPacket> packetClass = annotation.value();
            if (!ClientBoundPacket.class.isAssignableFrom(packetClass)) {
                crash("@HandleClientPacket references non-client packet type '%s' on handler '%s'".formatted(packetClass.getName(), handlerClass.getName()), null);
                continue;
            }

            ClientSidePacketHandler<? extends ClientBoundPacket> handler = instantiateClientHandler(handlerClass);
            ClientSidePacketHandler<? extends ClientBoundPacket> existing = bindings.putIfAbsent(packetClass, handler);
            if (existing != null) {
                crash("Duplicate client packet handler binding for packet '%s': '%s' and '%s'"
                        .formatted(packetClass.getName(), existing.getClass().getName(), handlerClass.getName()), null);
            }
        }

        return bindings;
    }

    private static Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> discoverServerHandlers(@Nonnull ModFileScanData scanner) {
        Objects.requireNonNull(scanner, "scanner");

        Set<Class<?>> handlerClasses = scanner.getAnnotatedBy(HandleServerPacket.class, ElementType.TYPE)
                .map(PacketHandlerAnnotationProcessor::loadClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());

        Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> bindings = new HashMap<>();
        for (Class<?> handlerClass : handlerClasses) {
            HandleServerPacket annotation = handlerClass.getAnnotation(HandleServerPacket.class);
            if (annotation == null) {
                crash("Missing @HandleServerPacket annotation on handler class: " + handlerClass.getName(), null);
                continue;
            }

            if (!ServerSidePacketHandler.class.isAssignableFrom(handlerClass)) {
                crash("@HandleServerPacket class does not implement ServerSidePacketHandler: " + handlerClass.getName(), null);
                continue;
            }

            Class<? extends ServerBoundPacket> packetClass = annotation.value();
            if (!ServerBoundPacket.class.isAssignableFrom(packetClass)) {
                crash("@HandleServerPacket references non-server packet type '%s' on handler '%s'".formatted(packetClass.getName(), handlerClass.getName()), null);
                continue;
            }

            ServerSidePacketHandler<? extends ServerBoundPacket> handler = instantiateServerHandler(handlerClass);
            ServerSidePacketHandler<? extends ServerBoundPacket> existing = bindings.putIfAbsent(packetClass, handler);
            if (existing != null) {
                crash("Duplicate server packet handler binding for packet '%s': '%s' and '%s'"
                        .formatted(packetClass.getName(), existing.getClass().getName(), handlerClass.getName()), null);
            }
        }

        return bindings;
    }

    private static ClientSidePacketHandler<? extends ClientBoundPacket> instantiateClientHandler(@Nonnull Class<?> handlerClass) {
        try {
            Constructor<?> constructor = handlerClass.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                String message = "Client packet handler must declare a public no-arg constructor: " + handlerClass.getName();
                crash(message, null);
            }
            return (ClientSidePacketHandler<? extends ClientBoundPacket>) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            String message = "Failed to instantiate client packet handler: " + handlerClass.getName();
            crash(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private static ServerSidePacketHandler<? extends ServerBoundPacket> instantiateServerHandler(@Nonnull Class<?> handlerClass) {
        try {
            Constructor<?> constructor = handlerClass.getDeclaredConstructor();
            if (!Modifier.isPublic(constructor.getModifiers())) {
                String message = "Server packet handler must declare a public no-arg constructor: " + handlerClass.getName();
                crash(message, null);
            }
            return (ServerSidePacketHandler<? extends ServerBoundPacket>) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            String message = "Failed to instantiate server packet handler: " + handlerClass.getName();
            crash(message, e);
            throw new IllegalStateException(message, e);
        }
    }

    private static Optional<Class<?>> loadClass(@Nonnull ModFileScanData.AnnotationData data) {
        try {
            String className = data.clazz().getClassName();
            return Optional.of(Class.forName(className, false, PacketHandlerAnnotationProcessor.class.getClassLoader()));
        } catch (Exception e) {
            String message = "Failed to load packet handler class from annotation scan: " + data.clazz().getClassName();
            crash(message, e);
            return Optional.empty();
        }
    }

    private static void crash(@Nonnull String message, Exception e) {
        IllegalStateException reason = e == null
                ? new IllegalStateException(message)
                : new IllegalStateException(message, e);
        CrashUtil.crash(new InvalidPacketRegistrationCrashReport(reason));
    }

}
