# Behavior Framework Refactor (P0 + P1) — Low-Level Design

**Date:** 2026-02-17  
**Project:** Settlements-Alpha  
**Status:** Proposed implementation plan  
**Related:** `docs/working/20260217_behavior_framework_high_level_design_review.md`

---

## 1) Objective

Define concrete, code-level changes for the P0 and P1 issues identified in the high-level review, so implementation can proceed with low risk and predictable behavior.

This document focuses on:

- API changes
- File-by-file modifications
- behavior/runtime semantics
- rollout and verification strategy

---

## 2) Scope

### In scope (P0 + P1)

1. **P0** — Standardize behavior-level handling of `StepResult` (`NoOp/Complete/Transition/Fail/Abort`)
2. **P0** — Fix `StagedStep.onStart` contract (currently ignores non-`NoOp`)
3. **P0** — Fix `NearbyBlockExistsCondition` `minimumTargetCount` contract
4. **P0** — Enforce migration direction away from legacy behavior style
5. **P1** — Null-safe/idempotent `BreedAnimalsBehavior.doStop`
6. **P1** — Reliable reset semantics for nested steps (especially interrupted `TimeBasedStep`)
7. **P1** — Extract common target-query helpers (reduce duplicated casting/filtering)
8. **P1** — Introduce `EffectsBus` abstraction boundary (phase-in)
9. **P1** — Deprecate legacy stage abstractions (`Stage`, `SimpleStage`, `ControlStages`)

### Out of scope

- Full migration of all legacy behaviors (covered by separate migration plan)
- P2 performance and telemetry work

---

## 3) Current Baseline (Relevant Code)

- `src/main/java/dev/breezes/settlements/models/behaviors/AbstractBehavior.java`
- `src/main/java/dev/breezes/settlements/models/behaviors/stages/StagedStep.java`
- `src/main/java/dev/breezes/settlements/models/behaviors/steps/TimeBasedStep.java`
- `src/main/java/dev/breezes/settlements/models/conditions/NearbyBlockExistsCondition.java`
- `src/main/java/dev/breezes/settlements/models/behaviors/ShearSheepBehaviorV2.java`
- `src/main/java/dev/breezes/settlements/models/behaviors/TameWolfBehaviorV2.java`
- `src/main/java/dev/breezes/settlements/models/behaviors/BreedAnimalsBehavior.java`

---

## 4) Detailed Design

## 4.1 P0-1 — Standardize `StepResult` handling at behavior boundary

### Problem

`ShearSheepBehaviorV2` and `TameWolfBehaviorV2` currently only stop on `Transition(END)`. `Fail` and `Abort` are not handled consistently at this boundary.

### Design

Add a reusable handler in staged behavior base class, then call it from each staged behavior tick.

### File-level changes

1. **Update** `BaseVillagerStagedBehavior.java`
   - Add helper method:

```java
protected final void handleStepResult(
        @Nonnull StepResult result,
        @Nonnull StageKey endStage,
        @Nonnull String behaviorName
)
```

Semantics:

- `NoOp` → continue
- `Complete` → continue (internal to `StagedStep`, not terminal by itself)
- `Transition(endStage)` → stop behavior
- `Fail` → log warn + stop behavior
- `Abort` → log error + stop behavior

Implementation style (minimal churn): throw `StopBehaviorException` from handler for stop conditions so `AbstractBehavior.tick()` continues to be the single stop gate.

2. **Update** `ShearSheepBehaviorV2` and `TameWolfBehaviorV2`
   - Extend `BaseVillagerStagedBehavior` (instead of `BaseVillagerBehavior`)
   - Replace manual end-stage check with handler call:

```java
StepResult result = this.controlStep.tick(this.context);
this.handleStepResult(result, ShearStage.END, "ShearSheepBehaviorV2");
```

### Notes

- Keep `AbstractBehavior` unaware of step internals (no API change there).
- This pattern becomes mandatory for all new staged behaviors.

---

## 4.2 P0-2 — Fix `StagedStep.onStart` contract

### Problem

`StagedStep.onStart` executes the configured `onStart` step but discards its `StepResult`, then always transitions to `initialActionStage`.

### Design

Honor non-`NoOp` return values from configured `onStart` step.

### File-level changes

**Update** `stages/StagedStep.java`

Current:

```java
if (this.onStart != null) {
    this.onStart.tick(context);
}
return StepResult.transition(this.initialActionStage);
```

Proposed:

