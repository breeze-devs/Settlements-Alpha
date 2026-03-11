# Behavior Controller UI v2.0 — Low-Level Design (LLD)

**Date:** 2026-02-27  
**Project:** Settlements-Alpha  
**Status:** Ready for implementation  
**Audience:** Junior engineer implementing P0

**Parent Design Doc:** `20260227_behavior_controller_ui_v2_neoforge_technical_design.md`

---

## 1) Purpose and scope

This LLD translates the signed-off technical design into concrete implementation instructions for **P0**.

## P0 in this LLD includes
1. Server-authoritative open/close and periodic full snapshot sync (10 ticks).
2. Behavior Controller `Screen` UI (custom-rendered, vanilla aesthetic).
3. Dynamic row rendering (RUNNING vs INACTIVE right-side layout).
4. Schedule banner with active phase highlight.
5. Session lifecycle handling (unsubscribe + heartbeat + timeout + unavailable handling).

## P1 deferred (not implemented in this LLD)
- Detailed precondition hover tooltip list.
- Stop reason categories in UI.
- Delta/static packet optimization.

---

## 2) Architecture summary (P0)

### 2.1 Runtime flow
1. Player runs inspect/open command while looking at villager.
2. Client sends `ServerBoundOpenBehaviorControllerPacket(villagerId)`.
3. Server validates and creates/updates session.
4. Server sends `ClientBoundOpenBehaviorControllerPacket(sessionId, villagerId)`.
5. Client opens `BehaviorControllerScreen` (stores sessionId).
6. Every 10 server ticks, server sends full `ClientBoundBehaviorControllerSnapshotPacket(sessionId, snapshot)`.
7. Client renders latest snapshot.
8. While open, client sends `ServerBoundHeartbeatBehaviorControllerPacket(sessionId)` every 40 ticks.
9. On close: client sends `ServerBoundCloseBehaviorControllerPacket(sessionId)`.
10. Server removes session; also removes stale/invalid sessions periodically (expire when no heartbeat for 120 ticks).

---

## 3) New classes and files

> Package names below are mandatory unless blocked by an existing style constraint.

## 3.1 Network packets

Create under:
`src/main/java/dev/breezes/settlements/infrastructure/network/behaviorui/packet/`

1. `ServerBoundOpenBehaviorControllerPacket.java`  
   Implements `ServerBoundPacket`

2. `ServerBoundCloseBehaviorControllerPacket.java`  
   Implements `ServerBoundPacket`

3. `ServerBoundHeartbeatBehaviorControllerPacket.java`  
   Implements `ServerBoundPacket`

4. `ClientBoundOpenBehaviorControllerPacket.java`  
   Implements `ClientBoundPacket`

5. `ClientBoundBehaviorControllerSnapshotPacket.java`  
   Implements `ClientBoundPacket`

6. `ClientBoundBehaviorControllerUnavailablePacket.java`  
   Implements `ClientBoundPacket`

## 3.2 Packet handlers

Create under:
`src/main/java/dev/breezes/settlements/infrastructure/network/behaviorui/handler/`

1. `ServerBoundOpenBehaviorControllerPacketHandler.java` (`ServerSidePacketHandler<...>`)
2. `ServerBoundCloseBehaviorControllerPacketHandler.java` (`ServerSidePacketHandler<...>`)
3. `ServerBoundHeartbeatBehaviorControllerPacketHandler.java` (`ServerSidePacketHandler<...>`)
4. `ClientBoundOpenBehaviorControllerPacketHandler.java` (`ClientSidePacketHandler<...>`)
5. `ClientBoundBehaviorControllerSnapshotPacketHandler.java` (`ClientSidePacketHandler<...>`)
6. `ClientBoundBehaviorControllerUnavailablePacketHandler.java` (`ClientSidePacketHandler<...>`)

## 3.3 Server packet receiver (new; missing today)

Create under:
`src/main/java/dev/breezes/settlements/infrastructure/network/packet/`

