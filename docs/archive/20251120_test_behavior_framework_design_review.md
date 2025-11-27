# Behavior Framework Design Review

Scope: This review focuses on the behavior framework (lifecycle, stages/steps, context/blackboard). Navigation internals and enum-map micro-optimizations are explicitly out of scope per guidance.

## Executive Summary
Your framework provides a clear separation of concerns: lifecycle and cooldown management (AbstractBehavior), a typed blackboard (BehaviorContext + states), and a composable execution model (Steps + Stages with StagedStep and TimeBasedStep). Concrete behaviors like ShearSheep and TameWolf demonstrate good composition with reusable primitives such as StayCloseStep and NavigateToTargetStep.

Primary improvements to reduce clunk and sharpen correctness:

- Introduce explicit StepResult (NOOP, COMPLETE, TRANSITION) instead of overloading Optional<Stage> and a magic STEP_END.
- Replace open-ended Stage objects with per-StagedStep StageKey (private enum) for stable identity and simpler reasoning.
- Clarify onStart/onEnd semantics (void) and add an explicit EndPolicy for completion behavior.
- Add an EffectsBus abstraction so steps emit domain events (particles/sounds/UI) rather than performing side-effects directly.
- Add BehaviorClock and RandomSource to BehaviorContext for deterministic time and testable randomness.

This keeps the mental model (small, composable steps with a minimal state machine) while making it harder to misuse and easier to test and evolve. Other paradigms (BT, Utility AI, GOAP) are viable, but the recommended path is a small evolution of the current HFSM-like design that preserves readability and reuse.

## Current Framework Overview
- Lifecycle
    - AbstractBehavior/BaseVillagerBehavior orchestrate: preconditions, continue-conditions, cooldowns, start/stop, and stop requests.
- Context/Blackboard
    - BehaviorContext stores typed BehaviorState keyed by BehaviorStateType (e.g., TargetState, ItemState). This avoids unchecked casting and encourages explicit data flow.
- Execution Model
    - BehaviorStep (tick returns Optional<Stage>), AbstractStep for naming/UUID and logging.
    - Stage/SimpleStage and ControlStages with STEP_END and a STEP_START factory.
    - StagedStep maps Stage → BehaviorStep, provides onStart/onEnd hooks, and treats STEP_END as a fall-through/complete signal.
    - TimeBasedStep schedules steps via onStart/onEnd, periodic steps, and keyframes over a tickable window.
- Composition in practice
    - ShearSheep: gather targets with preconditions; a StayCloseStep chooses between Navigate and Action(TimeBasedStep); onEnd picks up items and may loop.
    - TameWolf: similar composition, with a bounded attempt loop and success/failure effects.

## What’s Good (Keep and Build Upon)
- Strong composability: behaviors are assembled from small, testable steps.
- Deterministic sequencing: TimeBasedStep provides clear timing semantics and composable animation/action windows.
- Typed context: BehaviorContext + BehaviorStateType make state explicit and safe.
- Lifecycle rigor: explicit cooldowns, preconditions, continue-conditions, and a stop request path.
- Observability: consistent logging, named/UUID’d steps, and structured transitions.
- Config-driven tuning via annotations keeps balance knobs out of core logic.

## Pain Points and Risks
- Stage identity semantics
    - SimpleStage instances have random UUIDs; code treats ControlStages.STEP_END as a special singleton. This is easy to misuse (e.g., returning a different SimpleStage that looks the same but isn’t equal).
- Result signaling overload
    - Optional<Stage> encodes three implicit meanings: NOOP (empty), COMPLETE (STEP_END), TRANSITION (any other Stage). This is subtle and error-prone for implementors and reviewers.
- StagedStep onEnd contract
    - onEnd returns Optional<Stage> but the return value is not used by StagedStep; semantics are unclear. Either remove the return or make the transition policy explicit.
- Target locking/validation
    - Target validity (death/despawn/type change) is rechecked ad-hoc. A shared helper pattern would reduce duplication and edge cases.
- Determinism and testing
    - RandomUtil and step-local timing are not centrally controllable for tests; behavior outcomes are harder to reproduce.
- Side-effect coupling
    - Behaviors directly spawn particles/sounds/UI packets, which complicates testing and rate limiting. An effects bus would simplify mocking and policy control.

## Recommended Design: HFSM Refinement with StepResult and StageKey
Keep the StagedStep concept but make stage identity explicit and results unambiguous.

