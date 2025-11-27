# Configuration Architecture Options

## Context
We need to decouple behavior logic from static configuration fields while maintaining integration with the NeoForge configuration loader. The current system uses an annotation processor ConfigAnnotationProcessor that scans for annotated static fields and populates them via reflection.

## Option 1: The "Shadow Registry" Pattern (Low Risk, Medium Reward)

**Concept**: Separate the configuration *storage* from the configuration *definition*.

Create a static "Registry" class that holds the annotated fields. The Behavior class remains pure and accepts a configuration object (Record) in its constructor.

**Structure:**
```java
// 1. The Pure Config Record (What the Behavior sees)
public record ShearSheepConfig(CooldownConfig cooldown, int scanRange) {}

// 2. The Static Registry (What NeoForge sees)
public class ShearSheepConfigRegistry {
    @IntegerConfig(...) static int cooldownMin;
    @IntegerConfig(...) static int cooldownMax;
    
    public static ShearSheepConfig toRecord() {
        return new ShearSheepConfig(new CooldownConfig(cooldownMin, cooldownMax), ...);
    }
}

// 3. The Behavior (Pure)
public class ShearSheepBehaviorV2 {
    public ShearSheepBehaviorV2(ShearSheepConfig config) { ... }
}
```

**Pros:**

*   **Decoupling**: Behavior logic is 100% free of static config state.
*   **Testability**: Easy to unit test behaviors with mock records.
*   **Minimal Framework Change**: Works with the *existing* annotation processor.

**Cons:**

*   **Boilerplate**: Requires maintaining two files (Record + Registry) for every behavior.
*   **Indirection**: Developers must jump between files to see config definitions.

## Option 2: The "Active Record" Configuration (Medium Risk, High Reward)

**Concept**: The Configuration class *is* the definition. It uses instance fields for the record, but the framework manages the population behind the scenes.
Since NeoForge configs are loaded once at startup, we can bind them to a singleton instance or a Supplier.

**Structure:**
```java
// 1. The Configuration Class
@Configuration(name = "shear_sheep")
public class ShearSheepConfig {
    @ConfigVal(default = 10) private Supplier<Integer> cooldownMin;
    @ConfigVal(default = 20) private Supplier<Integer> cooldownMax;

    public CooldownConfig getCooldown() {
        return new CooldownConfig(cooldownMin.get(), cooldownMax.get());
    }
}

// 2. The Behavior
public class ShearSheepBehaviorV2 {
    public ShearSheepBehaviorV2(ShearSheepConfig config) { ... }
}
```

**Pros:**

*   **Co-location**: Config definition and access are in one place.
*   **Dynamic**: `Supplier<T>` allows for runtime config changes (if NeoForge supports hot-reloading).

**Cons:**

*   **Refactoring**: Requires rewriting the Annotation Processor to handle Classes and Suppliers instead of just static primitive fields.

## Option 3: The "Runtime Record Binding" (High Effort, Maximum Cleanliness) - **RECOMMENDED**

**Concept**: Use Java Records as the *source of truth*. The Annotation Processor scans the Record components, builds the NeoForge spec, and automatically creates a binding that constructs the Record.

**Structure:**
```java
// 1. The Definition
@BehaviorConfig("shear_sheep")
public record ShearSheepConfig(
    @ConfigItem(min=1, max=100) int cooldownMin,
    @ConfigItem(min=1, max=100) int cooldownMax
) {}

// 2. The Usage
ShearSheepConfig config = ConfigFactory.create(ShearSheepConfig.class);
new ShearSheepBehaviorV2(config);
```

**Mechanism:**

1.  **Processor**: Scans `ShearSheepConfig` record.
2.  **Spec Building**: Builds `ModConfigSpec` using record component names/annotations.
3.  **Binding**: Stores `ModConfigSpec.ConfigValue<T>` references in a map keyed by the Record Class.
4.  **Factory**: When `ConfigFactory.create(ShearSheepConfig.class)` is called, it looks up the values and invokes the Record constructor.

**Pros:**

*   **Zero Boilerplate**: Just define a Record.
*   **Immutability**: Configs are true immutable snapshots.
*   **Clean Architecture**: Separation of concerns is perfect. The framework handles the "dirty" work of bridging NeoForge to POJOs.

**Cons:**
*   **Initial Investment**: Requires significant changes to ConfigAnnotationProcessor to support Records and runtime instantiation.

## Recommendation

I recommend **Option 3 (Runtime Record Binding)**.

**Reasoning:**

1.  **Clean Code**: It aligns perfectly with modern Java (Records) and Clean Architecture.
2.  **Developer Experience**: Adding a new config is as simple as adding a field to a Record.
3.  **Scalability**: It eliminates the manual mapping code required in Option 1 and the `Supplier` boilerplate of Option 2.

**Implementation Plan for Option 3:**

1.  Create a generic `RecordConfigProcessor`.
2.  It uses reflection to inspect Record components.
3.  It builds a `ModConfigSpec` mirroring the Record structure.
4.  It registers a `Function<Void, T>` that constructs the Record from the spec values.
