# Configuration Refactoring Results: Runtime Record Binding

## Overview
We have successfully refactored the configuration system to use **Java Records** and a **Runtime Record Binding** architecture. This modernizes the codebase, improves type safety, and decouples behaviors from static configuration state.

### Key Benefits

*   **Immutability**: Configurations are now immutable Records, preventing accidental runtime modification.
*   **Type Safety**: Leveraging Java's type system and Record components.
*   **Clean Architecture**: Behaviors receive their configuration via dependency injection, making them easier to test and maintain.
*   **Zero Boilerplate**: No more manual `ModConfigSpec` building or static field assignment.

## Architecture

### 1. The Config Record
Configurations are defined as Java Records annotated with `@BehaviorConfig`. Each component of the record represents a configuration value and is annotated with a specific config annotation (e.g., `@IntegerConfig`, `@BooleanConfig`).

```java
@BehaviorConfig(name = "shear_sheep", type = ConfigurationType.BEHAVIOR)
public record ShearSheepConfig(
    @IntegerConfig(
        identifier = "precondition_check_cooldown_min",
        description = "Minimum ticks before checking preconditions again",
        defaultValue = 10,
        min = 1,
        max = 1200
    )
    int preconditionCheckCooldownMin,
    
    // ... other components
) {}
```

### 2. The Processor

At startup, the `RecordConfigProcessor`:

1.  Scans for `@BehaviorConfig` records.
2.  Inspects record components and their annotations.
3.  Builds the NeoForge `ModConfigSpec`.
4.  Registers a factory in `ConfigFactory` that knows how to instantiate the record from the loaded config values.

### 3. The Registry

The `ConfigFactory` is a thread-safe singleton that:

*   Caches instantiated config Records.
*   Provides access to configurations via `ConfigFactory.get(Class<T>)`.
*   Handles eager instantiation during mod loading.

### 4. Usage in Behaviors

Behaviors no longer have static config fields. Instead, they accept the config Record in their constructor.

```java
public class ShearSheepBehaviorV2 extends BaseVillagerBehavior {
    private final ShearSheepConfig config;

    public ShearSheepBehaviorV2(ShearSheepConfig config) {
        super(...);
        this.config = config;
        // Access values: config.preconditionCheckCooldownMin()
    }
}
```

## Migration Guide

To migrate an existing behavior to the new system:

1.  **Create a Config Record**:
    *   Create a new `record` in the same package as the behavior (e.g., `MyBehaviorConfig`).
    *   Annotate it with `@BehaviorConfig`.
    *   Move all `@IntegerConfig`, `@BooleanConfig`, etc., fields from the behavior to the record as components.

2.  **Update the Behavior**:
    *   Remove all static config fields.
    *   Add a constructor parameter for the config record.
    *   Store the config in a `private final` field.
    *   Replace all static field references with record accessor calls (e.g., `MyBehavior.SCAN_RANGE` -> `config.scanRange()`).

3.  **Update Registration**:
    *   In `CustomBehaviorPackages`, update the behavior instantiation to use `ConfigFactory`.
    *   Example: `new MyBehavior(ConfigFactory.create(MyBehaviorConfig.class))`

## Verification Results

### Game Startup

*   **Status**: ✅ Success
*   **Details**: The game starts without crashing. The `RecordConfigProcessor` successfully processes `ShearSheepConfig` and registers it.

### Config Generation

*   **Status**: ✅ Success
*   **File**: `run/config/settlements/behaviors/shear_sheep.toml`
*   **Content**: The file is generated with the correct structure and default values.

### In-Game Functionality

*   **Status**: ✅ Success
*   **Details**: `ShearSheepBehaviorV2` functions correctly using the injected configuration.

### Legacy Compatibility

*   **Status**: ✅ Success
*   **Details**: A filter was added to `ConfigAnnotationProcessor` to ensure legacy processors ignore Record components, preventing crashes during the migration phase.
