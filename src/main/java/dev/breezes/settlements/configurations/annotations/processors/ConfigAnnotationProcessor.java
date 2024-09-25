package dev.breezes.settlements.configurations.annotations.processors;

import dev.breezes.settlements.event.CommonModEvents;
import lombok.CustomLog;
import net.minecraftforge.common.ForgeConfigSpec;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CustomLog
public class ConfigAnnotationProcessor {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static ForgeConfigSpec SPEC;

    public static void process() {
        log.info("Processing mod config annotations");

        // Scan the package for fields annotated with the specified annotations
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage("dev.breezes.settlements"))
                .setScanners(Scanners.FieldsAnnotated));

        List<ConfigAnnotationSubProcessor<?>> processors = List.of(
                new BooleanConfigAnnotationProcessor(),
                new DoubleConfigAnnotationProcessor(),
                new FloatConfigAnnotationProcessor(),
                new IntegerConfigAnnotationProcessor()
        );
        List<Runnable> tasks = new ArrayList<>();
        for (ConfigAnnotationSubProcessor<?> processor : processors) {
            Set<Field> fields = reflections.getFieldsAnnotatedWith(processor.getAnnotationClass());
            tasks.add(processor.buildConfig(BUILDER, fields));
        }

        SPEC = BUILDER.build();
        CommonModEvents.loadCompleteTasks.addAll(tasks);
    }


}
