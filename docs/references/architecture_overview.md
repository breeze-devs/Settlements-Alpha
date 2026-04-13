# Architecture Overview

Settlements is a NeoForge Minecraft mod that enhances vanilla villager mechanics for immersion. Villagers gain
profession-specific behaviors (fishing, smelting, enchanting, animal husbandry, etc.), custom AI scheduling, speech
bubbles, procedural settlement generation, etc.

This document is the starting point for contributors. It covers the guiding principles, the layer structure,
and the package layout. For deeper dives, see the companion docs in this directory:

- [Dagger Guide](dagger_guide.md) — component hierarchy, modules, initialization lifecycle
- [Behavior System](behavior_system.md) — behavior registration, resolution, and the brain pipeline
- [Testing Guide](testing_guide.md) — test Dagger graph, Mockito patterns
- [Common Tasks](common_tasks.md) — step-by-step recipes for frequent contributor workflows

These are living documents; please ensure the architectural documentation is updated after implementing a
feature or making a design decision.

---

## Dependency Injection & Dependency Inversion

**Dependency Inversion Principle (DIP):** High-level modules should not depend on low-level modules. Both should depend
on abstractions. In practice, this means the `domain` layer defines interfaces (e.g., `BuildingRegistry`) and the
`infrastructure` layer provides concrete implementations (e.g., `BuildingDefinitionDataManager`). The domain never
imports infrastructure code.

**Dependency Injection (DI):** Instead of a class creating or locating its own dependencies (`new FooService()` or
`FooService.getInstance()`), dependencies are provided to the class from the outside — typically via constructor
parameters. This makes classes easier to test (swap in mocks) and easier to reason about (all dependencies are visible
in the constructor signature).

**Dagger 2** is the DI framework used in this project. Unlike Spring or Guice, Dagger generates all wiring code at
compile time — there is no runtime reflection, no classpath scanning, and no allocation overhead on the hot path. This
matters because villager behaviors tick every server tick; any per-tick DI overhead is unacceptable. Dagger generates
plain Java factory classes equivalent to hand-written `new Foo(dep1, dep2)` calls.

---

## Clean Architecture Layers

The project follows a layered architecture where **inner layers never reference outer layers**. Dependencies point
inward: `infrastructure` depends on `domain`, but `domain` never imports from `infrastructure`.

- **bootstrap**
    - NeoForge mod entry, event bus wiring, and registry setup for items, entities, and sounds.
- **di**
    - Dagger components, modules, scopes, and the static accessor (SettlementsDagger).
- **presentation**
    - Screens, client state, client utilities, commands, and keybindings.
- **infrastructure**
    - Minecraft/NeoForge adapters: entities, data managers, networking, config, rendering, and world generation.
- **application**
    - Use cases: behaviors, brain, UI services, enchanting engine, sensors, and sessions.
- **domain**
    - Pure Java: interfaces, models, conditions, and value objects — zero Minecraft imports.

### Layer Responsibilities

| Layer              | Package                                  | What goes here                                                                                                                                                                                                                  |
|--------------------|------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **domain**         | `dev.breezes.settlements.domain`         | Interfaces (`IBehavior`, `IBrain`, `BuildingRegistry`), models (`BehaviorDefinition`, `SettlementProfile`), conditions, value objects. No Minecraft or NeoForge imports.                                                        |
| **application**    | `dev.breezes.settlements.application`    | Use-case implementations: behavior classes (e.g., `FishingBehavior`), brain logic (`DefaultBrain`), UI services (`VillagerBubbleService`), enchanting engine, sensors. Depends on domain interfaces.                            |
| **infrastructure** | `dev.breezes.settlements.infrastructure` | Minecraft/NeoForge adapters: `BaseVillager`, data managers (`FishCatchDataManager`), network packet handlers, config annotation processing, world generation (`SettlementsStructure`), rendering. Implements domain interfaces. |
| **presentation**   | `dev.breezes.settlements.presentation`   | Client-only code: screens (`VillagerStatsScreen`, `BehaviorControllerScreen`), client state holders, keybindings, commands.                                                                                                     |
| **bootstrap**      | `dev.breezes.settlements.bootstrap`      | Mod initialization: `SettlementsMod` entry point, NeoForge event subscribers (`CommonModEvents`, `ServerLifecycleEvents`), deferred registries (items, entities, sounds, structures).                                           |
| **di**             | `dev.breezes.settlements.di`             | Dagger components, modules, custom scopes, the `SettlementsDagger` static accessor, and the behavior registration/resolution system.                                                                                            |
| **shared**         | `dev.breezes.settlements.shared`         | Cross-cutting utilities: logging (`@CustomLog`, `LoggerFactory`), annotations (`@ClientSide`, `@ServerSide`, `@FieldsAreNonnullByDefault`), math/string/entity utilities, crash reporting.                                      |

