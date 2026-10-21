package dev.breezes.settlements.infrastructure.config.annotations;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.bootstrap.event.CommonModEvents;
import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.config.annotations.doubles.DoubleConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.config.annotations.floats.FloatConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.config.annotations.integers.IntegerConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.config.annotations.maps.MapConfigAnnotationProcessor;
import dev.breezes.settlements.infrastructure.config.annotations.strings.StringConfigAnnotationProcessor;
import lombok.CustomLog;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.apache.commons.lang3.Validate;

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

    /**
     * Registers a record configuration of a STARTUP type and returns the record holding the loaded value from its file.
     * The values are read once, so edits to the file take effect at the next game start.
     * <p>
     * process skips these records, so each is registered only through this method.
     *
     * @throws IllegalArgumentException if the record's configuration type is not a STARTUP type
     */
    public static <T extends Record> T registerStartupConfig(@Nonnull ModContainer container, @Nonnull Class<T> recordClass) {
        return new RecordConfigProcessor().registerAtStartup(container, recordClass);
    }

    public static void process() {
        log.debug("Processing mod config annotations");

        ModContainer container = ModList.get()
                .getModContainerById(SettlementsMod.MOD_ID)
                .orElseThrow();

        // Scan the package for fields annotated with the specified annotations
        ModFileScanData scanner = container.getModInfo()
                .getOwningFile()
                .getFile()
                .getScanResult();

        ConfigurationAnnotationRegistry registry = new ConfigurationAnnotationRegistry();
        List<Runnable> tasks = new ArrayList<>();

        log.debug("Processing field-based configurations...");
        List<ConfigAnnotationSubProcessor<?>> processors = List.of(
                new BooleanConfigAnnotationProcessor(),
                new DoubleConfigAnnotationProcessor(),
                new FloatConfigAnnotationProcessor(),
                new IntegerConfigAnnotationProcessor(),
                new StringConfigAnnotationProcessor(),
                new MapConfigAnnotationProcessor());

        for (ConfigAnnotationSubProcessor<?> processor : processors) {
            Set<Field> fields = scanner.getAnnotatedBy(processor.getAnnotationClass(), ElementType.FIELD)
                    .map(ConfigAnnotationProcessor::getFieldFromAnnotationData)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(field -> !field.getDeclaringClass().isRecord()) // Exclude Record components from legacy
                    // processors
                    .collect(Collectors.toSet());
            if (!fields.isEmpty()) {
                log.debug("- Found {} @{} fields", fields.size(), processor.getAnnotationClass().getSimpleName());
                tasks.add(processor.buildConfig(registry, fields));
            }
        }

        log.debug("Processing Record-based configurations...");
        RecordConfigProcessor recordProcessor = new RecordConfigProcessor();

        Set<Class<?>> behaviorConfigRecords = scanner.getAnnotatedBy(BehaviorConfig.class, ElementType.TYPE)
                .map(ConfigAnnotationProcessor::getClassFromAnnotationData)
                .filter(Optional::isPresent)
                .map(Optional::get)
                // registerStartupConfig registers these; registering one here again would conflict over its file.
                .filter(recordClass -> recordClass.getAnnotation(BehaviorConfig.class).type().getModConfigType() != ModConfig.Type.STARTUP)
                .collect(Collectors.toSet());

        log.debug("- Found {} @BehaviorConfig records", behaviorConfigRecords.size());
        for (Class<?> recordClass : behaviorConfigRecords) {
            Runnable task = recordProcessor.process(recordClass, registry);
            tasks.add(task);
        }

        log.debug("Compiling {} config files", registry.getFileBuilderMap().size());
        for (Map.Entry<ConfigurationType, ModConfigSpec.Builder> entry : registry.getFileBuilderMap().entrySet()) {
            ConfigurationType type = entry.getKey();
            String filePath = type.getFilePath();
            // Only static fields reach here with a STARTUP type, since the record scan skips STARTUP records. Their
            // values would be populated at load-complete, too late for a STARTUP file, and registering the file again
            // would conflict with the record registerStartupConfig already registered for it.
            Validate.validState(type.getModConfigType() != ModConfig.Type.STARTUP,
                    "%s is a STARTUP configuration, so its settings must be components of a record registered through "
                            + "registerStartupConfig, not static fields", filePath);
            log.debug("  - {}", filePath);
            ModConfigSpec spec = entry.getValue().build();
            container.registerConfig(type.getModConfigType(), spec, filePath);
        }

        // Schedule post-load tasks (populate static fields and register factories)
        CommonModEvents.LOAD_COMPLETE_TASKS.addAll(tasks);
        log.info("Config processed: {} @BehaviorConfig records, {} config files registered, {} load-complete tasks scheduled",
                behaviorConfigRecords.size(), registry.getFileBuilderMap().size(), tasks.size());
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

    private static Optional<Class<?>> getClassFromAnnotationData(
            @Nonnull ModFileScanData.AnnotationData annotationData) {
        try {
            return Optional.of(Class.forName(annotationData.clazz().getClassName()));
        } catch (Exception e) {
            log.error("Failed to load class '{}' for annotation processing", annotationData.clazz().getClassName(), e);
            return Optional.empty();
        }
    }

}
