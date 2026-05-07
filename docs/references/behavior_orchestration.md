# Behavior Orchestration Framework

This document describes how Settlements villagers are orchestrated across activity contexts — how the
plan-driven foreground and vanilla ambient background coexist within the same brain tick without
conflicting over movement, interaction, or navigation targets.

---

## Overview

Villager intelligence in Settlements is split into two concerns running concurrently on the vanilla
brain:

1. **Foreground (plan-driven)** — `PlanRunner` executes a personalized `DayPlan` of sequential
   `PlanSlot` entries. It owns what the villager is *doing* at any given moment.
2. **Background (activity-ambient)** — contextual vanilla behaviors registered under the active
   activity fill the villager's time when `PlanRunner` is not occupying a resource channel. They
   own what the villager *feels like* during that time (strolling near the job site during work
   hours, gathering at the bell during social hours, wandering idly otherwise).

`PlanRunnerBehavior` lives in `Activity.CORE` at priority 20 and ticks every server tick regardless
of which activity is asserted. A companion CORE behavior, `PlanContextSwitcher` at priority 98,
reads the current plan and its authored schedule each tick and calls
`brain.setActiveActivityIfPossible()` to select the right ambient context. The active activity's
behaviors provide the background layer.

```
Server tick
  ├── Activity.CORE (always runs)
  │     ├── [vanilla @ priority 0: Swim, InteractWithDoor, LookAtTargetSink, WakeUp,
  │     │                          ReactToBell, SetRaidStatus, VillagerPanicTrigger, ...]
  │     ├── PlanRunnerBehavior   @ priority 20  ← executes DayPlan slots; suspends outside managed activities
  │     └── PlanContextSwitcher  @ priority 98  ← reads DayPlan schedule + active slot → sets active activity
  │
  └── Active activity (set by PlanContextSwitcher or vanilla overrides)
        Activity.WORK   → ambient: walk to job site, give gift to hero, show trades, etc  (see VanillaAmbientBehaviorPackages)
        Activity.MEET   → ambient: stroll to meeting point, socialize at bell, show trades, etc
        Activity.IDLE   → ambient: village-bound stroll, interact with villager/cat, show trades, etc
        Activity.REST   → walk home, sleep in bed, brownian wander  (PlanRunner is suspended)
        Activity.PANIC  → [vanilla]  ─┐
        Activity.PRE_RAID → [vanilla]  │
        Activity.RAID   → [vanilla]   ├─ override via vanilla CORE behaviors; PlanRunner suspends
        Activity.HIDE   → [vanilla]  ─┘
```

When vanilla CORE behaviors assert PANIC, RAID, or HIDE they take precedence because they fire
before `PlanContextSwitcher` in the same tick (lower priority number). `PlanRunnerBehavior` detects
it is outside the managed activity set on its next tick and suspends the active plan slot. When the
vanilla override clears and `PlanContextSwitcher` re-asserts a managed activity, the plan resumes.

---

## Key Concepts

### PlanRunnerBehavior

**File:** `infrastructure/minecraft/behavior/planning/PlanRunnerBehavior.java`

A `Behavior<Villager>` registered in `Activity.CORE` at priority 20. It wraps the `PlanRunner`
application service and drives it on every server tick.

| Property | Behavior |
|----------|----------|
| `timedOut()` | Always `false` — the behavior never expires by timeout. |
| `canStillUse()` | Always `true` — the behavior never yields to vanilla's lifecycle management; stop/start decisions belong to `PlanRunner` itself. |
| `checkExtraStartConditions()` | Guards on: entity is `BaseVillager`, not hurt by hostile. |
| `tick()` | See below. |
| `stop()` | Calls `planRunner.forceStop()` — cleans up current behavior and runtime state. |

**`tick()` logic (evaluated in order each tick):**

1. Re-checks the panic condition (hurt or hostile nearby). If unsafe → `planRunner.suspendIfActive()` and return.
2. If the active non-core activity is absent *or* not in the managed set `{WORK, MEET, IDLE}` →
   `planRunner.suspendIfActive()`, then `planRunner.ensurePlanForCurrentDay()`, and return.
   The `ensurePlanForCurrentDay` call is load-bearing: REST can span the game-day rollover, so the
   runner prepares a fresh plan before the activity flips back to a managed activity at wake time.
3. Otherwise → `planRunner.tick()`.

`PlanRunnerBehavior` is `@ServerScope` (scoped on the Dagger provider, not the class annotation)
and retrieved from its provider inside `BaseVillager.registerBrainGoals()`.

