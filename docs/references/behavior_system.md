# Behavior System

This document covers how villager behaviors are registered, resolved into a day plan, and executed against the Minecraft
entity tick. For the Dagger fundamentals that underpin this system, see [Dagger Guide](dagger_guide.md); for the
step-by-step "add a behavior" recipe, see [Common Tasks](common_tasks.md#add-a-new-behavior).

---

## Overview

Behaviors are the core gameplay feature of Settlements. Each behavior (fishing, harvesting pumpkins, trading, etc.) is a
stateful state machine (`VillagerStateMachineBehavior`) that runs for the villager that owns it. Settlements does
**not**
register its behaviors as vanilla brain `Behavior`/`Activity` entries and does **not** let vanilla `GateBehavior` pick
them by weight. Instead it builds a **day plan** per villager and runs the plan with a **custom executor** that ticks
the active behavior directly. The vanilla `Brain` is still ticked — but only to host that executor and to run vanilla
reflexes (panic, raid) and gated ambient life.

The pipeline has four stages:

1. **Registration** — `BehaviorCatalogModule` describes every behavior (`BehaviorCatalogEntry`); `PoolModule` maps each
   profession to the `BehaviorKey`s it may perform (`ProfessionBehaviorPool`).
2. **Resolution** — `BehaviorPoolResolver` joins a villager's profession pool (plus universal behaviors) against the
   catalog into a `List<WeightedBehavior>` (availability + planning metadata).
3. **Planning** — `HeuristicPlanGenerator` packs the available behaviors into an ordered `DayPlan` of time-windowed
   `PlanSlot`s, using each behavior's category, intensity, cooldown, duration, and opportunity.
4. **Execution** — `PlanRunner`, hosted inside the always-on `PlanRunnerBehavior` (vanilla CORE activity), walks the
   plan each server tick and calls `IBehavior.tick(...)` on the active behavior.

```
Registration                    Resolution                 Planning                    Execution
┌────────────────────┐          ┌──────────────────┐       ┌───────────────────┐       ┌──────────────────────┐
│ BehaviorCatalog    │          │ BehaviorPool     │       │ HeuristicPlan     │       │ PlanRunnerBehavior   │
│ Module (@IntoSet   │          │ Resolver         │       │ Generator         │       │  (vanilla CORE host) │
│ BehaviorCatalog-   │─catalog─→│ resolve(prof) +  │─List< │ generate(ctx)     │─Day-  │   └→ PlanRunner.tick │
│ Entry)             │          │ universals       │ Weig- │  → DayPlan/       │ Plan─→│      → IBehavior     │
│ PoolModule (@Into- │─pools──→ │  → List<Weighted │ hted> │    PlanSlot[]     │       │        .tick(...)    │
│ Set ProfessionPool)│          │  Behavior>       │       │ (+ Opportunity-   │       │  PlanContextSwitcher │
│ BehaviorKey        │          │                  │       │  Forecaster)      │       │  (aligns Activity)   │
└────────────────────┘          └──────────────────┘       └───────────────────┘       └──────────────────────┘
```

Sensors complete the read/write loop around memories: sensors write memory, behaviors and the planner read it. There are
**two live sensor frameworks** (see [Sensors](#sensors-the-read-side) below and
[Villager Memory](common_tasks.md#villager-memory-vanilla-backed-vs-decaying)).

---

## Stage 1 — Registration

Registration is split across two modules because "what a behavior *is*" and "which professions may *do* it" are separate
concerns.

### The catalog — `BehaviorCatalogModule`

**File:** `di/modules/server/BehaviorCatalogModule.java`

Every behavior has exactly one `@Provides @IntoSet static BehaviorCatalogEntry` method here. The entry *describes* the
behavior; it does **not** bind it to a profession.

**`BehaviorCatalogEntry`** (`di/catalog/BehaviorCatalogEntry.java`) is a record of:

- `BehaviorPlanningMetadata descriptor` — the planner-facing metadata: `BehaviorKey key`, `BehaviorCategory category`
  (`WORK | SOCIAL | SELF_CARE | LEISURE | COMBAT`), `WorkIntensity intensity` (`HEAVY | LIGHT | NONE`), the required
  `BehaviorChannel`s, `cooldown` (`CooldownRange`), `estimatedDuration`, `interruptible`, and zero or more
  `OpportunityRequirement`s.
- `BehaviorDisplayMetadata displayInfo` — display name key + icon item, for the day-plan UI.
- `Supplier<IBehavior<BaseVillager>> factory` — creates a fresh behavior instance per run.

```
@Provides
@IntoSet
static BehaviorCatalogEntry harvestPumpkin(HarvestPumpkinConfig config, BehaviorSupport support) {
    return BehaviorCatalogEntry.builder()
            .descriptor(BehaviorPlanningMetadata.builder()
                    .key(BehaviorKey.HARVEST_PUMPKIN)
                    .category(BehaviorCategory.WORK)
                    .intensity(WorkIntensity.HEAVY)
                    .requiredChannel(BehaviorChannel.MOVEMENT)
                    .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                    .opportunity(new OpportunityRequirement.KnownSiteOpportunity(
                            Set.of(MemoryTypeRegistry.RIPE_PUMPKIN_SITES)))
                    .build())
            .displayInfo(BehaviorDisplayMetadata.builder()...build())
            .factory(() -> new HarvestPumpkinBehavior(config, support))
            .build();
}
```

The set is indexed by `BehaviorCatalogImpl` (implements `IBehaviorCatalog`), which exposes `getDescriptor(key)`,
`createBehavior(key)` (invokes the entry's `factory` → fresh instance), and `exists(key)`.

### The pools — `PoolModule`

**File:** `di/modules/server/PoolModule.java`

Each profession's available behaviors are a `@Provides @IntoSet static ProfessionBehaviorPool` method — a pure
availability mapping from a `VillagerProfessionKey` (a record with static constants, not an enum) to a list of
`PoolEntry`s. A `PoolEntry` is a `BehaviorKey` plus an optional relative weight (default 1; larger weights bias the
planner toward that behavior in packed windows).

```
static ProfessionBehaviorPool farmerPool() {
    return ProfessionBehaviorPool.builder()
            .profession(VillagerProfessionKey.FARMER)
            .entry(PoolEntry.of(BehaviorKey.HARVEST_PUMPKIN))
            .entry(PoolEntry.of(BehaviorKey.HARVEST_MELON))
            // ...
            .build();
}
```

Universal behaviors are **not** listed per profession — see [Resolution](#stage-2--resolution).

### `BehaviorKey`

**File:** `domain/ai/catalog/BehaviorKey.java`

The stable identity used everywhere else (catalog descriptor, pool entries, plan slots, day-plan UI). A new behavior
needs a new `BehaviorKey` constant.

> **The wiring seam:** the catalog entry and its config are Dagger-validated at compile time, but the pool mapping and
> `BehaviorKey` are not checked against the catalog. A behavior with a catalog entry that no pool references is dead —
> it will never be planned. See the [checklist in Common Tasks](common_tasks.md#add-a-new-behavior).

---

## Stage 2 — Resolution

**File:** `application/ai/catalog/BehaviorPoolResolver.java` (`@ServerScope`)

Injected with the `IBehaviorCatalog` and the `Set<ProfessionBehaviorPool>` from `PoolModule`. Its one method:

```
List<WeightedBehavior> resolve(VillagerProfessionKey profession)
```

It merges the profession's pool with a hardcoded set of **universal** behaviors — `UNIVERSAL_ENTRIES` = `EAT_FOOD`,
`TRADE_INITIATE`, `COURTSHIP_INITIATE`, `MANAGE_CHESTS`, `CRAFT_GOODS` — via `putIfAbsent`, so a profession's own weight
for a key wins over the universal default. Each merged `BehaviorKey` is joined against
`catalog.getDescriptor(key)` and paired with its pool weight into a `WeightedBehavior(descriptor, weight)`.

`COLLECT_DEMANDED_ITEM` is deliberately **not** in this list — it is catalog-present but pool-absent, installed
reactively by `CollectDemandedItemOverridePolicy` (see [
`behavior_orchestration.md`](behavior_orchestration.md#reactive-override-lane))
rather than scheduled as a plan slot. Same pattern as `INVESTIGATE` / `TRADE_ACCEPT`.

The result is the "availability + metadata" list handed to the planner. There is no day-type filtering here — that is
applied later as planner multipliers. (This is what solved the old per-profession `EatFood` duplication: universals are
merged once, centrally.)

---

## Stage 3 — Planning

**File:** `application/ai/planning/HeuristicPlanGenerator.java` (implements `IPlanGenerator`)

```
DayPlan generate(PlanGenerationContext context)
```

The generator packs `context.availableBehaviors()` (the `List<WeightedBehavior>`) into an ordered `DayPlan`, using the
descriptor metadata:

- **`WorkIntensity` + `BehaviorCategory`** drive rest-day multipliers and afternoon ordering (e.g. a charisma-gated
  social preference).
- **`estimatedDuration`** sets each slot's length and the packer's window budget.
- **`cooldown`** sets per-key spacing/cadence in the window packer — this is where behavior cadence is enforced (**not**
  at runtime; see the execution note below).
- **`OpportunityRequirement`** applies a down-weight (`0.2×`) to any key the `OpportunityForecaster` reports as lacking
  a live opportunity, rather than hard-excluding it.

### Plan data structures (`domain/ai/planning/`)

- **`DayPlan`** — an ordered `List<PlanSlot>` (rigid meal anchors + greedily packed work/afternoon/rest windows), plus
  `dayType`, `wakeAtAbsoluteTick`, a coarse `DayPlanSchedule` (ambient IDLE/WORK/MEET/REST blocks), and mutable
  `status` / `currentSlotIndex`. Key methods: `getCurrentSlot()`, `advanceSlot()`, `isExhausted()`.
- **`PlanSlot`** — `startTick` (0–24000, 0 = 6 AM), `BehaviorKey`, `priority`, `flexible` (flexible slots are skipped
  when preconditions fail; rigid slots are retried), `estimatedDurationTicks`, `reason`, and a mutable
  `PlanSlotStatus` (`PENDING → ACTIVE → COMPLETED | SKIPPED | INTERRUPTED`).

### Opportunity forecasting

**File:** `application/ai/planning/OpportunityForecaster.java` (`@ServerScope`)

`forecastLackingOpportunity(BaseVillager, List<WeightedBehavior>)` evaluates each descriptor's `OpportunityRequirement`s
(AND-combined) against a live probe (inventory, job-site block, decaying sensed sites via `SensedSiteReader`). It **must
run on the server thread** (the decaying-memory read lazily expires entries), so `PlanRunner` runs it while building the
context and passes only the plain `Set<BehaviorKey>` into planning.

### Async path and storage

`HeuristicAsyncPlanGenerator` (implements `IAsyncPlanGenerator`) wraps the sync generator in
`CompletableFuture.supplyAsync(..., @PlanGenerationExecutor)` so a plan can be pre-computed off-thread before a
villager's wake tick. There is currently **no LLM planner** — the async wrapper exists to exercise that orchestration
path until a slower generator is introduced. The plan lives in memory on the villager entity
(`BaseVillager.getDayPlan()`/`setDayPlan()`) and is deliberately **not persisted**: an unload discards it, and the next
plan tick regenerates it through `PlanRunner`'s ordinary missing-plan path. Transient cursor state lives in
`PlanRuntimeState` (`villager.getPlanRuntimeState()`).

> For the design intent behind day planning (rest-day policy, opportunity weighting, and the planned LLM intent
> overlay), see the day-planning working docs rather than this reference.

---

## Stage 4 — Execution

This is what replaced the old vanilla-`Activity` consumption path. Settlements behaviors are ticked by a **custom
executor**, not by vanilla `GateBehavior`.

### Host — `PlanRunnerBehavior`

**File:** `infrastructure/minecraft/behavior/planning/PlanRunnerBehavior.java`

A vanilla `Behavior<Villager>` registered into the brain's **CORE** activity at priority 20, so it ticks every server
tick regardless of the active non-core activity. It never self-terminates (`timedOut()` → `false`, `canStillUse()` →
`true`) so it can suspend/resume the inner behavior across PANIC/RAID. Its `tick(...)`:

1. If unsafe (hurt / hostile nearby) → `planRunner.forceStop(...)`, return.
2. `if (planRunner.tickOverride(...)) return;` — reactive overrides fire **before** the plan gate.
3. Gate on the active non-core activity: if it is not in `MANAGED_ACTIVITIES = {WORK, MEET, IDLE}` →
   `planRunner.suspendIfActive(...)` + `ensureValidPlan(...)`, return.
4. Otherwise `planRunner.tick(...)`.

### The executor loop — `PlanRunner`

**File:** `application/ai/planning/PlanRunner.java` (`@ServerScope`)

`tick(level, villager)` advances the plan clock, adopts any async-pre-generated plan whose wake tick has arrived, runs a
cascade of invalidation guards (async overrun, backward time jump, missing/exhausted/overdue plan, calendar-day
mismatch — each may `hardReset` = force-stop + regenerate), then dispatches on the current slot's status:

- **`PENDING` → `tryStartSlot`** — checks the slot window, `catalog.createBehavior(key)` for a fresh instance,
  force-completes the instance's cooldowns (cadence was already enforced at plan-gen time), runs
  `behavior.tickPreconditions(...)`; on success `behavior.start(...)`, records it on `PlanRuntimeState`, sets the
  `PLAN_BEHAVIOR_ACTIVE` memory, and marks the slot `ACTIVE`. A failed flexible slot is `SKIPPED`; a rigid slot arms a
  retry.
- **`ACTIVE` → `tickActiveSlot`** — **calls `behavior.tick(delta, level, villager)`** — the actual per-tick drive into
  the `VillagerStateMachineBehavior`. A run-duration ceiling (`descriptor.getMaxRunDuration()`, else a 1-minute default)
  aborts a stuck behavior. When `behavior.getStatus() == STOPPED`, the outcome is published, the memory cleared, and
  `plan.advanceSlot()` moves on.
- **`COMPLETED | SKIPPED | INTERRUPTED`** → clear `PLAN_BEHAVIOR_ACTIVE`, `plan.advanceSlot()`.

### Overrides and interruptibility

`tickOverride(...)` runs a set of `OverridePolicy` (sorted by priority) before the plan tick — reactive behaviors like
accepting a trade/courtship invite that can pre-empt the plan. An override may only interrupt the current behavior if
its descriptor is `interruptible` (`canInterruptCurrentPlanBehavior`); this is where the `interruptible` metadata is
enforced. On completion the interrupted slot is re-queued `PENDING`.

### The `PLAN_BEHAVIOR_ACTIVE` latch

Whenever a plan slot or override is active, `PlanRunner` sets the vanilla-backed `PLAN_BEHAVIOR_ACTIVE` memory. Its
consumer is `AmbientBehaviors.gated(...)`, which wraps every vanilla ambient behavior in a `GateBehavior` requiring
`PLAN_BEHAVIOR_ACTIVE` to be **absent**. So the memory is the mutual-exclusion latch: while a Settlements behavior runs,
vanilla ambient life is suppressed.

> **Never leave a nav step uncapped.** Because a running behavior holds `PLAN_BEHAVIOR_ACTIVE`, a behavior that stalls
> (e.g. an unreachable navigation target) freezes not just the villager but the rest of its day plan until the
> run-duration ceiling or a PANIC frees it. Cap `StayCloseStep`/`NavigateToTargetStep` with a timeout + fail-over
> transition.

### Activity alignment — `PlanContextSwitcher`

**File:** `infrastructure/minecraft/behavior/planning/PlanContextSwitcher.java`

Also a vanilla CORE `Behavior<Villager>` (priority 98). Once per second it derives the target vanilla `Activity` from
the active slot's `BehaviorCategory` (`WORK → WORK`, `SOCIAL → MEET`) or the `DayPlanSchedule`, and calls
`brain.setActiveActivityIfPossible(...)`. This keeps the vanilla activity aligned so the `PlanRunnerBehavior` gate opens
and the correct ambient package is active.

### The behavior contract

Behaviors implement `IBehavior<BaseVillager>` (`domain/ai/behavior/contracts/IBehavior.java`): preconditions,
continue-conditions, precondition/behavior cooldown `ITickable`s, `getStatus()`, `tickPreconditions`, `start`, `tick`,
`stop`, `requestStop`. Concrete villager behaviors extend `application/ai/behavior/runtime/VillagerStateMachineBehavior`
(→ `StateMachineBehavior<BaseVillager>` → `AbstractBehavior`). See
[Common Tasks](common_tasks.md#add-a-new-behavior) for the constructor/state-machine pattern.

---

## Vanilla brain integration that remains

`registerCustomGoals()` is gone; brain wiring now happens in `BaseVillager.registerBrainGoals(Brain<Villager>)`. What
survives:

- **CORE** (`brain.addActivity(Activity.CORE, ...)`) — `VanillaBehaviorPackages.getCorePackage(...)` (look-at, swim,
  panic trigger, …) **plus** the two Settlements hosts: `PlanRunnerBehavior` @20 and `PlanContextSwitcher` @98 (adults
  only).
- **PANIC / PRE_RAID / RAID / HIDE** — fully vanilla packages.
- **Adult ambient WORK / MEET / IDLE / REST** (`addActivityWithConditions(...)`) — vanilla *ambient* villager life
  (strolling, gossip, look-at) from `VanillaAmbientBehaviorPackages`, each internally `AmbientBehaviors.gated(...)`
  behind `PLAN_BEHAVIOR_ACTIVE`.
- **Babies** — pure vanilla IDLE/PLAY/MEET/REST packages; no plan runner.

So `addActivityWithConditions(Activity.WORK, ...)` still exists, but it now registers **gated vanilla ambient life**,
not Settlements behaviors. **No Settlements behavior is ever a vanilla brain `Behavior`/`Activity` entry** — the vanilla
brain contributes reflexes, panic/raid, and ambient filler, and provides the tick loop + activity gate that host the two
plan behaviors. The Settlements runtime (`PlanRunner`) owns all Settlements-behavior execution.

The whole runtime rides inside the vanilla brain tick: `BaseVillager.customServerAiStep()` →
`super.customServerAiStep()`
ticks the vanilla `Brain` → CORE runs `PlanRunnerBehavior` and `PlanContextSwitcher`. There is no custom AI goal and no
separate server-tick event.

---

## Sensors (the read side)

Sensors translate world/entity state into brain memory that behaviors and the planner read. There are **two live sensor
frameworks** — the older doc's claim that the mod-native one is "dormant" is no longer true:

- **Mod-native `AbstractSensor<BaseVillager>`** — bound as `VillagerSensorFactory` `@IntoSet` in `SensorCatalogModule`
  and ticked every villager tick by `VillagerBrain.tick(...)` (`BaseVillager.tick()` → `settlementsBrain.tick(1)`).
  Examples: `BlockResourceSensor`, `EntityPerceptionSensor`, `EntitySightingEmitterSensor`. This is the actively-used
  path for new senses, and the block-resource sensor is the shared-index sensing spine (see
  [Add a New Block Resource](common_tasks.md#add-a-new-block-resource-villager-block-sensing)).
- **Vanilla `Sensor<Villager>`** — registered as a `SensorType` in `SensorTypeRegistry` and listed in
  `BaseVillager.sensorTypes()` (a private static **method**, not a `SENSOR_TYPES` field), ticked by the vanilla brain.
  Examples: `OwnedPetsSensor`, `VillageChestsSensor`, `CultivationSiteSensor`, `WillingCourtshipPartnersSensor`,
  `SettlementsHurtBySensor`. Used for entity and block-entity senses.

For the memory side (vanilla-backed vs. decaying spatial, and the fact that neither persists across a reload), see
[Villager Memory](common_tasks.md#villager-memory-vanilla-backed-vs-decaying).

---

## Behavior-status UI

The real-time behavior/plan UI is the **Day Plan snapshot pipeline**, built from the villager's current
`DayPlan`/`PlanSlot` schedule (not from a live behavior instance shared with the brain):

`DayPlanSnapshotAssembler` (`@ServerScope`, reads the villager's `DayPlan`) → `DayPlanSnapshot` (slots carry a
`DayPlanSlotVisualStatus`) → `DayPlanSnapshotPublisher` → `ClientBoundDayPlanSnapshotPacket` → `DayPlanScreen`.

---

## Adding a new behavior

The step-by-step recipe — config record, behavior class, `ConfigModule`, `BehaviorCatalogModule` entry, `BehaviorKey`,
and the required `PoolModule` mapping — lives in [Common Tasks](common_tasks.md#add-a-new-behavior). In short:

1. Create the behavior class in `application/ai/behavior/usecases/villager/<category>/`, extending
   `VillagerStateMachineBehavior`.
2. Create its `@BehaviorConfig` record in the same package.
3. Add a `@Provides @Singleton` config method in `di/modules/ConfigModule.java`.
4. Add a `@Provides @IntoSet BehaviorCatalogEntry` method in `di/modules/server/BehaviorCatalogModule.java`, and a
   `BehaviorKey` constant.
5. Add the `BehaviorKey` to the relevant profession pool (s) in `di/modules/server/PoolModule.java`.
6. Build (Dagger validates the catalog + config graph) and verify in-game that the villager actually plans and runs the
   behavior — steps 4–5's key/pool wiring is not compile-checked.
