# Behavior System

This document covers how villager behaviors are registered, resolved, and wired into the Minecraft brain. For the
Dagger fundamentals that underpin this system, see [Dagger Guide](dagger_guide.md).

---

## Overview

Behaviors are the core gameplay feature of Settlements. Each behavior (fishing, smelting, shearing sheep, etc.) is a
stateful use case that ticks every server tick for the villager that owns it. The behavior system has three layers:

1. **Registration** — `BehaviorModule` declares which behaviors exist and which professions use them.
2. **Resolution** — `BehaviorPackageResolver` groups registrations by profession and resolves them per villager.
3. **Consumption** — `BaseVillager.registerCustomGoals()` wires resolved behaviors into Minecraft's brain system.

Sensors complete the read/write loop around memories:

- Sensors write `MemoryModuleType` values into the brain.
- Behaviors read those memories as preconditions or live inputs.

For V1, custom sensors should be vanilla `Sensor<Villager>` subclasses registered through `SensorTypeRegistry` and
added to `BaseVillager.SENSOR_TYPES`. The older `AbstractSensor` framework under `application/ai/sensors/` is not
currently wired into the brain tick path.

The `application/ai/sensors/AbstractSensor` framework currently present in the codebase is a parallel design that is
not wired into `BaseVillager` and is therefore dormant. Do not add new sensors to that framework unless there is an
explicit plan to wire and tick it.

---

```
BehaviorModule                    BehaviorPackageResolver              BaseVillager
┌───────────────────┐            ┌───────────────────────┐             ┌─────────────────┐
│ @Provides @IntoSet│            │ @Inject constructor   │             │ registerCustom  │
│ BehaviorRegistra- │──→ Set ──→ │ groups by profession  │──resolve()─→│ Goals()         │
│ tion              │            │ at component creation │             │ adds to brain   │
└───────────────────┘            └───────────────────────┘             └─────────────────┘
```

---

## BehaviorRegistration

**File:** `di/behavior/BehaviorRegistration.java`

An immutable record that holds all metadata for a single behavior binding:

```
@Builder
public record BehaviorRegistration(
    
    // which profession uses this behavior
    VillagerProfessionKey profession,
    
    // creates a fresh behavior instance
    Supplier<IBehavior<BaseVillager>> behaviorFactory,
    
    // WORK, IDLE, MEET, etc.
    Activity activity,
    
    // relative weight in Minecraft's behavior choice
    int weight,
    
    // priority for UI snapshot ordering
    int priority

)
```

**Key design decisions:**

- **`Supplier` factory, not a direct instance.** Behaviors are stateful (they track cooldowns, targets, step progress).
  Each villager entity gets its own behavior instance. The `Supplier` defers creation until `resolve()` is called
  during villager brain registration.
- **`weight`** controls how Minecraft's `GateBehavior` selects among competing behaviors in the same activity. Higher
  weight = more likely to be chosen.
- **`priority`** controls ordering in the behavior controller UI snapshot. Higher priority = shown first.

---

## BehaviorModule

**File:** `di/modules/server/BehaviorModule.java`

The single, centralized catalog of all behavior-to-profession bindings. Every behavior in the mod has exactly one
`@Provides @IntoSet` method here.

### Structure

Each method:

1. Receives its config (and any other dependencies) as parameters — Dagger injects these from `ConfigModule` /
   `DataManagerModule`.
2. Returns a `BehaviorRegistration` that captures the profession, activity, and a factory `Supplier` that closes over
   the injected config.

```
@Provides
@IntoSet
static BehaviorRegistration fishermanFishing(FishingConfig config, HungerConfig hungerConfig) {
    return work(VillagerProfessionKey.FISHERMAN, () -> new FishingBehavior(config, hungerConfig));
}
```

### Helper methods

| Method                                        | Activity        | Weight | Priority |
|-----------------------------------------------|-----------------|--------|----------|
| `work(profession, factory)`                   | `Activity.WORK` | 10     | 10       |
| `core(profession, factory)`                   | `Activity.CORE` | 10     | 10       |
| `registration(profession, activity, factory)` | (any)           | 10     | 10       |

### Current registrations

- **Armorer**
    - RepairIronGolem
    - BlastOre
- **Butcher**
    - BreedPigs
    - SmokeMeat
    - ButcherLivestock
- **Cleric**
    - ThrowPotions (WORK, MEET, IDLE)
    - HarvestSoulSand
