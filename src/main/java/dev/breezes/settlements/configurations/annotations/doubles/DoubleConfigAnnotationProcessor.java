package dev.breezes.settlements.configurations.annotations.doubles;

import com.google.common.base.CaseFormat;
import dev.breezes.settlements.configurations.annotations.ConfigAnnotationSubProcessor;
import dev.breezes.settlements.configurations.annotations.ConfigurationAnnotationRegistry;
import dev.breezes.settlements.configurations.annotations.ConfigurationType;
import lombok.CustomLog;
import net.neoforged.neoforge.common.ModConfigSpec;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CustomLog
public class DoubleConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<DoubleConfig> {

    @Override
    public Class<DoubleConfig> getAnnotationClass() {
        return DoubleConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ConfigurationAnnotationRegistry registry, @Nonnull Set<Field> fields) {
        log.debug("Found {} fields annotated with {}", fields.size(), this.getAnnotationClass().getSimpleName());

        Map<Field, ModConfigSpec.DoubleValue> configValues = new HashMap<>();
        for (Field field : fields.stream().sorted(Comparator.comparing(Field::getName)).toList()) {
            DoubleConfig annotation = field.getAnnotation(DoubleConfig.class);
            ConfigurationType type = annotation.type();
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            ModConfigSpec.Builder builder = registry.getBuilder(type.getFilePath(className));
            builder.push(className);
            ModConfigSpec.DoubleValue configValue = builder.comment(annotation.description())
                    .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            builder.pop();

            log.debug("Built double config entry '{}:{}' with value '{}' [{}, {}]", className, annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            configValues.put(field, configValue);
        }

        return () -> populateDoubles(configValues);
    }

    private void populateDoubles(@Nonnull Map<Field, ModConfigSpec.DoubleValue> doubleConfigValues) {
        log.debug("Populating {} double fields from config", doubleConfigValues.size());

        for (Map.Entry<Field, ModConfigSpec.DoubleValue> entry : doubleConfigValues.entrySet()) {
            Field field = entry.getKey();
            ModConfigSpec.DoubleValue configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get()); // instance is null for static fields
                log.debug("Set field '{}.{}' to {}", field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get());
            } catch (Exception e) {
                log.error("Failed to set field '{}.{}' to {}", field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get(), e);
            }
        }
    }

}
