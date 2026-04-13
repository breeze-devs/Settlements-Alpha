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

Extend `BaseVillagerBehavior` (or `StateMachineBehavior` for multi-step workflows). Accept the config as a constructor
parameter.

```
public class CompostBehavior extends BaseVillagerBehavior {
    public CompostBehavior(CompostConfig config) {
        super(config.enabled(), config.cooldown());
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
static BehaviorRegistration farmerCompost(CompostConfig config) {
    return work(VillagerProfessionKey.FARMER, () -> new CompostBehavior(config));
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
