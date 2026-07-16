# The Event Lane

This document describes the **event-lane subsystem** — the perception → knowledge → reaction
machinery that lets Settlements villagers notice things happening around them and act on them. It is
the upstream half of the reactive override lane: it produces the stimuli (sightings, harvests,
trade/courtship invites) that the override policies in
[`behavior_orchestration.md`](behavior_orchestration.md) consume.

This doc is scoped to the **orchestration-relevant** architecture: the world-event bus, the
per-villager perception pipeline, the knowledge store, and the SocialCue lane. The deeper
social-cognition subsystems that hang off it — gossip exchange and LLM dialogue — are represented
here as named seams with pointers, not expanded in full (see
[Out of scope](#out-of-scope-for-this-doc)).

---

## Why it exists

Two goals drive the design:

1. **Reactivity without telepathy.** A villager should be able to react to a fresh, important event
   near it — a zombie sighted, a resource harvested, an invite extended — but only to events it
   could *plausibly have perceived*. The event lane enforces a hard anti-telepathy rule: direct
   knowledge is gated by distance; long-range knowledge is only possible via villager-to-villager
   gossip.
2. **Decoupled producers and consumers.** Behaviors and presenters emit semantic events without
   knowing who (if anyone) will react. The bus fans out; each villager pulls what it can perceive on
   its own tick. Producers never block on consumers, and `PlanRunner` functions correctly even if the
   bus is never drained — events are optional fan-out, not required in-system flow.

---

## Pipeline at a glance

```
Producers (behaviors, presenters, PlanRunner)
      │  WorldEventEmitter.emit*(...)
      ▼
┌───────────────────────────────┐
│ WorldEventBus                 │  global append-only log, monotonic seq, ~5 s TTL
│  (per-consumer cursors)       │  SYSTEM events bypass perception; WORLD events are observable
└───────────────────────────────┘
      │  visitDelta(lastSeenSeq, …)   ← each villager drains only its own delta
      ▼
   PerceptionGate         namespace reject (SYSTEM) + Manhattan chunk-distance reject (≤ 4 chunks / 64 blocks)
      ▼
   ObservationFactory  →  ObservationBuffer      (per-villager, capacity-bounded)
      ▼
   MemoryImportanceGate   score by profession / genetics / peer-frequency / self-deed
      ▼
┌───────────────────────────────┐
│ VillagerKnowledgeStore        │  per-villager episodic memory (FIFO, ~200 entries)
│  first-hand + hearsay         │
└───────────────────────────────┘
      │
      ├──► gossip exchange                    (villager-to-villager hearsay propagation)
      └──► SocialCue triggers                 (some cues scan knowledge / perceived entities)
```

The SocialCue lane and the reactive override lane both *read* from this pipeline; neither is part of
the drain itself.

---

## Per-tick wiring

Everything runs off `BaseVillager.customServerAiStep()`, in this order:

```
1. seedEventCursorOnFirstTick()   ← on first tick, set the bus cursor to the current high-water mark
                                     so a freshly loaded villager never replays pre-load history
2. super.customServerAiStep()     ← vanilla sensors + brain housekeeping
3. settlementsBrain.tick(1)       ← brain tick; PlanRunner.tickOverride reads session registries and
                                     sensor memories — NOT the knowledge store (verified 2026-07-14)
4. tickSocialCue()                ← SocialCueArbiter.tick  (runs after the brain so it sees fresh channel occupancy)
5. tickPerception()  (throttled)  ← PerceptionPipeline.tick  (drain bus → knowledge; PERCEPTION_COOLDOWN_TICKS cadence)
6. tickReconciler()               ← unrelated: crash-orphaned teardown obligations
```

Perception is throttled because it is a catch-up cursor drain — a coarser cadence only *batches* the
delta rather than dropping events. The SocialCue admission scan is likewise throttled (~1 Hz) while
active-cue dispatch runs every tick.

Eviction is a separate server-tick job: `WorldEventBusReaperServerEvents` trims the bus roughly every
20 ticks by overworld time.

---

## WorldEventBus — the log

**File:** `domain/ai/worldevent/WorldEventBus.java` (`@ServerScope` singleton)

Modeled on Kafka's log-and-offsets pattern:

- **O(1) append.** `emit(builder, gameTick)` assigns a monotonic `seq` (from an `AtomicLong` starting
  at 1) and appends. Producers never wait for consumers.
- **Per-consumer cursors.** Each villager stores its own `lastSeenSeq` (on `SocialCueRuntimeState`,
  transient, not persisted). `visitDelta(lastSeenSeq, visitor)` visits only newer events without
  copying the log slice and returns the highest seq seen. `currentSeq()` gives the high-water mark
  used to skip history on load.
- **TTL eviction.** Events older than `EventLaneConfig.worldEventTtlTicks` (default 100 ticks / ~5 s)
  are head-trimmed by the reaper. The bus is a *transient reaction surface*, not durable memory —
  anything worth keeping is promoted into a knowledge store before it evicts.
- **Single-threaded.** All access is on the server tick thread, so no locking is needed for the log;
  the `AtomicLong` is a cheap guard for any future async emission path.

**Discipline rules (from the class doc):** `PlanRunner` must work even if the bus is never ticked;
emit only at semantic boundaries, never per-tick state; `WorldEventNamespace.SYSTEM` events bypass
per-villager perception gates.

## Producers — WorldEventEmitter and WorldEventType

**Emitter facade:** `domain/ai/worldevent/WorldEventEmitter.java` (`@ServerScope`). Behaviors and
presenters call this rather than touching the bus directly, so chunk-coordinate capture and
game-tick sourcing are centralized (it always sources overworld time so TTL matches the reaper, even
for events emitted in the Nether/End). All methods are no-ops if the bus is absent. Current surface:

`emitBehaviorStarted`, `emitBehaviorCompleted`, `emitBehaviorFailed`, `emitTerminalBehaviorEvent`,
`emitTradeInviteSent`, `emitCourtshipInviteSent`, `emitSighting`, `emitDayPlanInvalidated`,
`emitPlanExhausted`. (`emitSighting` dedupes co-witness re-announcements of the same sighting.)

**Event catalog:** `domain/ai/worldevent/WorldEventType.java` — the typed constants, each the single
source of truth for its own classification so perception/inference/memory never drift as constants
are added. Every constant carries:

| Field | Meaning |
|-------|---------|
| `namespace` (`WorldEventNamespace`) | `WORLD` = observable world fact (can pass the perception gate); `SYSTEM` = infrastructure signal (e.g. `DayPlanInvalidated`, `PlanExhausted`) that is never perceivable. |
| `observationType` (`ObservationType`) | Category for scoring/compaction: `RESOURCE`, `SOCIAL`, `ENVIRONMENT`, `TASK_COMPLETION`, `TASK_FAILURE`, … |
| base importance (`float`) | Starting salience before the importance gate applies profession/genetics/context. |
| `forceRemember` (`boolean`) | Bypass the importance gate — always admit to knowledge (e.g. sightings, trades). |
| `seedWorthy` (`boolean`) | Eligible to seed downstream generation (e.g. dialogue). Decoupled from `forceRemember` on purpose. |
| `selfWitnessed` (`boolean`) | Events with no single doer — every perceiver is an equal first-hand witness (the sightings). Defaults false; only sighting constants opt in. |

Representative constants: `BEHAVIOR_STARTED/COMPLETED/FAILED`, `SHEEP_SHEARED`, `SHEEP_DYED`,
`RESOURCE_HARVESTED`, `FARMLAND_CULTIVATED`, `TRADE_COMPLETED`, `COURTSHIP_COMPLETED`,
`COURTSHIP_REJECTED`, `TRADE_INVITE_SENT`, `COURTSHIP_INVITE_SENT`,
plus the `*_SIGHTED` sighting family (e.g. `ZOMBIE_SIGHTED`, `PLAYER_SIGHTED`) and `BELL_RUNG`.

## Consumer — PerceptionPipeline

**File:** `application/ai/perception/PerceptionPipeline.java` (`@ServerScope`). One `tick(villager,
runtimeState, gameTime)` call per throttled perception pass:

1. **Drain** the bus delta from the villager's `lastSeenSeq` cursor via `visitDelta`.
2. **Gate** each event through `PerceptionGate.admits(...)` — the anti-telepathy predicate:
    - Reject `SYSTEM`-namespace events outright.
    - Reject events whose chunk origin is farther than `MAX_PERCEPTION_CHUNK_RADIUS` (4 chunks / 64
      blocks) in chunk-Manhattan distance. Chunk coords come from the event envelope, so this is O(1)
      and needs no world lookup. (Line-of-sight/falloff is intentionally not yet modeled.)
3. **Convert** admitted events to `Observation`s via `ObservationFactory.fromEvent` and buffer them
   in the per-villager `ObservationBuffer`.
4. **Advance the cursor even if everything was filtered**, so rejected events are never re-read.
5. **Score and promote.** Drain the buffer and score each observation through `MemoryImportanceGate`
   (weighing profession, genetics, how many similar events co-occurred this pass, and whether the
   villager is the doer — own deeds get a salience bump). Qualifying observations — or any
   `forceRemember`/first-hand event — are promoted into the villager's `VillagerKnowledgeStore` as
   first-hand `KnowledgeEntry` records.

The hard anti-telepathy rule lives here: long-range knowledge is only possible via gossip, never by
direct bus admission.

## VillagerKnowledgeStore — episodic memory

**File:** `domain/ai/knowledge/VillagerKnowledgeStore.java` (pure domain, no Minecraft state,
fully unit-testable). Holds `KnowledgeEntry` records that are either directly observed (promoted by
`MemoryImportanceGate`) or received as hearsay during a gossip exchange. Bounded by a max-entry
setting (`EventLaneConfig.knowledgeStoreMaxEntries`, default 200) with insertion-order FIFO eviction
— knowledge decays naturally as new facts arrive. Independent corroboration of an existing fact
bumps its weight modestly (`CORROBORATION_BUMP`) rather than duplicating the entry.

Downstream, the store is read by the gossip lane (shareable entries are re-propagated to nearby
villagers) and by the LLM grounding path (episodic entries seed monologue and plan requests).

---

## The SocialCue lane

**File:** `application/ai/socialcue/SocialCueArbiter.java` (`@ServerScope`)

A separate tick-level lane (ticked at step 4 above) that presents ambient *social cues* — gaze,
gesture, and speech-bubble beats — without competing with plan or override execution for the body's
resource channels. On each `tick`:

1. **Dispatch** the active cue's script, firing any `CueStep`s whose start-tick has arrived (every
   tick).
2. **Admit** — if no cue is active, scan the cue catalog (`Set<SocialCueCatalogEntry>`) for the first
   eligible cue (throttled to ~1 Hz). A cue is admissible only when its `BehaviorChannel` set is
   **disjoint** from the currently running behavior's `requiredChannels` (a `null` descriptor means no
   behavior is running, so all channels are free). This is the channel-granular arbitration the binary
   `PLAN_BEHAVIOR_ACTIVE` flag deliberately does *not* do for the foreground/ambient boundary.