### PlanContextSwitcher

**File:** `infrastructure/minecraft/behavior/planning/PlanContextSwitcher.java`

A `Behavior<Villager>` registered in `Activity.CORE` at priority 98. It runs on a 1-second
cooldown and caches the last derived activity to avoid redundant `setActiveActivityIfPossible`
calls on stable ticks. Each eligible tick it derives a target activity and calls
`brain.setActiveActivityIfPossible(target)`.

**Phase derivation (evaluated in order):**

| Condition | Target activity |
|-----------|----------------|
| No current `DayPlan`, or plan is for a different game-day | `fallbackActivity()` — see below |
| `linearNow >= schedule.bedtimeTick()` (wake-relative) | `Activity.REST` |
| Active slot with `BehaviorCategory.WORK` | `Activity.WORK` |
| Active slot with `BehaviorCategory.SOCIAL` | `Activity.MEET` |
| Active slot with any other category (SELF_CARE, LEISURE, COMBAT) | fall through |
| Current tick within a `DayPlanActivityBlock` with `DayPlanActivityContext.WORK` | `Activity.WORK` |
| Current tick within a `DayPlanActivityBlock` with `DayPlanActivityContext.MEET` | `Activity.MEET` |
| Current tick within a `DayPlanActivityBlock` with `DayPlanActivityContext.IDLE` | `Activity.IDLE` |
| Current tick within a `DayPlanActivityBlock` with `DayPlanActivityContext.REST` | `Activity.REST` |
| No block contains the current tick | `Activity.IDLE` |

The "active slot" check reads `BehaviorPlanningMetadata` from `PlanRuntimeState.getCurrentDescriptor()`.
The slot must have status `ACTIVE`; a pending or completed slot does not claim the context.

All comparisons use **wake-relative linear time** — tick values are normalised modulo the day length
relative to the plan's `wakeTick` — so professions that start before vanilla tick zero are handled
correctly without wraparound errors.

**`fallbackActivity()` — plan-absent path:**

When the villager has no valid plan for the current game-day (unloaded chunk, first spawn, plan
generation lag), `PlanContextSwitcher` falls back to a profession-default schedule rather than
freezing the villager:

- Loads `ScheduleProfile.defaultFor(professionKey)` for the villager's current profession.
- Computes REST, WORK, and MEET windows in wake-relative linear time from that profile.
- Professions with no work interval (currently Nitwit) receive `Activity.IDLE` throughout.

`PlanContextSwitcher` does not assert PANIC, RAID, or HIDE — those are owned by vanilla CORE
behaviors (`VillagerPanicTrigger`, `SetRaidStatus`, `ReactToBell`) which fire at priority 0.
Because `setActiveActivityIfPossible` is gated on activity preconditions, a vanilla override that
is already active simply blocks `PlanContextSwitcher`'s assertion on subsequent ticks until the
override clears.

### DayPlanSchedule and DayPlanActivityBlock

These two records are the source of truth for the authored schedule that `PlanContextSwitcher`
reads when a plan is present.

**`DayPlanSchedule`** (`domain/ai/planning/DayPlanSchedule.java`):
- `wakeTick` — game tick when the authored day begins (may be before vanilla tick 0 for early-rise professions).
- `bedtimeTick` — game tick when the authored day ends; `PlanContextSwitcher` transitions to REST at this boundary.
- `activityBlocks` — ordered list of `DayPlanActivityBlock`.

**`DayPlanActivityBlock`** (`domain/ai/planning/DayPlanActivityBlock.java`):
- `context` — `DayPlanActivityContext` enum (`WORK`, `MEET`, `IDLE`, `REST`).
- `startTick` / `endTick` — inclusive/exclusive tick range; may wrap across midnight.
- `reason` — human-readable label for debugging/logging.

`HeuristicPlanGenerator` populates these blocks when it generates the day plan. `PlanContextSwitcher`
iterates them linearly (the list is small by design; switch to binary search if it grows).

### PLAN_BEHAVIOR_ACTIVE Memory

**Registry:** `domain/ai/memory/MemoryTypeRegistry.java`

Defined as a `MemoryType<Boolean>` backed by `MemoryModuleTypeRegistry.PLAN_BEHAVIOR_ACTIVE`
(the Forge `DeferredRegister<MemoryModuleType<?>>` entry lives in
`bootstrap/registry/memory/MemoryModuleTypeRegistry.java`).

**Ownership: `PlanRunner` exclusively.** `PlanContextSwitcher` does not touch this memory — it owns
activity selection only. Only `PlanRunner` has visibility into whether an `IBehavior` is currently
executing.

