package dev.breezes.settlements.configurations.annotations.processors;

import com.google.common.base.CaseFormat;
import dev.breezes.settlements.configurations.annotations.declarations.FloatConfig;
import lombok.CustomLog;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CustomLog
public class FloatConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<FloatConfig> {

    @Override
    public Class<FloatConfig> getAnnotationClass() {
        return FloatConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ForgeConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields) {
        log.debug("Found %d fields annotated with %s".formatted(fields.size(), this.getAnnotationClass().getSimpleName()));

        Map<Field, ForgeConfigSpec.FloatValue> configValues = new HashMap<>();
        for (Field field : fields) {
            FloatConfig annotation = field.getAnnotation(FloatConfig.class);
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            configBuilder.push(className);
            ForgeConfigSpec.FloatValue configValue = configBuilder.comment(annotation.description())
                    .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            configBuilder.pop();

            log.debug("Built float config entry '%s:%s' with value '%f' [%f, %f]".formatted(className, annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max()));
            configValues.put(field, configValue);
        }

        return () -> populateFloats(configValues);
    }

    private void populateFloats(@Nonnull Map<Field, ForgeConfigSpec.FloatValue> floatConfigValues) {
        log.debug("Populating %d float fields from config".formatted(floatConfigValues.size()));

        for (Map.Entry<Field, ForgeConfigSpec.FloatValue> entry : floatConfigValues.entrySet()) {
            Field field = entry.getKey();
            ForgeConfigSpec.FloatValue configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get()); // instance is null for static fields
                log.debug("Set field '%s.%s' to %f".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()));
            } catch (IllegalAccessException e) {
                log.error("Failed to set field '%s.%s' to %f".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()), e);
            }
        }
    }

}