```java
if (this.onStart != null) {
    StepResult startResult = this.onStart.tick(context);
    if (!(startResult instanceof StepResult.NoOp)) {
        return startResult;
    }
}
return StepResult.transition(this.initialActionStage);
```

### Semantics after change

- `onStart -> NoOp` => default transition to initial action stage
- `onStart -> Transition(X)` => transition to `X`
- `onStart -> Complete` => complete parent staged step normally
- `onStart -> Fail/Abort` => bubble up

---

## 4.3 P0-3 — Fix `NearbyBlockExistsCondition` minimum-count contract

### Problem

Constructor validates `minimumTargetCount`, but value is never stored and never used in `test()`. Condition currently returns true for any non-empty result.

### Design

Store and enforce `minimumTargetCount`; early-exit scan when threshold met.

### File-level changes

**Update** `conditions/NearbyBlockExistsCondition.java`

1. Add field:

```java
private final int minimumTargetCount;
```

2. Set in constructor.
3. In scan loop, after adding target:

```java
if (this.targets.size() >= this.minimumTargetCount) {
    log.sensorStatus("Found at least {} blocks nearby", this.minimumTargetCount);
    return true;
}
```

4. End of method:

```java
return this.targets.size() >= this.minimumTargetCount;
```

### Behavioral impact

- Preserves current behavior for `minimumTargetCount == 1`
- Correctly supports `>1` use cases
- Improves average runtime with early exit

---

## 4.4 P0-4 — Enforce migration direction (legacy freeze)

### Problem

Legacy and V2 paradigms coexist; developers may continue introducing legacy-style behaviors.

### Design

Introduce explicit compile-time guidance and documentation-level policy.

### File-level changes

1. **Update** `AbstractInteractAtTargetBehavior.java`
   - Add `@Deprecated(forRemoval = true, since = "2026.02")`
   - Javadoc: “Do not use for new behaviors; use staged framework (`StagedStep`, `TimeBasedStep`, `StepResult`)”

2. **Update** `stages/Stage.java`, `SimpleStage.java`, `ControlStages.java`
   - Mark deprecated with same policy
   - Point to `steps.StageKey`

3. **Update docs**
   - Add migration rule in behavior authoring doc (or this doc referenced in team process)

---

## 4.5 P1-1 — Null-safe and idempotent `BreedAnimalsBehavior.doStop`

### Problem

`doStop()` calls `this.breedTarget1.getType()` unguarded.

### Design

Make stop path null-safe and idempotent.

### File-level changes

**Update** `BreedAnimalsBehavior.java`

Proposed stop flow:

1. `villager.clearHeldItem()`
2. Claim babies only if `breedTarget1 != null`
3. Loop over `{breedTarget1, breedTarget2}` with null guard
4. Reset tickables and state fields
5. Ensure repeated calls do not throw

Pseudo:

```java
if (this.breedTarget1 != null) {
    this.claimNearbyBabyAnimals(villager, this.breedTarget1.getType());
}
for (Animal target : new Animal[]{this.breedTarget1, this.breedTarget2}) {
    if (target == null) continue;
    target.dropLeash(true, false);
    target.setAge(6000);
}
```

---

## 4.6 P1-2 — Deterministic reset semantics for nested steps

### Problem

`StagedStep.reset()` currently only resets `currentStage`. If behavior is interrupted mid-`TimeBasedStep`, internal tickables may retain elapsed progress depending on step composition.

### Design

Add optional reset contract to all steps and apply recursively.

### API change

**Update** `BehaviorStep.java`

```java
default void reset() {
    // no-op by default
}
```

### File-level changes

1. **Update** `TimeBasedStep`
   - `reset()` should:
     - reset its own tickable
     - call `reset()` on `onStart`, `onEnd`, periodic steps, keyframe steps

2. **Update** `ConditionalStep`
   - `reset()` calls `trueStep.reset()` and `falseStep.reset()`

3. **Update** `StagedStep`
   - `reset()` should reset `currentStage` and reset all contained stage steps
   - avoid duplicate resets if same step instance appears multiple times (use local `Set<BehaviorStep>`)

4. **Update** stateful concrete steps (`WaitStep`)
   - reset internal tickable

### Compatibility

- Existing stateless steps remain unaffected due to default no-op reset.

---

## 4.7 P1-3 — Extract target query helpers

### Problem

Repeated target-match/cast logic appears in multiple behaviors.

### Design

Create utility helper to query and cast from `TargetState` consistently.

### New file

`src/main/java/dev/breezes/settlements/models/behaviors/states/registry/targets/TargetQueries.java`