- **Set** by `PlanRunner` immediately when a plan slot behavior transitions to `ACTIVE`
  (after `behavior.start()` returns).
- **Cleared** by `PlanRunner` when the behavior stops (`COMPLETED`, `SKIPPED`, `INTERRUPTED`),
  or when `suspendIfActive()` or `forceStop()` is called.

Ambient behaviors registered under `Activity.WORK`, `Activity.MEET`, and `Activity.IDLE` require
`PLAN_BEHAVIOR_ACTIVE: VALUE_ABSENT` as a precondition. This means they only activate during genuine
idle windows — between plan slots, during exhaustion mode, or while a plan slot behavior is in a
passive waiting state — and they never compete with plan execution for walk targets, look targets,
or interaction locks.

**Explicit design decision:** the flag is binary (occupied / not occupied) rather than per-channel.
Per-channel gating — where, for example, an ambient movement behavior could run alongside a plan
behavior that claims only `BehaviorChannel.COGNITION` — is the intended Phase 3 upgrade path. The
`BehaviorChannel` declarations already exist on `BehaviorPlanningMetadata` and are reserved for
that purpose. The binary flag is the deliberate Phase 1 simplification.

### AmbientBehaviors

**File:** `infrastructure/minecraft/behavior/ambient/AmbientBehaviors.java`

A utility class with a single static factory method that wraps any vanilla `BehaviorControl` with
the `PLAN_BEHAVIOR_ACTIVE: VALUE_ABSENT` precondition gate:

```java
// Wraps a vanilla BehaviorControl so it only activates when PlanRunner is not occupying the entity
public static <T extends LivingEntity> BehaviorControl<T> gated(@Nonnull BehaviorControl<? super T> behavior)
```

All ambient behaviors registered under `Activity.WORK`, `Activity.MEET`, and `Activity.IDLE` are
created through this wrapper — never registered bare. `Activity.REST` behaviors are registered
directly without gating because `PlanRunner` is always suspended during REST.

`ShowTradesToPlayer` is intentionally registered without `gated()` in the WORK, MEET, and IDLE
activities so that player-initiated trading can preempt plan slot execution regardless of plan state.

For the full list of ambient behaviors registered under each activity, see
`VanillaAmbientBehaviorPackages`.

### Managed Activity Set

The set `{Activity.WORK, Activity.MEET, Activity.IDLE}` defines where `PlanRunner` is active.
These are stock vanilla `Activity` values — no new activity registration is required for the core
orchestration design.

`Activity.REST` is deliberately excluded from the managed set. The plan covers waking hours only;
`PlanContextSwitcher` transitions to REST when `linearNow >= schedule.bedtimeTick()`, and vanilla's
`WakeUp` (already in CORE) returns the villager at wake tick. The `HeuristicPlanGenerator` does
not produce a sleep `PlanSlot` — sleep timing is a biological schedule concern, not a task the
planner decides.

---

## Removed components

| Removed | Replaced by |
|---------|-------------|
| `ActivityRegistry.SETTLEMENTS_PLAN` | Vanilla `Activity.WORK/MEET/IDLE/REST` |
| `ScheduleRegistry.PLAN_SCHEDULE` | `ScheduleRegistry.SETTLEMENTS_SCHEDULE` — a minimal stub (default `Activity.IDLE` at noon) set on adults so the vanilla brain has a non-null schedule; actual activity transitions are driven by `PlanContextSwitcher`, not this schedule |
| `UpdateActivityFromSchedule` in adult brain | `PlanContextSwitcher` at priority 98 (babies still use `UpdateActivityFromSchedule` via the vanilla baby packages) |

---