### Key Conventions

- **`@ClientSide` / `@ServerSide`** annotations mark methods and fields that must only be accessed on the respective
  logical side. Violations cause classloading crashes in dedicated server or client environments.
- **`@FieldsAreNonnullByDefault` / `@MethodsReturnNonnullByDefault`** are applied at the package level via
  `package-info.java`. Fields and return values are non-null unless explicitly annotated `@Nullable`.
- **Lombok** is used extensively: `@CustomLog` for logging, `@Builder` for construction, `@Getter` for accessors.
  Records are used for pure data carriers; Lombok classes are used when behavior or builder patterns are needed.

---

## Package Map

Below is a condensed view of the top-level packages under `dev.breezes.settlements`:

```
settlements/
├── SettlementsMod.java              # @Mod entry point
├── package-info.java                # Package-level nullability defaults
│
├── domain/                          # Pure interfaces & models
│   ├── ai/                          #   Behavior contracts, conditions, memory, schedule, sensors
│   ├── entities/                    #   ISettlementsVillager, VillagerProfessionKey, Expertise
│   ├── generation/                  #   Building, layout, history, scoring, survey models
│   ├── enchanting/                  #   Enchanting domain models
│   ├── fishing/                     #   FishCatchEntry
│   └── ...                          #   genetics, inventory, skills, tags, time, world
│
├── application/                     # Use-case implementations
│   ├── ai/
│   │   ├── behavior/
│   │   │   ├── runtime/             #     AbstractBehavior, BaseVillagerBehavior, StateMachineBehavior
│   │   │   ├── usecases/villager/   #     All concrete behaviors (fishing, farming, animals, etc.)
│   │   │   └── workflow/            #     State machine: steps, stages, context
│   │   ├── brain/                   #     DefaultBrain, VanillaBehaviorPackages
│   │   └── sensors/                 #     Custom sensors (e.g., NearestSugarcaneSensor)
│   ├── enchanting/engine/           #   EnchantmentEngine, selection logic
│   └── ui/                          #   Bubble service, behavior/stats sessions & snapshots
│
├── infrastructure/                  # Minecraft/NeoForge adapters
│   ├── config/                      #   ConfigFactory, annotation processors
│   ├── minecraft/
│   │   ├── data/                    #     JSON data managers (fish, enchanting, buildings, etc.)
│   │   ├── entities/villager/       #     BaseVillager — the core entity class
│   │   └── worldgen/                #     Settlement structure generation
│   ├── network/                     #   Packet handlers, codecs, packet registry
│   └── rendering/                   #   Speech bubbles, particle rendering
│
├── presentation/                    # Client-only
│   ├── ui/                          #   Screens, client state, UI framework
│   └── commands/                    #   In-game commands
│
├── bootstrap/                       # Mod initialization
│   ├── event/                       #   CommonModEvents, ServerLifecycleEvents, etc.
│   └── registry/                    #   Deferred registries (items, entities, sounds, etc.)
│
├── di/                              # Dagger DI
│   ├── SettlementsComponent.java    #   Root @Singleton component
│   ├── ServerComponent.java         #   @ServerScope subcomponent
│   ├── ClientComponent.java         #   @ClientScope subcomponent
│   ├── SettlementsDagger.java       #   Static accessor (bridge for non-injectable code)
│   ├── ServerScope.java             #   Custom scope annotation
│   ├── ClientScope.java             #   Custom scope annotation
│   ├── behavior/                    #   BehaviorRegistration, BehaviorPackageResolver
│   └── modules/                     #   ConfigModule, DataManagerModule, GenerationModule,
│                                    #   server/ (BehaviorModule, ServerNetworkModule, etc.)
│                                    #   client/ (ClientNetworkModule, ClientStateModule)
│
└── shared/                          # Cross-cutting utilities
    ├── annotations/                 #   @ClientSide, @ServerSide, nullability defaults
    ├── logging/                     #   @CustomLog, LoggerFactory, ILogger
    └── util/                        #   Math, entity, string, crash utilities
```
