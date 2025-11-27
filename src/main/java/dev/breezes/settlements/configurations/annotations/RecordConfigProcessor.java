package dev.breezes.settlements.configurations.annotations;

import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import dev.breezes.settlements.configurations.ConfigFactory;
import dev.breezes.settlements.configurations.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.configurations.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.configurations.annotations.floats.FloatConfig;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfig;
import dev.breezes.settlements.configurations.annotations.maps.ConfigLoadingException;
import dev.breezes.settlements.configurations.annotations.maps.MapConfig;
import dev.breezes.settlements.configurations.annotations.maps.MapEntry;
import dev.breezes.settlements.configurations.annotations.maps.deserializers.MapConfigDeserializer;
import dev.breezes.settlements.configurations.annotations.maps.deserializers.MapConfigDeserializers;
import dev.breezes.settlements.configurations.annotations.strings.StringConfig;
import dev.breezes.settlements.util.crash.CrashUtil;
import dev.breezes.settlements.util.crash.report.ConfigLoadingCrashReport;
import lombok.CustomLog;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes Java Records annotated with @BehaviorConfig to create runtime
 * configuration bindings.
 * <p>
 * This processor is the heart of the Runtime Record Binding architecture. It:
 * 1. Scans Record components for config annotations
 * 2. Builds NeoForge ModConfigSpec entries for each component
 * 3. Registers a factory in ConfigFactory that instantiates the Record from
 * loaded values
 * <p>
 * ARCHITECTURE OVERVIEW:
 * - Each Record component must have exactly ONE config annotation
 * (@IntegerConfig, @BooleanConfig, etc.)
 * - Component order in the Record determines constructor parameter order
 * - Config values are eagerly loaded and cached in ConfigFactory
 * - Records are immutable snapshots of the config state at startup
 * <p>
 * SUPPORTED TYPES:
 * - Primitives: int, boolean, double, float
 * - String
 * - Map<String, ?> (with deserializers)
 */
