package dev.breezes.settlements.configurations.annotations;

import dev.breezes.settlements.configurations.annotations.booleans.BooleanConfigAnnotationProcessor;
import dev.breezes.settlements.configurations.annotations.doubles.DoubleConfigAnnotationProcessor;
import dev.breezes.settlements.configurations.annotations.floats.FloatConfigAnnotationProcessor;
import dev.breezes.settlements.configurations.annotations.integers.IntegerConfigAnnotationProcessor;
import dev.breezes.settlements.configurations.annotations.maps.MapConfigAnnotationProcessor;
import dev.breezes.settlements.configurations.annotations.strings.StringConfigAnnotationProcessor;
import dev.breezes.settlements.event.CommonModEvents;
import lombok.CustomLog;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.ModFileScanData;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CustomLog
public class ConfigAnnotationProcessor {

    public static void process(@Nonnull ModContainer container) {
        log.info("Processing mod config annotations");

        // Scan the package for fields annotated with the specified annotations
        ModFileScanData scanner = ModLoadingContext.get()
                .getActiveContainer()
                .getModInfo()
                .getOwningFile()
                .getFile()
                .getScanResult();

        List<ConfigAnnotationSubProcessor<?>> processors = List.of(
                new BooleanConfigAnnotationProcessor(),
                new DoubleConfigAnnotationProcessor(),
                new FloatConfigAnnotationProcessor(),
                new IntegerConfigAnnotationProcessor(),
                new StringConfigAnnotationProcessor(),
                new MapConfigAnnotationProcessor()
        );

        List<Runnable> tasks = new ArrayList<>();
        ConfigurationAnnotationRegistry registry = new ConfigurationAnnotationRegistry();
        for (ConfigAnnotationSubProcessor<?> processor : processors) {
            Set<Field> fields = scanner.getAnnotatedBy(processor.getAnnotationClass(), ElementType.FIELD)
                    .map(ConfigAnnotationProcessor::getFieldFromAnnotationData)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            tasks.add(processor.buildConfig(registry, fields));
        }

        CommonModEvents.LOAD_COMPLETE_TASKS.addAll(tasks);

        log.info("Compiling {} config files", registry.getFileBuilderMap().size());
        for (Map.Entry<String, ModConfigSpec.Builder> entry : registry.getFileBuilderMap().entrySet()) {
            log.info("- {}", entry.getKey());
            ModConfigSpec spec = entry.getValue().build();
            container.registerConfig(ModConfig.Type.COMMON, spec, entry.getKey());
        }
    }

    private static Optional<Field> getFieldFromAnnotationData(@Nonnull ModFileScanData.AnnotationData annotationData) {
        try {
            return Optional.of(Class.forName(annotationData.clazz().getClassName())
                    .getDeclaredField(annotationData.memberName()));
        } catch (Exception e) {
            log.error("Failed to process annotation in class '{}' field '{}'", annotationData.clazz().getClassName(), annotationData.memberName(), e);
            return Optional.empty();
        }
    }

}
