# Bubble System

This document covers the villager speech-bubble framework: how messages are produced on the server, replicated
to clients, and rendered above villagers. For the architectural motivation behind the current design, see
[bubble_refactor_roadmap.md](../working/bubble_refactor_roadmap.md).

---

## Overview

Speech bubbles are short-lived UI artifacts that float above a villager to surface what the villager is doing or
saying (trading, shearing sheep, reacting to the player, etc.). The framework has four layers:

1. **Production** — application-layer code (behaviors, presenters) builds a `BubbleMessage` describing what to
   show, then tells the authoritative `VillagerBubbleService` to upsert/push/remove it on a channel.
2. **State + replication** — `VillagerBubbleService` mutates the villager's `VillagerBubbleState` according to
   the channel's `ChannelPolicy`, then publishes a snapshot packet to tracking clients.
3. **Client reconciliation** — `BubbleManager` applies the snapshot diff, creating new `SpeechBubble` instances
   for new entries and expiring vanished ones.
4. **Rendering** — each `SpeechBubble` walks its segment list through `SegmentRenderer` and draws the composite
   inside a nine-slice boundary above the entity.

```
behavior/presenter            VillagerBubbleService         network snapshot         BubbleManager (client)
┌───────────────────┐         ┌───────────────────┐         ┌──────────────┐         ┌──────────────────┐
│ builds a          │         │ applyCommand      │         │ ClientBound  │         │ applySnapshot    │
│ BubbleMessage     │──upsert→│ mutates state,    │──pub────│ BubbleSnap-  │────────→│ diffs by UUID,   │
│ with segments     │         │ prunes expired,   │         │ shotPacket   │         │ builds bubbles   │
└───────────────────┘         │ publishes snapshot│         └──────────────┘         └──────────────────┘
                              └───────────────────┘                                         │
                                                                                            ▼
                                                                              SegmentComposedSpeechBubble
                                                                              → SegmentRenderer → canvas
```

The design keeps **application code purely semantic** — it describes content, never pixels. Rendering decisions
(icon sizes, gap widths, `×N` labels) live only on the client.

---

## Key concepts

### BubbleMessage

**File:** `application/ui/bubble/BubbleMessage.java`

Immutable payload describing one bubble:

```
BubbleMessage.builder()
    .priority(10)                // tie-break within a channel (higher wins)
    .ttl(Ticks.seconds(2))       // clamped by channel policy
    .sourceType("trade")         // stable identity string for logs/filtering
    .segments(List.of(...))      // content, at least one segment
    .build();
```

`sourceType` is a free-form string — use a short, stable identifier per producing subsystem (`"trade"`,
`"behavior"`, `"chat"`, etc.). It's the only identity the framework carries beyond `BubbleChannel`; there is
no enum of bubble types.

### BubbleChannel and ChannelPolicy

**Files:** `application/ui/bubble/BubbleChannel.java`, `application/ui/bubble/ChannelPolicy.java`

Each bubble lives on one of three channels. Channels dictate capacity and render order, not rendering shape:

| Channel    | `maxActive` | Overflow policy    | TTL cap | Typical producers            |
|------------|-------------|--------------------|---------|------------------------------|
| `BEHAVIOR` | 1           | `REPLACE_EXISTING` | 30s     | AI behaviors, trade sessions |
| `CHAT`     | 3           | `DROP_OLDEST`      | 20s     | Dialogue, reactions          |
| `SYSTEM`   | 2           | `DROP_OLDEST`      | 10s     | Debug/test bubbles, notices  |

Policies are registered
in [VillagerBubbleService.java](../../src/main/java/dev/breezes/settlements/application/ui/bubble/VillagerBubbleService.java)
constructor. The comparator used by the client (`BubbleManager`) orders visible bubbles by
`(channel.ordinal, -priority, createdGameTime, sequenceNumber)` and stacks them vertically.

### BubbleCommand

**File:** `application/ui/bubble/BubbleCommand.java`

Sealed interface describing mutations. Producers never touch `VillagerBubbleState` directly.

| Command         | When to use                                                                       |
|-----------------|-----------------------------------------------------------------------------------|
| `Upsert`        | Same logical bubble updated over time (keyed by `ownerKey`). Ideal for behaviors. |
| `Push`          | New one-off bubble, no dedupe key. Ideal for reactions or notifications.          |
| `RemoveById`    | Delete a specific bubble by its UUID.                                             |
| `RemoveByOwner` | Delete the bubble for a given `(channel, ownerKey)` pair. Paired with `Upsert`.   |
| `ClearChannel`  | Wipe the entire channel — rarely needed, mostly for tests.                        |

**`ownerKey` is required for `Upsert` and `RemoveByOwner` and must be non-blank.** Choose a stable string that
uniquely identifies the logical bubble within its channel for that villager — e.g. `"trade-session-" + sessionId`,
`"shear-sheep"`. If the behavior fires again with the same key, the existing bubble is replaced in place.

### BubbleSegment (the content vocabulary)

**File:** `application/ui/bubble/BubbleSegment.java`