@CustomLog
public class RecordConfigProcessor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new Gson());

    /**
     * Process a configuration Record class.
     * <p>
     * This method:
     * 1. Validates the class is a Record and has @BehaviorConfig
     * 2. Builds ModConfigSpec entries for each component
     * 3. Creates a factory lambda that instantiates the Record
     * 4. Registers the factory in ConfigFactory
     *
     * @param recordClass The Record class to process
     * @param registry    The configuration registry for ModConfigSpec builders
     * @return A Runnable that, when executed, will register the factory in
     * ConfigFactory
     */
    @Nonnull
    public Runnable process(@Nonnull Class<?> recordClass, @Nonnull ConfigurationAnnotationRegistry registry) {
        // Validate the class is a Record
        if (!recordClass.isRecord()) {
            String message = "@BehaviorConfig can only be applied to Records, but %s is not a Record"
                    .formatted(recordClass.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(message)));
        }

        // Get the @BehaviorConfig annotation
        BehaviorConfig behaviorConfig = recordClass.getAnnotation(BehaviorConfig.class);
        if (behaviorConfig == null) {
            String message = "Record %s is missing @BehaviorConfig annotation".formatted(recordClass.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(message)));
        }

        String configName = behaviorConfig.name();
        ConfigurationType configurationType = behaviorConfig.type();
        String configFilePath = configurationType.getFilePath(configName);

        log.info("Processing @BehaviorConfig record: {} -> {}", recordClass.getSimpleName(), configFilePath);

        // Get the builder for this config file
        ModConfigSpec.Builder builder = registry.getBuilder(configFilePath);
        builder.push(configName);

        // Process each record component
        RecordComponent[] components = recordClass.getRecordComponents();
        List<ComponentBinding> bindings = new ArrayList<>();

        for (RecordComponent component : components) {
            ComponentBinding binding = processComponent(component, builder, configName);
            bindings.add(binding);
            log.info("  - {}: {} ({})", component.getName(), component.getType().getSimpleName(),
                    binding.configValue.getClass().getSimpleName());
        }

        builder.pop();

        log.info("Successfully processed {} components for {}", bindings.size(), recordClass.getSimpleName());

        // Return a Runnable that registers the factory
        return () -> registerFactory(recordClass, bindings, configName);
    }

    /**
     * Process a single Record component and create its ModConfigSpec binding.
     */
    @Nonnull
    private ComponentBinding processComponent(@Nonnull RecordComponent component,
                                              @Nonnull ModConfigSpec.Builder builder,
                                              @Nonnull String configName) {
        String componentName = component.getName();

        // Try each annotation type
        if (component.isAnnotationPresent(IntegerConfig.class)) {
            return processIntegerComponent(component, builder);
        } else if (component.isAnnotationPresent(BooleanConfig.class)) {
            return processBooleanComponent(component, builder);
        } else if (component.isAnnotationPresent(DoubleConfig.class)) {
            return processDoubleComponent(component, builder);
        } else if (component.isAnnotationPresent(FloatConfig.class)) {
            return processFloatComponent(component, builder);
        } else if (component.isAnnotationPresent(StringConfig.class)) {
            return processStringComponent(component, builder);
        } else if (component.isAnnotationPresent(MapConfig.class)) {
            return processMapComponent(component, builder);
        } else {
            String errorMsg = "Record component '%s.%s' has no config annotation".formatted(component.getDeclaringRecord().getSimpleName(), componentName);
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(errorMsg)));
            throw new RuntimeException("Unreachable");
        }
    }

    private ComponentBinding processIntegerComponent(@Nonnull RecordComponent component,
                                                     @Nonnull ModConfigSpec.Builder builder) {
        IntegerConfig annotation = component.getAnnotation(IntegerConfig.class);
        ModConfigSpec.IntValue configValue = builder
                .comment(annotation.description())
                .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
        return new ComponentBinding(configValue, ConfigValueType.INTEGER);
    }

    private ComponentBinding processBooleanComponent(@Nonnull RecordComponent component,
                                                     @Nonnull ModConfigSpec.Builder builder) {
        BooleanConfig annotation = component.getAnnotation(BooleanConfig.class);
        ModConfigSpec.BooleanValue configValue = builder
                .comment(annotation.description())
                .define(annotation.identifier(), annotation.defaultValue());
        return new ComponentBinding(configValue, ConfigValueType.BOOLEAN);
    }

    private ComponentBinding processDoubleComponent(@Nonnull RecordComponent component,
                                                    @Nonnull ModConfigSpec.Builder builder) {
        DoubleConfig annotation = component.getAnnotation(DoubleConfig.class);
        ModConfigSpec.DoubleValue configValue = builder
                .comment(annotation.description())
                .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
        return new ComponentBinding(configValue, ConfigValueType.DOUBLE);
    }

    private ComponentBinding processFloatComponent(@Nonnull RecordComponent component,
                                                   @Nonnull ModConfigSpec.Builder builder) {
        FloatConfig annotation = component.getAnnotation(FloatConfig.class);
        // NeoForge doesn't have a FloatValue, so we use DoubleValue
        ModConfigSpec.DoubleValue configValue = builder
                .comment(annotation.description())
                .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
        return new ComponentBinding(configValue, ConfigValueType.FLOAT);
    }

    private ComponentBinding processStringComponent(@Nonnull RecordComponent component,
                                                    @Nonnull ModConfigSpec.Builder builder) {
        StringConfig annotation = component.getAnnotation(StringConfig.class);
        ModConfigSpec.ConfigValue<String> configValue = builder
                .comment(annotation.description())
                .define(annotation.identifier(), annotation.defaultValue());
        return new ComponentBinding(configValue, ConfigValueType.STRING);
    }

    private ComponentBinding processMapComponent(@Nonnull RecordComponent component,
                                                 @Nonnull ModConfigSpec.Builder builder) {
        MapConfig annotation = component.getAnnotation(MapConfig.class);

        // Convert default MapEntry[] to a serialized string
        String defaultValueStr = serializeMapEntries(annotation.defaultValue());

        // Store as a string in TOML (NeoForge doesn't support complex Maps natively)
        ModConfigSpec.ConfigValue<String> configValue = builder
                .comment(annotation.description() + " (Format: key1=value1,key2=value2)")
                .define(annotation.identifier(), defaultValueStr);

        // Get the deserializer
        MapConfigDeserializer<?> deserializer = MapConfigDeserializers.getDeserializer(annotation.deserializer());

        return new ComponentBinding(configValue, ConfigValueType.MAP, deserializer);
    }

    /**
     * Serialize MapEntry[] to JSON format compatible with TOML.
     * Matches the legacy MapConfigAnnotationProcessor behavior.
     */
    private String serializeMapEntries(@Nonnull MapEntry[] entries) {
        Map<String, String> map = new HashMap<>();
        for (MapEntry entry : entries) {
            map.put(entry.key(), entry.value());
        }

        // Serialize to JSON and replace " with ' for TOML compatibility
        return OBJECT_MAPPER.writeValueAsString(map).replace("\"", "'");
    }

    /**
     * Register the factory in ConfigFactory.
     * This is called after ModConfigSpec is built and values are loaded.
     */
    private <T> void registerFactory(@Nonnull Class<T> recordClass,
                                     @Nonnull List<ComponentBinding> bindings,
                                     @Nonnull String configName) {
        log.debug("Registering factory for {}", recordClass.getSimpleName());

        ConfigFactory.register(recordClass, () -> {
            try {
                // Extract values from config bindings
                Object[] args = new Object[bindings.size()];
                for (int i = 0; i < bindings.size(); i++) {
                    args[i] = bindings.get(i).getValue();
                }

                // Invoke the canonical constructor
                return invokeCanonicalConstructor(recordClass, args);
            } catch (Exception e) {
                String errorMsg = "Failed to instantiate config Record %s from TOML values".formatted(recordClass.getSimpleName());
                throw new RuntimeException(errorMsg, e);
            }
        });
    }

    /**
     * Invoke the canonical (all-args) constructor of a Record.
     */
    @SuppressWarnings("unchecked")
    private <T> T invokeCanonicalConstructor(@Nonnull Class<T> recordClass, @Nonnull Object[] args) {
        try {
            // Get parameter types from record components
            RecordComponent[] components = recordClass.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            for (int i = 0; i < components.length; i++) {
                paramTypes[i] = components[i].getType();
            }

            // Find and invoke the constructor
            Constructor<?> constructor = recordClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            String errorMsg = "Failed to invoke canonical constructor for %s".formatted(recordClass.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(errorMsg, e)));
            throw new RuntimeException("Unreachable");
        }
    }

    /**
     * Holds a binding between a ModConfigSpec.ConfigValue and its type.
     */
    private static class ComponentBinding {

        private final ModConfigSpec.ConfigValue<?> configValue;
        private final ConfigValueType type;
        private final MapConfigDeserializer<?> mapDeserializer; // Only for MAP type

        public ComponentBinding(@Nonnull ModConfigSpec.ConfigValue<?> configValue, @Nonnull ConfigValueType type) {
            this(configValue, type, null);
        }

        public ComponentBinding(@Nonnull ModConfigSpec.ConfigValue<?> configValue,
                                @Nonnull ConfigValueType type,
                                @Nullable MapConfigDeserializer<?> mapDeserializer) {
            this.configValue = configValue;
            this.type = type;
            this.mapDeserializer = mapDeserializer;
        }

        /**
         * Get the value from the config, with the appropriate type handling.
         */
        @Nonnull
        public Object getValue() {
            Object rawValue = this.configValue.get();

            return switch (type) {
                case INTEGER -> ((Number) rawValue).intValue();
                case BOOLEAN -> (Boolean) rawValue;
                case DOUBLE -> ((Number) rawValue).doubleValue();
                case FLOAT -> ((Number) rawValue).floatValue();
                case STRING -> (String) rawValue;
                case MAP -> deserializeMap((String) rawValue);
            };
        }

        /**
         * Deserialize map from JSON string using the appropriate deserializer.
         * Matches the legacy MapConfigAnnotationProcessor behavior.
         */
        @SuppressWarnings("unchecked")
        private Map<String, ?> deserializeMap(@Nonnull String serialized) {
            if (this.mapDeserializer == null) {
                throw new IllegalStateException("Map deserializer is null for MAP type");
            }

            try {
                // Parse JSON to Map<String, String>
                Map<String, String> rawMap = OBJECT_MAPPER.readValue(serialized, Map.class);

                // Deserialize each value using the deserializer
                Map<String, Object> deserializedMap = new HashMap<>();
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    Object deserializedValue = this.mapDeserializer.deserialize(entry.getValue());
                    deserializedMap.put(entry.getKey(), deserializedValue);
                }

                return deserializedMap;
            } catch (Exception e) {
                throw new RuntimeException("Failed to deserialize map config from JSON: " + serialized, e);
            }
        }
    }

    /**
     * Enum representing the different config value types we support.
     */
    private enum ConfigValueType {
        INTEGER,
        BOOLEAN,
        DOUBLE,
        FLOAT,
        STRING,
        MAP
    }

}
