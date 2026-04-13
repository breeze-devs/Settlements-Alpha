# Dagger Dependency Injection Guide

This document covers the Dagger 2 component hierarchy, all modules, the static accessor, the initialization lifecycle,
and how implicit bindings work. For the big-picture layer overview, see
[Architecture Overview](architecture_overview.md).

---

## Component Hierarchy

The Dagger graph is a three-level tree: one root component and two scoped subcomponents.

```
@Singleton SettlementsComponent               (common — lives for the entire mod session)
│
│   Modules:
│   ├── ConfigModule          — 16 config records via ConfigFactory
│   ├── DataManagerModule     — 8 data managers + EnchantmentEngine
│   └── GenerationModule      — domain registry interfaces + GenerationPipeline
│
├── @ServerScope ServerComponent              (created per server start, cleared on server stop)
│   │
│   │   Modules:
│   │   ├── BehaviorModule        — Set<BehaviorRegistration> via @IntoSet multibinding
│   │   └── ServerNetworkModule   — Map<Class, ServerSidePacketHandler> via @IntoMap multibinding
│   │
│   │   Implicit bindings (@Inject constructors, no module needed):
│   │   ├── BehaviorControllerSessionService
│   │   ├── VillagerStatsSessionService
│   │   ├── VillagerBubbleService
│   │   ├── BehaviorControllerSnapshotBuilder
│   │   ├── VillagerStatsSnapshotBuilder
│   │   ├── BehaviorPackageResolver
│   │   ├── ServerSidePacketReceiver
│   │   ├── BehaviorControllerServerEvents
│   │   └── VillagerStatsServerEvents
│   │
│
└── @ClientScope ClientComponent              (created once on client dist, lives for the session)
    │
    │   Modules:
    │   └── ClientNetworkModule   — Map<Class, ClientSidePacketHandler> via @IntoMap multibinding
    │
    │   Implicit bindings:
    │   ├── ClientSidePacketReceiver
    │   ├── BehaviorControllerClientState
    │   └── VillagerStatsClientState
```

### What each component owns

**SettlementsComponent** (`di/SettlementsComponent.java`)

- Scope: `@Singleton` — created once during `FMLLoadCompleteEvent`, never destroyed.
- Owns all configs, data managers, generation pipeline, and reload listeners.
- Exposes factory methods for both subcomponents: `serverComponentFactory()`, `clientComponentFactory()`.

**ServerComponent** (`di/ServerComponent.java`)

- Scope: `@ServerScope` — created on `ServerAboutToStartEvent`, cleared on `ServerStoppedEvent`.
- Owns server-only services: session management, bubble service, snapshot builders, behavior resolution, server-side
  packet dispatch, and server event handlers.
- In single-player, this is created and destroyed each time the player opens/closes a world.

**ClientComponent** (`di/ClientComponent.java`)

- Scope: `@ClientScope` — created once during `FMLLoadCompleteEvent` (client dist only), never destroyed.
- Owns client-only state holders and client-side packet dispatch.
- Never instantiated on a dedicated server.

### Custom Scope Annotations

```
di/ServerScope.java    — @Scope annotation for ServerComponent bindings
di/ClientScope.java    — @Scope annotation for ClientComponent bindings
```

Scopes ensure that within a given component instance, each scoped binding produces exactly one instance. When the
`ServerComponent` is destroyed and re-created (world change), all `@ServerScope` instances are fresh.

---

## Implicit Bindings

Not every class that Dagger provides needs a `@Module` entry. If a class has an `@Inject` constructor, Dagger can
create it automatically — this is called an **implicit binding**. The class just needs to be reachable from the
component that exposes it.

In this project, Lombok generates the `@Inject` constructor via `onConstructor_`:

```
// No-arg example (no dependencies):
@NoArgsConstructor(onConstructor_ = @Inject)
public final class BehaviorControllerSessionService { ... }

// All-args example (dependencies injected via constructor params):
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class ServerSidePacketReceiver {
    private final Map<Class<?>, ServerSidePacketHandler<?>> handlers;  // injected by Dagger
}
```

