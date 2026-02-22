# Behavior Framework Refactoring Guide: Composition & State Machines

**Date:** 2026-02-21
**Author:** Senior Engineer
**Target Audience:** SDE Interns

## Overview
We are refactoring our core Villager Behavior Framework to strictly enforce a "Composition over Inheritance" pattern. By standardizing on our `StagedStep` state-machine as the core engine for complex behaviors, we can reduce boilerplate, prevent state leaks, and make behaviors much easier to test and visualize.

Since the game is unreleased, we are doing a **hard cutover** and no backward compatibility is needed. We will refactor the base class in-place (renaming `BaseVillagerStagedBehavior` to `StateMachineBehavior`) and immediately migrate **all** existing behaviors to the new format.

Currently, every behavior implementation has to manually implement lifecycle hook tracking and Context managing:
```java
// BAD: Boilerplate every behavior has to copy-paste
private BehaviorContext context;
private final StagedStep controlStep;

@Override
public void doStart(...) { this.context = new BehaviorContext(...); ... }
@Override
public void tickBehavior(...) { this.controlStep.tick(this.context); ... }
```

We are pushing this responsibility up to `StateMachineBehavior`, so behaviors only need to define *what* they do, not *how* they are ticked.

---

## Step 1: Refactoring the Base Class

We are replacing `BaseVillagerStagedBehavior` with a new, strictly controlled base class called `StateMachineBehavior`.

**File:** `dev\breezes\settlements\models\behaviors\StateMachineBehavior.java`

### 1. Define the internal State
Add fields to hold the context and the control step.
```java
public abstract class StateMachineBehavior extends AbstractBehavior<BaseVillager> {
    private final StagedStep controlStep;
    @Nullable private BehaviorContext context;

    protected StateMachineBehavior(ILogger log, ITickable preconditionCheckCooldown, ITickable behaviorCoolDown) {
        super(log, preconditionCheckCooldown, behaviorCoolDown);
        // Step 1: Initialize the control step exactly once using an abstract supplier method
        this.controlStep = this.createControlStep();
    }

    // Subclasses MUST implement this to define their behavior tree/state machine
    protected abstract StagedStep createControlStep();
}
```

### 2. Lock down the Core Lifecycle Methods
We use `final` to guarantee standard execution flow. Do not let subclasses override these directly!

```java
@Override
public final void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
    this.context = new BehaviorContext(entity);
    this.onBehaviorStart(world, entity, this.context);
    this.controlStep.reset();
}

@Override
public final void tickBehavior(int delta, @Nonnull Level world, @Nonnull BaseVillager entity) {
    if (this.context == null) throw new StopBehaviorException("Context is null");
    
    StepResult result = this.controlStep.tick(this.context);
    
    // You should port the existing `handleStepResult` logic here to catch failures/transitions!
    this.handleStepResult(result, getExpectedEndStage(), this.getClass().getSimpleName());
}

@Override
public final void doStop(@Nonnull Level world, @Nonnull BaseVillager entity) {
    this.onBehaviorStop(world, entity);
    this.context = null;
    this.controlStep.reset();
}
```

### 3. Add Safe Hooks for Subclasses
Because we locked down `doStart` and `doStop`, we provide safe, optional hooks for subclasses that need to run initialization logic (like picking a target to Breed).

```java
/**
 * Optional hook called IMMEDIATELY before the control step begins.
 * Use this to fetch initial targets and populate the BehaviorContext.
 */
protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity, @Nonnull BehaviorContext context) {}

/**
 * Optional hook called IMMEDIATELY after the behavior finishes or is interrupted.
 * Use this to clean up resources, drop leashes, etc.
 */
protected void onBehaviorStop(@Nonnull Level world, @Nonnull BaseVillager entity) {}

/**
 * Returns the StageKey that signifies the end of this behavior.
 * Defaults to a generic END stage if not overridden.
 */
protected abstract StageKey getExpectedEndStage();
```

---

## Step 2: Migrating an Existing Behavior

Let's look at `ShearSheepBehaviorV2` and see how an intern should migrate it.

### Step 2a: Update Class Signature & Constructor
1. Change `extends BaseVillagerStagedBehavior` to `extends StateMachineBehavior`.
2. Remove the `private BehaviorContext context;` and `private final StagedStep controlStep;` fields from the behavior class. We don't need them anymore!
3. Remove the assignment of `this.controlStep = StagedStep.builder()...` from the constructor.

### Step 2b: Implement `createControlStep()`
Create a new method containing the exact builder logic you removed from the constructor:
```java
@Override
protected StagedStep createControlStep() {
    return StagedStep.builder()
        .name("ShearSheepBehaviorV2")
        .initialStage(ShearStage.SHEAR_SHEEP)
        ...
        .build();
}

@Override
protected StageKey getExpectedEndStage() {
    return ShearStage.END;
}
```

### Step 2c: Migrate `doStart` and `doStop` Hooks
Replace the legacy `@Override public void doStart(...)` with the new hook signature.

**Before:**
```java
@Override
public void doStart(@Nonnull Level world, @Nonnull BaseVillager entity) {
    this.context = new BehaviorContext(entity); // BOILERPLATE!
    
    // Real logic:
    Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
    ...
}
```

**After:**
```java
@Override
protected void onBehaviorStart(@Nonnull Level world, @Nonnull BaseVillager entity, @Nonnull BehaviorContext context) {
    // Just the real logic! Context is handed to us already initialized.
    Expertise expertise = context.getInitiator().getMinecraftEntity().getExpertise();
    ...
}
```

Similarly, move cleanup logic to `onBehaviorStop()`. Be sure to remove `this.context = null` and `this.controlStep.reset()`, as the base class does it for us now!

### Step 2d: Remove manual ticking
Simply delete the `@Override public void tickBehavior(...)` block. We don't need it at all anymore!

---

## Intern Checklist Summary
When picking up a JIRA ticket to migrate a legacy Behavior class to this new framework, do this:
1. Replace base class with `StateMachineBehavior`.
2. Delete `controlStep` and `context` fields.
3. Move `StagedStep` generation to `createControlStep()`.
4. Delete `tickBehavior()`.
5. Change `doStart` -> `onBehaviorStart` and remove context init boilerplate.
6. Change `doStop` -> `onBehaviorStop` and remove teardown boilerplate.

---

## Target Behaviors for Migration
The following behaviors currently extend `BaseVillagerStagedBehavior` and must be fully migrated:

- [ ] `BlastOreBehavior.java`
- [ ] `BreedAnimalsBehavior.java`
- [ ] `CutStoneBehavior.java`
- [ ] `HarvestSoulSandBehavior.java`
- [ ] `HarvestSugarCaneBehavior.java`
- [ ] `RepairIronGolemBehavior.java`
- [ ] `ShearSheepBehaviorV2.java`
- [ ] `TameWolfBehaviorV2.java`
- [ ] `ThrowPotionsBehavior.java`
