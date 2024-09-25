package dev.breezes.settlements.configurations.annotations.processors;

import com.google.common.base.CaseFormat;
import dev.breezes.settlements.configurations.annotations.declarations.BooleanConfig;
import lombok.CustomLog;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CustomLog
public class BooleanConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<BooleanConfig> {

    @Override
    public Class<BooleanConfig> getAnnotationClass() {
        return BooleanConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ForgeConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields) {
        log.debug("Found %d fields annotated with %s".formatted(fields.size(), this.getAnnotationClass().getSimpleName()));

        Map<Field, ForgeConfigSpec.BooleanValue> configValues = new HashMap<>();
        for (Field field : fields) {
            BooleanConfig annotation = field.getAnnotation(BooleanConfig.class);
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            configBuilder.push(className);
            ForgeConfigSpec.BooleanValue configValue = configBuilder.comment(annotation.description())
                    .define(annotation.identifier(), annotation.defaultValue());
            configBuilder.pop();

            log.debug("Built boolean config entry '%s:%s' with value '%b'".formatted(className, annotation.identifier(), annotation.defaultValue()));
            configValues.put(field, configValue);
        }

        return () -> populateBooleans(configValues);
    }

    private void populateBooleans(@Nonnull Map<Field, ForgeConfigSpec.BooleanValue> booleanConfigValues) {
        log.debug("Populating %d boolean fields from config".formatted(booleanConfigValues.size()));

        for (Map.Entry<Field, ForgeConfigSpec.BooleanValue> entry : booleanConfigValues.entrySet()) {
            Field field = entry.getKey();
            ForgeConfigSpec.BooleanValue configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get()); // instance is null for static fields
                log.debug("Set field '%s.%s' to %b".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()));
            } catch (IllegalAccessException e) {
                log.error("Failed to set field '%s.%s' to %b".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()), e);
            }
        }
    }

}
