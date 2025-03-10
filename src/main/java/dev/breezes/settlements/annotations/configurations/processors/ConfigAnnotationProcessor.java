package dev.breezes.settlements.annotations.configurations.processors;

import dev.breezes.settlements.event.CommonModEvents;
import lombok.CustomLog;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforgespi.language.ModFileScanData;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@CustomLog
public class ConfigAnnotationProcessor {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static ModConfigSpec SPEC;

    public static void process() {
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
                new StringConfigAnnotationProcessor()
        );
        List<Runnable> tasks = new ArrayList<>();
        for (ConfigAnnotationSubProcessor<?> processor : processors) {
            Set<Field> fields = scanner.getAnnotatedBy(processor.getAnnotationClass(), ElementType.FIELD)
                    .map(ConfigAnnotationProcessor::getFieldFromAnnotationData)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
            tasks.add(processor.buildConfig(BUILDER, fields));
        }

        SPEC = BUILDER.build();
        CommonModEvents.loadCompleteTasks.addAll(tasks);
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
