# Behavior Framework High-Level Design Review

**Date:** 2026-02-17  
**Project:** Settlements-Alpha  
**Author:** AI design review (senior-engineer perspective)  
**Status:** Draft for team review

---

## 1) Context and Scope

This review evaluates the behavior framework currently used for villager actions in:

- `src/main/java/dev/breezes/settlements/models/behaviors/**`
- `src/main/java/dev/breezes/settlements/models/behaviors/steps/**`
- `src/main/java/dev/breezes/settlements/models/behaviors/stages/**`
- `src/main/java/dev/breezes/settlements/models/behaviors/states/**`
- Related condition utilities in `src/main/java/dev/breezes/settlements/models/conditions/**`

This review specifically considered:

- `ShearSheepBehaviorV2`
- `TameWolfBehaviorV2`
- Commit `efbb642d04580af663d73f7129c2e442cd9c76d7`

---

## 2) Executive Summary

The framework direction is **strong and worth continuing**. The `StepResult` + `StageKey` refactor is a meaningful improvement over the old `Optional<Stage>` model and gives cleaner control flow semantics.

The system is currently in a **“good architecture, partially hardened”** state. Before full migration of all behaviors, we should close a short list of P0 correctness and consistency issues (mainly error propagation and condition utility correctness), then proceed in phased migration.

**Recommendation:** keep this framework, harden P0 first, then migrate all legacy behaviors to the V2 pattern.

---

## 3) Design Goals (Inferred)

The framework appears designed for:

1. **Composable behavior scripting** through reusable steps.
2. **Deterministic sequencing** via staged and time-based execution.
3. **Separation of concerns** across lifecycle, execution, and state.
4. **Compatibility with vanilla AI runtime** via `DefaultBehaviorAdapter`.
5. **Config-driven tuning** per behavior.

These are appropriate goals for this project.

---

## 4) Architectural Assessment

### 4.1 What is working well

- **Clear layering**:
    - Lifecycle in `AbstractBehavior` (`start/tick/stop`, cooldowns, preconditions)
    - Execution primitives in `BehaviorStep`, `StagedStep`, `TimeBasedStep`
    - Typed blackboard in `BehaviorContext` + `BehaviorStateType`
- **Good composability** demonstrated in V2 behaviors (`StayCloseStep` + `NavigateToTargetStep` + timed action).
- **Adapter boundary** (`DefaultBehaviorAdapter`) cleanly wraps custom behavior in vanilla behavior system.
- **Control-flow clarity** improved by `StepResult` (`NoOp`, `Complete`, `Transition`, `Fail`, `Abort`).

### 4.2 Current architectural debt

- Two paradigms coexist:
    - Legacy `AbstractInteractAtTargetBehavior` style
    - New V2 staged/step style
- This split increases maintenance overhead and slows migration consistency.

---

## 5) Strengths and Weaknesses by Dimension

### A) API Design

**Strengths**
- `StepResult` is explicit and safer than overloaded `Optional<Stage>`.
- `StageKey` enables compile-time-safe transitions.

**Weaknesses**
- Target extraction/casting patterns are duplicated across behaviors.
- Legacy stage artifacts (`Stage`, `SimpleStage`, `ControlStages`) still exist and create conceptual noise.

### B) Correctness and Safety

**Strengths**
- Stage/timing control paths are easier to reason about after the refactor.

**Weaknesses**
- `Fail/Abort` handling is not yet consistently enforced at behavior top boundary.
- A few low-level utilities have correctness issues (see prioritized list).

### C) Performance

**Strengths**
- Current behavior complexity is still manageable at project scale.

**Weaknesses**
- Some condition scans are more expensive than needed (`NearbyBlockExistsCondition`, `NearbyEntityExistsCondition`).

### D) Testability and Operability

**Strengths**
- Logging is already useful for tracing.

**Weaknesses**
- Very limited behavior-framework tests currently.
- No lightweight telemetry counters for failure/abort causes.

---

## 6) Prioritized Low-Level Code/Framework Issues

> Legend: **P0 = must fix before broad migration**, **P1 = should fix during migration**, **P2 = optimize/harden after migration baseline**