1. `ServerSidePacketReceiver.java`
   - same pattern as existing `ClientSidePacketReceiver`
   - maintains `Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<?>>`

## 3.4 DTO/model classes

Create under:
`src/main/java/dev/breezes/settlements/presentation/ui/behavior/model/`

1. `BehaviorControllerSnapshot.java`
2. `BehaviorRowSnapshot.java`
3. `SchedulePhase.java`
4. `PreconditionSummary.java`

## 3.5 Session service (server)

Create under:
`src/main/java/dev/breezes/settlements/application/behaviorui/session/`

1. `BehaviorControllerSession.java`
2. `BehaviorControllerSessionService.java`

## 3.6 Snapshot builder (server)

Create under:
`src/main/java/dev/breezes/settlements/application/behaviorui/snapshot/`

1. `BehaviorControllerSnapshotBuilder.java`
2. `BehaviorDisplayMetadataRegistry.java`
   - maps behavior class -> (`behaviorId`, `displayNameKey`, `iconItemId`)
3. `BehaviorBinding.java`
   - `IBehavior<BaseVillager> behavior`
   - `int priority`
   - `String behaviorId`

## 3.7 UI client state + screen

Create under:
`src/main/java/dev/breezes/settlements/presentation/ui/behavior/`

1. `BehaviorControllerClientState.java`
2. `BehaviorControllerScreen.java`

## 3.8 Events / ticks

Create under:
`src/main/java/dev/breezes/settlements/bootstrap/event/`

1. `BehaviorControllerServerEvents.java`
   - `@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)`
   - handles server tick and player logout cleanup

---

## 4) Packet schema (field-by-field)

Use typed `StreamCodec` and `FriendlyByteBuf` primitives. Do **not** use JSON payload strings.

## 4.1 `ServerBoundOpenBehaviorControllerPacket`
- `int villagerEntityId`

## 4.2 `ServerBoundCloseBehaviorControllerPacket`
- `long sessionId`

## 4.3 `ServerBoundHeartbeatBehaviorControllerPacket`
- `long sessionId`

## 4.4 `ClientBoundOpenBehaviorControllerPacket`
- `long sessionId`
- `int villagerEntityId`

## 4.5 `ClientBoundBehaviorControllerSnapshotPacket`
- `long sessionId`
- `BehaviorControllerSnapshot snapshot`

## 4.6 `ClientBoundBehaviorControllerUnavailablePacket`
- `long sessionId`
- `String reasonKey` (translation key style, e.g. `ui.settlements.behavior.unavailable`)

---

## 5) DTO definitions

Use records unless project style disallows; keep immutable.

## 5.1 `SchedulePhase`
Enum values:
- `REST`
- `WORK`
- `IDLE`
- `MEET`
- `UNKNOWN`

## 5.2 `PreconditionSummary`
Enum values:
- `PASS`
- `FAIL`
- `UNKNOWN`

## 5.3 `BehaviorRowSnapshot`
Fields:
- `String behaviorId`
- `String displayNameKey`
- `ResourceLocation iconItemId`
- `int priority`
- `boolean running`
- `@Nullable String currentStageLabel`
- `int cooldownRemainingTicks`
- `PreconditionSummary preconditionSummary`

## 5.4 `BehaviorControllerSnapshot`
Fields:
- `long gameTime`
- `int villagerEntityId`
- `String villagerName`
- `SchedulePhase scheduleBucket`
- `String rawActivityKey`
- `List<BehaviorRowSnapshot> rows`

---

## 6) Session service design

## 6.1 `BehaviorControllerSession`
Fields:
- `long sessionId`
- `UUID playerUuid`
- `int villagerEntityId`
- `long openedAtGameTime`
- `long lastSentGameTime`
- `long lastClientAckOrKeepAliveGameTime` *(optional; for timeout safety)*

## 6.2 `BehaviorControllerSessionService`

