package dev.breezes.settlements.annotations.configurations.processors;

import com.google.common.base.CaseFormat;
import dev.breezes.settlements.annotations.configurations.declarations.StringConfig;
import lombok.CustomLog;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CustomLog
public class StringConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<StringConfig> {

    @Override
    public Class<StringConfig> getAnnotationClass() {
        return StringConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ModConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields) {
        log.debug("Found %d fields annotated with %s".formatted(fields.size(), this.getAnnotationClass().getSimpleName()));

        Map<Field, ModConfigSpec.ConfigValue<String>> configValues = new HashMap<>();
        for (Field field : fields.stream().sorted(Comparator.comparing(Field::getName)).toList()) {
            StringConfig annotation = field.getAnnotation(StringConfig.class);
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            configBuilder.push(className);
            ModConfigSpec.ConfigValue<String> configValue = configBuilder.comment(annotation.description())
                    .define(annotation.identifier(), annotation.defaultValue());
            configBuilder.pop();

            log.debug("Built string config entry '%s:%s' with value '%b'".formatted(className, annotation.identifier(), annotation.defaultValue()));
            configValues.put(field, configValue);
        }

        return () -> populateStrings(configValues);
    }

    private void populateStrings(@Nonnull Map<Field, ModConfigSpec.ConfigValue<String>> stringConfigValues) {
        log.debug("Populating %d string fields from config".formatted(stringConfigValues.size()));

        for (Map.Entry<Field, ModConfigSpec.ConfigValue<String>> entry : stringConfigValues.entrySet()) {
            Field field = entry.getKey();
            ModConfigSpec.ConfigValue<String> configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get()); // instance is null for static fields
                log.debug("Set field '%s.%s' to '%s'".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()));
            } catch (IllegalAccessException e) {
                log.error("Failed to set field '%s.%s' to %b".formatted(field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get()), e);
            }
        }
    }

}