## Explicit Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| `PlanRunnerBehavior` location | `Activity.CORE` | Must survive activity transitions without stop/start; CORE always ticks regardless of active activity. Placing it in each activity would restart it on every transition, interrupting mid-slot execution. |
| Activity transitions | Dynamic (`PlanContextSwitcher`) | Per-villager schedules vary by profession and authored plan blocks. A static `Schedule` cannot represent this. |
| Vanilla activities reused | `Activity.WORK`, `MEET`, `IDLE`, `REST` | No new activity registration required. Vanilla brain priority already handles PANIC/RAID/HIDE override for free via existing CORE behaviors. |
| Plan covers waking hours only | No sleep `PlanSlot` | Sleep timing is biological (`DayPlanSchedule.bedtimeTick`), not a task. The heuristic planner and future LLM planner generate task slots; neither should decide when a villager sleeps. |
| `PLAN_BEHAVIOR_ACTIVE` is binary | Yes/No flag | Simpler than a per-channel bitmask. `BehaviorChannel` metadata already exists on `BehaviorPlanningMetadata` for the Phase 3 upgrade to channel-granular gating. |
| `Activity.REST` excluded from managed set | PlanRunner suspends | Sleep is fully ambient; no plan slots exist for that window. Clean boundary between task scheduling and biological rhythm. |
| Ambient behaviors wrap vanilla | `AmbientBehaviors.gated(inner)` | Reuses vanilla behavior logic unchanged; only the precondition gate is added. |
| `ShowTradesToPlayer` is not gated | Registered bare in WORK, MEET, IDLE | Player-initiated trading must be able to preempt plan slot execution. Gating it would prevent the trade GUI from opening while a behavior is running. |
| `PlanContextSwitcher` uses 1-second cooldown | `Ticks.seconds(1)` | Activity context only changes at schedule or plan-slot boundaries. Re-deriving every tick wastes CPU and produces spurious `setActiveActivityIfPossible` calls on stable ticks. |
| Gene offsets applied at generation time | In `HeuristicPlanGenerator` | `ScheduleProfile` is a plain per-profession record of defaults. CON gene offsets wake/sleep tick and WIL gene offsets work-end tick; these adjustments belong to plan generation, not the profile data model. |
| Fallback when plan is absent | `PlanContextSwitcher.fallbackActivity()` | Unloaded or first-spawn villagers must still cycle through plausible ambient activities instead of freezing on `Activity.IDLE` permanently. |

---

## Extending the Framework

### Adding an ambient behavior to an existing activity

1. Wrap the vanilla `BehaviorControl` with `AmbientBehaviors.gated(...)` (unless it must preempt
   plan execution — see `ShowTradesToPlayer` above).
2. Register it in `VanillaAmbientBehaviorPackages` under the relevant activity's list at an
   appropriate priority.
3. Update this document.

### Adding a new managed activity

Example: a future festival or ceremony activity.

1. Register the activity in `ActivityRegistry` if it is Settlements-owned, or reuse a vanilla
   `Activity` value if appropriate.
2. Register its ambient behaviors in `VanillaAmbientBehaviorPackages`, gated or ungated per the
   intent described above.
3. Add the activity to `PlanRunnerBehavior`'s managed set if plan slots should execute during it.
   Omitting it gives the suspend behavior for free.
4. Add a derivation case to `PlanContextSwitcher` if the activity is asserted on a time or
   plan-phase basis. If the activity is triggered reactively (like PANIC via `VillagerPanicTrigger`)
   no `PlanContextSwitcher` change is needed — the reactive trigger fires before it and
   `setActiveActivityIfPossible` is naturally blocked by the override.
5. Update this document.

### Adding a plan-driven (foreground) behavior

Plan-driven behaviors execute as `PlanSlot` entries and are not registered here. They belong to the
behavior catalog and pool system. Ambient background behaviors and plan slot behaviors are entirely
separate concerns — one fills idle time, the other is the scheduled task.

---

## Vanilla-Delegated Activities

The following activities are currently fully vanilla. Settlements registers no behaviors under them
and makes no modifications to their logic.

| Activity | What triggers it | Handler |
|----------|-----------------|---------|
| `Activity.PANIC` | `VillagerPanicTrigger` (CORE, priority 0) detects nearby threat | Vanilla |
| `Activity.PRE_RAID` | Vanilla pre-raid logic (CORE) | Vanilla |
| `Activity.RAID` | `SetRaidStatus` (CORE, priority 0) detects active raid | Vanilla |
| `Activity.HIDE` | `ReactToBell` (CORE, priority 0) hears bell with no active raid | Vanilla |
| `Activity.PLAY` | Baby villagers — set at birth on the vanilla baby schedule | Vanilla |

`PlanRunnerBehavior` is not registered for baby villagers. Babies remain on the vanilla
PLAY/IDLE/REST schedule with standard `VillagerGoalPackages` behaviors. The behavior catalog
contains adult profession routines and is not appropriate for baby villagers.

For PANIC, RAID, and HIDE: when vanilla asserts one of these, `PlanContextSwitcher` no longer
controls the active activity. `PlanRunnerBehavior` detects this on its next tick (current activity
not in managed set) and calls `planRunner.suspendIfActive()`. When the vanilla override clears,
`PlanContextSwitcher` re-asserts the appropriate managed activity and the plan resumes from where
it was interrupted.