- **Farmer**
    - HarvestSugarCane
    - CollectHoney
    - HarvestHoneycomb
    - TameWolf
    - TameCat
    - BreedChickens
    - MilkCow
- **Fisherman**
    - TameCat
    - Fishing
- **Fletcher**
    - BreedChickens
- **Leatherworker**
    - BreedCows
- **Librarian**
    - EnchantItem
- **Mason**
    - CutStone
    - HarvestOre
- **Shepherd**
    - ShearSheep
    - TameWolf
- **Toolsmith**
    - RepairIronGolem
- **Weaponsmith**
    - RepairIronGolem

**Cross-profession CORE behaviors** — registered once per vanilla profession constant (including `NONE` and `NITWIT`):

- **All professions** (CORE)
    - EatFood

Note: Cleric's `ThrowPotions` is registered for three activities (WORK, MEET, IDLE), so it appears in each of those
activity phases. Each activity registration creates a separate behavior instance.

Note: `EatFood` is a universal behavior that every villager needs regardless of profession. It is currently registered
with a separate `@Provides @IntoSet` method per profession constant. This is a known DRY limitation of the flat-set
registration model — any behavior that must apply to all professions requires one entry per profession.

---

## BehaviorPackageResolver

**File:** `di/behavior/BehaviorPackageResolver.java`

A `@ServerScope` service that pre-processes the full `Set<BehaviorRegistration>` into a lookup map at component
creation time, then serves resolved behavior packages on demand.

### Construction (eager grouping)

```
@Inject
public BehaviorPackageResolver(Set<BehaviorRegistration> registrations) {
    this.professionBehaviors = registrations.stream()
        .sorted(...)   // by profession ID → activity → priority → weight
        .collect(Collectors.groupingBy(BehaviorRegistration::profession));
}
```

This runs once when the `ServerComponent` is created. Villager entity spawns do not re-scan the catalog.

### Resolution

```
ResolvedBehaviors resolve(VillagerProfessionKey profession, Activity activity)
```

For a given profession and activity, `resolve()`:

1. Filters registrations to the requested activity.
2. Calls each `behaviorFactory.get()` to create a fresh behavior instance.
3. Wraps it in a `DefaultBehaviorAdapter` for Minecraft's brain system.
4. Creates a `BehaviorBinding` for UI tracking.
5. Returns both lists bundled as `ResolvedBehaviors`.

The **same behavior instance** is shared between the brain (which ticks it) and the UI snapshot builder (which reads
its live state). This is intentional — the UI shows real-time behavior status.

### ResolvedBehaviors

```
public record ResolvedBehaviors(
    List<Pair<? extends BehaviorControl<? super Villager>, Integer>> choiceBehaviors,  // for brain
    List<BehaviorBinding> trackedBindings                                              // for UI
)
```

---

## Consumption in BaseVillager

**File:** `infrastructure/minecraft/entities/villager/BaseVillager.java`

When a villager's brain is registered (`registerCustomGoals()`), it:

1. Gets the `BehaviorPackageResolver` from `SettlementsDagger.serverOrThrow().behaviorPackageResolver()`.
2. Resolves behaviors for each activity (IDLE, WORK, MEET).
3. Passes `choiceBehaviors` to `VanillaBehaviorPackages` which merges them with vanilla behaviors into Minecraft's
   `Brain`.
4. Collects `trackedBindings` for the behavior controller UI.

```
BehaviorPackageResolver resolver = SettlementsDagger.serverOrThrow().behaviorPackageResolver();

// For each activity:
BehaviorPackageResolver.ResolvedBehaviors workResolved = resolver.resolve(professionKey, Activity.WORK);
brain.addActivityWithConditions(Activity.WORK,
    VanillaBehaviorPackages.getWorkPackage(profession, 0.5F, workResolved.choiceBehaviors()), ...);
trackedBehaviors.addAll(workResolved.trackedBindings());
```

`BaseVillager` uses `SettlementsDagger` because Minecraft controls entity creation — constructor injection is not
possible here.

---

## Adding a New Behavior

Step-by-step instructions are in [Common Tasks](common_tasks.md#add-a-new-behavior). The short version:

1. Create the behavior class in `application/ai/behavior/usecases/villager/<category>/`.
2. Create its config class in the same package.
3. Add a `@Provides @Singleton` method for the config in `ConfigModule`.
4. Add a `@Provides @IntoSet` method in `BehaviorModule`.
5. Build — Dagger validates the full graph at compile time. If the config binding is missing, the build fails with a
   clear error message pointing to the missing dependency.