Proposed API:

```java
public final class TargetQueries {
    public static <E extends Entity> Optional<E> firstEntity(
            @Nonnull BehaviorContext context,
            @Nonnull EntityType<E> type,
            @Nonnull Class<E> castTo
    )

    public static Optional<Targetable> firstMatching(
            @Nonnull BehaviorContext context,
            @Nonnull ICondition<Targetable> condition
    )
}
```

### Adoption

- Replace `getTargetSheep(...)` and `getTargetWolf(...)` internals to use helper.

---

## 4.8 P1-4 — Introduce `EffectsBus` boundary (phase-in)

### Problem

Behavior logic directly performs side effects (particles/sounds/bubble packets), reducing testability and central policy control.

### Design

Introduce abstraction; keep default implementation behavior-equivalent.

### New package

`src/main/java/dev/breezes/settlements/models/behaviors/effects/`

Proposed interfaces/classes:

```java
public interface BehaviorEffectsBus {
    void playSound(...);
    void displayParticles(...);
    void showBubble(...);
    void removeBubble(...);
}

public final class DirectBehaviorEffectsBus implements BehaviorEffectsBus {
    // wraps current direct implementation logic
}
```

### Integration plan

1. Add bus field to `BehaviorContext` with default `DirectBehaviorEffectsBus`.
2. Convert V2 behaviors first.
3. Keep old direct calls temporarily where migration cost is high.

---

## 4.9 P1-5 — Deprecate legacy stage abstractions

### Design

Deprecate now, remove after all staged behaviors rely only on `StageKey`.

### File-level changes

- `stages/Stage.java` → `@Deprecated(forRemoval = true, since = "2026.02")`
- `stages/SimpleStage.java` → same
- `stages/ControlStages.java` → same

---

## 5) Implementation Order (PR Plan)

### PR-1 (P0 hardening)

- P0-1 `StepResult` handling helper + adopt in V2 behaviors
- P0-2 `StagedStep.onStart` contract fix
- P0-3 `NearbyBlockExistsCondition` minimum-count fix
- P0-4 deprecation annotations + migration guidance docs

### PR-2 (P1 correctness)

- P1-1 `BreedAnimalsBehavior.doStop` null-safety/idempotence
- P1-2 reset-contract implementation (`BehaviorStep.reset` + recursive reset)

### PR-3 (P1 maintainability)

- P1-3 `TargetQueries` helper + adopt in V2 behaviors
- P1-4 `EffectsBus` abstraction (introduce + wire into 1-2 behaviors)
- P1-5 legacy stage deprecations finalized

---

## 6) Verification Strategy

## 6.1 Unit-level checks (if/when test harness is available)

1. `StagedStep` onStart-return behavior:
   - onStart `NoOp` → transitions to initial stage
   - onStart `Fail` → bubbles `Fail`
   - onStart `Abort` → bubbles `Abort`

2. `NearbyBlockExistsCondition`:
   - minimum=1 with one target -> true
   - minimum=2 with one target -> false
   - minimum=2 with two targets -> true

3. reset behavior:
   - interrupted `TimeBasedStep` restarts from tick 0 after `reset()`

## 6.2 Manual/integration checks (current project baseline)

1. Run game/server and trigger:
   - `ShearSheepBehaviorV2`
   - `TameWolfBehaviorV2`
   - `BreedAnimalsBehavior` start/interrupt/stop

2. Validate:
   - No regressions in behavior completion
   - Proper stop when `Fail/Abort` is produced
   - No NPE in breed stop path

---

## 7) Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Reset recursion affects many step types | Use default no-op `reset()` and roll out stateful overrides incrementally |
| New standardized stop handling may alter edge behavior timing | Keep semantics aligned with existing `StopBehaviorException` stop path |
| EffectsBus introduces additional abstraction complexity | Start with thin facade (`DirectBehaviorEffectsBus`) and migrate incrementally |

---

## 8) Acceptance Criteria

- [ ] V2 behaviors use shared result handler and react to `Fail/Abort`
- [ ] `StagedStep.onStart` no longer discards non-`NoOp` result
- [ ] `NearbyBlockExistsCondition` enforces minimum target count
- [ ] `BreedAnimalsBehavior.doStop` is null-safe/idempotent
- [ ] Step reset contract implemented for stateful nested step flows
- [ ] Target query helper introduced and used by at least V2 behaviors
- [ ] Legacy stage APIs marked deprecated with migration guidance
