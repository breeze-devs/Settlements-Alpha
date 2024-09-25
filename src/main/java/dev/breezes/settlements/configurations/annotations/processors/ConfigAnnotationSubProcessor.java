package dev.breezes.settlements.configurations.annotations.processors;

import net.minecraftforge.common.ForgeConfigSpec;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

public interface ConfigAnnotationSubProcessor<T extends Annotation> {

    Class<T> getAnnotationClass();

    /**
     * Build the configuration for the fields annotated with the specified annotation
     *
     * @return a runnable to populate the class variables from the config after config load complete
     */
    Runnable buildConfig(@Nonnull ForgeConfigSpec.Builder configBuilder, @Nonnull Set<Field> fields);

}
