# Villager Info GUI — Product Requirements (PM Draft)

**Date:** 2026-02-22  
**Project:** Settlements-Alpha  
**Audience:** Product Manager, Gameplay Design, Engineering  
**Status:** Draft for implementation kickoff

---

## 1) Purpose

Define a player-facing **Villager Info GUI** that explains what a villager is doing and why, with explicit sections for:

1. **Behaviors**
   - Ongoing behaviors (including current stage)
   - Stopped behaviors (including stop reason such as cooldown/precondition failure)
2. **Sensors**
3. **Genetics**

This doc focuses on what should be shown to the end user, what data is currently available in code, what is missing, and a phased delivery plan.

---

## 2) Product Goals

1. Improve player understanding of villager AI decisions.
2. Reduce “black box” confusion when villagers appear idle or stuck.
3. Provide balancing/debug visibility for design and QA without requiring logs.
4. Keep UI lightweight enough for real gameplay use (not only developer debug).

### Non-Goals (MVP)

- Full AI debugger with step-by-step timeline replay.
- Editing villager AI state from GUI.
- Displaying every vanilla memory/sensor internals in raw form.

---

## 3) Primary Users / Personas

- **Player (power user):** Wants to understand why villager is not doing expected job.
- **Designer/PM:** Wants to validate behavior coverage and balancing outcomes.
- **QA/Dev:** Needs quick diagnosis without attaching debugger.

---

## 4) UI Information Architecture

## 4.1 Entry Point

MVP recommendation:
- Open GUI by targeting a villager and using an inspect action (temporary command-based opening is acceptable in first internal build).

## 4.2 Layout (Single Screen, Tabbed)

### Header (Always visible)

- Villager name / entity id
- Profession + level/expertise
- Current high-level activity bucket (if available)
- Last refresh time indicator

### Tabs

1. **Behaviors**
2. **Sensors**
3. **Genetics**

---

## 5) Detailed Display Requirements

## 5.1 Behaviors Tab

Show two sections:

### A) Ongoing Behaviors

Per behavior card:
- Behavior display name
- Status (RUNNING)
- **Current stage** (for state-machine/staged behaviors)
- Time in current run (seconds)
- Continue condition summary (pass/fail badge)

### B) Stopped Behaviors

Per behavior row:
- Behavior display name
- Status (STOPPED)
- **Stop reason category**:
  - Cooling down
  - Preconditions not met
  - Continue condition failed
  - Failed (StepResult.Fail)
  - Aborted (StepResult.Abort)
  - Not selected/scheduled
- Cooldown remaining (if applicable)
- Last stop timestamp / elapsed time since stop

### UX Rules

- Sort ongoing behaviors first by priority, then start time.
- Sort stopped behaviors by “most recently changed”.
- Use color badges for reason category (e.g., blue=cooldown, yellow=precondition, red=abort).

---

## 5.2 Sensors Tab

Per sensor row:
- Sensor name
- Enabled/disabled status
- Scan range (horizontal/vertical)
- Cooldown / next scan ETA
- Latest result summary (e.g., nearest target)
- Result freshness (ticks/seconds ago)
- Memory key written by sensor

If no result: explicit “No valid target found” state instead of blank.

---

## 5.3 Genetics Tab

### Core Gene Table

Show all genes with:
- Gene name (Strength, Constitution, Agility, Intelligence)
- Raw value (0.00–1.00)
- Tier badge (Low / Normal / High / Ultra)
- Gameplay interpretation tooltip

### Derived Effects Panel

MVP should show at least:
- Inventory size derived from genetics
- Any currently active threshold effects that are already implemented

Future-friendly fields:
- Mutation lineage hints (if added later)
- Parent comparison (if parent tracking is implemented)

---

## 6) Data Availability Matrix (Current Codebase)

| UI Data | Availability | Notes / Source |
|---|---|---|
| Behavior status (RUNNING/STOPPED) | **Available** | `IBehavior#getStatus`, `BehaviorStatus`, `AbstractBehavior` |
| Behavior staged current stage | **Partially available** | Exists in `StagedStep.currentStage`, not exposed via UI snapshot API |
| Stop reason (cooldown vs precondition vs fail/abort) | **Missing unified model** | Logic exists across behavior lifecycle, but no standardized externally-consumable reason object |
| Preconditions/continue conditions list | **Available (definitions)** | In `BehaviorDefinition` / behavior precondition lists; runtime failed-condition history not persisted |
| Cooldown remaining | **Partially available** | Runtime tickables track cooldown; needs explicit DTO field export |
| Sensor framework contract | **Available** | `ISensor`, `AbstractSensor`, result objects |
| Sensor coverage breadth | **Limited** | Concrete sensor implementation is minimal (notably `NearestSugarcaneSensor`) |
| Sensor latest result + freshness in UI form | **Missing** | Result goes to brain/memory; no player-facing aggregation snapshot |
| Genetics values | **Available** | `BaseVillager.genetics`, `GeneticsProfile`, `GeneType`, saved/loaded via NBT |
| Derived inventory size from genetics | **Available** | `GeneticInventoryProvider` maps STR/CON to size |
| Existing villager info GUI | **Not available** | Only config screen + test command inventory screen exist |

