# Behavior Orchestration Framework

This document describes how Settlements villagers are orchestrated across activity contexts — how the
plan-driven foreground, the reactive override lane, and the vanilla ambient background coexist within
the same brain tick without conflicting over movement, interaction, or navigation targets.

For the perception/knowledge/social-cue machinery that *feeds* the reactive lane, see
[`event_lane.md`](event_lane.md). For the plan-generation internals (behavior catalog, pool
resolution, window packing), see [`behavior_system.md`](behavior_system.md).

---

## Overview

Villager intelligence in Settlements runs as several concurrent lanes on the vanilla brain plus a few
post-brain ticks driven directly from `BaseVillager.customServerAiStep()`:

1. **Foreground (plan-driven)** — `PlanRunner` executes a personalized `DayPlan` of sequential
   `PlanSlot` entries. It owns what the villager is *doing* at any given moment.
2. **Reactive override** — a preemption slot that rides *inside* `PlanRunnerBehavior`. Pluggable
   `OverridePolicy` strategies can interrupt the running plan slot (independently of vanilla
   PANIC/RAID/HIDE) when a fresh, high-priority stimulus arrives — a trade/courtship invite to
   accept, or a demanded item spotted nearby.
3. **Background (activity-ambient)** — contextual vanilla behaviors registered under the active
   activity fill the villager's time when the foreground is not occupying a resource channel. They
   own what the villager *feels like* during that time (strolling near the job site during work
   hours, gathering at the bell during social hours, wandering idly otherwise).
4. **SocialCue + perception lanes** — ticked *after* the brain each server tick. The SocialCue lane
   presents gaze/gesture/bubble cues (channel-arbitrated against the running behavior); the
   perception pipeline drains the world-event bus into per-villager knowledge. Both belong to the
   event-lane subsystem — see [`event_lane.md`](event_lane.md).

`PlanRunnerBehavior` lives in `Activity.CORE` at priority 20 and ticks every server tick regardless
of which activity is asserted. A companion CORE behavior, `PlanContextSwitcher` at priority 98,
reads the current plan and its authored schedule each tick and calls
`brain.setActiveActivityIfPossible()` to select the right ambient context. The active activity's
behaviors provide the background layer.