Use singleton-style service for now (consistent with current project patterns).

Required methods:
1. `BehaviorControllerSession startOrReplaceSession(ServerPlayer player, int villagerEntityId, long gameTime)`
2. `void closeSession(UUID playerUuid, long sessionId)`
3. `void recordHeartbeat(UUID playerUuid, long sessionId, long gameTime)`
4. `Optional<BehaviorControllerSession> getSession(UUID playerUuid)`
5. `Collection<BehaviorControllerSession> getAllSessions()`
6. `void cleanupInvalidSessions(MinecraftServer server, long gameTime)`

Rules:
- one active session per player,
- replacing an existing session is allowed,
- reject close requests with mismatched sessionId,
- remove sessions when:
  - player disconnected,
  - villager not found,
  - villager dead/unloaded,
  - timeout exceeded (no heartbeat for 120 ticks).

---

## 7) Snapshot builder logic

## 7.1 Behavior source

For P0, include **Settlements custom behaviors** tracked on the target villager via:

- `BaseVillager#trackedCustomBehaviors : List<BehaviorBinding>`.

This list should be populated at behavior registration time with the same behavior instances used by runtime execution.

Important polling rule:
- Do not scan all villagers in world.
- For each active session, resolve only its target villager and build snapshot from that villager’s tracked list.

## 7.2 Priority

Priority should reflect package order/weight where available; if unavailable, fallback to deterministic order and use `priority = 0`.

## 7.3 Current stage mapping

If behavior is a staged behavior and currently running:
- expose stage label from runtime (`StagedStep.currentStage.name()`).

Else:
- `currentStageLabel = null`.

## 7.4 Cooldown remaining

Use `IBehavior#getBehaviorCoolDown()` and compute remaining ticks from current `ITickable` state where possible.

If exact value unavailable:
- fallback `cooldownRemainingTicks = 0`.

## 7.5 Precondition summary

For P0:
- Evaluate all preconditions on snapshot build using current villager state.
- If all true: `PASS`.
- If any false: `FAIL`.
- If evaluation cannot be safely performed: `UNKNOWN`.

## 7.6 Schedule phase mapping

From villager active activity:
- `Activity.REST -> REST`
- `Activity.WORK -> WORK`
- `Activity.MEET -> MEET`
- `Activity.IDLE` or `Activity.PLAY -> IDLE`
- fallback `UNKNOWN`

Also include `rawActivityKey` in snapshot for forward compatibility with future activities.

---

## 8) Event wiring and periodic publish

## 8.1 Tick event handler

In `BehaviorControllerServerEvents` subscribe to server tick.

Two periodic responsibilities:
1. Publish full snapshots every 10 ticks.
2. Cleanup invalid sessions and heartbeat timeouts.

Pseudo:

```java
@SubscribeEvent
public static void onServerTick(ServerTickEvent.Post event) {
    MinecraftServer server = event.getServer();
    long gameTime = server.overworld().getGameTime();

    // Every 10 ticks only
    if (gameTime % 10 != 0) return;

    BehaviorControllerSessionService sessions = BehaviorControllerSessionService.getInstance();
    BehaviorControllerSnapshotBuilder builder = BehaviorControllerSnapshotBuilder.getInstance();

    sessions.cleanupInvalidSessions(server, gameTime);

    for (BehaviorControllerSession session : sessions.getAllSessions()) {
        ServerPlayer player = server.getPlayerList().getPlayer(session.playerUuid());
        if (player == null) continue;

        Optional<BaseVillager> villager = resolveVillager(player.serverLevel(), session.villagerEntityId());
        if (villager.isEmpty()) {
            sendUnavailable(player, session.sessionId(), "ui.settlements.behavior.unavailable");
            sessions.closeSession(session.playerUuid(), session.sessionId());
            continue;
        }

        BehaviorControllerSnapshot snapshot = builder.build(villager.get(), gameTime);
        sendSnapshot(player, session.sessionId(), snapshot);
        session.setLastSentGameTime(gameTime);
    }
}
```

