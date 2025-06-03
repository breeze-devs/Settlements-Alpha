package dev.breezes.settlements.configurations.annotations.floats;

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
public class FloatConfigAnnotationProcessor implements ConfigAnnotationSubProcessor<FloatConfig> {

    @Override
    public Class<FloatConfig> getAnnotationClass() {
        return FloatConfig.class;
    }

    @Override
    public Runnable buildConfig(@Nonnull ConfigurationAnnotationRegistry registry, @Nonnull Set<Field> fields) {
        log.debug("Found {} fields annotated with {}", fields.size(), this.getAnnotationClass().getSimpleName());

        Map<Field, ModConfigSpec.DoubleValue> configValues = new HashMap<>();
        for (Field field : fields.stream().sorted(Comparator.comparing(Field::getName)).toList()) {
            FloatConfig annotation = field.getAnnotation(FloatConfig.class);
            ConfigurationType type = annotation.type();
            String className = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getDeclaringClass().getSimpleName());

            ModConfigSpec.Builder builder = registry.getBuilder(type.getFilePath(className));
            builder.push(className);
            ModConfigSpec.DoubleValue configValue = builder.comment(annotation.description())
                    .defineInRange(annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            builder.pop();

            log.debug("Built float config entry '{}:{}' with value '{}' [{}, {}]", className, annotation.identifier(), annotation.defaultValue(), annotation.min(), annotation.max());
            configValues.put(field, configValue);
        }

        return () -> populateFloats(configValues);
    }

    private void populateFloats(@Nonnull Map<Field, ModConfigSpec.DoubleValue> floatConfigValues) {
        log.debug("Populating {} float fields from config", floatConfigValues.size());

        for (Map.Entry<Field, ModConfigSpec.DoubleValue> entry : floatConfigValues.entrySet()) {
            Field field = entry.getKey();
            ModConfigSpec.DoubleValue configValue = entry.getValue();
            try {
                field.setAccessible(true);
                field.set(null, configValue.get().floatValue()); // instance is null for static fields
                log.debug("Set field '{}.{}' to {}", field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get());
            } catch (Exception e) {
                log.error("Failed to set field '{}.{}' to {}", field.getDeclaringClass().getSimpleName(), field.getName(), configValue.get(), e);
            }
        }
    }

}
