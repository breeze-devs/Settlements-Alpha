package dev.breezes.settlements.configurations;

import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import dev.breezes.settlements.configurations.annotations.maps.ConfigLoadingException;
import dev.breezes.settlements.util.crash.CrashUtil;
import dev.breezes.settlements.util.crash.report.ConfigLoadingCrashReport;
import lombok.CustomLog;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Singleton registry that holds cached configuration Record instances.
 * <p>
 * This factory is populated by the
 * {@link dev.breezes.settlements.configurations.annotations.RecordConfigProcessor}
 * during mod initialization. Each configuration Record is instantiated once and
 * cached here.
 * <p>
 * Behaviors request their configuration via {@link #create(Class)}.
 * <p>
 * Thread-safe implementation using ConcurrentHashMap for use during parallel
 * mod loading.
 */
@CustomLog
public class ConfigFactory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new Gson());

    /**
     * Registry mapping config Record classes to their cached instances.
     * Uses ConcurrentHashMap for thread-safety during mod loading.
     */
    private static final Map<Class<?>, Object> INSTANCES = new ConcurrentHashMap<>();

    /**
     * Registers a configuration Record class with its factory Supplier.
     * <p>
     * This method is called by RecordConfigProcessor during mod initialization.
     * The Supplier is invoked immediately to create the instance, which is then
     * cached.
     * <p>
     * DESIGN DECISION: We eagerly create the instance here rather than lazily on
     * first access
     * because configs are immutable snapshots of the TOML file state at startup.
     * All config values should be finalized before behaviors are instantiated.
     *
     * @param type    The Record class representing the configuration
     * @param factory A Supplier that creates an instance of the Record from config
     *                values
     * @param <T>     The type of the configuration Record
     */
    public static <T> void register(@Nonnull Class<T> type, @Nonnull Supplier<T> factory) {
        try {
            log.debug("Registering config for {}", type.getSimpleName());
            T instance = factory.get();
            INSTANCES.put(type, instance);
            log.debug("Successfully created and cached config instance for {}: {}",
                    type.getSimpleName(), OBJECT_MAPPER.writeValueAsString(instance));
        } catch (Exception e) {
            String message = "Failed to create config instance for %s".formatted(type.getSimpleName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(message, e)));
        }
    }

    /**
     * Retrieves a cached configuration Record instance.
     * <p>
     * This method is called by behaviors during construction to obtain their
     * configuration.
     *
     * @param type The Record class representing the configuration
     * @param <T>  The type of the configuration Record
     * @return The cached configuration instance
     * @throws RuntimeException if no configuration was registered for this type
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T> T create(@Nonnull Class<T> type) {
        Object instance = INSTANCES.get(type);
        if (instance == null) {
            String errorMessage = "No configuration registered for: %s. Did you forget to annotate the Record with @BehaviorConfig?".formatted(type.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(errorMessage)));
        }
        return (T) instance;
    }

    /**
     * Clears all registered configurations.
     * Used primarily for testing. DO NOT call this in production code.
     */
    public static void clearAll() {
        log.warn("Clearing all registered configurations - this should only happen in tests!");
        INSTANCES.clear();
    }
}
