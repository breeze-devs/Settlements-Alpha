# Behavior Controller UI v2.0 — NeoForge Technical Design (Revised for PM Sign-off)

**Date:** 2026-02-27  
**Project:** Settlements-Alpha  
**Status:** Proposed (Revised after PM discussion)  
**Audience:** PM, Tech Lead, Junior Engineer (implementation handoff)

---

## 1) Purpose of this document

This is the implementation-level technical design for the **Behavior Controller UI v2.0**.

It documents:
1. PM requirement interpretation,
2. alternatives considered (server/client/data model),
3. final architecture recommendation,
4. P0 vs P1 scope split,
5. implementation sequencing and sign-off criteria.

This revision incorporates all feedback from PM/engineering discussion.

---

## 2) Source inputs reviewed

1. PM draft: `20260222_villager_info_gui_product_requirements.md`
2. Initial engineer proposal: `20260221_behavior_ui_system_initial.md`
3. Codebase architecture review (NeoForge packet infra, UI, behavior runtime, villager internals)

---

## 3) Updated PM priorities (authoritative)

These points supersede earlier assumptions:

1. **Server authority is required** (AI truth on server).
2. **Near-instant updates are not required**.
   - Acceptable lag: ~10–20 ticks (0.5–1s).
3. **Dynamic row layout is required** (running vs inactive rendering).
4. **Precondition hover tooltip is P1** (good-to-have, not P0).
5. **Vanilla visual style is required**, but this does **not** require chest-style layout or inventory-slot UI.

---

## 4) Current codebase reality (impact on design)

### 4.1 Networking
- Packet registration path exists (`PacketRegistry`, handlers).
- Current registered packets are bubble-render related.
- Existing bubble packet serialization uses JSON strings via `FriendlyByteBuf#writeUtf`.
  - This is acceptable for low-frequency packets,
  - not ideal for a repeatedly refreshed behavior UI.

### 4.2 UI
- Custom UI is minimal (`ConfigScreen` stub).
- No existing behavior diagnostics UI/menu yet.

### 4.3 Behavior runtime
- Available now:
  - `BehaviorStatus` (RUNNING/STOPPED)
  - precondition lists
  - cooldown tickables
  - staged behavior current stage (`StagedStep.currentStage`)
- Missing for polished diagnostics:
  - unified stop reason model,
  - standardized condition label contract,
  - explicit snapshot DTO layer.

### 4.4 Villager schedule/activity
- `BaseVillager` already configures vanilla activities and schedule updates.
- Needed schedule phase information is available from brain activity state.

---

## 5) Alternatives considered and decision record

## 5.1 Server synchronization strategy

### Option S1 — Periodic full snapshot push (server -> client)
**Description:** server sends full UI snapshot every fixed interval while screen is open.

**Pros**
- simplest implementation,
- easiest debugging,
- low desync risk,
- very suitable when 10–20 tick lag is acceptable.

**Cons**
- sends repeated static data,
- less bandwidth-efficient at scale than deltas.

---

### Option S2 — Client polling request/response
**Description:** client requests snapshot every interval; server responds.

**Pros**
- explicit demand-driven flow,
- server does not need aggressive push scheduling.

**Cons**
- noisier protocol,
- easy to abuse without throttling,
- no practical benefit over S1 for this use case.

---

### Option S3 — Delta/event-driven + heartbeat hybrid
**Description:** send only changed rows immediately; periodic rebase snapshot.

**Pros**
- best network efficiency,
- best responsiveness.

**Cons**
- highest complexity,
- more failure modes,
- harder for junior implementation in first delivery.

---

### Option S4 — SynchedEntityData-only
**Description:** encode UI state into entity synced fields.

**Pros**
- leverages existing entity replication.

**Cons**
- poor fit for large, structured per-behavior lists,
- brittle and hard to evolve.

---

### Server final decision (for P0)
✅ **Choose S1: periodic full snapshot push** every **10 ticks** while UI is open.

Rationale:
- aligns with updated PM tolerance (0.5–1s latency acceptable),
- minimizes implementation risk,
- easier correctness and PM demo confidence.

> P1/P2 may introduce static/dynamic split or delta optimization if profiling justifies it.

---

## 5.2 Client architecture strategy