Cadence is charisma-shaped: base cooldowns from `EventLaneConfig` are scaled between low- and
high-CHARISMA multipliers (linear or exponential per config), jittered per cue, and floored by a
lane refractory gap (`LANE_REFRACTORY`, 12 s) between spontaneous bubbles. **Reactive** cues
(`bypassLaneRefractory`) opt out of the refractory because they answer something external. Fresh-load
crowds are spread by a random initial admission offset so they don't all act on the same tick.
Presentation goes through `SocialCuePresenter`.

Some cue triggers scan perceived entities and the knowledge store, which is why the arbiter is part
of the event lane rather than the plain ambient background.

---

## How it feeds the reactive override lane

The event lane is the producer side of the override lane documented in
[`behavior_orchestration.md`](behavior_orchestration.md#reactive-override-lane):

| Override policy | Reads from the event lane |
|-----------------|---------------------------|
| `SocialAcceptOverridePolicy` | the `TradeSessionRegistry` / `CourtshipSessionRegistry`, populated when a presenter emits `TRADE_INVITE_SENT` / `COURTSHIP_INVITE_SENT`. An open invite → `TRADE_ACCEPT` / `COURTSHIP_ACCEPT`. |

---

## Tuning

Since the Stage 0 SIS kill-switch, the event-lane knobs are split across two files by whether they
still matter when the switch is off.

### `inference.toml` — EventLaneConfig (cognition lane; only meaningful when the kill-switch is on)

**File:** `domain/ai/eventlane/EventLaneConfig.java` (`@BehaviorConfig(name = "event_lane", type = INFERENCE)`)

| Knob | Default | Governs |
|---|---|---|
| `worldEventTtlTicks` | 100 | Bus retention before eviction (~5 s). |
| `observationBufferCapacity` | 50 | Max observations buffered per villager per pass. |
| `knowledgeStoreMaxEntries` | 200 | Episodic knowledge entries retained before FIFO eviction. |
| `gossipMaxDistanceSquared` | 25 | Max squared block distance for gossip to be possible. |
| `gossipInitiateCooldownSeconds` / `gossipAcceptCooldownSeconds` / `gossipTargetCooldownSeconds` | 120 / 10 / 300 | Gossip initiation, accept, and per-receiver cooldowns. |

### `general.toml` — SocialCueConfig (always-on scripted cues; unaffected by the kill-switch)

**File:** `application/ai/socialcue/SocialCueConfig.java` (`@BehaviorConfig(name = "social_cue", type = GENERAL)`)

| Knob | Default | Governs |
|---|---|---|
| `villagerChatterCooldownSeconds` | 120 | Base ambient chatter cooldown (before CHA + jitter). |
| `socialCueLowCharismaCooldownMultiplier` / `...High...` | 4.0 / 0.5 | Cue cooldown multipliers at CHA=0.0 / CHA=1.0. |
| `socialCueCooldownJitterFraction` | 0.25 | Per-cue cooldown jitter half-width. |
| `socialCueCharismaCooldownScaling` | `exponential` | How CHA maps between the low/high multipliers (`linear` \| `exponential`). |

Because the entity constructor path is not Dagger-created, per-villager stores read these values
through the current server component with constant fallbacks during early bootstrap.

### Kill-switch gating

The whole event lane is cognition-lane work, gated by `InferenceGate.isEnabled()` (`inference.toml`
`enabled`, default `false`). When off, the lane is inert: `WorldEventEmitter` no-ops every emit,
`BaseVillager.tickPerception` early-returns before draining the bus, the sighting sensor and the two
gossip cues drop out of their multibindings (they live in `@CognitionScoped` sets that are merged into
the effective set only when the gate is on), and the bus reaper is left unregistered. Knowledge and
observation state is still constructed and its NBT still round-trips, so a world toggled off keeps its
data inert and resumes cleanly when toggled back on. The scripted social layer (`SocialCueConfig`) is
untouched — chatter, greets, and zombie-reaction cues keep firing.

---

## Explicit design decisions

| Decision | Choice | Rationale |
|---|---|---|
| Bus is fire-and-forget | Producers never block on consumers; `PlanRunner` works if the bus is never drained | Keeps emission cheap and the subsystem optional/additive; nothing in the core loop depends on a consumer existing. |
| Bus is transient, not memory | ~5 s TTL, head-trimmed | The bus is a reaction surface. Durable facts are promoted into per-villager knowledge stores before eviction; the log never grows unbounded. |
| Per-consumer cursors | `lastSeenSeq` per villager | Each villager reads only its own delta; no shared read position, no re-scan of the whole log. |
| Anti-telepathy at the gate | `PerceptionGate`: SYSTEM reject + 64-block chunk-Manhattan reject | A villager only learns first-hand what it could plausibly perceive; long-range spread must go through gossip, which is socially costed. |
| Event metadata co-located on the enum | `WorldEventType` carries `observationType` / importance / flags | Single source of truth; downstream perception, inference, and compaction read fields instead of parallel switch statements that drift. |
| `forceRemember` decoupled from `seedWorthy` | Two independent flags | Sightings must always be remembered without necessarily seeding dialogue, and vice versa. |
| Knowledge store is pure domain | No Minecraft state, FIFO-bounded | Fully unit-testable without mocks (per project rule: Minecraft objects are not mockable); bounded size prevents unbounded growth. |
| SocialCue uses channel arbitration | Cue channels must be disjoint from the running behavior's `requiredChannels` | Lets ambient social presentation run *alongside* work that doesn't claim those channels, which the binary `PLAN_BEHAVIOR_ACTIVE` flag can't express. |
| Perception is throttled | `PERCEPTION_COOLDOWN_TICKS` | The drain is a catch-up cursor; a coarser cadence batches the delta instead of dropping events, saving per-tick cost on quiet villagers. |

---

## Out of scope for this doc

The following subsystems consume or extend the event lane but are large enough (and, in the case of
LLM dialogue, still moving) to warrant their own treatment. They are listed here as seams so the
boundary is explicit; expand into dedicated docs as they stabilize.

- **Gossip exchange** — `application/ai/gossip/` (`GossipSessionRegistry`, `GossipSession`,
  `GossipPhase`, `GossipInvite`) and `domain/ai/knowledge/` (`GossipWeightCalculator`,
  `KnowledgeEntry` corroboration). The villager-to-villager hearsay path that carries knowledge
  beyond direct perception range. Cadence knobs live in `EventLaneConfig`.
- **LLM dialogue & monologue** — `application/ai/dialogue/` (`DialogueProvider` and the
  packs/live/off providers, `DialogueRequestQueue`, `PromptAssembler`) and the monologue path into
  the **Settlements Inference Service**. `seedWorthy` events feed generation. This is the newest and
  least settled area (SIS integration is recent); treat the code as the source of truth.

---

## Related docs

- [`behavior_orchestration.md`](behavior_orchestration.md) — the plan/override/ambient lanes that
  consume this subsystem's output.
- [`behavior_system.md`](behavior_system.md) — behavior catalog, pool resolution, and plan generation.
