# Common Tasks

Step-by-step recipes for frequent contributor workflows. Each recipe includes file paths and code pointers. For the
concepts behind these patterns, see [Dagger Guide](dagger_guide.md) and [Behavior System](behavior_system.md).

---

## Add a New Behavior

**Example:** the real `HarvestPumpkinBehavior` for the Farmer profession. A behavior touches **four** seams — config,
behavior class, catalog entry, and profession pool (plus a `BehaviorKey` constant). The behavior class and catalog entry
are Dagger-validated at compile time, but the pool mapping is not: miss it and the behavior compiles and registers yet
never fires.

### 1. Create the config record

**File:** `application/ai/behavior/usecases/villager/farming/HarvestPumpkinConfig.java`

Annotate a `record` with `@BehaviorConfig(name = ..., type = ConfigurationType.BEHAVIOR)` and `implements
BehaviorTimingConfig`. Each component carries a config annotation (`@IntegerConfig`, `@DoubleConfig`, …); the
`ConfigAnnotationProcessor` scans `@BehaviorConfig` types at startup and builds the NeoForge `ModConfigSpec` (record
components are handled by `RecordConfigProcessor`). Reuse the standard identifiers from `BehaviorConfigConstants` for the
four cooldown fields plus `experienceReward`.

```
@BehaviorConfig(name = "harvest_pumpkin", type = ConfigurationType.BEHAVIOR)
public record HarvestPumpkinConfig(
        @IntegerConfig(type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.PRECONDITION_CHECK_COOLDOWN_MIN_IDENTIFIER, ...) int preconditionCheckCooldownMin,
        // ... cooldown max, behavior cooldown min/max ...
        @IntegerConfig(type = ConfigurationType.BEHAVIOR,
                identifier = BehaviorConfigConstants.EXPERIENCE_REWARD_IDENTIFIER, ...) int experienceReward
) implements BehaviorTimingConfig {
    public HarvestPumpkinConfig {
        BehaviorCooldownValidator.validateRanges(preconditionCheckCooldownMin, preconditionCheckCooldownMax,
                behaviorCooldownMin, behaviorCooldownMax);
    }
}
```

`BehaviorTimingConfig` supplies the `createPreconditionCheckCooldownTickable()` / `createBehaviorCooldownTickable()`
default methods used by the behavior's `super(...)` call.

### 2. Create the behavior class

**File:** `application/ai/behavior/usecases/villager/farming/HarvestPumpkinBehavior.java`

Villager behaviors extend `VillagerStateMachineBehavior` (which is `StateMachineBehavior<BaseVillager>`). The
constructor takes the behavior's own config plus a `BehaviorSupport` — **not** `HungerConfig`. `BehaviorSupport` is the
shared collaborator bag (target resolver, hunger config, etc.); the hunger-based cooldown multiplier is applied on stop
through `BehaviorSupport.getHungerConfig()`, you never pass `HungerConfig` in yourself.

```
public HarvestPumpkinBehavior(HarvestPumpkinConfig config, BehaviorSupport support) {
    super(log,
          config.createPreconditionCheckCooldownTickable(),
          config.createBehaviorCooldownTickable(),
          support,
          config.experienceReward());
    this.targetResolver = support.getBlockMemoryTargetResolver();  // pull collaborators off support
    this.preconditions.add(KnownBlockSitesPrecondition.builder()
            .memoryType(MemoryTypeRegistry.RIPE_PUMPKIN_SITES)
            // ...
            .build());
    this.initializeStateMachine(this.createControlStep(), Stage.END);  // build the staged state machine
}
```

(There is no `BaseVillagerBehavior` "simple single-tick" base — every villager behavior extends
`VillagerStateMachineBehavior`.)

### 3. Register the config in ConfigModule

**File:** `di/modules/ConfigModule.java`

```
@Provides
@Singleton
static HarvestPumpkinConfig harvestPumpkinConfig() {
    return ConfigFactory.create(HarvestPumpkinConfig.class);
}
```