### StepResult (explicit outcomes)
```java
public interface BehaviorStep {
  StepResult tick(BehaviorContext ctx);
}

public sealed interface StepResult {
  record NoOp() implements StepResult {}
  record Complete() implements StepResult {}
  record Transition(StageKey key) implements StepResult {}

  static StepResult noOp() { return new NoOp(); }
  static StepResult complete() { return new Complete(); }
  static StepResult transition(StageKey key) { return new Transition(key); }
}
```

### Per-StagedStep StageKey (stable identity)
```java
public final class StagedStepV2 extends AbstractStep {
  private enum StageKey { STEP_START, ACTION, END } // private to this staged step

  private final Map<StageKey, BehaviorStep> steps;
  private final StageKey initial;
  private final EndPolicy endPolicy; // see below
  private StageKey current;

  public enum EndPolicy { FALL_THROUGH, TRANSITION_TO_END, REPEAT_ACTION }

  public StagedStepV2(String name, Map<StageKey, BehaviorStep> steps, StageKey initial, EndPolicy endPolicy) {
    super("StagedStep[" + name + "]");
    this.steps = steps; this.initial = initial; this.endPolicy = endPolicy; this.current = StageKey.STEP_START;
  }

  @Override
  public StepResult tick(BehaviorContext ctx) {
    BehaviorStep step = steps.get(current);
    StepResult result = step.tick(ctx);
    if (result instanceof StepResult.NoOp) return StepResult.noOp();
    if (result instanceof StepResult.Complete) {
      return switch (endPolicy) {
        case FALL_THROUGH -> StepResult.complete();
        case TRANSITION_TO_END -> StepResult.transition(StageKey.END);
        case REPEAT_ACTION -> { current = StageKey.ACTION; yield StepResult.noOp(); }
      };
    }
    if (result instanceof StepResult.Transition tr) {
      this.current = tr.key();
      return StepResult.noOp();
    }
    return StepResult.noOp();
  }
}
```
Notes:
- StageKey is private to each staged step, preventing cross-behavior collisions and keeping identity trivial.
- EndPolicy makes completion behavior explicit and readable, replacing implicit STEP_END fall-through.
- onStart/onEnd should be void hooks (if needed). If they must influence flow, model that via EndPolicy or an explicit Transition.

### TimeBasedStep V2 (StepResult-based)
```java
public class TimeBasedStepV2 extends AbstractStep {
  private final ITickable tickable;
  private final List<BehaviorStep> everyTick = new ArrayList<>();
  private final Map<Long, BehaviorStep> keyframes = new HashMap<>();
  private final Runnable onStart; // or Consumer<BehaviorContext>
  private final Runnable onEnd;   // side effects routed to EffectsBus

  @Override
  public StepResult tick(BehaviorContext ctx) {
    if (tickable.getTicksElapsed() == 0 && onStart != null) onStart.run();

    boolean completed = tickable.tickCheckAndReset(1);
    long elapsed = tickable.getTicksElapsedRounded();

    // periodic
    for (BehaviorStep s : everyTick) s.tick(ctx);

    // keyframe
    if (keyframes.containsKey(elapsed)) {
      StepResult kr = keyframes.get(elapsed).tick(ctx);
      if (!(kr instanceof StepResult.NoOp)) return kr; // allow keyframe to redirect
    }

    if (completed) {
      if (onEnd != null) onEnd.run();
      return StepResult.complete();
    }
    return StepResult.noOp();
  }
}
```

### EffectsBus (decouple side-effects)
```java
public interface EffectsBus {
  void playParticles(ParticlesSpec spec);
  void playSound(SoundSpec spec);
  void showBubble(BubbleSpec spec);
  // ...
}

public final class ContextEffects {
  public static EffectsBus of(BehaviorContext ctx) { return ctx.effects(); }
}

// Usage inside steps
ContextEffects.of(ctx).playParticles(ParticlesSpec.heartAt(wolfLoc));
```

### Deterministic Time and Randomness
```java
public final class BehaviorContext {
  private final BehaviorClock clock;      // time source for behaviors
  private final RandomSource random;      // seeded per behavior or entity
  private final EffectsBus effects;       // for side-effects
  // getters ...
}
```
- Steps use ctx.random() instead of global RandomUtil, enabling seeded tests.
- TimeBasedStepV2 uses ctx.clock() if desired, or continues to use an injected ITickable; tests can stub both.

### Error/Failure Semantics for StepResult (Fail vs Abort)
To make error handling explicit and actionable, extend StepResult with two additional outcomes:

```java
public sealed interface StepResult {
  // existing
  record NoOp() implements StepResult {}
  record Complete() implements StepResult {}
  record Transition(StageKey key) implements StepResult {}

  // new
  record Fail(String code, Map<String, Object> details) implements StepResult {}
  record Abort(String code, Throwable cause) implements StepResult {}
}
```