A sealed interface with three record variants. These are the primitives from which every bubble is composed.

```
sealed interface BubbleSegment permits Item, Text, Sprite {

    record Item(ResourceLocation itemId, int count)                        // vanilla item icon
    record Text(String literal, ChatFormatting color, boolean bold, float scale)
    record Sprite(SpriteRef sprite, Ticks frameDuration)                   // mod-owned textured sprite
}
```

The segment list is composed horizontally (left to right, centered) by `BubbleHorizontalCompositeElement`. You
can mix and match segments freely:

```
// [wheat ×4] [emerald ×3] [▼]
List.of(
    BubbleSegment.Item.builder().itemId(wheatId).count(4).build(),
    BubbleSegment.Item.builder().itemId(emeraldId).count(3).build(),
    TradeMarker.DOWN.asSegment()
)
```

**Per-variant rules:**

- `Item` — `count >= 0`. `count == 0` renders the icon alone; `count > 0` renders `[icon][×N]` where the label
  is drawn by the client at 0.7 scale in bold black. Use `BubbleSegment.Item.iconOnly(id)` for the icon-only
  case.
- `Text` — non-blank literal, `scale > 0`. Translation keys and click/hover events are *not* supported;
  upgrade the codec to serialized `Component` if you ever need them.
- `Sprite` — `frameDuration` controls animation cadence. For single-frame textures, `Ticks.ZERO` disables
  advancement.

---

## Producing a bubble (server side)

### Short path — from a behavior step

```
ISettlementsVillager villager = context.getInitiator();

BubbleMessage message = BubbleMessage.builder()
    .priority(0)
    .ttl(Ticks.seconds(5))
    .sourceType("behavior")
    .segments(List.of(
        BubbleSegment.Sprite.builder()
            .sprite(SpriteRef.SHEARS)
            .frameDuration(Ticks.seconds(0.5))
            .build(),
        BubbleSegment.Sprite.builder()
            .sprite(SpriteRef.SHEEP)
            .frameDuration(Ticks.seconds(0.6))
            .build()))
    .build();

villager.upsertBubble(BubbleChannel.BEHAVIOR, "shear-sheep", message);
```

`upsertBubble` is defined on `ISettlementsVillager` and wraps a `BubbleCommand.Upsert` — the facade behaviors
typically use. The matching `removeBubbleByOwner(channel, ownerKey)` exists for explicit teardown, but most
producers rely on TTL expiry instead.

### Explicit command path — from an application service

```
// TradeSessionPresenter.java — stateful multi-step bubble over a trade session
this.villagerBubbleService.applyCommand(
    self,
    new BubbleCommand.Upsert(
        BubbleChannel.BEHAVIOR,
        "trade-session-" + session.getSessionId(),
        message),
    self.level().getGameTime());
```

Use `VillagerBubbleService.applyCommand` directly when the producer is an `@Inject`ed service (presenter,
coordinator) rather than a per-step behavior closure. `VillagerBubbleService` is `@ServerScope`.

### Server-only

All production happens on the authoritative server. Never build or dispatch a `BubbleCommand` from a
`@ClientSide` path — the server's state is the source of truth and replication is one-way.

---

## Rendering (client side)

### Pipeline

`BubbleManager.applySnapshot(entries, gameTime)` ([BubbleManager.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/bubbles/BubbleManager.java))
diffs incoming snapshot entries against local state:

- Entries present locally but missing in the snapshot → `setExpired()` and dropped.
- Entries in the snapshot but not local → a new `SegmentComposedSpeechBubble` is constructed and kept.
- Entries present on both sides → metadata refreshed, rendered bubble instance kept (preserves per-bubble
  animation state).

### SegmentComposedSpeechBubble

**File:** `infrastructure/rendering/bubbles/registry/SegmentComposedSpeechBubble.java`

The **only** `SpeechBubble` implementation for replicated bubbles. It takes a segment list, maps each segment
to a `BubbleInnerElement` via `SegmentRenderer.toElement`, and wraps the composite in a
`BubbleBoundaryElement` (the nine-slice shell).

### SegmentRenderer

**File:** `infrastructure/rendering/bubbles/registry/SegmentRenderer.java`

Pure pattern-matched dispatch:

| Segment         | Rendered as                                                        |
|-----------------|--------------------------------------------------------------------|
| `Item(id, 0)`   | `BubbleItemStackElement` (icon only)                               |
| `Item(id, n>0)` | Horizontal composite of icon + `×N` label (0.7 scale, bold, black) |
| `Text`          | `BubbleTextElement` with style/bold/scale applied                  |
| `Sprite`        | `AnimatedSpriteElement` resolving its texture via `SpriteCatalog`  |

Unknown item IDs fall back to `Items.BARRIER` with an error log rather than crashing the render pass.

### Canvas primitives

**Package:** `infrastructure/rendering/bubbles/canvas`