### Option C1 — `Screen` (custom, advancements-style)
**Pros**
- no inventory/menu boilerplate,
- fully custom layout,
- matches non-chest GUI patterns.

**Cons**
- lifecycle cleanup must be handled manually.

---

### Option C2 — `AbstractContainerScreen` with slotless menu
**Pros**
- strong open/close lifecycle scaffolding,
- very robust in multiplayer edge cases.

**Cons**
- extra server menu plumbing for no-slot UI,
- added complexity for first version.

---

### Important clarification
`AbstractContainerScreen` does **not** force chest visuals.  
It only provides container/menu lifecycle plumbing.

Likewise, many vanilla non-inventory experiences use plain `Screen` (advancements-style UX pattern).

---

### Client final decision (for P0)
✅ **Choose C1: plain `Screen`** with custom vanilla-themed rendering.

Add lifecycle safeguards:
- explicit open session token from server,
- client unsubscribe packet on `onClose()`,
- server-side timeout/cleanup for safety.

---

## 5.3 Data model / instrumentation strategy

### Option D1 — compute transient values at snapshot time only
**Pros:** minimal runtime changes.
**Cons:** weaker explainability/history and harder future expansion.

### Option D2 — explicit diagnostics instrumentation model
**Pros:** stable schema and extensibility.
**Cons:** more upfront runtime edits.

### Data final decision
✅ **Use D2-lite for P0**:
- keep instrumentation minimal but explicit,
- enough fields for schedule + dynamic rows,
- defer detailed condition tooltips to P1.

---

## 6) Networking reliability note

Minecraft/NeoForge packet delivery is on TCP (ordered/reliable transport), so random packet loss is less likely than UDP-style protocols.

Even so, choosing full snapshots (S1) is still prudent for P0 because:
- protocol and debugging are simpler,
- no need to recover missing delta history,
- correctness remains clear during development.

---

## 7) Final architecture (P0 approved target)

## 7.1 High-level flow
1. Player targets villager and triggers inspect action/command.
2. Client sends open request packet with villager entity id.
3. Server validates target and starts a viewer session.
4. Server sends open-UI packet + initial full snapshot.
5. Client opens `BehaviorControllerScreen` and renders data.
6. Server pushes full snapshot every 10 ticks while session is active.
7. On close/unavailable/disconnect, session is cleaned up.

---

## 7.2 Packet contract (P0)

### Serverbound
- `ServerBoundOpenBehaviorControllerPacket(int villagerEntityId)`
- `ServerBoundCloseBehaviorControllerPacket(long sessionId)`

### Clientbound
- `ClientBoundOpenBehaviorControllerPacket(long sessionId, int villagerEntityId)`
- `ClientBoundBehaviorControllerSnapshotPacket(long sessionId, BehaviorControllerSnapshot snapshot)`
- `ClientBoundBehaviorControllerUnavailablePacket(long sessionId, String reasonKey)`

Encoding:
- Use typed `StreamCodec` fields,
- avoid JSON string payloads for refresh packets.

---

## 7.3 Session manager

Create `BehaviorControllerSessionService` with:
- `Map<UUID playerUuid, BehaviorControllerSession>`

Session fields:
- sessionId,
- player UUID,
- villager entity id,
- openedAtGameTime,
- lastSentGameTime.

Lifecycle rules:
- one active behavior UI session per player,
- invalidate on close/disconnect/villager missing/out-of-range,
- stale timeout guard (e.g., 5s without valid client presence check).

---

## 7.4 Snapshot DTO shape

`BehaviorControllerSnapshot`
- `long gameTime`
- `int villagerEntityId`
- `String villagerName`
- `SchedulePhase phase` (`REST|WORK|IDLE|MEET|UNKNOWN`)
- `List<BehaviorRowSnapshot> rows`

`BehaviorRowSnapshot`
- `String behaviorId`
- `String displayNameKey`
- `ResourceLocation iconItemId`
- `int priority`
- `boolean running`
- `@Nullable String currentStageLabel`
- `int cooldownRemainingTicks`
- `PreconditionSummary preconditionSummary` (`PASS|FAIL|UNKNOWN`)

P1 extension fields:
- detailed precondition list (label + bool),
- stop reason category,
- last transition tick.