- Fail: recoverable, expected domain failure (e.g., a tame attempt fails). Authors or the parent step can decide to retry, backoff, pick a new target, or fall through.
- Abort: unrecoverable/system failure (contract violation, missing critical state). The framework should stop the behavior immediately, emit diagnostics, and record metrics.

StagedStep handling should define a FailPolicy alongside EndPolicy:

```java
public enum FailPolicy { BUBBLE_UP, REPEAT_CURRENT, TRANSITION_TO(StageKey target), COMPLETE_OUTER }
```

- On Fail: apply the configured FailPolicy. If none specified, default to BUBBLE_UP so the caller decides.
- On Abort: propagate upward; the top-level behavior stops and logs an error with the provided code/cause.

Behavior integration:
- AbstractBehavior.tick should:
  - Convert unexpected runtime exceptions thrown by steps into Abort once, centrally.
  - On Abort: stop behavior, log at error with context, increment metrics.
  - On Fail that bubbles to top: treat per framework default (e.g., stop gracefully) or allow behaviors to register a top-level FailPolicy.

Mapping to BT (hybrid):
- NoOp → RUNNING; Complete → SUCCESS; Fail → FAILURE; Transition → RUNNING (internal stage change);
- Abort → either convert to FAILURE in the leaf and escalate via a decorator, or short-circuit stop in the hosting runtime.

Usage examples:
- Tame attempt miss (domain failure): `return StepResult.fail("TAME_ATTEMPT_FAIL", Map.of("wolfId", wolf.getUUID()));`
  - Parent FailPolicy: `REPEAT_CURRENT` (decrement attempts) or `TRANSITION_TO(COOLDOWN)`.
- Missing target (system issue): `return StepResult.abort("TARGET_MISSING", null);`
  - Framework stops behavior and emits diagnostics.

Telemetry & logging:
- Fail: log at debug/warn with code; count as controlled failure.
- Abort: log at error with code/cause; count in an "abort" metric by code; tag with entity/context.

Migration notes:
- Replace StopBehaviorException sites with `Abort` (or `Fail` where appropriate). Keep exceptions for truly exceptional cases and translate them once at the framework boundary.

## Migration Plan (Low-Risk, Phased)
1) Introduce StepResult and adapters
    - Add StepResult and update BehaviorStep.tick signature.
    - Provide a thin adapter in StagedStep to interpret old Optional<Stage> if needed during transition (temporary shim).
2) Replace SimpleStage with per-step StageKey
    - Convert StagedStep to use an internal enum StageKey; remove ControlStages.STEP_END from behavior authors.
3) Clarify onStart/onEnd
    - Make these void hooks. Introduce EndPolicy on StagedStep for completion behavior.
4) EffectsBus
    - Add a minimal bus (no external dependencies). Update TameWolf/ShearSheep to emit events instead of direct side-effects.
5) Determinism
    - Add BehaviorClock + RandomSource to BehaviorContext. Gradually refactor steps off global utilities.
6) Add Fail/Abort semantics
    - Extend StepResult with Fail/Abort, add StagedStep FailPolicy support, and update AbstractBehavior to stop on Abort and handle Fail defaults.
7) Clean-up and docs
    - Update examples and templates; remove the old STEP_END patterns and Optional<Stage> return style.

## Testing and Observability
- BehaviorTestKit
    - Provides a fake BehaviorContext with:
        - Seeded RandomSource
        - Controllable BehaviorClock
        - Mock EffectsBus that records emitted events
    - Helpers to tick steps/behaviors and assert on: StepResult, blackboard changes, and emitted effects.
- Metrics (optional)
    - Behavior start/stop counts, average duration, retry counts, failure reasons. Useful for tuning and regression detection.

## Alternatives (Brief)
- Behavior Trees (BT)
    - Pros: Established, composable, readable with decorators for timers and guards.
    - Cons: Requires a tree runtime; migrating staged/keyframe scripts takes effort.
- Utility AI (consideration-based)
    - Pros: Great for dynamic prioritization across many behaviors.
    - Cons: Less suited to deterministic, scripted sequences; tuning heaviness.
- GOAP
    - Pros: Powerful for emergent problem-solving.
    - Cons: Overkill for vignette-like actions; planning cost; harder to debug.
- Coroutines (script-style)
    - Pros: Natural expression of time-based flows.
    - Cons: Requires coroutine infra; not native in Java; discipline required.

The recommended path is the HFSM refinement above: smallest change, biggest clarity gain, retains current strengths.

