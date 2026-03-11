# Behavior Controller UI v2 — Problem Review (Phases A–D)

**Date:** 2026-03-03  
**Reviewer:** Senior engineer review  
**Scope:** Current implementation of Phase A/B/C/D in `Settlements-Alpha`

---

## 1) Alignment and disposition summary

This document reflects the latest team alignment from review jam discussion.

## Disposition matrix

| Item | Status | Notes |
|---|---|---|
| P1 | Keep (high priority) | Replace reflection with behavior-owned `BehaviorDescriptor` + `BehaviorRuntimeInformation` (hard mandate). |
| P2 | **Removed from issue list** | Team decision: behaviors always provide preconditions; solved by P1 redesign ownership. |
| P3 | Keep (implement last) | Add deterministic behavior UI ID (`0..n`) and schedule-registration context in row UI. |
| P4 | **Removed from issue list** | Heartbeat ACK packet is accepted design. Option C is not equivalent for client→server liveness. |
| P5 | Keep | Prebuild/cached row render data on snapshot apply. |
| P6 | Keep | Move shared model package to `application.behaviorui.model`. |
| P7 | Keep | Choose Option A: behavior-owned descriptor contract (no fallback registry). |
| P8 | Keep (deferred hardening) | Keep permissive behavior now; add explicit TODO for future server-side validation. |
| P9 | Keep | Localize debug fallback message. |
| P10 | Keep | Remove emotional TODOs after fixes; keep only ticketed actionable TODOs. |

---

## 2) Build/health check

- Build check executed: `./gradlew.bat --no-daemon compileJava -x test`
- Result: **BUILD SUCCESSFUL**

---

## 3) Detailed issue list (updated)

## P1 — Reflection in snapshot builder hot path

**Severity:** High  
**Category:** Correctness + Efficiency + Maintainability

### Agreed direction
Use typed behavior contracts with **no reflection** and **no fallback path**.

Hard rule:
- every behavior shown in Behavior UI must provide descriptor + runtime info.

Design split is intentional:
- descriptor = static/precomputed display data
- runtime information = dynamic polled state only

### Low-level target design

```java
public record BehaviorDescriptor(
        @Nonnull String displayNameKey,
        @Nonnull ResourceLocation iconItemId,
        @Nullable String displaySuffix
) {}
```

```java
public record BehaviorRuntimeInformation(
        @Nullable String currentStageLabel,
        int cooldownRemainingTicks,
        @Nonnull PreconditionSummary preconditionSummary
) {}
```

```java
public interface IBehaviorInfoProvider {
    @Nonnull BehaviorDescriptor getBehaviorDescriptor();
    @Nonnull BehaviorRuntimeInformation getBehaviorRuntimeInformation(@Nonnull BaseVillager villager);
}
```

```java
// Snapshot builder: no reflection + no direct re-evaluation logic.
IBehaviorInfoProvider info = (IBehaviorInfoProvider) binding.behavior();
BehaviorDescriptor descriptor = info.getBehaviorDescriptor();
BehaviorRuntimeInformation runtime = info.getBehaviorRuntimeInformation(villager);

return new BehaviorRowSnapshot(
        binding.behavior().getClass().getSimpleName(), // temporary until/if row model drops behaviorId
        descriptor.displayNameKey(),
        descriptor.iconItemId(),
        binding.priority(),
        binding.behavior().getStatus() == BehaviorStatus.RUNNING,
        runtime.currentStageLabel(),
        runtime.cooldownRemainingTicks(),
        runtime.preconditionSummary()
);
```

### Policy
- All custom behaviors in behavior UI must implement `IBehaviorInfoProvider`.
- Reflection path should be removed entirely.
- Descriptor suffix/context (if needed, e.g. species list) should be precomputed in constructor and returned by descriptor, not runtime information.

---

## P3 — Possible duplicate logical behaviors in tracked rows

**Severity:** High  
**Category:** Correctness + UX  
**Priority note:** Implement this **last** among current fixes.

### Clarification
Risk is mainly **UI/model ambiguity**, not guaranteed AI runtime collision.  
Example: if logical behavior appears in both MEET and IDLE bindings, UI can show multiple rows for what players perceive as one behavior.

### Agreed UX update
- Show a deterministic row prefix: `ID <n>` (`n` starts from 0).
- Show schedule registration context in gray text or tooltip:
  - e.g. `Schedule: MEET`
  - or `Schedule: WORK, MEET`

### Low-level implementation direction

```java
public record BehaviorBinding(
        @Nonnull IBehavior<BaseVillager> behavior,
        int priority,
        int uiBehaviorIndex,
        @Nonnull Set<Activity> registeredActivities
) {}
```

