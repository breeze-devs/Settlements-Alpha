# Common Tasks

Step-by-step recipes for frequent contributor workflows. Each recipe includes file paths and code pointers. For the
concepts behind these patterns, see [Dagger Guide](dagger_guide.md) and [Behavior System](behavior_system.md).

---

## Add a New Behavior

**Example:** Adding a `CompostBehavior` for the Farmer profession.

### 1. Create the config class

**File:** `application/ai/behavior/usecases/villager/farming/CompostConfig.java`

Create a config record with the `@BehaviorConfig` annotation. Fields annotated with `@IntegerConfig`, `@DoubleConfig`,
etc. are auto-registered with NeoForge's config system via `ConfigAnnotationProcessor`.

### 2. Create the behavior class

**File:** `application/ai/behavior/usecases/villager/farming/CompostBehavior.java`

Extend `StateMachineBehavior` (for multi-step workflows) or `BaseVillagerBehavior` (for simple single-tick behaviors).
Accept the behavior's own config and `HungerConfig` as constructor parameters — `HungerConfig` is required by
`BaseVillagerBehavior` and controls the hunger-based cooldown multiplier applied when the behavior stops.

```
public class CompostBehavior extends StateMachineBehavior {
    public CompostBehavior(CompostConfig config, HungerConfig hungerConfig) {
        super(log,
              config.createPreconditionCheckCooldownTickable(),
              config.createBehaviorCooldownTickable(),
              hungerConfig);
        // ...
    }
}
```

### 3. Register the config in ConfigModule

**File:** `di/modules/ConfigModule.java`

```
@Provides
@Singleton
static CompostConfig compostConfig() {
    return ConfigFactory.create(CompostConfig.class);
}
```

### 4. Register the behavior in BehaviorModule

**File:** `di/modules/server/BehaviorModule.java`

```
@Provides
@IntoSet
static BehaviorRegistration farmerCompost(CompostConfig config, HungerConfig hungerConfig) {
    return work(VillagerProfessionKey.FARMER, () -> new CompostBehavior(config, hungerConfig));
}
```

For non-WORK activities, use `registration(profession, Activity.IDLE, ...)` instead.

### 5. Build and verify

Run the Gradle build. Dagger validates the full dependency graph at compile time — if the `CompostConfig` binding is
missing from `ConfigModule`, the build fails with a clear error pointing to the unresolved dependency.

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

### 1. Create the data manager class

**File:** `infrastructure/minecraft/data/<feature>/MyDataManager.java`

Extend `SimpleJsonResourceReloadListener`. Implement any domain-layer registry interface if applicable (dependency
inversion).

### 2. Register in DataManagerModule

**File:** `di/modules/DataManagerModule.java`

```
@Provides
@Singleton
static MyDataManager myDataManager() {
    return new MyDataManager();
}
```

### 3. Expose from SettlementsComponent (if needed externally)

**File:** `di/SettlementsComponent.java`

Add an accessor method if the data manager needs to be exposed for reload listener registration or direct access:

```
MyDataManager myDataManager();
```

### 4. Register as a reload listener (if applicable)

**File:** `bootstrap/event/CommonModEvents.java` — in `registerReloadListeners()`

```
event.addListener(component.myDataManager());
```

### 5. Expose via domain interface (if applicable)

If the data manager implements a domain-layer registry interface, add a binding in `GenerationModule`:

```
@Provides
@Singleton
static MyRegistry myRegistry(MyDataManager manager) {
    return manager;
}
```

---

## Add a New Custom Sensor

**Example:** Adding a `NeedFoodSensor` that writes a custom memory when a villager is low on food.

### 1. Register the memory type

**File:** `domain/ai/memory/MemoryTypeRegistry.java`

Add a new `MemoryType<T>` entry and register it so `BaseVillager` can include the resulting
`MemoryModuleType<?>` in its `MEMORY_TYPES` list.

### 2. Create the sensor class

**File:** `infrastructure/minecraft/ai/sensors/NeedFoodSensor.java`

Implement a vanilla `Sensor<Villager>` subclass (or the concrete villager subtype used by the codebase) and keep the
logic focused on translating world/entity state into brain memory writes. This keeps the sensor on the infrastructure
side of the architecture boundary because it directly depends on Minecraft brain APIs.

### 3. Register the sensor type

**File:** `bootstrap/registry/sensors/SensorTypeRegistry.java`

Create a `DeferredRegister<SensorType<?>>`, register the sensor type, and expose a `register(IEventBus)` method so the
mod entry point can bind it to the mod event bus.

### 4. Wire the registry into the mod bootstrap

**File:** `SettlementsMod.java`

Register `SensorTypeRegistry` on the mod event bus alongside the other deferred registries.

### 5. Add the memory and sensor to BaseVillager

**File:** `infrastructure/minecraft/entities/villager/BaseVillager.java`

Append the custom memory module to `MEMORY_TYPES` and the registered sensor type to `SENSOR_TYPES`. If either side is
missing, the brain will not have the full contract the sensor expects.

### 6. Build and verify

Run the Gradle build and then verify in-game that the target villager gains and clears the memory on the expected sense
interval. Sensors write memories; behaviors consume them later.

---

## Add a New Packet Handler

### Server-bound packet (client sends to server)

**Files to edit:**

1. Create the packet class in `infrastructure/network/features/ui/<feature>/packet/`.
2. Create the handler class in `infrastructure/network/features/ui/<feature>/handler/`. Give it an `@Inject`
   constructor that accepts any dependencies it needs.
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
