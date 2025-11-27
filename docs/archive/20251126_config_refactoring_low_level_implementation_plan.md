# Low-Level Implementation Plan: Behavior Configuration Refactoring
**Date:** 2025-11-26
**Status:** Approved for Implementation

## 1. Context & Objective

### Context
Currently, our behavior classes (e.g., `ShearSheepBehaviorV2`) contain **static fields** annotated with configuration metadata (e.g., `@IntegerConfig`). These fields are populated via reflection by the `ConfigAnnotationProcessor` at startup.

**Problems with current approach:**

1.  **Tight Coupling**: Behavior logic is coupled to the configuration loading mechanism.
2.  **Testability**: Static fields make it hard to test behaviors with different configurations in parallel.
3.  **Verbosity**: Behavior classes are cluttered with config definitions.

### Objective
Refactor the system to use **Java Records** as configuration objects. These records will be:

1.  Defined separately from the behavior logic.
2.  Instantiated and populated by the framework at runtime.
3.  Injected into the Behavior's constructor.

**Target Architecture:** "Runtime Record Binding"
We will use reflection to inspect a Record's components, build the NeoForge configuration specification, and then create a factory that can instantiate that Record with values loaded from the config file.

---

## 2. Architecture Overview

### The Components

1.  **`@BehaviorConfig`**: A new annotation to mark a Record as a configuration object.
2.  **`ConfigFactory`**: A singleton registry that holds `Supplier<Record>` for each registered config class.
3.  **`RecordConfigProcessor`**: The workhorse. It scans `@BehaviorConfig` records, builds the NeoForge `ModConfigSpec`, and registers the binding in `ConfigFactory`.
4.  **The Record**: A simple Java Record (e.g., `ShearSheepConfig`) with annotated components.
5.  **The Behavior**: A class that takes the Record in its constructor.

### Data Flow

1.  **Startup**: `ConfigAnnotationProcessor` finds `@BehaviorConfig` classes.
2.  **Processing**: It delegates to `RecordConfigProcessor`.
3.  **Building**: `RecordConfigProcessor` iterates over record components (fields), creating `ModConfigSpec.ConfigValue<?>` objects for each.
4.  **Registration**: It creates a `Supplier` that calls the Record's constructor using values from the `ConfigValue` objects and registers it in `ConfigFactory`.
5.  **Runtime**: When a Behavior is created, we call `ConfigFactory.create(MyConfig.class)` to get the populated config object.

---

## 3. Implementation Steps

### Phase 1: Core Infrastructure

#### Step 1.1: Create `ConfigFactory`
**Location**: `src/main/java/dev/breezes/settlements/configurations/ConfigFactory.java`

This class is a simple registry.

```java
public class ConfigFactory {
    private static final Map<Class<?>, Supplier<?>> FACTORIES = new HashMap<>();

    public static <T> void register(Class<T> type, Supplier<T> factory) {
        FACTORIES.put(type, factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> type) {
        Supplier<?> factory = FACTORIES.get(type);
        if (factory == null) throw new IllegalStateException("No config registered for " + type.getName());
        return (T) factory.get();
    }
}
```

#### Step 1.2: Update Annotations
**Location**: `src/main/java/dev/breezes/settlements/configurations/annotations/*`

We need to allow our config annotations to be placed on **Record Components**.
Update `@IntegerConfig`, `@BooleanConfig`, `@DoubleConfig`, `@FloatConfig`, `@StringConfig`, `@MapConfig`.

**Change:**
```java
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT}) // Add RECORD_COMPONENT
public @interface IntegerConfig { ... }
```

#### Step 1.3: Create `RecordConfigProcessor`
**Location**: `src/main/java/dev/breezes/settlements/configurations/annotations/RecordConfigProcessor.java`

This is the most complex part. It needs to handle all types.

**Pseudocode Logic:**
```java
public class RecordConfigProcessor {
    public void process(Class<?> recordClass, ModConfigSpec.Builder builder) {
        List<Object> configValues = new ArrayList<>();
        
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.isAnnotationPresent(IntegerConfig.class)) {
                // Build IntValue
                IntegerConfig ann = component.getAnnotation(IntegerConfig.class);
                configValues.add(builder.defineInRange(ann.identifier(), ann.defaultValue(), ann.min(), ann.max()));
            } 
            else if (component.isAnnotationPresent(BooleanConfig.class)) {
                // Build BooleanValue
                // ...
            }
            // ... handle Double, Float, String, Map ...
        }

        // Register the supplier
        ConfigFactory.register(recordClass, () -> {
            // Reflectively invoke constructor
            Object[] args = configValues.stream()
                .map(val -> ((ModConfigSpec.ConfigValue<?>)val).get())
                .toArray();
            return invokeCanonicalConstructor(recordClass, args);
        });
    }
}
```

**Critical Detail**: For `MapConfig`, you will likely need to bind it to a `ConfigValue<String>` (since NeoForge doesn't natively support complex Maps well in TOML without extra work) and deserialize it inside the Supplier lambda using the specified deserializer.

---

### Phase 2: Pilot Implementation (Shear Sheep)

#### Step 2.1: Create the Config Record
**Location**: `src/main/java/dev/breezes/settlements/models/behaviors/ShearSheepConfig.java`

```java
@BehaviorConfig(name = "shear_sheep")
public record ShearSheepConfig(
    @IntegerConfig(...) int cooldownMin,
    @IntegerConfig(...) int cooldownMax,
    @IntegerConfig(...) int scanRangeHorizontal,
    @MapConfig(...) Map<String, Integer> expertiseLimits
) {}
```

#### Step 2.2: Refactor the Behavior
**Location**: `src/main/java/dev/breezes/settlements/models/behaviors/ShearSheepBehaviorV2.java`

1.  **Remove** all static fields.
2.  **Update Constructor**:
    ```java
    public ShearSheepBehaviorV2(ShearSheepConfig config) {
        super(log, 
              RandomRangeTickable.of(Ticks.of(config.cooldownMin()), Ticks.of(config.cooldownMax())), 
              ...);
        // ... use config.scanRangeHorizontal() etc.
    }
    ```

#### Step 2.3: Update Registration
**Location**: `src/main/java/dev/breezes/settlements/models/brain/CustomBehaviorPackages.java`

```java
// Old
// new ShearSheepBehaviorV2()

// New
new ShearSheepBehaviorV2(ConfigFactory.create(ShearSheepConfig.class))
```

---

## 4. Verification Checklist

1.  **Compilation**: Ensure no compile errors after annotation target changes.
2.  **Startup**: Run the game. Check logs for "Registered config for ShearSheepConfig".
3.  **Config File**: Check `settlements-common.toml` (or equivalent). Ensure `shear_sheep` section exists with correct values.
4.  **Behavior**: Spawn a shepherd. Verify they still shear sheep.
5.  **Hot Reload (Optional)**: If NeoForge supports it, change the config file while running and verify the next `ConfigFactory.create()` call returns the new values (or if the `ConfigValue` updates dynamically). *Note: Since we create the behavior once, hot reload might require behavior restart, which is acceptable.*
