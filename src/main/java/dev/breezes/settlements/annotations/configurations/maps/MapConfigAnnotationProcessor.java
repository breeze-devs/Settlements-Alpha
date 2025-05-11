package dev.breezes.settlements.annotations.configurations.maps;

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.mojang.authlib.minecraft.client.ObjectMapper;
import dev.breezes.settlements.annotations.configurations.ConfigAnnotationSubProcessor;
import dev.breezes.settlements.annotations.configurations.maps.deserializers.MapConfigDeserializer;
import dev.breezes.settlements.annotations.configurations.maps.deserializers.MapConfigDeserializers;
import dev.breezes.settlements.util.crash.CrashUtil;
import dev.breezes.settlements.util.crash.report.ConfigLoadingCrashReport;
import lombok.CustomLog;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.*;

@CustomLog
public class MapConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<MapConfig> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(new Gson());

    @Override
    public Class<MapConfig> getAnnotationClass() {
        return MapConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ModConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields) {
        log.debug("Found {} fields annotated with {}", fields.size(), this.getAnnotationClass().getSimpleName());

        List<ConfigEntry> configValues = new ArrayList<>();
        for (Field field : fields.stream().sorted(Comparator.comparing(Field::getName)).toList()) {
            MapConfig annotation = field.getAnnotation(MapConfig.class);
            Map<String, String> map = new HashMap<>();
            for (MapEntry entry : annotation.defaultValue()) {
                map.put(entry.key(), entry.value());
            }
            String serializedMap = OBJECT_MAPPER.writeValueAsString(map).replace("\"", "'");

            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());
            configBuilder.push(className);
            ModConfigSpec.ConfigValue<String> configValue = configBuilder.comment(annotation.description())
                    .define(annotation.identifier(), serializedMap);
            configBuilder.pop();

            log.debug("Built map config entry '{}:{}': {}", className, annotation.identifier(), serializedMap);
            configValues.add(new ConfigEntry(field, configValue, annotation.deserializer()));
        }

        return () -> populateMaps(configValues);
    }

    private void populateMaps(@Nonnull List<ConfigEntry> mapConfigValues) {
        log.debug("Populating {} map fields from config", mapConfigValues.size());

        for (ConfigEntry configEntry : mapConfigValues) {
            Field field = configEntry.field();
            ModConfigSpec.ConfigValue<String> configValue = configEntry.value();
            try {
                // Deserialize JSON to map
                Map<String, String> rawMap = OBJECT_MAPPER.readValue(configValue.get(), Map.class);
                MapConfigDeserializer<?> valueDeserializer = MapConfigDeserializers.getDeserializer(configEntry.valueDeserializer());
                Map<String, Object> deserializedMap = new HashMap<>();
                for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                    deserializedMap.put(entry.getKey(), valueDeserializer.deserialize(entry.getValue()));
                }

                // Reflection
                field.setAccessible(true);
                field.set(null, deserializedMap); // instance is null for static fields
                log.debug("Set field '{}.{}' to {}", field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get());
            } catch (Exception e) {
                String errorMessage = "Failed to set field '%s.%s' to %s".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get());
                CrashUtil.crash(new ConfigLoadingCrashReport(new ConfigLoadingException(errorMessage, e)));
            }
        }
    }

    private record ConfigEntry(Field field, ModConfigSpec.ConfigValue<String> value, String valueDeserializer) {
    }

}