## Additional Notes
- Out of scope: Navigation details and vanilla AI interplay are intentionally excluded from this design.
- Guard/DSL
    - Keep minimal now. A future guard combinator (when(condition).then(A).otherwise(B)) could generalize StayCloseStep without adding complexity today.

## Appendix: Example Refactor Sketch (Taming Action Window)
```java
// Pseudocode showcasing TimeBasedStepV2 + StepResult
BehaviorStep attemptTame = ctx -> {
  var wolfOpt = TargetQueries.entityOfType(ctx, EntityType.WOLF);
  if (wolfOpt.isEmpty()) return StepResult.complete();
  var wolf = wolfOpt.get();

  // try tame with ctx.random()
  if (ctx.random().nextDouble() < 0.33) {
    ctx.effects().playParticles(ParticlesSpec.hearts(wolf));
    // spawn SettlementsWolf, set owner, etc. via domain service
    return StepResult.complete();
  } else {
    ctx.effects().playParticles(ParticlesSpec.smoke(wolf));
    return StepResult.noOp(); // or StepResult.fail("TAME_ATTEMPT_FAIL", Map.of("wolfId", wolf.getUUID()))
  }
};

var actionWindow = new TimeBasedStepV2(Ticks.seconds(1).asTickable())
  .everyTick(ctx -> { ctx.getInitiator().setHeldItem(Items.BONE.getDefaultInstance()); return StepResult.noOp(); })
  .keyframe(Ticks.seconds(0.5), attemptTame)
  .onEnd(ctx -> ctx.getInitiator().clearHeldItem());

var staged = new StagedStepV2("TameWolfV2",
  Map.of(StageKey.ACTION, actionWindow),
  StageKey.ACTION,
  EndPolicy.FALL_THROUGH /* end outer step */
);
```

---

# Behavior Trees (BT) — Detailed Elaboration

Behavior Trees are a hierarchical, declarative way to model decision-making and action execution. They are widely used in games due to their composability, readability, and predictable tick semantics.

## Core Concepts
- Status model: Every node returns one of { Success, Failure, Running } when ticked.
- Node types:
    - Leaf nodes: Action (do something over time, often returning Running until done), Condition (quick check returning Success/Failure).
    - Composite nodes:
        - Sequence: tick children in order; if any child fails, the sequence fails; succeeds only if all succeed. If a child returns Running, the sequence returns Running and will resume that child on the next tick (memory sequence).
        - Selector (a.k.a. Fallback): tick children in order; returns Success when the first child succeeds; returns Failure if all fail; Running if a child is Running.
        - Parallel (optional): tick all children; configurable success policy (e.g., succeed-on-all or succeed-on-one).
    - Decorators:
        - Inverter, Succeeder, Failer: transform child status.
        - Repeater/UntilSuccess/UntilFailure: repeat ticking child until condition.
        - Cooldown/Throttle/Wait: time control around a child.
        - Guard: evaluate a condition before ticking child; if guard fails, return Failure (or skip).
- Blackboard: Tree-wide or per-entity memory for facts; in your framework, BehaviorContext maps neatly to a BT blackboard.

## Execution Semantics
- The tree is ticked top-down each frame (or on your behavior tick cadence).
- Composites and decorators manage which child to tick and how to interpret statuses.
- Memory variants (MemorySequence/MemorySelector) remember the currently Running child and resume it next tick, avoiding re-evaluation costs and preserving intent.

## Mapping Your Framework to BT
- BehaviorContext → Blackboard (typed, you already have this).
- Conditions (e.g., NearbyShearableSheepExistsCondition) → Condition leaf nodes.
- Steps like StayClose + Navigate + Action(TimeBased) → A Sequence with a guard and an action:
    - Sequence( Guard(IsCloseEnough), Action(TimedShear) ) with a Selector to Navigate if not close:
        - Selector( Sequence(Guard(IsCloseEnough), Action(Shear)), Action(NavigateToTarget) )
- Time-based windows → Action nodes that hold internal timers and return Running until complete, or Decorators like Wait/Timeout wrapped around an instantaneous action.
- Attempt loops → Decorators (Repeater with max attempts) or Sequence with Counter state on the blackboard.