| Priority | Issue | Impact | Recommendation |
|---|---|---|---|
| **P0** | Behavior top-level handling does not consistently branch on `Fail/Abort` | Inconsistent failure semantics and harder recovery/debugging | Add a standard `handleStepResult(...)` path in base behavior layer and enforce uniform handling |
| **P0** | `StagedStep.onStart` currently ignores non-noop return from configured hook | Hook cannot safely influence control flow; hidden incorrect continuation risk | Either honor returned `StepResult` or formally constrain hook to side effects and enforce contract |
| **P0** | `NearbyBlockExistsCondition` takes `minimumTargetCount` but does not enforce/store it | API contract bug; incorrect condition result behavior | Add field + validate using `targets.size() >= minimumTargetCount` |
| **P0** | Legacy and V2 paradigms coexist | Increased maintenance and inconsistent behavior style | Freeze new legacy pattern usage and migrate all behaviors to V2 pattern |
| **P1** | Null-safety stop path risk in `BreedAnimalsBehavior.doStop` (`breedTarget1.getType()`) | Potential NPE on partial start/early stop | Make stop idempotent and null-safe throughout |
| **P1** | Reset semantics for nested/timed steps may be incomplete in interruption cases | Potential state leakage between behavior runs | Add explicit recursive reset conventions or recreate step instances on behavior start |
| **P1** | Repeated target query/cast boilerplate | Duplication and repeated bug surface | Add shared target-query helpers (`TargetQueries`) |
| **P1** | Side effects are directly coupled to behavior code | Harder unit testing and policy control | Introduce minimal `EffectsBus` abstraction for particles/sounds/UI packets |
| **P1** | Legacy stage classes still present | API confusion and onboarding friction | Deprecate/remove `Stage`, `SimpleStage`, `ControlStages` when migration completes |
| **P2** | `NearbyEntityExistsCondition` uses `parallelStream` + sorting + `findAny` for min=1 | Overhead and non-determinism risk for small entity sets | Use deterministic nearest selection and profile before introducing parallelism |
| **P2** | Cubic scans in `NearbyBlockExistsCondition` can be expensive | Tick-time pressure as behaviors scale | Add early exit when minimum found; consider scan throttling/caching |
| **P2** | No focused framework tests | Regression risk during migration | Add tests for `StagedStep`, `TimeBasedStep`, and representative V2 behavior flows |
| **P2** | Limited runtime metrics for behavior outcomes | Slower diagnosis/tuning | Add counters for starts/stops/fails/aborts (by behavior + reason code) |

---

## 7) Recommended Plan

### Phase P0 (Hardening Gate)

1. Standardize top-level `StepResult` handling for all behavior controllers.
2. Resolve `StagedStep.onStart` contract.
3. Fix `NearbyBlockExistsCondition` minimum-count correctness.
4. Formalize migration policy: no new legacy-style behavior implementations.

### Phase P1 (Migration + Cleanup)

5. Migrate low/medium complexity behaviors first:
    - `HarvestSugarCaneBehavior`
    - `RepairIronGolemBehavior`
    - `ThrowPotionsBehavior`
    - `HarvestSoulSandBehavior`
    - `BlastOreBehavior`
6. Migrate high complexity behaviors with extra validation:
    - `BreedAnimalsBehavior`
    - `CutStoneBehavior`
7. Remove legacy stage artifacts after consumers are gone.

### Phase P2 (Optimization + Quality)

8. Optimize scan utilities guided by profiling.
9. Add framework-focused tests and deterministic test scaffolding.
10. Add lightweight framework-level metrics.

---

## 8) Final Recommendation

Continue with the current framework and complete migration to the V2 model.

This framework has the right long-term shape for Settlements-Alpha. With the P0 hardening changes, it can become a stable platform for adding new AI behaviors quickly and safely. After migration and P2 quality work, the framework should be in strong production condition.

---

## 9) Suggested Acceptance Checklist

- [ ] P0 issues resolved and reviewed
- [ ] All behavior classes migrated to V2 staged/step pattern
- [ ] Legacy stage abstractions removed (or explicitly deprecated with removal date)
- [ ] Behavior-framework unit tests added for core step/stage primitives
- [ ] Metrics/logging include `Fail/Abort` reason visibility