---

## 7) MVP Functional Requirements

1. Player can open Villager Info GUI on targeted villager.
2. Behaviors tab shows:
   - Ongoing behaviors + current stage
   - Stopped behaviors + stop reason category + cooldown remaining
3. Sensors tab shows all registered settlement sensors for villager with latest result summary.
4. Genetics tab shows all gene values with tier labeling and derived inventory size.
5. Data refreshes periodically (recommended every 10 ticks) while GUI is open.
6. If villager unloads/despawns, UI shows clear “entity unavailable” state.

---

## 8) Engineering Requirements (Product-facing)

To support the above UI, engineering should add a server-authoritative snapshot pipeline:

## 8.1 Snapshot DTOs

- `VillagerInfoSnapshot`
  - identity/profession/expertise
  - `List<BehaviorSnapshot>`
  - `List<SensorSnapshot>`
  - `GeneticsSnapshot`
  - timestamp

- `BehaviorSnapshot`
  - behaviorName
  - status
  - currentStage (nullable)
  - stopReasonCategory (nullable when running)
  - cooldownRemainingTicks
  - lastTransitionTime

- `SensorSnapshot`
  - sensorName
  - enabled
  - rangeHorizontal/rangeVertical
  - cooldownRemainingTicks
  - latestResultSummary
  - resultAgeTicks
  - memoryKey

- `GeneticsSnapshot`
  - gene map
  - derived effects (inventory size, etc.)

## 8.2 Required Instrumentation Gaps

1. Add standardized **behavior stop reason enum** and set it whenever status changes to STOPPED.
2. Expose staged **current stage** in behavior snapshots.
3. Add sensor registry/collector so all active sensors can be listed and summarized.
4. Add packet(s) for request/response and periodic refresh while screen is open.

---

## 9) Phased Delivery Plan

### Phase 1 (MVP / Internal Playtest)

- Implement snapshot DTOs + packet path.
- Build tabbed GUI shell and header.
- Behaviors tab with running/stopped + reason categories.
- Genetics tab with values and inventory-size derivation.
- Sensors tab with currently available sensors.

### Phase 2 (Polish + Expansion)

- Better reason explanations/tooltips (human-friendly copy).
- Filtering/sorting/search in behavior/sensor lists.
- Timeline/event history panel (last N transitions).
- Additional sensors and richer summaries.

### Phase 3 (Advanced Debug UX)

- Optional “developer mode” for deeper memory/condition diagnostics.
- Export/share snapshot for bug reports.

---

## 10) Acceptance Criteria

- A player can inspect any loaded villager and see all three tabs.
- For at least one staged behavior, current stage is visible while running.
- Stopped behavior reason is visible and correctly distinguishes cooldown vs unmet conditions.
- Gene values match saved villager genetics data.
- No severe client/server performance regressions when one info screen is open.

---

## 11) Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| Missing unified stop-reason model | Incomplete behavior section | Add explicit stop reason enum + transition instrumentation first |
| Sensor ecosystem currently sparse | Sensors tab looks empty | Ship with current sensors + clear “more sensors coming” framing |
| Snapshot refresh overhead | Server/client performance cost | Use capped refresh interval and only while screen is open |
| Data inconsistency between server/client | Confusing UI | Keep server authoritative; send timestamped snapshots |

---

## 12) Open PM Decisions

1. Should this UI be a player progression unlock, debug tool, or both?
2. Should non-technical players see raw internals (stage keys/reason codes), or translated summaries only?
3. Should genetics include “breeding guidance” text in MVP or Phase 2?
4. Should this screen pause updates while unfocused to reduce packet traffic?

---

## 13) Recommended Next Step

Approve this PM scope, then execute Phase 1 with a technical design ticket set for:
- snapshot contracts,
- behavior stop-reason instrumentation,
- sensor aggregation,
- client GUI implementation.
