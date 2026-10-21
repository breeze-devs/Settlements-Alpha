package dev.breezes.settlements.infrastructure.logging;

import dev.breezes.settlements.infrastructure.config.annotations.booleans.BooleanConfig;
import dev.breezes.settlements.infrastructure.config.annotations.enums.EnumConfig;
import dev.breezes.settlements.shared.logging.LoggingPolicy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class LoggingConfigTest {

    @Test
    void declaredDefaults_resolveToTheDefaultPolicy() throws ReflectiveOperationException {
        // Counterexample: a default in the generated file changes while LoggingPolicy.DEFAULT does not, so loggers
        // follow one policy before the configuration resolves and another after it, with no setting edited.
        RecordComponent[] components = LoggingConfig.class.getRecordComponents();
        Class<?>[] types = Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Object[] defaults = Arrays.stream(components).map(LoggingConfigTest::declaredDefault).toArray();

        LoggingConfig declared = LoggingConfig.class.getDeclaredConstructor(types).newInstance(defaults);

        assertEquals(LoggingPolicy.DEFAULT, declared.toPolicy());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object declaredDefault(RecordComponent component) {
        if (component.isAnnotationPresent(BooleanConfig.class)) {
            return component.getAnnotation(BooleanConfig.class).defaultValue();
        }
        if (component.isAnnotationPresent(EnumConfig.class)) {
            return Enum.valueOf((Class) component.getType(), component.getAnnotation(EnumConfig.class).defaultValue());
        }
        return fail("No default reader for " + component.getName());
    }

}
