package dev.breezes.settlements.infrastructure.config.annotations;

import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfig;
import dev.breezes.settlements.infrastructure.config.annotations.enums.EnumConfig;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfig;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.ConfigLoadingException;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfig;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapEntry;
import dev.breezes.settlements.infrastructure.config.annotations.maps.deserializers.MapConfigDeserializer;
import dev.breezes.settlements.infrastructure.config.annotations.maps.deserializers.MapConfigDeserializers;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfig;
import dev.breezes.settlements.infrastructure.config.factory.ConfigFactory;
import dev.breezes.settlements.shared.util.crash.CrashUtil;
import dev.breezes.settlements.shared.util.crash.report.ConfigLoadingCrashReport;
import lombok.CustomLog;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.Validate;

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
 * - Config values are eagerly loaded and cached in ConfigFactory, except for records of a STARTUP type, which
 * registerAtStartup returns to its caller instead
 * - Records are immutable snapshots of the config state at startup
 * <p>
 * SUPPORTED TYPES:
 * - Primitives: int, boolean, double, float
 * - String
 * - Map<String, ?> (with deserializers)
 * - Enums
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
    public Runnable process(@Nonnull Class<?> recordClass, @Nonnull ConfigurationAnnotationRegistry registry) {
        BehaviorConfig behaviorConfig = requireBehaviorConfig(recordClass);
        String configName = behaviorConfig.name();
        ConfigurationType configurationType = behaviorConfig.type();

        log.debug("Processing @BehaviorConfig record {} into the {} config file", recordClass.getSimpleName(), configurationType);

        List<ComponentBinding> bindings = defineComponents(recordClass, configName, registry.getBuilder(configurationType));

        log.debug("Successfully processed {} components for {}", bindings.size(), recordClass.getSimpleName());

        // Return a Runnable that registers the factory
        return () -> registerFactory(recordClass, bindings, configName);
    }

    /**
     * Registers the record as the only section of its configuration type's file, and returns the record holding the
     * values loaded from it.
     *
     * @throws IllegalArgumentException if the record's configuration type is not a STARTUP type
     */
    <T extends Record> T registerAtStartup(@Nonnull ModContainer container, @Nonnull Class<T> recordClass) {
        BehaviorConfig behaviorConfig = requireBehaviorConfig(recordClass);
        ConfigurationType configurationType = behaviorConfig.type();
        Validate.isTrue(configurationType.getModConfigType() == ModConfig.Type.STARTUP,
                "%s is a %s configuration; only STARTUP configurations load during mod construction",
                recordClass.getName(), configurationType.getModConfigType());

        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        List<ComponentBinding> bindings = defineComponents(recordClass, behaviorConfig.name(), builder);
        container.registerConfig(ModConfig.Type.STARTUP, builder.build(), configurationType.getFilePath());

        // NeoForge loads a STARTUP file before registerConfig returns, so its values are readable now.
        return instantiate(recordClass, bindings);
    }

    private BehaviorConfig requireBehaviorConfig(@Nonnull Class<?> recordClass) {
        if (!recordClass.isRecord()) {
            String message = "@BehaviorConfig can only be applied to Records, but %s is not a Record"
                    .formatted(recordClass.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(message)));
        }

        BehaviorConfig behaviorConfig = recordClass.getAnnotation(BehaviorConfig.class);
        if (behaviorConfig == null) {
            String message = "Record %s is missing @BehaviorConfig annotation".formatted(recordClass.getName());
            CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(message)));
        }
        return behaviorConfig;
    }

    /**
     * Defines every component of the record in its own section of the builder, in component order.
     */
    private List<ComponentBinding> defineComponents(@Nonnull Class<?> recordClass,
                                                    @Nonnull String configName,
                                                    @Nonnull ModConfigSpec.Builder builder) {
        builder.push(configName);
        List<ComponentBinding> bindings = new ArrayList<>();
        for (RecordComponent component : recordClass.getRecordComponents()) {
            bindings.add(processComponent(component, builder, configName));
        }
        builder.pop();
        return bindings;
    }

    /**
     * Process a single Record component and create its ModConfigSpec binding.
     */
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
        } else if (component.isAnnotationPresent(EnumConfig.class)) {
            return processEnumComponent(component, builder);
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

    private ComponentBinding processEnumComponent(@Nonnull RecordComponent component,
                                                  @Nonnull ModConfigSpec.Builder builder) {
        EnumConfig annotation = component.getAnnotation(EnumConfig.class);
        ModConfigSpec.EnumValue<?> configValue = defineEnum(builder.comment(annotation.description()),
                annotation.identifier(),
                component.getType(),
                annotation.defaultValue());
        return new ComponentBinding(configValue, ConfigValueType.ENUM);
    }

    // The component's enum type is known only at runtime, so the type defineEnum binds cannot be named here.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ModConfigSpec.EnumValue<?> defineEnum(@Nonnull ModConfigSpec.Builder builder,
                                                         @Nonnull String identifier,
                                                         @Nonnull Class<?> enumType,
                                                         @Nonnull String defaultName) {
        return builder.defineEnum(identifier, Enum.valueOf((Class) enumType, defaultName));
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
        ConfigFactory.register(recordClass, () -> instantiate(recordClass, bindings));
    }

    /**
     * Instantiates the record from the loaded values of its bindings.
     */
    private <T> T instantiate(@Nonnull Class<T> recordClass, @Nonnull List<ComponentBinding> bindings) {
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
        public Object getValue() {
            Object rawValue = this.configValue.get();

            return switch (type) {
                case INTEGER -> ((Number) rawValue).intValue();
                case BOOLEAN -> (Boolean) rawValue;
                case DOUBLE -> ((Number) rawValue).doubleValue();
                case FLOAT -> ((Number) rawValue).floatValue();
                case STRING -> (String) rawValue;
                case MAP -> deserializeMap((String) rawValue);
                case ENUM -> (Enum<?>) rawValue;
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
        MAP,
        ENUM
    }

}