Complexity note:
- `O(activeSessions * behaviorsOnTargetVillager)` per publish cycle.

## 8.2 Logout cleanup

Subscribe to player logout event and close any active session for that player.

---

## 9) Packet registration updates

Update `PacketRegistry#setupPackets`:

1. Keep existing bubble packet registrations.
2. Add behavior UI packet registrations:
   - `playToServer(...)` for open/close packets using `ServerSidePacketReceiver`.
   - `playToClient(...)` for open/snapshot/unavailable packets using `ClientSidePacketReceiver`.

Also update receivers’ handler maps to include new packet handlers.

---

## 10) Command integration (P0 open path)

Use temporary command-based entry point (already acceptable by PM).

## 10.1 Update `TestCommand`
Add new subcommand:
- `/stest open_behavior_ui`

Behavior:
1. raytrace for `ISettlementsVillager` similarly to existing `open_inventory`.
2. send `ServerBoundOpenBehaviorControllerPacket(villagerId)` from client context.

If command runs server-side only in your current setup, then directly invoke server-side session start + send open packet back to player.

---

## 11) Client state + screen behavior

## 11.1 `BehaviorControllerClientState`
Static singleton-like state:
- `long activeSessionId`
- `int activeVillagerEntityId`
- `@Nullable BehaviorControllerSnapshot latestSnapshot`

Methods:
1. `openSession(sessionId, villagerId)`
2. `applySnapshot(sessionId, snapshot)` (ignore stale sessionId)
3. `markUnavailable(sessionId, reasonKey)`
4. `clearSession(sessionId)`
5. `tickHeartbeatIfNeeded()` (sends heartbeat every 40 ticks while active)

## 11.2 `BehaviorControllerScreen`

Extends `Screen`.

Core fields:
- `long sessionId`
- `@Nullable BehaviorControllerSnapshot snapshot`
- `@Nullable Component unavailableMessage`
- scroll offset/index for rows.

Core methods:
1. `init()`
   - add `ENABLE ALL` button (no-op with toast/message in P0)
   - add `BACK` button (`onClose`)

2. `render(...)`
   - draw background panel (vanilla style)
   - draw title
   - draw schedule banner
   - draw row list with clipping/scroll
   - draw footer buttons

3. `onClose()`
   - send `ServerBoundCloseBehaviorControllerPacket(sessionId)`
   - clear client session state
   - call `super.onClose()`

4. `mouseScrolled(...)`
   - update row scroll position.

5. `tick()`
   - call client-state heartbeat method when session active.

---

## 12) UI rendering details and constants

Define constants in `BehaviorControllerScreen`:
- `GUI_WIDTH = 256`
- `GUI_HEIGHT = 220`
- `HEADER_HEIGHT = 36`
- `FOOTER_HEIGHT = 24`
- `ROW_HEIGHT = 20`
- `MAX_VISIBLE_ROWS = 7` (depending on final sizing)

### 12.1 Header
- title centered: `BEHAVIOR CONTROLLER`
- schedule icons in fixed order: REST / WORK / IDLE / MEET
- active phase: draw bright border/outline around icon.

### 12.2 Row rendering

Common:
- icon at left,
- translated behavior name using `displayNameKey`.

RUNNING row:
- draw thick green border around icon area,
- right side: `Stage: <label>` or `Stage: N/A`.

INACTIVE row:
- normal border,
- right side: hourglass marker + `cooldownRemainingTicks / 20` as seconds,
- precondition summary marker (`PASS` green check, `FAIL` red X, `UNKNOWN` gray `?`).

### 12.3 Footer
- left button: `ENABLE ALL`
- right button: `BACK`

---

## 13) Localization additions (`en_us.json`)