| Class                              | Purpose                                                                      |
|------------------------------------|------------------------------------------------------------------------------|
| `BubbleInnerElement` (interface)   | Contract: `render`, `getBoundingBox`, `adjustPoseStack`, `preRender`, `copy` |
| `BubbleBoundaryElement`            | Nine-slice bubble shell; accepts one `innerElement` and draws the border     |
| `BubbleHorizontalCompositeElement` | Horizontal layout container with configurable gap                            |
| `BubbleItemStackElement`           | Renders a vanilla `ItemStack` via `ItemRenderer`                             |
| `BubbleTextElement`                | Renders a `Component` with scale and opacity                                 |
| `AnimatedSpriteElement`            | Single-texture animated sprite, per-instance frame clock                     |

**`copy()` contract:** every `BubbleInnerElement` must return a fresh, independent instance. The contract
exists because animated elements hold per-instance `AtomicInteger` frame counters — two concurrent bubbles
showing the same sprite must not share a clock. Respect this when writing new elements.

---

## Adding a new sprite

1. Add the texture asset at `src/main/resources/assets/settlements/textures/bubble/<name>.png`. Sprites are
   horizontal strips — width = `frameCount × frameWidth`. A single-frame static sprite uses `frameCount=1`.
2. Add an enum entry
   in [SpriteRef.java](../../src/main/java/dev/breezes/settlements/application/ui/bubble/SpriteRef.java).
3. Add a binding
   in [SpriteCatalog.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/bubbles/texture/SpriteCatalog.java):
   ```
   SpriteRef.FOO, AnimatedFrameTexture.builder()
       .id("bubble_foo")
       .texturePath("textures/bubble/anim_bubble_foo.png")
       .width(64).height(32).frameWidth(32).frameCount(2).build()
   ```
4. Use it: `BubbleSegment.Sprite.builder().sprite(SpriteRef.FOO).frameDuration(Ticks.seconds(0.4)).build()`.

The enum is closed on purpose — the mod owns every sprite asset and exhaustive compile-time checking is
worth more than dynamic extensibility today. The wire codec encodes `SpriteRef` by name, so if this ever
migrates to a string-keyed registry for addon contribution, the network protocol stays stable.

---

## Adding a new bubble flow

The framework is fully data-driven now — **you do not add new enum values or new renderer classes to introduce
a new bubble**. The full recipe:

1. **Choose a `sourceType` string** — short, stable, unique per producing subsystem. Used for logs and any
   future filtering.
2. **Choose a channel** — `BEHAVIOR` for AI-driven, `CHAT` for dialogue, `SYSTEM` for notices.
3. **Choose an `ownerKey` convention** (if using `Upsert`) — stable across updates for the same logical
   bubble, e.g. `"trade-session-<id>"`.
4. **Assemble a segment list** describing content compositionally. Reuse `Item` / `Text` / `Sprite` as-is. If
   the bubble is marker-like, call `TradeMarker.<NAME>.asSegment()` or define a domain enum exposing an
   `asSegment()` factory.
5. **Dispatch via `villager.upsertBubble(...)` or `VillagerBubbleService.applyCommand(...)`**.

Examples currently in the codebase:

- [ShearSheepBehaviorV2.java](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/animals/ShearSheepBehaviorV2.java) —
  two animated `Sprite` segments, `sourceType="behavior"`, `ownerKey="shear-sheep"`.
- [TradeSessionPresenter.java](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/trading/TradeSessionPresenter.java) —
  two `Item` segments + optional `TradeMarker`, `sourceType="trade"`,
  `ownerKey="trade-session-<uuid>"`.

---

## Wire format and caps

**Codec:
** [BubbleEntrySnapshotCodec.java](../../src/main/java/dev/breezes/settlements/infrastructure/network/features/ui/bubble/codec/BubbleEntrySnapshotCodec.java)

Segments ride on `ClientBoundBubbleSnapshotPacket`. The segment codec is a tagged union with a discriminator
byte per variant. `SpriteRef` is encoded by name (not ordinal) to stay resilient across enum reordering.

**Protocol caps (enforced by the codec):**

- Segments per bubble: **≥ 1**, **≤ 8**.
- Text literal length: **≤ 128** chars.
- `sourceType` length: **≤ 256** chars.

Malformed payloads throw `IllegalArgumentException` at read time and drop the packet — misbehaving producers
are surfaced, not silently accepted.

---

## Testing

- **Unit tests** — segment codec round-trip tests live alongside the codec. When adding a new segment variant,
  mirror the existing per-variant test coverage: arrange an instance, round-trip through
  `write`→`read`, assert equality.
- **Segment renderer tests** — assert each variant returns the expected `BubbleInnerElement` subtype and that
  `Item(count == 0)` vs. `Item(count > 0)` produces different composites.
- **Manual** — the `/test bubble` command family (see `TestCommand`) posts segment-based bubbles on demand
  for visual verification in-world. Use this to eyeball new sprites, item icons, and marker glyphs.

`VillagerBubbleService` is `@ServerScope` and constructor-injected; tests can instantiate it directly with no
mocks for pure state transitions. For end-to-end flow, stub `ISettlementsVillager` with a real
`VillagerBubbleState`.