---

## 7.5 Behavior instrumentation requirements

### P0 required
1. Expose running/stopped status.
2. Expose stage label when behavior is staged and running.
3. Expose cooldown remaining.
4. Expose aggregate precondition summary pass/fail.

### P1 required
1. Precondition detailed tooltip list.
2. Optional stop reason categories.
3. Optional transition history metadata.

---

## 7.6 Rendering spec (P0)

### Global style
- Vanilla-inspired dark panel, raised borders, Minecraft font.

### Header
- Centered title: `BEHAVIOR CONTROLLER`
- Schedule icon strip under title:
  - Bed (REST)
  - Crafting Table (WORK)
  - Music Disc (IDLE)
  - Bell (MEET)
- Active icon highlighted.

### Middle list (dynamic rows)
- Sorted by priority descending (top highest priority).
- Common elements: icon + behavior name.

Running row:
- thick green icon border,
- right text: `Stage: <...>`.

Inactive row:
- default border,
- right text: cooldown + summary precondition marker.

### Footer
- Left button: `ENABLE ALL` (P0 can be stub/no-op with tooltip)
- Right button: `BACK` closes screen.

### P1
- Hover over precondition marker shows detailed condition tooltip.

---

## 8) P0 vs P1 scope split

## P0 (must-have for sign-off)
1. Server-authoritative screen with periodic full snapshots (10 tick cadence).
2. Schedule banner + active phase highlight.
3. Dynamic row rendering (running vs inactive content).
4. Robust close/unavailable handling.
5. Vanilla-themed visual style.

## P1 (post-P0 quality/debug depth)
1. Detailed precondition hover tooltip list.
2. Stop reason categorization display.
3. Sync optimization (split static/dynamic or delta if needed).

---

## 9) Risks and mitigation

| Risk | Impact | Mitigation |
|---|---|---|
| Over-frequent full snapshots | unnecessary bandwidth | 10-tick cadence cap, optional P1 optimization |
| Client forgets to close session cleanly | stale server session | server timeout + disconnect cleanup |
| Missing stage for non-staged behavior | confusing UI | show `Stage: N/A` or hide stage text when null |
| Behavior naming/icons not standardized | poor UX consistency | add behavior metadata mapping registry |
| Junior implementation introduces lifecycle bugs | stability risk | provide explicit LLD with packet/flow/state diagrams |

---

## 10) PM sign-off checklist

PM should confirm:

1. P0 permits 10-tick update cadence.
2. Tooltip detail is P1 (not blocking P0).
3. Full-snapshot push model is acceptable for first release.
4. `Screen`-based client UI is acceptable (not menu-backed for P0).
5. `ENABLE ALL` button behavior in P0 can be placeholder or simple global toggle.

---

## 11) Implementation roadmap after sign-off

### Step 1 — Protocol and session foundation
- Add packets and codec registration.
- Implement session service and periodic publisher.

### Step 2 — Snapshot builder
- Build server snapshot assembler from villager behavior runtime.
- Add phase mapping and row sorting.

### Step 3 — Client screen
- Build `BehaviorControllerScreen` with vanilla-themed rendering.
- Implement dynamic rows and footer interactions.

### Step 4 — Hardening and playtest
- Add unavailable states, close semantics, and logging.
- Validate in multiplayer and long-running sessions.

### Step 5 — P1 debug enrichments
- Add detailed precondition tooltip and optional stop reasons.

---

## 12) Handoff note for upcoming LLD

After PM sign-off, next artifact should be a dedicated **LLD for junior implementation** including:
- exact package/class list,
- packet field-by-field schema,
- server tick/update pseudocode,
- UI rendering pseudocode and layout constants,
- state machine for open/close/unavailable transitions,
- test plan (unit + in-game integration).

This technical design is intentionally architecture-heavy; LLD should be task-by-task and implementation-prescriptive.

---

## 13) Post-signoff Q&A decisions (authoritative addendum)

These decisions came from follow-up PM/engineering Q&A and are now authoritative for implementation.

### 13.1 Session liveness and keep-alive

To avoid orphan server sessions when client closes/crashes without sending close packet:

1. Keep `C2S Close(sessionId)` as fast-path teardown.
2. Add `C2S Heartbeat(sessionId)` every **40 ticks** while UI is open.
3. Expire session if no heartbeat for **120 ticks**.
4. On publish ticks, still validate player and target villager existence.

### 13.2 Open-request protocol path

- Command-based open can start directly on server.
- Future keybind open requires C2S open request.

Decision: keep C2S open packet in protocol design for forward compatibility, while allowing command flow to invoke open use-case directly server-side.

### 13.3 Schedule extensibility

Do not lock schedule modeling only to the current 4 icons.

Snapshot should carry:
- `scheduleBucket` (REST/WORK/IDLE/MEET/UNKNOWN) for current UI rendering,
- `rawActivityKey` (string) for future activities (PLAY/RAID/PANIC/etc.).

### 13.4 Behavior data acquisition approach

For P0, use **polling**, not behavior event bus listeners.

Important constraint:
- do not poll all villagers globally,
- poll only villagers referenced by active UI sessions.

Performance model per publish cycle:

`O(active_sessions * behaviors_per_target_villager)`

### 13.5 Runtime linkage for safe polling

Add explicit tracked bindings to avoid fragile introspection into vanilla brain internals:

- `BehaviorBinding`
  - `IBehavior<BaseVillager> behavior`
  - `int priority`
  - `String behaviorId`

- `BaseVillager#trackedCustomBehaviors : List<BehaviorBinding>`

When custom behavior instances are created/registered, store these same instances in `trackedCustomBehaviors`.
Snapshot builder reads from this tracked list.

---

## 14) Diagram appendix

## 14.1 Component architecture diagram

```text
+---------------------------+        +----------------------------------+
|        Presentation       |        |         Infrastructure           |
|---------------------------|        |----------------------------------|
| BehaviorControllerScreen  |<------>| Behavior UI Packet Handlers      |
| BehaviorControllerState   |        | PacketRegistry / Receivers       |
+-------------+-------------+        +----------------+-----------------+
              |                                       |
              v                                       v
+-------------+-------------+        +----------------+-----------------+
|        Application        |        |        Domain / Runtime          |
|---------------------------|        |----------------------------------|
| SessionService            |------->| BaseVillager + tracked behaviors |
| SnapshotBuilder           |        | IBehavior / StagedStep / cooldown|
+---------------------------+        +----------------------------------+
```

## 14.2 Class relationship diagram

```text
BehaviorControllerServerEvents
  -> BehaviorControllerSessionService
  -> BehaviorControllerSnapshotBuilder
  -> PacketDistributor(send ClientBound...Snapshot)

BehaviorControllerSnapshotBuilder
  -> BaseVillager#trackedCustomBehaviors (List<BehaviorBinding>)
  -> BehaviorDisplayMetadataRegistry
  -> BehaviorControllerSnapshot

BehaviorBinding
  - IBehavior<BaseVillager> behavior
  - int priority
  - String behaviorId
```

## 14.3 Sequence diagram (open/publish/close)

```text
Client(Screen) -> C2S Open -> Server Open Handler -> SessionService(start)
Server -> C2S OpenAck -> Client opens UI

loop every 10 ticks
  ServerTick -> SessionService(list)
             -> SnapshotBuilder(build)
             -> send snapshot
  Client -> render latest snapshot
end

loop every 40 ticks while screen open
  Client -> C2S Heartbeat
end

Client -> C2S Close -> SessionService(close)
alt close packet missing
  heartbeat timeout -> SessionService(expire)
end
```

---

## 15) Delivery staging update (game-jam execution)

Implementation rollout should follow these explicit stages:

### Stage A — UI mock first
- Build `BehaviorControllerScreen` with fake/mock snapshot data.
- Validate visual style, schedule banner, row rendering, scrolling.

### Stage B — Packet plumbing only
- Register packets and handlers.
- Send/receive test payloads; verify via logs/chat.

### Stage C — Session lifecycle
- Implement open/close/heartbeat/timeout flow.
- Use mock snapshot source first.

### Stage D — Runtime integration
- Wire snapshot builder to tracked villager behavior bindings.
- Enable periodic server publish every 10 ticks.

### Stage E — Hardening
- unavailable flows,
- multiplayer verification,
- cleanup and polish.