Add keys:
- `ui.settlements.behavior.title`
- `ui.settlements.behavior.enable_all`
- `ui.settlements.behavior.back`
- `ui.settlements.behavior.stage`
- `ui.settlements.behavior.stage_na`
- `ui.settlements.behavior.unavailable`
- `ui.settlements.behavior.cooldown`

Behavior display names:
- use `displayNameKey` values from metadata registry.

---

## 14) Behavior metadata registry (P0)

`BehaviorDisplayMetadataRegistry` maps behavior class name to:
- `behaviorId` (stable string),
- `displayNameKey`,
- `iconItemId`.

Populate for known custom behaviors in `CustomBehaviorPackages` (e.g. shear sheep, tame wolf, etc.).

Fallback rule:
- if behavior class not registered:
  - `behaviorId = classSimpleName`,
  - `displayNameKey = ui.settlements.behavior.unknown.<classSimpleName>` (or literal fallback),
  - `iconItemId = minecraft:barrier`.

---

## 15) Non-functional requirements (P0)

1. Snapshot cadence must remain at 10 ticks.
2. No per-tick allocations in hot render loop beyond unavoidable strings.
3. Ignore stale or mismatched session packets on client.
4. Never crash when snapshot has empty rows.
5. If villager disappears, display unavailable message and auto-close after short delay (optional) or immediate close.

---

## 16) Test plan

## 16.1 Manual test matrix

1. **Open success**
   - `/stest open_behavior_ui` while targeting villager opens screen.

2. **Periodic updates**
   - observe rows update over time at ~0.5s cadence.

3. **Running/inactive row switch**
   - verify row right-side content changes correctly.

4. **Schedule highlight**
   - verify active schedule icon changes with villager activity.

5. **Close behavior**
   - click BACK; verify no further server updates for player session.

6. **Villager unavailable**
   - killed or unload villager; verify unavailable packet/state handling.

7. **Logout cleanup**
   - open UI then disconnect; ensure session removed.

8. **Multiple players**
   - two players inspect same/different villagers; ensure session isolation.

## 16.2 Logging expectations

Add debug logs (can be gated):
- session created/closed,
- snapshot sent,
- stale packet ignored,
- unavailable sent.

---

## 17) Implementation checklist

Use this checklist as the source of truth for execution. Complete each phase in order.

### Phase A — UI mock-first (no networking dependency)

**Build tasks**
1. [ ] Create `BehaviorControllerScreen` with vanilla-styled frame/background/border.
2. [ ] Add header title + schedule banner rendering using hardcoded mock values.
3. [ ] Add row renderer with both states:
   - running row (`Stage: ...`),
   - inactive row (cooldown + summary marker).
4. [ ] Add footer controls (`ENABLE ALL`, `BACK`) with no-op behavior allowed.
5. [ ] Add scroll behavior for behavior list.

**Completion criteria (must all pass)**
- [ ] Screen opens in client dev run with mock data and no crashes.
- [ ] Visual style is clearly vanilla-like (dark panel + raised border + MC font).
- [ ] Running/inactive row switch is visually verifiable using toggled mock data.

### Phase B — Packet plumbing (transport only)

**Build tasks**
1. [ ] Implement snapshot DTO/enums (`BehaviorControllerSnapshot`, `BehaviorRowSnapshot`, `SchedulePhase`, `PreconditionSummary`).
2. [ ] Implement all 6 packets + codecs (open/close/heartbeat/openAck/snapshot/unavailable).
3. [ ] Implement client handlers for open/snapshot/unavailable.
4. [ ] Implement server handlers for open/close/heartbeat.
5. [ ] Create and wire `ServerSidePacketReceiver`.
6. [ ] Register all behavior UI packets in `PacketRegistry`.

**Completion criteria (must all pass)**
- [ ] Test packet send/receive logs confirm all packet paths work. Use the test command and orchestrate multiple behavior simulation
- [ ] No handler-missing errors for registered packet types.
- [ ] Packet codec roundtrip tested for snapshot payload (serialize/deserialize consistency).