### 4. Register a catalog entry in BehaviorCatalogModule

**File:** `di/modules/server/BehaviorCatalogModule.java`

The behavior is *described* here as a `@Provides @IntoSet BehaviorCatalogEntry` — its `BehaviorKey`, category +
intensity, required channels, cooldown, and the `factory` that constructs it. **Profession is not set here.**

```
@Provides
@IntoSet
static BehaviorCatalogEntry harvestPumpkin(HarvestPumpkinConfig config, BehaviorSupport support) {
    return BehaviorCatalogEntry.builder()
            .descriptor(BehaviorPlanningMetadata.builder()
                    .key(BehaviorKey.HARVEST_PUMPKIN)
                    .category(BehaviorCategory.WORK)          // WORK | SOCIAL | SELF_CARE | LEISURE | COMBAT
                    .intensity(WorkIntensity.HEAVY)           // HEAVY | LIGHT | NONE
                    .requiredChannel(BehaviorChannel.MOVEMENT)
                    .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                    .opportunity(new OpportunityRequirement.KnownSiteOpportunity(
                            Set.of(MemoryTypeRegistry.RIPE_PUMPKIN_SITES)))   // for site-gated work
                    .build())
            .displayInfo(BehaviorDisplayMetadata.builder()...build())
            .factory(() -> new HarvestPumpkinBehavior(config, support))
            .build();
}
```

There is no vanilla `Activity` here — categorization is `BehaviorCategory` + `WorkIntensity`. Use `SELF_CARE` for
eating, `SOCIAL` for trade/courtship, and `LEISURE` + `WorkIntensity.NONE` for nitwit/idle behaviors. (A nitwit-eligible
behavior **must** be `LEISURE`/`NONE`: a `WORK` behavior gets a zero-length work window on a nitwit and never fires.)

### 5. Add the BehaviorKey constant

The `.key(BehaviorKey.HARVEST_PUMPKIN)` above must exist in `BehaviorKey` (`domain/ai/catalog/`). Add it if new.

### 6. Map the behavior to a profession in PoolModule

**File:** `di/modules/server/PoolModule.java`

This is the seam that is *not* compile-checked — a catalog entry no pool references is dead. Add a `PoolEntry` to the
profession's pool (`VillagerProfessionKey` is a record with static constants, not an enum):

```
static ProfessionBehaviorPool farmerPool() {
    return ProfessionBehaviorPool.builder()
            .profession(VillagerProfessionKey.FARMER)
            .entry(PoolEntry.of(BehaviorKey.HARVEST_PUMPKIN))
            // ... other farmer entries ...
            .build();
}
```

Universal behaviors (eat, wander, rest, trade) are merged in by `BehaviorPoolResolver` and are not listed per-profession.

### 7. Build and verify

Dagger validates the graph at compile time, but steps 5–6 are not checked against the catalog. Verify in-game that the
villager actually runs the behavior.

---

## Add a New Config

**File to edit:** `di/modules/ConfigModule.java`

1. Create the config record (e.g., `MyFeatureConfig.java`) with appropriate config annotations.
2. Add a `@Provides @Singleton` method in `ConfigModule`:

   ```
   @Provides
   @Singleton
   static MyFeatureConfig myFeatureConfig() {
       return ConfigFactory.create(MyFeatureConfig.class);
   }
   ```

The config is now injectable anywhere in the Dagger graph. Any class with an `@Inject` constructor can declare it as a
parameter.

---

## Add a New Data Manager