The component interface exposes the binding via an accessor method:

```
// In ServerComponent.java — no module needed, Dagger finds the @Inject constructor
BehaviorControllerSessionService behaviorControllerSessionService();
```

**When to use implicit bindings vs. modules:**

- Use implicit bindings when the class has a straightforward constructor and no special factory logic.
- Use a `@Module` with `@Provides` when construction requires calling a factory method (e.g., `ConfigFactory.create()`),
  when the return type is an interface (dependency inversion), or when the instance needs to be added to a multibinding
  collection.

---

## Modules Reference

### ConfigModule

**File:** `di/modules/ConfigModule.java`
**Installed in:** `SettlementsComponent`

Bridges NeoForge config records into the Dagger graph. Each method calls `ConfigFactory.create(ConfigClass.class)` and
returns the materialized config record as a `@Singleton`.

NeoForge still owns config discovery and TOML loading. This module only wraps the already-materialized records. Config
values are **restart-only** — changes require a server restart.

Current configs: `FishingConfig`, `ShearSheepConfig`, `TameCatConfig`, `TameWolfConfig`, `BreedAnimalsConfig`,
`ButcherLivestockConfig`, `MilkCowConfig`, `SmokeMeatConfig`, `BlastOreConfig`, `CutStoneConfig`,
`EnchantItemConfig`, `HarvestSugarCaneConfig`, `HarvestSoulSandConfig`, `HarvestOreConfig`,
`RepairIronGolemConfig`, `ThrowPotionsConfig`.

### DataManagerModule

**File:** `di/modules/DataManagerModule.java`
**Installed in:** `SettlementsComponent`

Provides `@Singleton` instances of all JSON data managers. Data managers extend
`SimpleJsonResourceReloadListener` — they load data from resource packs on initial load and on `/reload`.

Current data managers: `FishCatchDataManager`, `EnchantmentCostDataManager`, `SpecializationDataManager`,
`BuildingDefinitionDataManager`, `BiomeSurveyDataManager`, `TraitScorerDataManager`,
`TraitDefinitionDataManager`, `HistoryEventDataManager`.

Also provides `EnchantmentEngine` (application-layer service that depends on `EnchantmentCostDataManager`).

### GenerationModule

**File:** `di/modules/GenerationModule.java`
**Installed in:** `SettlementsComponent`

Applies the **Dependency Inversion Principle**: exposes concrete data managers through their domain-layer interfaces so
that callers depend on stable abstractions rather than infrastructure details.

| Concrete data manager           | Exposed as domain interface |
|---------------------------------|-----------------------------|
| `BuildingDefinitionDataManager` | `BuildingRegistry`          |
| `BiomeSurveyDataManager`        | `BiomeSurveyLookup`         |
| `TraitScorerDataManager`        | `TraitScorerRegistry`       |
| `TraitDefinitionDataManager`    | `TraitRegistry`             |
| `HistoryEventDataManager`       | `HistoryEventRegistry`      |

Also constructs the `GenerationPipeline` from all five registry interfaces.

### BehaviorModule

**File:** `di/modules/server/BehaviorModule.java`
**Installed in:** `ServerComponent`

The central behavior catalog. Each `@Provides @IntoSet` method creates a `BehaviorRegistration` and adds it to the
injected `Set<BehaviorRegistration>`. See [Behavior System](behavior_system.md) for full details.

### ServerNetworkModule

**File:** `di/modules/server/ServerNetworkModule.java`
**Installed in:** `ServerComponent`

An abstract module using `@Binds @IntoMap @ClassKey` to map server-bound packet classes to their handler
implementations. The resulting `Map<Class<?>, ServerSidePacketHandler<?>>` is injected into
`ServerSidePacketReceiver`.

Each handler class has an `@Inject` constructor and implements `ServerSidePacketHandler<T>`. To add a new handler,
add a `@Binds` method mapping the packet class to the handler class.

### ClientNetworkModule

**File:** `di/modules/client/ClientNetworkModule.java`
**Installed in:** `ClientComponent`