### Phase C — Session lifecycle service

**Build tasks**
1. [ ] Implement `BehaviorControllerSession` model.
2. [ ] Implement `BehaviorControllerSessionService`:
   - start/replace,
   - close with session-id validation,
   - heartbeat update,
   - cleanup invalid/expired sessions.
3. [ ] Add timeout logic (expire at heartbeat lapse > 120 ticks).
4. [ ] Add close fast-path + timeout fallback path.
5. [ ] Add logout cleanup integration.

**Completion criteria (must all pass)**
- [ ] Closing screen sends close packet and removes session immediately.
- [ ] Force-closing client (without close packet) expires session by timeout.
- [ ] Logout with active session always cleans up server state.

### Phase D — Runtime integration (real villager data)

**Build tasks**
1. [ ] Introduce `BehaviorBinding` and `BaseVillager#trackedCustomBehaviors`.
2. [ ] Populate tracked bindings at behavior registration time (same behavior instances used by runtime).
3. [ ] Implement `BehaviorDisplayMetadataRegistry` with fallback mapping.
4. [ ] Implement `BehaviorControllerSnapshotBuilder` using tracked bindings.
5. [ ] Implement schedule mapping (`scheduleBucket`) + `rawActivityKey`.
6. [ ] Add `BehaviorControllerServerEvents` periodic publisher every 10 ticks.
7. [ ] Ensure polling is session-targeted only (no global villager scan).

**Completion criteria (must all pass)**
- [ ] Snapshot rows reflect real behavior status/stage/cooldown/precondition summary.
- [ ] Update cadence is ~10 ticks while session active.
- [ ] Complexity behavior observed as session-targeted (no global polling logic introduced).

### Phase E — Command integration + hardening

**Build tasks**
1. [ ] Add `/stest open_behavior_ui` command path (raytrace target villager).
2. [ ] Wire command to session open flow.
3. [ ] Implement unavailable-state behavior and UX message.
4. [ ] Add localization keys and behavior display name keys.
5. [ ] Add debug logs for session open/close/timeout/snapshot push.
6. [ ] Execute manual test matrix and record outcomes/screenshots.

**Completion criteria (must all pass)**
- [ ] Command opens UI on valid villager and rejects invalid target cleanly.
- [ ] Villager unload/death triggers unavailable handling without client crash.
- [ ] Multiplayer test (2 players) shows session isolation and no cross-leak.

### P0 sign-off gate (final)

Before merging, verify all are true:
1. [ ] Phases A–E completion criteria are all checked.
2. [ ] No unresolved TODOs in packet/session/snapshot/screen critical path.
3. [ ] PM-visible demo run completed with expected behavior.

---

## 18) Diagram appendix

## 20.1 Component architecture

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

## 20.2 Class relationship diagram

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

## 20.3 Sequence diagram (open/publish/close)

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

## 19) Game-jam staged rollout plan

### Stage A — UI mock first
- Build screen with fake/mock snapshot data.
- Validate visuals, dynamic rows, scrolling.

### Stage B — Packet plumbing only
- Register packets and handlers.
- Verify send/receive logs/chat output.

### Stage C — Session lifecycle
- Open/close/heartbeat/timeout handling.
- Keep mock snapshot source.

### Stage D — Runtime integration
- Populate and consume villager `trackedCustomBehaviors`.
- Enable real 10-tick snapshot publishing.

### Stage E — Hardening
- Unavailable-state handling, multiplayer testing, polish.

---

## 20) Out-of-scope reminders (do not do in P0)

- No detailed per-precondition tooltip list.
- No stop-reason badges.
- No delta synchronization.
- No menu/container migration unless major blocker appears.

---

## 21) Escalation / design guardrails

Escalate to senior engineer before changing:
1. packet schema,
2. update cadence,
3. session lifecycle policy,
4. Screen-vs-Container architectural direction.

This prevents accidental divergence from PM-approved design.