## Example: Tame Wolf in BT
```
Root
└─ Sequence
   ├─ Condition: OwnershipBelowLimit
   ├─ Condition: NearbyUntamedWolfExists
   ├─ Action: SelectTargetWolf (writes TARGET)
   ├─ Selector
   │  ├─ Sequence
   │  │  ├─ Condition: IsCloseEnoughToTarget
   │  │  └─ Repeater(max=5)
   │  │     └─ Wait(1s)
   │  │        └─ Action: AttemptTame   // returns Success on tame, Failure otherwise
   │  └─ Action: NavigateToTarget
   └─ Action: CelebrateAndFinish (particles/sounds via EffectsBus)
```
Notes:
- AttemptTame can return Success → the Repeater stops early; Sequence completes.
- If all attempts fail, Repeater returns Failure and the Selector may re-evaluate navigation or end.

## Example: Shear Sheep in BT
```
Root
└─ Sequence
   ├─ Condition: NearbyShearableSheepExists
   ├─ Action: AcquireTargets (writes TARGET list)
   ├─ Repeater(while: HasTargets)
   │  └─ Selector
   │     ├─ Sequence
   │     │  ├─ Condition: IsCloseEnoughToCurrentTarget
   │     │  ├─ Wait(1s)
   │     │  └─ Action: ShearCurrentTarget
   │     └─ Action: NavigateToCurrentTarget
   └─ Action: PickupDroppedItems
```

## Pros (When BT Shines)
- Readability/composability: Trees visualize decision logic; selectors/sequences align with natural “try A else B” and “do A then B” reasoning.
- Reuse and testability: Leaves and decorators are small, reusable units; easy to mock blackboard and tick deterministically.
- Guards and priorities: Selectors provide natural prioritization; adding/removing behaviors is often localized.
- Industry-proven: Broad adoption and tooling; your team may benefit from common patterns and docs.

## Cons (Trade-offs)
- Runtime and tooling: You need a small BT runtime and optional visualization/logging to make debugging pleasant.
- Migration cost: Converting staged/keyframe logic into tree nodes takes time; your TimeBasedStep maps to BT but may be less concise than your current DSL.
- Strictly scripted sequences: Fine-grained keyframing is more ergonomic in a custom FSM/DSL; BT is excellent at flow control, less so at “cinematic” timing unless wrapped.

## Performance Considerations
- Use memory composites to avoid re-evaluating higher nodes when a child is Running.
- Keep trees shallow and factor shared subtrees.
- Preallocate nodes per entity; avoid per-tick allocations.
- Tick cadence: you can tick some subtrees less frequently (e.g., sensors) via decorators or a scheduler.

## Minimal BT Runtime Sketch (Java)
```java
public enum Status { SUCCESS, FAILURE, RUNNING }

public interface Node { Status tick(BehaviorContext ctx); }

public abstract class Composite implements Node {
  protected final List<Node> children = new ArrayList<>();
}

public final class Sequence extends Composite {
  private int index = 0; // memory sequence
  public Status tick(BehaviorContext ctx) {
    while (index < children.size()) {
      Status s = children.get(index).tick(ctx);
      if (s == Status.RUNNING) return Status.RUNNING;
      if (s == Status.FAILURE) { index = 0; return Status.FAILURE; }
      index++; // success
    }
    index = 0; return Status.SUCCESS;
  }
}

public final class Selector extends Composite {
  private int index = 0;
  public Status tick(BehaviorContext ctx) {
    while (index < children.size()) {
      Status s = children.get(index).tick(ctx);
      if (s == Status.RUNNING) return Status.RUNNING;
      if (s == Status.SUCCESS) { index = 0; return Status.SUCCESS; }
      index++; // failure
    }
    index = 0; return Status.FAILURE;
  }
}

public abstract class Decorator implements Node { protected final Node child; /* ctor */ }

public final class Wait extends Decorator {
  private final long ticks; private long acc;
  public Status tick(BehaviorContext ctx) {
    acc += 1; return (acc >= ticks) ? Status.SUCCESS : Status.RUNNING;
  }
}

public abstract class Action implements Node { /* override tick */ }
```

## Hybrid Approach (Often Best in Practice)
Use BT at the top level to select and sequence goals/tasks. Implement each leaf Action as a small HFSM (your StagedStep/TimeBasedStep refined with StepResult). This gives you:
- BT: clean high-level decision flow and prioritization
- HFSM leaves: ergonomic, deterministic scripting for time windows and effects

## Migration Plan Toward BT (If Desired)
1) Build the minimal BT runtime above (or adopt a lightweight library) and bridge it to BehaviorContext as the blackboard.
2) Wrap existing steps as Action nodes (thin adapters). Use your refined HFSM/StepResult inside those actions for timing.
3) Start with one behavior (e.g., TameWolf) authored as a tree; measure readability and bug surface.
4) Add logging/visualization (tree dumps with per-node status) for debugging.
5) Expand as needed; keep both approaches side-by-side if some behaviors are better served by HFSM-only.