Same pattern as `ServerNetworkModule`, but for client-bound packets. Maps packet classes to
`ClientSidePacketHandler<T>` implementations, injected into `ClientSidePacketReceiver`.

---

## The Static Accessor: SettlementsDagger

**File:** `di/SettlementsDagger.java`

A single static access point that bridges non-injectable code into the Dagger graph. This exists because
Minecraft/NeoForge controls the lifecycle of certain objects (entities, structures, event subscribers) and does not
support constructor injection for them.

### What it holds

```
@Nullable
private static SettlementsComponent component;       // root — set once, never cleared
@Nullable
private static ServerComponent serverComponent;      // server — set on start, cleared on stop
@Nullable
private static ClientComponent clientComponent;      // client — set once (client dist only)
```

### Access methods

| Method            | Returns                     | Throws if null? |
|-------------------|-----------------------------|-----------------|
| `component()`     | `SettlementsComponent`      | Yes             |
| `serverOrThrow()` | `ServerComponent`           | Yes             |
| `serverOrNull()`  | `ServerComponent` or `null` | No              |
| `client()`        | `ClientComponent`           | Yes             |

### When to use SettlementsDagger vs. @Inject

**Prefer `@Inject`** whenever possible. Most application-layer and infrastructure-layer classes receive their
dependencies through constructor injection — this is the standard path.

**Use `SettlementsDagger`** only when Minecraft/NeoForge creates the object and you cannot control its constructor:

| Call site               | Why                                                   |
|-------------------------|-------------------------------------------------------|
| `BaseVillager`          | Entity created by Minecraft's entity spawning system  |
| `SettlementsStructure`  | World generation structure created by Minecraft       |
| `ServerLifecycleEvents` | Static NeoForge event subscriber — cannot be injected |
| `CommonModEvents`       | Static NeoForge event subscriber — cannot be injected |
| Packet receivers        | Registered from static event code                     |

If you find yourself adding a new `SettlementsDagger` call site, consider whether the class can be restructured to
receive its dependencies via `@Inject` instead.

---

## Initialization Lifecycle

The Dagger graph is initialized in phases, tied to NeoForge's mod loading events:

```
┌─────────────────────────────────────────────────────────────┐
│  1. FMLLoadCompleteEvent (CommonModEvents.java)             │
│     └── LOAD_COMPLETE_TASKS run (config factories populate) │
│     └── DaggerSettlementsComponent.create()                 │
│     └── SettlementsDagger.initialize(component)             │
│     └── registerReloadListeners(component)                  │
│     └── [CLIENT dist only]                                  │
│         └── component.clientComponentFactory().create()     │
│         └── SettlementsDagger.initializeClient(client)      │
├─────────────────────────────────────────────────────────────┤
│  2. ServerAboutToStartEvent (ServerLifecycleEvents.java)    │
│     └── component.serverComponentFactory().create()         │
│     └── SettlementsDagger.initializeServer(server)          │
│     └── NeoForge.EVENT_BUS.register(server event handlers)  │
├─────────────────────────────────────────────────────────────┤
│  3. ServerStoppedEvent (ServerLifecycleEvents.java)         │
│     └── NeoForge.EVENT_BUS.unregister(server event handlers)│
│     └── SettlementsDagger.clearServer()                     │
│     (If player opens another world, step 2 repeats)         │
└─────────────────────────────────────────────────────────────┘
```

### Key timing details

- **Root component is created after load-complete tasks** because `ConfigFactory` populates config records during those
  tasks. The root component must observe the finalized startup state.
- **Reload listeners are registered from the root component** because `AddReloadListenerEvent` fires before the server
  subcomponent exists for a given world/session.
- **Server component is re-created per world/session.** In single-player, closing and reopening a world triggers
  `ServerStoppedEvent` → `ServerAboutToStartEvent`, creating a fresh `ServerComponent` with fresh `@ServerScope`
  instances.
- **Server event handlers are unregistered on server stop** to prevent stale references and memory leaks. Because they
  are `@ServerScope`, Dagger returns the exact same instances that were registered, so `unregister()` works correctly.
- **Client component is created once** and never destroyed. Client state persists across world changes.
