package dev.breezes.settlements.configurations.annotations.processors;

import com.google.common.base.CaseFormat;
import dev.breezes.settlements.configurations.annotations.declarations.IntegerConfig;
import lombok.CustomLog;
import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CustomLog
public class IntegerConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<IntegerConfig> {

    @Override
    public Class<IntegerConfig> getAnnotationClass() {
        return IntegerConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ForgeConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields) {
        log.debug("Found %d fields annotated with %s".formatted(fields.size(), this.getAnnotationClass().getSimpleName()));

        Map<Field, ForgeConfigSpec.IntValue> configValues = new HashMap<>();
        for (Field field : fields) {
            IntegerConfig annotation = field.getAnnotation(IntegerConfig.class);
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            configBuilder.push(className);
            ForgeConfigSpec.IntValue configValue = configBuilder.comment(annotation.description())
                    .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            configBuilder.pop();

            log.debug("Built integer config entry '%s:%s' with value '%d' [%d, %d]".formatted(className, annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max()));
            configValues.put(field, configValue);
        }

        return () -> populateInts(configValues);
    }

    private void populateInts(@Nonnull Map<Field, ForgeConfigSpec.IntValue> integerConfigValues) {
        log.debug("Populating %d integer fields from config".formatted(integerConfigValues.size()));

        for (Map.Entry<Field, ForgeConfigSpec.IntValue> entry : integerConfigValues.entrySet()) {
            Field field = entry.getKey();
            ForgeConfigSpec.IntValue configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get()); // instance is null for static fields
                log.debug("Set field '%s.%s' to %d".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()));
            } catch (IllegalAccessException e) {
                log.error("Failed to set field '%s.%s' to %d".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()), e);
            }
        }
    }

}