```java
// assign deterministic index during registration collection
int nextUiBehaviorIndex = 0;
trackedBehaviors.add(new BehaviorBinding(behavior, weight, nextUiBehaviorIndex++, Set.of(Activity.MEET)));
```

```java
// row render prefix example
Component idPrefix = Component.literal("ID " + row.uiBehaviorIndex());
Component scheduleMeta = Component.literal("Schedule: " + row.scheduleLabel());
```

---

## P5 — UI render path has avoidable allocations

**Severity:** Medium  
**Category:** Efficiency

### Agreed fix
Build row render artifacts once in `applySnapshot(...)`, render from cache each frame.

### Low-level example

```java
private static final class CachedRowView {
    final ItemStack icon;
    final Component leftText;
    final Component rightText;
    final int rightColor;
    final boolean running;

    CachedRowView(ItemStack icon, Component leftText, Component rightText, int rightColor, boolean running) {
        this.icon = icon;
        this.leftText = leftText;
        this.rightText = rightText;
        this.rightColor = rightColor;
        this.running = running;
    }
}

private List<CachedRowView> cachedRows = List.of();

public void applySnapshot(BehaviorControllerSnapshot snapshot) {
    this.snapshot = snapshot;
    this.cachedRows = snapshot.rows().stream().map(this::toCachedRow).toList();
}
```

Render method should only draw cached primitives/components.

---

## P6 — Layering leak: presentation model used in network payloads

**Severity:** Medium  
**Category:** Clean architecture

### Agreed fix
Move shared snapshot/row/enums from presentation package to:
- `application.behaviorui.model`

### Impacted references
- Packet payload classes
- Snapshot builder
- Client state/screen/handlers

---

## P7 — Metadata registry is hard-coded and closed for extension

**Severity:** Medium  
**Category:** Maintainability / architecture

### Agreed direction
Choose **Option A**: behavior-owned metadata descriptor.

### Option A details (selected)

```java
public record BehaviorDescriptor(
    @Nonnull String displayNameKey,
    @Nonnull ResourceLocation iconItemId,
    @Nullable String displaySuffix
) {}

public interface IBehaviorDisplayMetadataProvider {
    @Nonnull BehaviorDescriptor getBehaviorDescriptor();
}
```

Resolution rule:
1. Every custom behavior used by behavior UI must implement provider.
2. No central fallback registry path.
3. Descriptor may be precomputed in ctor and returned as immutable value.

### Option B note (not selected now)
Data-driven JSON mapping remains possible later as an override layer, but not primary source.

---

## P8 — Server open validation may be too permissive

**Severity:** Medium (deferred hardening)  
**Category:** Security-hardening

### Agreed decision
Keep current permissive behavior for now (pre-launch stage), but document TODO explicitly.

### TODO template to add in server open handler

```java
// TODO(#behavior-ui-security): Add server-side inspect validation before session open:
// 1) distance cap from player to villager
// 2) optional LOS check
// 3) debug/dev bypass flag for local testing
```

---

## P9 — Non-localized fallback string in client debug open flow

**Severity:** Low  
**Category:** UX / i18n consistency

### Agreed fix
Replace literal with translatable key and add key to `en_us.json`.

---

## P10 — TODO debt left in core runtime path

**Severity:** Low/Medium  
**Category:** Code quality / maintainability

### Agreed handling
After P1/P6/P7/P9 are implemented:
- remove stale/emotional TODO comments,
- keep only actionable TODOs tied to a ticket/issue id.

---

## 4) Non-issues removed from problem list

## P2 (removed)
No longer tracked as a standalone issue per team decision and planned P1 redesign ownership.

## P4 (removed)
Heartbeat ACK packet is accepted architecture.  
Note: server->client snapshot piggyback (former Option C) cannot replace client->server heartbeat for session timeout semantics.

---

## 5) Suggested implementation sequence (updated)

1. P6 — move models to `application.behaviorui.model` and update imports.
2. P1 — introduce `BehaviorDescriptor` + `BehaviorRuntimeInformation` + behavior provider interface; remove reflection.
3. P7 — behavior-owned metadata contract (no fallback registry).
4. P5 — render cache/prebuilt components.
5. P9 — localization cleanup.
6. P10 — TODO cleanup pass.
7. P8 — add explicit deferred-security TODO comments.
8. P3 — UI behavior ID + schedule registration metadata (implement last).

---

## 6) Practical sign-off recommendation

Do **not** final-sign-off A–D yet.  
After implementing P1/P6/P7/P5 and quick cleanup items (P9/P10), this should be in strong shape for sign-off with P3 scheduled as the final UX/clarity pass.