Datapack loaders are codec-backed and extend one of the base classes in `infrastructure/minecraft/data/framework`
— you do **not** hand-roll a `SimpleJsonResourceReloadListener`, and registration is a single `@Binds` line, not
an edit to `CommonModEvents`. The full walkthrough (choosing a base, authoring the codec, wiring, and testing)
lives in the [Datapack System](datapack_system.md) reference — see [Adding a new loader](datapack_system.md#adding-a-new-loader).

In short:

1. Pick the lowest-fitting base (`KeyedCatalogDataManager`, `ProfessionCatalogDataManager`, `WeightedYieldDataManager`,
   or plain `CodecJsonDataManager` for a bespoke shape).
2. Author the domain `record` + its co-located `FooCodec` (`public static final Codec<Foo> CODEC`).
3. Write the loader; `implements` its domain registry interface.
4. `@Provides @Singleton` the concrete loader in `di/modules/DataManagerModule.java`, and bind it to its registry
   port in the relevant feature module.
5. Add one `@Binds @IntoSet @DataReloadListeners` line in `di/modules/ReloadListenerModule.java`.
6. Drop the JSON under `data/settlements/settlements/<directory>/` and test via `manager.reload(entries)`.

---

## Add a New Block Resource (villager block sensing)

Villagers sense harvestable/collectable **blocks** (crops, ore, sand, gravel, full hives, …) through a shared,
server-scoped `WorldResourceIndex`, **not** through per-sensor world scans. An off-thread scanner (`ResourceIndexRefresher`,
on the `@WorldScanExecutor`) keeps the index fresh using palette-prefiltered section snapshots; a single stateless
`BlockResourceSensor` queries it for every resource at once and folds the hits into each villager's decaying spatial
memory (bounded, self-expiring — see [Villager Memory](#villager-memory-vanilla-backed-vs-decaying)).

**So adding a block resource is pure registration — you write no sensor.** Three steps:

### 1. Add a BlockMatcher

**File:** `domain/world/blocks/BlockMatchers.java`

A `BlockMatcher` is a cheap state predicate (drives the palette prefilter) plus an optional neighbor/context predicate
(run during the section scan):

```
public static final BlockMatcher HARVESTABLE_PUMPKIN = new BlockMatcher(
        state -> state.is(Blocks.PUMPKIN),                                    // state-only, must be cheap
        (pos, view) -> Direction.Plane.HORIZONTAL.stream()                    // context/neighbor test
                .anyMatch(d -> view.getBlockState(pos.relative(d)).is(Blocks.ATTACHED_PUMPKIN_STEM)));
```

Use the single-arg constructor when no neighbor test is needed (see `RIPE_SWEET_BERRY_BUSH`, `RIPE_CROP`).

### 2. Add a DecayingSpatialMemoryType

**File:** `domain/ai/memory/MemoryTypeRegistry.java`

Declare the memory slot with a retention (TTL) and `maxEntries` (the nearest-K cap), and add it to
`decayingSpatialTypes()`:

```
public static final MemoryType.DecayingSpatialMemoryType RIPE_PUMPKIN_SITES =
        MemoryType.decaying("ripe_pumpkin_sites", ClockTicks.minutes(40), 32);
```

Decaying types need **no** vanilla `MemoryModuleType` and are **not** added to `BaseVillager` — they live in the
`SettlementsMemoryStore`, not the vanilla brain.

### 3. Bind a BlockResource into the multibound set

**File:** `di/modules/server/SensorCatalogModule.java`

```
@Provides
@IntoSet
static BlockResource ripePumpkin() {
    return new BlockResource(BlockMatchers.HARVESTABLE_PUMPKIN, MemoryTypeRegistry.RIPE_PUMPKIN_SITES);
}
```

Because `BlockResourceSensor` and `ResourceIndexRefresher` both inject `Set<BlockResource>`, the new resource is now
scanned off-thread, indexed, capped, decayed, and written into every villager's memory automatically. To make villagers
*act* on it, write a behavior that reads `RIPE_PUMPKIN_SITES` via `KnownBlockSitesPrecondition` +
`BlockMemoryTargetResolver` (see [Add a New Behavior](#add-a-new-behavior)).

> **Exception:** block **entities** (chests, cultivation lilies) need NBT/block-entity access and cannot use the
> section-palette scan — they use a bespoke sensor (below), not a `BlockResource`.

---

## Add a Custom Sensor (entity & block-entity senses)

Reach for a bespoke sensor only when the sense is **not** a `BlockState` scan — i.e. entity senses (nearby pets,
courtship partners, hurt-by) or block-entity senses (chests, cultivation lilies). For harvestable blocks, add a Block
Resource (above) instead.

Two sensor bases exist in the codebase:

- **Vanilla `Sensor<Villager>`** — registered as a `SensorType` and added to `BaseVillager.sensorTypes()`; ticked by the
  vanilla brain. Examples: `OwnedPetsSensor`, `VillageChestsSensor`, `CultivationSiteSensor`,
  `WillingCourtshipPartnersSensor`. This is the usual path for a new entity/BE sense — the steps below cover it.
- **Mod-native `AbstractSensor<BaseVillager>`** — bound as a `VillagerSensorFactory` `@IntoSet` in `SensorCatalogModule`
  (cooldown-driven, off the vanilla sensor tick). Examples: `EntityPerceptionSensor`, `EntitySightingEmitterSensor`, and
  the block-resource sensor itself. Use this when you need the mod's own sensor lifecycle rather than the vanilla brain tick.

### 1. Register the memory the sensor writes

**File:** `domain/ai/memory/MemoryTypeRegistry.java`

Add a `MemoryType.vanillaBacked(id, MemoryModuleTypeRegistry.X)` slot, backed by a `MemoryModuleType` you also register
in `bootstrap/registry/memory/MemoryModuleTypeRegistry.java`. (These mod memories are transient — see
[Villager Memory](#villager-memory-vanilla-backed-vs-decaying).)

### 2. Create the sensor class

**File:** `infrastructure/minecraft/ai/sensors/MySensor.java`

Extend vanilla `Sensor<Villager>` and downcast to `BaseVillager` inside `doTick`. It stays on the infrastructure side
because it depends directly on Minecraft brain APIs.

### 3. Register the sensor type

**File:** `bootstrap/registry/sensors/SensorTypeRegistry.java`

Add to the `DeferredRegister<SensorType<?>>` with `() -> new SensorType<>(MySensor::new)`; the class already exposes
`register(IEventBus)`.

### 4. Wire the registry into the mod bootstrap

**File:** `SettlementsMod.java`

`SensorTypeRegistry.register(modEventBus)` is already called alongside the other deferred registries — nothing to add
unless you introduced a new registry class.

### 5. Add the memory and sensor to BaseVillager

**File:** `infrastructure/minecraft/entities/villager/BaseVillager.java`

`memoryTypes()` and `sensorTypes()` are **private static methods** fed to `Brain.provider(...)` — not `MEMORY_TYPES` /
`SENSOR_TYPES` fields. Append `MemoryTypeRegistry.X.getModuleType()` to `memoryTypes()` and
`SensorTypeRegistry.MY_SENSOR.get()` to `sensorTypes()`. If either side is missing, the brain lacks the contract the
sensor expects.

### 6. Build and verify

Run the Gradle build, then verify in-game that the villager gains and clears the memory on the expected sense interval.
Sensors write memories; behaviors consume them later.

---

## Villager Memory: vanilla-backed vs decaying

Villager memory comes in two flavors, and **neither survives a world reload** — both are rebuilt from sensors after
load. Anything that must persist (owned wolves, genetics, inventory, day plan, settlement metadata) lives in a
codec-serialized attachment or `SettlementSavedData`, deliberately *outside* the memory system.

| | Vanilla-backed (`MemoryType.VanillaMemoryType<T>`) | Decaying (`MemoryType.DecayingSpatialMemoryType`) |
|---|---|---|
| Payload | any (`Boolean`, entity, `List<GlobalPos>`, …) | `List<GlobalPos>` only |
| Storage | vanilla `Brain` map (part of villager entity NBT) | `SettlementsMemoryStore` — RAM-only entity attachment |
| Persists reload? | **No** — all mod modules registered `new MemoryModuleType<>(Optional.empty())`, no codec, so the brain serializer skips them. (Vanilla's own `HOME`/`JOB_SITE`/etc. persist because vanilla gave *them* codecs.) | **No** — attachment built without `.serialize(...)`; store is documented "Transient: not serialized". |
| Decay | none (uncapped) | per-entry TTL + stalest-first eviction at `maxEntries` |
| Written via | `IBrain.setMemory` | `IBrain.updateSites` (site upsert + confirmed-absence purge); a direct `setMemory` **throws** |
| Use for | flags/entities/lists the vanilla brain machinery reads, or sensor-written lists that don't need decay (`VILLAGE_CHESTS`, `CULTIVATION_SITES`) | high-cardinality block-resource sites that should self-expire and stay bounded |

Both are declared in `MemoryTypeRegistry` via the `vanillaBacked(...)` / `decaying(...)` factories. Practical
consequence: right after a restart a villager has **no** Settlements memories (no remembered crop/ore sites, no
`PLAN_BEHAVIOR_ACTIVE`); the next sensor scan cycle reconstructs them.

> **A `List<GlobalPos>` site memory's identifier is a cross-repo wire contract.** `SnapshotAssembler` derives the SIS
> monologue token from it (`_sites` stripped, uppercased), and the SIS phrasing table is keyed on the result. SIS does
> not reject a token it does not recognize — it falls back to generic phrasing, so a rename that lands on only one side
> produces vaguer villager narration and no error anywhere. Adding or renaming one of these identifiers means changing
> the SIS phrasing table in the same window.

---

## Add a New Packet Handler

### Server-bound packet (client sends to server)

**Files to edit:**

1. Create the packet class in `infrastructure/network/features/<area>/<feature>/packet/`.
2. Create the handler class in `infrastructure/network/features/<area>/<feature>/handler/`. Give it an `@Inject`
   constructor that accepts any dependencies it needs.

   > The `<area>` is usually `ui` (with sub-features like `sync`, `bubble`, `dayplan`, `stats`), but not always —
   > non-UI features sit directly under `features/` (e.g. `features/debug/`). Match an existing sibling.
3. Register in `ServerNetworkModule`:

   ```
   @Binds
   @IntoMap
   @ClassKey(MyServerBoundPacket.class)
   abstract ServerSidePacketHandler<?> myHandler(MyServerBoundPacketHandler impl);
   ```

### Client-bound packet (server sends to client)

Same pattern, but in `ClientNetworkModule`:

```
@Binds
@IntoMap
@ClassKey(MyClientBoundPacket.class)
abstract ClientSidePacketHandler<?> myHandler(MyClientBoundPacketHandler impl);
```

---

## Access a Service from Non-Injectable Code

When Minecraft/NeoForge creates an object (entities, structures, static event handlers) and you cannot use `@Inject`,
access the Dagger graph through `SettlementsDagger`:

```
// For root-level services (configs, data managers, generation pipeline):
MyConfig config = SettlementsDagger.component().myConfig();

// For server-scoped services (sessions, bubbles, behavior resolver):
MyService service = SettlementsDagger.serverOrThrow().myService();

// For client-scoped services (client state, client packet receiver):
MyClientState state = SettlementsDagger.client().myClientState();
```

**Important:** The service must be exposed as an accessor method on the relevant component interface. If it isn't,
add one.

For server-scoped access, prefer `serverOrThrow()` when you are certain a server is running (e.g., inside a
server tick handler). Use `serverOrNull()` when the server may not be available (e.g., during client-only phases).

---

## Add an Implicitly-Bound Service

If your new class has a simple constructor with all dependencies injectable, you do not need a module entry. Just:

1. Add `@Inject` to the constructor (or use Lombok `onConstructor_ = @Inject`):

   ```
   @AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
   public class MyService {
       private final SomeDependency dependency;  // Dagger injects this
   }
   ```

2. Add an accessor method to the appropriate component interface:

   ```
   // In ServerComponent.java, ClientComponent.java, or SettlementsComponent.java:
   MyService myService();
   ```

3. Build — Dagger resolves the constructor parameters from the existing graph. If any parameter type is not bound,
   the build fails with a clear error.