```
Server tick → BaseVillager.customServerAiStep()
  1. seedEventCursorOnFirstTick()   ← skip pre-load WorldEventBus history on a freshly loaded villager
  2. super.customServerAiStep()     ← vanilla sensors + brain housekeeping
  3. settlementsBrain.tick(1)       ← the vanilla Brain tick (expanded below)
  4. tickSocialCue()                ← SocialCueArbiter: gaze/gesture/bubble cues     ┐ event lane
  5. tickPerception()  (throttled)  ← PerceptionPipeline: drain bus → knowledge      ┘ (see event_lane.md)
  6. tickReconciler()               ← discharge crash-orphaned teardown obligations

  Brain.tick (step 3):
    Activity.CORE (always runs)
      ├── [vanilla @ 0: Swim, InteractWithDoor, LookAtTargetSink, WakeUp,
      │                 ReactToBell, SetRaidStatus, VillagerPanicTrigger, ...]
      ├── PlanRunnerBehavior   @ 20  ← panic→forceStop; else tickOverride (reactive lane); else plan tick
      └── PlanContextSwitcher  @ 98  ← reads DayPlan schedule + active slot → sets active activity
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

The **reactive override lane** is different: it is *not* a vanilla brain override and *not* a new
activity. It runs inside `PlanRunnerBehavior.tick()` before the activity gate, so it can preempt the
plan under any managed activity. See [Reactive Override Lane](#reactive-override-lane) below.

---

## Key Concepts

### PlanRunnerBehavior

**File:** `infrastructure/minecraft/behavior/planning/PlanRunnerBehavior.java`

A `Behavior<Villager>` registered in `Activity.CORE` at priority 20. It wraps the `PlanRunner`
application service and drives it on every server tick. It is only registered for **adult**
villagers (`if (!this.isBaby())` in `BaseVillager.registerBrainGoals()`); babies stay on the vanilla
baby packages.

| Property | Behavior |
|----------|----------|
| `timedOut()` | Always `false` — the behavior never expires by timeout. |
| `canStillUse()` | Always `true` — the behavior never yields to vanilla's lifecycle management; stop/start decisions belong to `PlanRunner` itself. |
| `checkExtraStartConditions()` | Guards on: entity is `BaseVillager`, and is safe (`!VillagerPanicTrigger.isHurt` and `!VillagerPanicTrigger.hasHostile`). |
| `tick()` | See below. |
| `stop()` | Calls `planRunner.forceStop()` — discharges both the override slot and the active plan behavior. |

**`tick()` logic (evaluated in order each tick):**

1. **Panic guard.** Re-checks safety (hurt or hostile nearby). If unsafe → `planRunner.forceStop()`
   and return. `forceStop` discharges *both* the override slot and the active plan behavior.
2. **Override gate.** `planRunner.tickOverride(level, villager)` — evaluates and advances the
   reactive override slot. If it returns `true`, an override is active this tick and the plan tick is
   skipped entirely (return). This runs *before* the activity gate so reactive accepts fire ahead of
   normal plan execution, under any activity. See [Reactive Override Lane](#reactive-override-lane).
3. **Activity gate.** If the active non-core activity is absent *or* not in the managed set
   `{WORK, MEET, IDLE}` → `planRunner.suspendIfActive()`, then `planRunner.ensureValidPlan()`, and
   return. The `ensureValidPlan` call is load-bearing: REST can span the game-day rollover, so the
   runner prepares/repairs a valid plan before the activity flips back to a managed activity at wake
   time.
4. Otherwise → `planRunner.tick()`.

`PlanRunnerBehavior` is `@ServerScope` (scoped on the Dagger provider, not the class annotation)
and retrieved from its provider inside `BaseVillager.registerBrainGoals()`. `PlanContextSwitcher`,
by contrast, is plain-constructed (`new PlanContextSwitcher()`).

### PlanContextSwitcher

**File:** `infrastructure/minecraft/behavior/planning/PlanContextSwitcher.java`

A `Behavior<Villager>` registered in `Activity.CORE` at priority 98. It runs on a 1-second
cooldown and caches the last derived activity to avoid redundant `setActiveActivityIfPossible`
calls on stable ticks. Each eligible tick it derives a target activity and calls
`brain.setActiveActivityIfPossible(target)`. It does **not** consult the override or event lanes —
those deliberately sit inside `PlanRunnerBehavior` so they can fire regardless of which activity
`PlanContextSwitcher` last asserted.

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
- `authoredDayDurationTicks()` — derived helper: `bedtimeTick - wakeTick`.

**`DayPlanActivityBlock`** (`domain/ai/planning/DayPlanActivityBlock.java`):
- `context` — `DayPlanActivityContext` enum (`WORK`, `MEET`, `IDLE`, `REST`).
- `startTick` / `endTick` — inclusive/exclusive tick range; may wrap across midnight.
- `reason` — human-readable label for debugging/logging.

`HeuristicPlanGenerator` populates these blocks when it generates the day plan (only `WORK`, `MEET`,
and `IDLE` blocks are emitted; REST is handled by the `bedtimeTick` boundary, not a block).
`PlanContextSwitcher` iterates them linearly (the list is small by design; switch to binary search
if it grows).

### PLAN_BEHAVIOR_ACTIVE Memory

**Registry:** `domain/ai/memory/MemoryTypeRegistry.java`

Defined as a `MemoryType<Boolean>` backed by `MemoryModuleTypeRegistry.PLAN_BEHAVIOR_ACTIVE`
(the `DeferredRegister<MemoryModuleType<?>>` entry lives in
`bootstrap/registry/memory/MemoryModuleTypeRegistry.java`).

**Ownership: `PlanRunner` exclusively.** `PlanContextSwitcher` does not touch this memory — it owns
activity selection only. Only `PlanRunner` has visibility into whether an `IBehavior` is currently
executing.

- **Set** by `PlanRunner` when a plan slot behavior transitions to `ACTIVE` (in `tryStartSlot`),
  and also when a reactive **override** slot is installed (in `tryStartOverride`). Both cases mean
  "the runner is occupying the body."
- **Cleared** by `PlanRunner` when the plan or override behavior stops (`COMPLETED`, `SKIPPED`,
  `INTERRUPTED`), or when `suspendIfActive()`, `forceStop()`, or the override abort/complete paths run.

Ambient behaviors registered under `Activity.WORK`, `Activity.MEET`, and `Activity.IDLE` require
`PLAN_BEHAVIOR_ACTIVE: VALUE_ABSENT` as a precondition. This means they only activate during genuine
idle windows — between plan slots, during exhaustion mode, or while a plan slot behavior is in a
passive waiting state — and they never compete with plan execution (or an active override) for walk
targets, look targets, or interaction locks.

**Explicit design decision:** the flag is binary (occupied / not occupied) rather than per-channel.
Per-channel gating — where, for example, an ambient movement behavior could run alongside a plan
behavior that claims only `BehaviorChannel.COGNITION` — is the intended future upgrade path. The
`BehaviorChannel` declarations already exist on `BehaviorPlanningMetadata` and are used *today* by
the SocialCue lane (whose cues are admitted only when their channel set is disjoint from the running
behavior's `requiredChannels`), but the foreground/ambient boundary is still governed by the binary
flag.

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
`VanillaAmbientBehaviorPackages` (`application/ai/brain/VanillaAmbientBehaviorPackages.java`).

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

## Reactive Override Lane

The override lane lets a fresh, high-priority stimulus interrupt the running plan slot — *without*
being a vanilla brain override and *without* registering a new activity. It rides inside
`PlanRunnerBehavior` (the priority-20 CORE behavior), so it can fire under any activity, and it is
strictly bounded so a wedged override never freezes the villager.

### Where it runs

`PlanRunnerBehavior.tick()` calls `planRunner.tickOverride(level, villager)` *before* the activity
gate (step 2 of the tick logic above). If it returns `true`, the plan tick is skipped. Vanilla panic
still short-circuits everything before the override gate is reached.

**File:** `application/ai/planning/PlanRunner.java` (override state machine),
`application/ai/planning/PlanRuntimeState.java` (the override slot).

### The override slot

`PlanRuntimeState` holds a dedicated override slot (`overrideBehavior`, `overrideBehaviorKey`,
`overrideElapsedTicks`) separate from the plan slot, plus `isOverrideActive()` / `installOverride()`
/ `clearOverride()`. Keeping it separate means an override never mutates plan-slot bookkeeping; the
interrupted slot is simply re-queued when the override ends. (The runtime comment notes the slot
"could become a Deque later to support a small priority stack.")

### `tickOverride` state machine

- **Active override** → `tickActiveOverride`: increments elapsed ticks; if it exceeds
  `MAX_OVERRIDE_DURATION_TICKS` (**60 s** — a safety-net ceiling matching the default per-behavior
  ceiling), `abortStuckOverride` force-stops it, clears the slot and the plan-active lock, and
  re-queues the interrupted slot. Otherwise it ticks the override; on `STOPPED` it calls
  `onOverrideCompleted`.
- **No active override** → `tryStartOverride`:
  1. Bails unless the current plan behavior is interruptible
     (`canInterruptCurrentPlanBehavior` → `descriptor == null || descriptor.isInterruptible()`).
  2. Evaluates the injected `Set<OverridePolicy>` in **priority order** (`OverridePolicy.priority()`
     descending, class-name tie-break), taking the first `OverrideRequest`.
  3. Builds the requested behavior from the catalog, force-completes its cooldowns, and checks
     preconditions. If preconditions are not yet met, it returns `false` — the policy polls every
     tick, so nothing is consumed and the still-live trigger is re-evaluated next tick.
  4. Suspends the running plan behavior (`suspendIfActive` marks the slot `INTERRUPTED`), installs
     the override, sets `PLAN_BEHAVIOR_ACTIVE`, and starts it.

### Override policies (the non-vanilla preemption set)

Registered as a Dagger multibinding in `di/modules/server/OverridePolicyModule.java`
(`@Multibinds Set<OverridePolicy>`, all policies `@IntoSet`):

| Policy | Priority | Fires when | Installs |
|--------|----------|-----------|----------|
| `SocialAcceptOverridePolicy` | 100 | Another villager has sent a trade or courtship invite. Delegates to `OverrideTriggerDetector`, which polls `CourtshipSessionRegistry` / `TradeSessionRegistry` (courtship > trade). | `COURTSHIP_ACCEPT` / `TRADE_ACCEPT` |
| `CollectDemandedItemOverridePolicy` | 50 | `DemandedGroundItemSensor` has flagged a demanded item nearby (`MemoryTypeRegistry.DEMANDED_GROUND_ITEM_NEARBY`) **and** no plan/override behavior is currently active **and** the villager's active non-core activity is one of `WORK`/`MEET`/`IDLE`. | `BehaviorKey.COLLECT_DEMANDED_ITEM` |

Unlike `SocialAcceptOverridePolicy`, `CollectDemandedItemOverridePolicy` explicitly checks `PLAN_BEHAVIOR_ACTIVE`
itself rather than relying solely on `tryStartOverride`'s `canInterruptCurrentPlanBehavior` gate.
That gate only blocks on a non-interruptible plan behavior — `COLLECT_DEMANDED_ITEM` (like most
behaviors) is `interruptible(true)`, so without the explicit check this policy could preempt an
in-progress WORK/MEET/IDLE behavior. The design intent is opportunistic idle-time top-up only,
never a work interruption, so the policy adds its own idle-gap + managed-activity gate on top. The
managed-activity check also matters because `tickOverride` runs before the activity gate and is
therefore evaluated during REST too, where `PLAN_BEHAVIOR_ACTIVE` is likewise absent — restricting
to `WORK`/`MEET`/`IDLE` is what keeps this from firing during sleep.

Both are `@ServerScope` and must be stateless/pure — `evaluate(level, villager)` returns
`Optional<OverrideRequest>` and is polled every tick.

On completion, `onOverrideCompleted` publishes the behavior outcome and re-queues the interrupted
slot so the villager resumes its day where the override pre-empted it.

### Where the stimuli come from

`SocialAcceptOverridePolicy` reads the session registries fed by trade/courtship invite emissions;
`CollectDemandedItemOverridePolicy` reads the demanded-item sensor memory. The invite upstream is the
**event lane** — see [`event_lane.md`](event_lane.md).

---

## Context-Aware Planning (foreground shaping)

The plan the foreground lane runs is **opportunity-shaped**: behaviors whose real-world
preconditions cannot currently be met are down-weighted at generation time so the plan spends the
villager's day on work that can actually happen. This is a plan-*generation* concern; the full
generator internals live in [`behavior_system.md`](behavior_system.md). The orchestration-relevant
summary:

- **Declared requirements.** A behavior declares zero or more `OpportunityRequirement`s on its
  `BehaviorPlanningMetadata.opportunities`. `OpportunityRequirement`
  (`domain/ai/planning/OpportunityRequirement.java`) is a **sealed interface** with three record
  variants: `KnownSiteOpportunity` (a named spatial memory holds a live site),
  `InventoryItemOpportunity` (the villager holds one of the items), and `JobSiteBlockOpportunity`
  (the `JOB_SITE` memory points at a matching block). Each self-evaluates against an
  `OpportunityProbe` — a server-thread seam over the three world reads, so the logic is testable
  without Minecraft.
- **Forecast, don't gate.** `OpportunityForecaster`
  (`application/ai/planning/OpportunityForecaster.java`) computes the set of `BehaviorKey`s that
  *lack* opportunity (a behavior lacks it if *any* declared requirement is unmet; behaviors with no
  requirements are never down-weighted). This is computed on the server thread inside
  `PlanRunner.createGenerationContext` **before** the async plan-generation handoff, because
  decaying-memory reads are not thread-safe — only the plain `Set<BehaviorKey>` crosses the thread
  boundary (as `PlanGenerationContext.behaviorsLackingOpportunity`).
- **Down-weight, not filter.** `HeuristicPlanGenerator` applies a `0.2×` multiplier
  (`LOW_OPPORTUNITY_MULTIPLIER`) to lacking keys rather than removing them, so a temporarily
  starved behavior stays in the pool and can still surface if nothing better exists. The
  `WindowPacker` drops only behaviors whose effective weight collapses to ≤ 0.

Note the async seam: `PlanRunner` generates plans through `IAsyncPlanGenerator`
(`HeuristicAsyncPlanGenerator`), which today just runs the heuristic generator off-thread. There is
still **no LLM planner** — the async wrapper exists to exercise that orchestration for a future
slower generator. The heuristic remains the source of truth.

---

## Removed components

| Removed | Replaced by |
|---------|-------------|
| `ActivityRegistry.SETTLEMENTS_PLAN` | Vanilla `Activity.WORK/MEET/IDLE/REST` (survives only as commented-out code) |
| `ScheduleRegistry.PLAN_SCHEDULE` | `ScheduleRegistry.SETTLEMENTS_SCHEDULE` — a minimal stub (default `Activity.IDLE` at noon) set on adults so the vanilla brain has a non-null schedule; actual activity transitions are driven by `PlanContextSwitcher`, not this schedule |
| `UpdateActivityFromSchedule` in adult brain | `PlanContextSwitcher` at priority 98 (babies still use `UpdateActivityFromSchedule` via the vanilla baby packages) |

---

## Explicit Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| `PlanRunnerBehavior` location | `Activity.CORE` | Must survive activity transitions without stop/start; CORE always ticks regardless of active activity. Placing it in each activity would restart it on every transition, interrupting mid-slot execution. |
| Reactive override rides inside `PlanRunnerBehavior` | Not a new activity, not a vanilla brain override | Only `PlanRunner` knows whether a plan behavior is running and interruptible; the override must be able to fire under any managed activity and must hand the body over cleanly (suspend → install → resume). A brain-priority behavior could not coordinate that. |
| Override precedence is policy-level | `OverridePolicy.priority()` | Cross-policy ordering (SocialAccept 100 > CollectDemandedItem 50) is independent of brain priority; the plan runner evaluates the multibound set in one place. |
| Override has a 60 s ceiling | `MAX_OVERRIDE_DURATION_TICKS` | Overrides are reactive and short-lived; a wedged one (e.g. mirroring a session that never closes) must not freeze the villager. On trip, force-stop and re-queue the interrupted slot. |
| Opportunity requirements down-weight, not filter | `0.2×` multiplier | Keeps starved behaviors in the pool so plans stay resilient; the packer prunes only zero-weight entries. Forecast is computed on the server thread before the async handoff (decaying-memory reads aren't thread-safe). |
| Activity transitions | Dynamic (`PlanContextSwitcher`) | Per-villager schedules vary by profession and authored plan blocks. A static `Schedule` cannot represent this. |
| Vanilla activities reused | `Activity.WORK`, `MEET`, `IDLE`, `REST` | No new activity registration required. Vanilla brain priority already handles PANIC/RAID/HIDE override for free via existing CORE behaviors. |
| Plan covers waking hours only | No sleep `PlanSlot` | Sleep timing is biological (`DayPlanSchedule.bedtimeTick`), not a task. The heuristic planner generates task slots; it should not decide when a villager sleeps. |
| `PLAN_BEHAVIOR_ACTIVE` is binary | Yes/No flag | Simpler than a per-channel bitmask for the foreground/ambient boundary. `BehaviorChannel` metadata drives finer-grained arbitration in the SocialCue lane instead. |
| `Activity.REST` excluded from managed set | PlanRunner suspends | Sleep is fully ambient; no plan slots exist for that window. Clean boundary between task scheduling and biological rhythm. |
| Ambient behaviors wrap vanilla | `AmbientBehaviors.gated(inner)` | Reuses vanilla behavior logic unchanged; only the precondition gate is added. |
| `ShowTradesToPlayer` is not gated | Registered bare in WORK, MEET, IDLE | Player-initiated trading must be able to preempt plan slot execution. Gating it would prevent the trade GUI from opening while a behavior is running. |
| `PlanContextSwitcher` uses 1-second cooldown | `ClockTicks.seconds(1)` | Activity context only changes at schedule or plan-slot boundaries. Re-deriving every tick wastes CPU and produces spurious `setActiveActivityIfPossible` calls on stable ticks. |
| Gene offsets applied at generation time | In `HeuristicPlanGenerator` | `ScheduleProfile` is a plain per-profession record of defaults. Chronotype/gene offsets adjust wake/sleep/meal and work-end ticks; these adjustments belong to plan generation, not the profile data model. |
| Fallback when plan is absent | `PlanContextSwitcher.fallbackActivity()` | Unloaded or first-spawn villagers must still cycle through plausible ambient activities instead of freezing on `Activity.IDLE` permanently. |

---

## Extending the Framework

### Adding an ambient behavior to an existing activity

1. Wrap the vanilla `BehaviorControl` with `AmbientBehaviors.gated(...)` (unless it must preempt
   plan execution — see `ShowTradesToPlayer` above).
2. Register it in `VanillaAmbientBehaviorPackages` under the relevant activity's list at an
   appropriate priority.
3. Update this document.

### Adding a reactive override

1. Implement `OverridePolicy` (`@ServerScope`, stateless): `priority()` for cross-policy precedence
   and `evaluate(level, villager)` returning the `OverrideRequest` (the `BehaviorKey` to install).
   Keep it cheap — it is polled every tick — and short-circuit early (as `SocialAcceptOverridePolicy`
   does when no invite is pending).
2. Bind it `@IntoSet` in `OverridePolicyModule`.
3. Ensure the target behavior exists in the catalog and declares `isInterruptible()` appropriately
   on the behaviors it may interrupt.
4. If the trigger depends on a new stimulus, emit it onto the `WorldEventBus` — see
   [`event_lane.md`](event_lane.md).
5. Update this document.

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
behavior catalog and pool system — see [`behavior_system.md`](behavior_system.md). Ambient
background behaviors and plan slot behaviors are entirely separate concerns — one fills idle time,
the other is the scheduled task.

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

`PlanRunnerBehavior` and `PlanContextSwitcher` are not registered for baby villagers. Babies remain
on the vanilla PLAY/IDLE/REST schedule with standard `VillagerGoalPackages` behaviors (including
`UpdateActivityFromSchedule`) and use `Schedule.VILLAGER_BABY`. The behavior catalog contains adult
profession routines and is not appropriate for baby villagers.

For PANIC, RAID, and HIDE: when vanilla asserts one of these, `PlanContextSwitcher` no longer
controls the active activity. `PlanRunnerBehavior` detects this on its next tick (current activity
not in managed set) and calls `planRunner.suspendIfActive()`; if the villager is actually unsafe
(hurt/hostile), the panic guard calls `planRunner.forceStop()` first. When the vanilla override
clears, `PlanContextSwitcher` re-asserts the appropriate managed activity and the plan resumes from
where it was interrupted.
