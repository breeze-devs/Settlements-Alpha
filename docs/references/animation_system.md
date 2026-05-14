# Animation System v1

This document covers how villager animations are authored, resolved per-villager, and applied during rendering.
For the Dagger fundamentals that underpin the wiring, see [Dagger Guide](dagger_guide.md).
For server-side behavior wiring (which calls into the motion plane), see [Behavior System](behavior_system.md).

---

## Overview

The system has three orthogonal planes. Each plane is owned by a different layer, lives on a different lifetime, and never encodes another:

| Plane | What it describes | Authority | Lifetime |
|---|---|---|---|
| **Motion** | What motion the villager is currently *expressing* (e.g. `SWING_HEAVY`) | Server intent, client picks animation | Transient — changes per behavior phase |
| **Loadout** | What items / cosmetics / props the villager *carries* and where | Server | Persistent |
| **Presentation** | How a given (slot, item-category) pair *renders* visually | Client config | Static — registry lookup |

The render layer is the single point where the three meet. **No plane encodes another:** an animation never names an item, a slot never names a behavior, a presentation profile never names a state. This is what keeps the surface area small as new tools, cosmetics, and motions land.

A second guiding rule: **the renderer iterates attachments — it does not special-case "the main hand."** Every visible thing on the villager (held tool, future belt axe, future hat) is an [RenderableAttachment](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java) with an anchor, content, and a category. The render path is uniform.

---

## End-to-end flow

A single frame of villager rendering walks all three planes:

1. **Behavior sets motion intent** on the server. [ButcherLivestockBehavior](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/animals/butchering/ButcherLivestockBehavior.java:100) calls `villager.setMotion(AnimationArchetype.SWING_HEAVY)`.
2. **The motion byte syncs.** [BaseVillager](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:99) declares `DATA_MOTION_ARCHETYPE`, a `SynchedEntityData<Byte>` that vanilla broadcasts on change.
3. **The renderer polls.** [SettlementsVillagerRenderer.render](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:58) reads `villager.getMotion()` and compares to the animator's last-seen archetype.
4. **The animator transitions** if the archetype changed. [VillagerAnimator.onArchetypeChanged](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:30) calls the resolver, moves the previous animation to "outgoing" for crossfade-out, and starts the new animation.
5. **The resolver picks an animation.** [DefaultAnimationResolver.resolve](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java:14) looks up `(archetype, mainHandCategory)` in the [AnimationLibrary](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLibrary.java) with a fallback chain (exact → archetype × `GENERIC` → idle).
6. **The animator samples a frame.** [VillagerAnimator.sample](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:79) returns an [AnimationFrame](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java), blending the outgoing animation into the current one if a crossfade is in progress.
7. **The frame is stashed** on the renderer and the model. The model reads ModelPart-flavored targets in `setupAnim` ([VanillaVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/VanillaVillagerModel.java:165)); the attachment layer reads slot-flavored targets ([AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:76)).

The animator and the behavior tick at the same rate against the same `setMotion(...)` start point, so timing constants live in the animation file (e.g. `ButcheringAnimations.SWING_IMPACT_TICK`) and are imported directly by the server behavior when it needs to coordinate damage windows or sounds. This is a coordination contract, not duplication — there is no event bus between the two.

---

## Motion plane

Motion is named in **archetypal** terms, not items or tasks. A butcher swinging an axe and a soldier swinging a mace both use `SWING_HEAVY`. The item taxonomy (handled by the loadout plane) is what disambiguates which animation gets played for that archetype.

### `AnimationArchetype`

**File:** [AnimationArchetype.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java)

A small enum of motion intents: `IDLE`, `HOLD_TOOL_VERTICAL`, `HOLD_TOOL_HORIZONTAL`, `SWING_HEAVY`, `SWING_LIGHT`, `STAB`, `CAST`, `REEL_IN`, `REEL_OUT`, `TILL_DOWN`, `TILL_UP`, `POINT`, `CELEBRATE`. Provides `toNetworkByte()` / `fromNetworkByte()` with safe fallback to `IDLE` on unknown values.

Encoded as `byte` because the enum will not exceed 256 values for the foreseeable future, and one byte through `SynchedEntityData` is the cheapest possible sync surface.

### Sync API on the villager

**File:** [BaseVillager.java:99-160](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:99)

- `DATA_MOTION_ARCHETYPE` — synced byte declared via `SyncedDataWrapper`.
- `setMotion(AnimationArchetype)` — server-side write.
- `getMotion()` — read back (client during render, server during behavior tick).

Behaviors call `setMotion(...)` at phase transitions. The default is `IDLE`. Behaviors should restore `IDLE` on completion or interruption, otherwise the last motion sticks until the next transition.

### Change detection is a poll, not an event

The renderer reads the synced byte each frame and compares to the animator's `lastSeenArchetype`. This avoids event-subscription lifecycle issues during world load and the cost is one cached byte read per villager. State changes that arrive before the equipment sync (see [tickContext lag fix](#crossfade-and-the-equipment-sync-lag)) are resolved on the next frame without re-entering the archetype-change path.

---

## Loadout plane

The loadout is the source of truth for what the villager is carrying. Today only `MAIN_HAND` and `OFF_HAND` are wired; the design carries enough naming to add belt / back / cosmetic slots without renderer or animation refactors.

### `AttachmentSlot` and `EquipmentSlot`

**Files:** [AttachmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentSlot.java), [EquipmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/inventory/EquipmentSlot.java)

`AttachmentSlot` is the supertype the renderer indexes by. `EquipmentSlot` is the v1 implementation with `MAIN_HAND` and `OFF_HAND`. Future cosmetic slot enums can be added as parallel implementations of `AttachmentSlot` without touching the renderer.

### `RenderableAttachment` and `AttachmentContent`

**Files:** [RenderableAttachment.java](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java), [AttachmentContent.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentContent.java)

A `RenderableAttachment` is a `(slot, content, category)` triple. `AttachmentContent` is a sealed hierarchy:
- `ItemContent(ItemStack)` — rendered via `ItemInHandRenderer`. The only path implemented in v1.
- `ModelContent` — placeholder for future custom models (hats, balloons).
- `BillboardContent` — placeholder for future textured-quad props.

### `AttachmentProvider` — Dagger multibinding

**File:** [AttachmentProvider.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentProvider.java)

Providers contribute `RenderableAttachment` lists into the render path. Bound `@IntoSet` in [ClientAttachmentModule:21](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java:21). The layer sorts them by `renderOrder()` before iteration so layering between providers is deterministic.

V1 has exactly one: [EquipmentAttachmentProvider](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/attachment/EquipmentAttachmentProvider.java), which reads `IVillagerEquipment` and emits one `ItemContent` per occupied slot.

### `ItemCategory` — taxonomy for held items

**File:** [ItemCategory.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java)

An open enum (`AXE`, `SWORD`, `MACE`, `PICKAXE`, `SHOVEL`, `HOE`, `FISHING_ROD`, `TORCH`, `LANTERN`, `SPYGLASS`, `GENERIC`) with a single `of(ItemStack)` classifier centralizing all item-type-to-category dispatch. This is the only place in the codebase that branches on concrete item classes; render and animation code never do.

Category-based dispatch is what makes "swinging an iron axe ≈ swinging a diamond axe" automatic: both resolve to `(SWING_HEAVY, AXE)`. Material is irrelevant.

---

## Presentation plane

The presentation plane converts a `(slot, category)` pair into a sequence of transforms ready to feed to the `PoseStack`. Three pieces of data feed it: the **socket** (where the slot lives on the rig), the **display profile** (how this category-of-item sits in this slot), and the **frame** (per-frame animation deltas).

### `Socket` — bone + local transform

**File:** [Socket.java](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java)

A socket is an attach point on the rig. It carries `id`, `bone` ([ModelPartRef](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java)), `localTranslation`, `localRotation`, `localScale`, and `inheritsBoneTransform` (default `true`).

This is the same pattern Unity ships as **sockets**, Unreal as **skeletal sockets**, and Source as **`$attachment`**. Identical shape: `(bone, local-translation, local-rotation)`. When `inheritsBoneTransform` is true, the socket's world transform follows whatever the bone is doing in the current frame (rotated arms, walking bob, etc.). When false, the socket is a free-floating world anchor.

The reason this concept exists in addition to `SlotAnchor` is decoupling: a future split-arm model can move `MAIN_HAND` from `CROSSED_ARMS_CENTER` to a per-hand socket without touching any animation or slot consumer. Animations are written against archetypes and slots, never against bones directly.

### `SocketId` — named attach points

**File:** [SocketId.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java)

Enum, not `ResourceLocation`, for compile-time exhaustiveness — same rationale as `AttachmentSlot`. One value today: `CROSSED_ARMS_CENTER` (the hand-meeting point on the vanilla crossed-arms silhouette).

### `SocketRegistry` and `SlotAnchor`

**Files:** [SocketRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketRegistry.java), [InMemorySocketRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java), [SlotAnchor.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java), [InMemorySlotAnchorRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java)

`SocketRegistry.get(SocketId)` returns the registered `Socket`, or `Socket.identity(id)` (root-anchored, no inheritance) as a safe fallback for unknown ids.

`SlotAnchor` is the per-slot binding to a socket plus slot-only render concerns: `socket: SocketId`, `defaultDisplayContext: ItemDisplayContext`. That's it. The plane-of-attach lives entirely on `Socket`; the slot only chooses which socket to ride.

The v1 defaults bind both `MAIN_HAND` and `OFF_HAND` to `CROSSED_ARMS_CENTER`. This is acceptable because the crossed-arms model cannot visually distinguish the hands; once a split-arm model lands, the two slots can be re-bound to per-hand sockets as data, not code.

### `AttachmentDisplayProfile` — per `(slot, category)` transform

**Files:** [AttachmentDisplayProfile.java](../../src/main/java/dev/breezes/settlements/domain/presentation/AttachmentDisplayProfile.java), [InMemoryAttachmentDisplayProfileRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemoryAttachmentDisplayProfileRegistry.java)

Different items sit differently in the same slot. The display profile lets a designer tune just the deltas that matter:

- Lookup order: exact `(slot, category)` → `(slot, GENERIC)` → identity profile.

This means a single `(slot, GENERIC)` entry covers every uncategorized item, and specific entries override it as you author them.

---

## Animation primitives

The pure-domain animation core lives under [domain/animation](../../src/main/java/dev/breezes/settlements/domain/animation/) and has no Minecraft renderer coupling — every type here is unit-testable. The tests in [src/test/.../domain/animation](../../src/test/java/dev/breezes/settlements/domain/animation/) cover sampling, easing, blending, loop modes, and library fallback.

### `AnimationTarget<V>`

**File:** [AnimationTarget.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java)

A typed handle for one addressable animated property. Carries:

- `id: String` — globally unique. Equality and hashing use this id only, so separately-resolved handles to the same target compare equal.
- `valueType: Class<V>` — used to validate values at pose construction.
- `neutralValue: V` — the "safe harbor" used during crossfade when one side of the blend doesn't define the target. Colocated with the target definition so a designer cannot forget it.
- `interpolator: Interpolator<V>` — the type-specific lerp.
- `policy: AnimationTargetPolicy` — *intent metadata*, see below.

### Target catalogs

Two static factories own every target in the system:

**[AnimationTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java)** — model-part targets:

| Target | Value type | Neutral | Intent |
|---|---|---|---|
| `ARMS_ROTATION` | `Vector3f` | zero | Additive on top of vanilla arms pose |
| `ARMS_TRANSLATION` | `Vec3` | zero | Additive |
| `HEAD_ROTATION_OVERRIDE` | `Vector3f` | zero | Absolute — replaces vanilla head tracking |
| `BODY_ROTATION` | `Vector3f` | zero | Additive |

**[SlotTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java)** — per-slot targets, interned via `ConcurrentHashMap`:

| Factory | Value type | Neutral | Intent |
|---|---|---|---|
| `translation(slot)` | `Vec3` | zero | Layered onto PoseStack |
| `rotation(slot)` | `Vector3f` | zero | Layered onto PoseStack |
| `scale(slot)` | `Float` | `1.0` | Multiplicative |
| `visibility(slot)` | `Boolean` | `true` | Absolute — early-return skip |
| `displayContext(slot)` | `ItemDisplayContext` | `NONE` | Absolute, falls back to profile/anchor |

Interning matters: `SlotTargets.translation(MAIN_HAND)` returns the same instance on every call, so frame map lookups stay flat.

### The policy convention — intent metadata, not enforcement

**Important:** [AnimationTargetPolicy](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargetPolicy.java) (`ADDITIVE`, `MULTIPLICATIVE`, `ABSOLUTE`) is declared on every target but **not consulted at runtime**. The application style is hardcoded at the consumption sites:

- ModelPart targets in [VanillaVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/VanillaVillagerModel.java:165): `applyRotation` does `+=`, `HEAD_ROTATION_OVERRIDE` does `=` inside a `frame.has(...)` guard.
- Slot targets in [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:76): translation/rotation/scale layer onto the `PoseStack`, visibility early-returns, displayContext falls back through `displayContextFor(...)`.

When adding a new target, set the policy enum to match the consumer behavior, but understand that the renderer will do whatever it's coded to do — the enum is a contract for the next reader, not a runtime dispatcher.

### `Keyframe`, `AnimationTrack`, `KeyframeAnimation`

**Files:** [Keyframe.java](../../src/main/java/dev/breezes/settlements/domain/animation/Keyframe.java), [AnimationTrack.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTrack.java), [KeyframeAnimation.java](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java)

A `Keyframe<V>` is a tick offset, a value, and the easing curve to the *next* keyframe. An `AnimationTrack<V>` is one target plus a sorted list of keyframes plus the interpolator. A `KeyframeAnimation` is a `ResourceLocation` id, a duration, a `LoopMode`, blend-in/out durations, and a list of tracks.

`KeyframeAnimation.sample(elapsedTicks)` resolves the elapsed-ticks against the loop mode ([LoopMode.java](../../src/main/java/dev/breezes/settlements/domain/animation/LoopMode.java) — `ONCE`, `LOOP`, `PING_PONG`, `HOLD_LAST`), samples each track, and returns an `AnimationFrame`.

The animation id is a `ResourceLocation` rather than a string — keeps the door open for datapack-loaded animations and animation-bound sounds/particles.

### `AnimationFrame`

**File:** [AnimationFrame.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java)

Immutable map of `AnimationTarget<?> → Object`. `frame.get(target)` returns the target's neutral value if absent, or a typed value otherwise. `frame.has(target)` lets consumers distinguish "explicitly absent" from "explicitly neutral" — used by `HEAD_ROTATION_OVERRIDE` so a track present but holding the neutral still overrides vanilla, and a missing track does not.

`frame.blendTo(other, t)` produces a blended frame for crossfade. Each target's `from`/`to` defaults to its `neutralValue` when missing, so partial animations cannot leave stale poses behind. This is the "safe harbor" pattern: the neutral is colocated with the target, the blend uses it automatically, designers do not have to think about it.

### `Easing` and `Interpolator`

**Files:** [Easing.java](../../src/main/java/dev/breezes/settlements/domain/animation/Easing.java), [Interpolator.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolator.java), [Interpolators.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolators.java)

`Easing` is a `float → float` shape applied to normalized progress between two keyframes (`LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`, `STEP`, `CUBIC`). `Interpolator<V>` is the type-specific lerp. Separation means a `Vec3` track and a `Float` track can both use `EASE_IN_OUT` without duplicating curve code.

Standard interpolators in `Interpolators`: `VEC3`, `VECTOR3F`, `FLOAT`, `BOOLEAN_STEP`, `ITEM_DISPLAY_CONTEXT_STEP`. Boolean and display-context targets use step interpolators (no notion of "halfway between two contexts") and pair with `ABSOLUTE` policy.

Quaternion-based rotation interpolation is not implemented in v1. The current animation set stays under 180° single-axis pitch where Euler `lerp` is correct. Add a parallel `Quaternion` target + slerp interpolator when a motion wraps past 180° or combines three axes near gimbal angles.

---

## Authoring animations with poses

Direct track authoring is supported but discouraged. The pose-driven builder is the primary API — same `KeyframeAnimation` output, much less typing, and the pose pattern composes (`pose.with(other)`).

### `Pose`

**File:** [Pose.java](../../src/main/java/dev/breezes/settlements/domain/animation/Pose.java)

A `Pose` is a **snapshot in space** — an immutable `AnimationTarget<?> → value` map describing one configuration of bones and slot transforms. It is not a path in time. Combine poses on a timeline with the builder.

Construction: `Pose.of(target, value)` for a single-target pose, `pose.with(target, value)` to add or override one target, `pose.with(otherPose)` to merge two poses (later overlay wins on collision). Construction validates value types against `target.getValueType()`, so a type mismatch fails at pose creation, not at sample time.

Pose factories are per-behavior `*Poses.java` classes, not a global enum. [ButcheringPoses.java](../../src/main/java/dev/breezes/settlements/domain/animation/ButcheringPoses.java) is the canonical example: three named `public static final Pose` constants (`ARMS_REST`, `ARMS_RAISED`, `ARMS_IMPACT`) built from a private `arms(pitchDegrees)` factory. This keeps related poses colocated and named, the same way `BehaviorRegistration` factories cluster in `BehaviorModule`.

### `PoseAnimationBuilder`

**Files:** [PoseAnimationBuilder.java](../../src/main/java/dev/breezes/settlements/domain/animation/PoseAnimationBuilder.java), [KeyframeAnimation.fromPoses()](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:22)

Entry point: `KeyframeAnimation.fromPoses()` returns a `PoseAnimationBuilder`. Chain `.id(...)`, `.durationTicks(...)`, `.loopMode(...)`, `.blendInTicks(...)`, `.blendOutTicks(...)` like the underlying builder, then add timeline entries with `.at(tick, pose, easingToNext)`.

Under the hood, the builder expands poses into independent per-target tracks: each target gets a sorted track containing one keyframe per pose that defines it. **Targets in a pose are keyed independently** — if an intermediate pose omits a target, that target interpolates directly between the surrounding poses that *do* include it. This is what makes sparse poses work: you do not have to repeat every target on every keyframe.

Varying segment speeds emerge from **tick density × easing curve**, not from a pose property. A long tick gap with `EASE_IN` is slow; a short gap with `EASE_OUT` is fast. Fishing's "slow wind-up, fast release" is two `.at(...)` calls timed appropriately against the same pose set.

### Track override escape hatch

`PoseAnimationBuilder.track(AnimationTrack<?>)` accepts hand-authored tracks alongside pose-driven `.at(...)` calls. If a manual track targets the same property as a pose, the manual track wins (the pose contribution for that target is dropped). Use this when one target needs a curve no clean pose set captures — a small recoil bounce mid-swing, an item-slip on impact. Poses for the bulk, tracks for the rough edges, same `KeyframeAnimation` underneath.

### The canonical example

[ButcheringAnimations.swingHeavyAxe()](../../src/main/java/dev/breezes/settlements/domain/animation/ButcheringAnimations.java) is the working example: 30-tick `ONCE` animation, one-tick blend-in, three-tick blend-out, four pose keyframes through rest → raised → impact → rest. One animation target (`ARMS_ROTATION`), no slot-target tracks. The held axe follows the arm because `MAIN_HAND` is bound to `CROSSED_ARMS_CENTER` which rides the `ARMS` bone — no hand-coordinated compensation needed.

---

## Resolution

A villager's current animation is chosen by `(archetype, item-category)`, with fallback to archetype-default to keep the system graceful when an unknown item is picked up.

### `AnimationSelectionContext`

**File:** [AnimationSelectionContext.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationSelectionContext.java)

Record carrying `mainHandCategory: ItemCategory`. This is the resolver's input alongside the archetype. The shape is a record so future fields (off-hand category, time of day, mood) can be added without touching call sites.

### `AnimationKey` and `AnimationLibrary`

**Files:** [AnimationKey.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationKey.java), [AnimationLibrary.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLibrary.java), [InMemoryAnimationLibrary.java](../../src/main/java/dev/breezes/settlements/domain/animation/InMemoryAnimationLibrary.java)

`AnimationKey` is a `(archetype, category)` record. `AnimationLibrary.resolve(archetype, category)` returns the chosen `KeyframeAnimation` with a three-tier fallback chain in `InMemoryAnimationLibrary`:

1. Exact `(archetype, category)` — most specific.
2. `(archetype, GENERIC)` — archetype-default for unknown items.
3. `(IDLE, GENERIC)` — last resort. If absent, returns a built-in empty `idle` animation (zero duration, no tracks).

The chain means three things in practice:
- "Swinging an iron axe ≈ swinging a diamond axe" — both resolve to `(SWING_HEAVY, AXE)`. Material does not enter the picture.
- "Swinging an axe ≈ swinging a mace" — if `(SWING_HEAVY, MACE)` is authored it is used; otherwise the system falls back to `(SWING_HEAVY, GENERIC)`. Copy/tune incrementally.
- New item categories never break anything. They fall back gracefully.

### `AnimationResolver` and `DefaultAnimationResolver`

**Files:** [AnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationResolver.java), [DefaultAnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java)

The resolver is a thin adapter that pulls `mainHandCategory` out of the context and delegates to the library. It exists so future selection logic (read multiple slots, mood-driven variant selection, A/B by villager UUID) can be slotted in without changing the runtime call sites.

---

## Runtime: `VillagerAnimator`

**File:** [VillagerAnimator.java](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java)

One animator per villager. Holds the current animation, the outgoing animation (during crossfade), and game-time markers for both. Constructed with an `AnimationResolver`, initialized in the `IDLE` archetype against the resolver's default.

### Archetype change → crossfade

`onArchetypeChanged(archetype, context, gameTime)` moves the current animation to outgoing (preserving its start time so it keeps advancing during blend-out), resolves the new animation, and sets it as current starting at `gameTime`. Early-returns if the archetype is unchanged.

`sample(gameTime, partialTicks)` samples the current animation, and — if outgoing is non-null — samples outgoing and blends them via `frame.blendTo(...)`. Blend progress is `elapsedTicksSinceTransition / currentAnimation.blendInTicks`, clamped to `[0, 1]`. When the blend completes the outgoing reference is dropped *after* sampling, which prevents a one-frame pop at the transition boundary.

Missing tracks blend against the target's neutral value, so a crossfade between two animations with disjoint track sets cannot leave stale poses on either side.

### Crossfade and the equipment sync lag

`tickContext(context, gameTime)` is the subtlety. `SynchedEntityData` (the motion byte) arrives on the client one frame before `ClientboundSetEquipmentPacket` (the item update), so the first frame after `setMotion(SWING_HEAVY)` may resolve against `mainHandCategory = GENERIC` and pick the wrong variant.

The renderer calls `tickContext(...)` every frame the archetype has *not* changed. If the context now differs from `lastResolvedContext` and the re-resolved animation has a different id, the animator transitions current → outgoing and starts the corrected animation under a fresh blend. This is treated as a soft transition, not an archetype change, so `lastSeenArchetype` does not move.

The net effect: a one-frame visual glitch under the wrong variant, then a clean crossfade to the right one. Most players will never see it.

---

## Render layer composition

The render path applies a fixed composition order onto the `PoseStack`. Each layer adds a delta on top of the previous one; nothing replaces.

For each visible attachment, the order is:

1. **Bone world transform** — if `socket.inheritsBoneTransform`, the bone's `translateAndRotate(poseStack)` is applied first. This is what makes the attachment ride the bone.
2. **Socket local transform** — the socket's `localTranslation`, `localRotation`, `localScale` describe the offset from the bone pivot to the attach point.
3. **Display profile transform** — per-`(slot, category)` translation/rotation/scale that tunes how *this* item sits in *this* slot.
4. **Animation slot deltas** — the current frame's `SlotTargets.translation/rotation/scale` for this slot. These are *deltas* on top of the resting pose. In an empty animation they are neutral (zero translation, zero rotation, unit scale).
5. **`renderContent(...)`** — dispatches on `AttachmentContent` subtype. v1 implements `ItemContent` via `ItemInHandRenderer`; the other two paths are TODO stubs with clear extension points.

The exact site is [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:76).

### IDLE = vanilla baseline

A villager in `IDLE` with no tracks evaluated returns an empty `AnimationFrame` from `KeyframeAnimation.sample` (or the EMPTY constant short-circuited at the start of sampling). Every target lookup returns its neutral, every slot delta is zero, and the render reduces to `bone × socket × profile × renderContent`. For `MAIN_HAND` with the calibrated `CROSSED_ARMS_CENTER` socket this matches the vanilla `CrossedArmsItemLayer` output. **We do not risk breaking the look at rest.**

### Model parts: `setupAnim` and additive application

**File:** [VanillaVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/VanillaVillagerModel.java:165)

The model's `setupAnim` first calls `resetPose` on every part, then applies vanilla's own logic (head tracking, leg swing, unhappy-villager wobble), then calls `applyAnimationFrame`. The reset prevents frame-to-frame drift and cross-villager model-state leaks since the same `VanillaVillagerModel` instance renders every villager.

ModelPart targets are applied **additively** (e.g. `arms.xRot += animatedRotation.x()`). The additive policy preserves the "living, looking-at-you" quality: head tracking still works during a swing, the unhappy wobble still fires during a chop, idle leg motion still happens during a cast. `HEAD_ROTATION_OVERRIDE` is the exception — it replaces head tracking absolutely (used when an animation needs to lock the head).

---

## Renderer orchestration

**File:** [SettlementsVillagerRenderer.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java)

Replaces the vanilla `VillagerRenderer` for `BaseVillager`. The vanilla `CrossedArmsItemLayer` is intentionally *not* registered; its job is now done by `AttachmentRenderLayer`. The two vanilla layers that still apply (`CustomHeadLayer`, `VillagerProfessionLayer`) are registered alongside.

Per-frame, the renderer:

1. Looks up the per-villager animator via [ClientAnimatorRegistry.getOrCreate](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java:30).
2. Builds an `AnimationSelectionContext` from the villager's main-hand item category (via [EquipmentLookup](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/attachments/EquipmentLookup.java)).
3. If `villager.getMotion() != animator.lastSeenArchetype`, calls `onArchetypeChanged(...)`. Otherwise calls `tickContext(...)` to handle the equipment-sync lag.
4. Samples a frame, stashes it on `this.currentFrame` and on `model.setAnimationFrame(...)`.
5. Calls `super.render(...)` which triggers `setupAnim` (model reads the frame) and then iterates layers (`AttachmentRenderLayer` reads `renderer.currentFrame()`).
6. Clears both fields in a `finally` block so a thrown render does not leak frame state to the next entity.

The renderer construction reaches into the Dagger graph via `SettlementsDagger.client()` because Minecraft owns renderer construction — constructor injection is not possible, the same workaround pattern as `BaseVillager`.

---

## Per-entity animator lifecycle

**File:** [ClientAnimatorRegistry.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java)

Per-villager `VillagerAnimator` instances live in a `ConcurrentHashMap<Integer, VillagerAnimator>` keyed by client entity id. `getOrCreate(villager)` lazily creates on first render.

Pruning is **periodic, not event-driven**: every 600 lookups the registry walks its keys and removes entries whose entity no longer exists in the client level. This single check covers despawn, death, chunk unload, and dimension changes without coupling `BaseVillager` (or any entity class) to renderer state. The trade-off is up to 600 lookups of stale state in the worst case, which is negligible at typical lookup rates.

Lives in the `@ClientSessionScope` graph (see [DI wiring](#di-wiring) below) — the whole registry resets on client logout, which is what we want.

---

## DI wiring

Two modules carry the framework's bindings. Both are client-side.

### `ClientAnimationModule`

**File:** [ClientAnimationModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java)

- `@Binds AnimationResolver` ← `DefaultAnimationResolver`.
- `@Provides @ClientSessionScope AnimationLibrary` — the v1 catalog. Currently one entry: `(SWING_HEAVY, AXE) → ButcheringAnimations.swingHeavyAxe()`.

`AnimationLibrary` is `@ClientSessionScope` so that future hot-reloads or session-scoped overrides have somewhere to hang. The provider returns a fresh `InMemoryAnimationLibrary` per session; the underlying `KeyframeAnimation` instances are immutable and safely shared.

### `ClientAttachmentModule`

**File:** [ClientAttachmentModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java)

- `@Binds @IntoSet AttachmentProvider` ← `EquipmentAttachmentProvider` (multibinding so additional providers can be added without touching the renderer).
- `@Provides @ClientScope SlotAnchorRegistry` — defaults from `InMemorySlotAnchorRegistry.defaults()`.
- `@Provides @ClientScope SocketRegistry` — defaults from `InMemorySocketRegistry.defaults()`.
- `@Provides @ClientScope AttachmentDisplayProfileRegistry` — defaults from `InMemoryAttachmentDisplayProfileRegistry.defaults()`.

These are `@ClientScope` (app-lifetime), not `@ClientSessionScope` (session-lifetime), because the rig and profile data does not change per-session.

---

## Adding a new animation

A new animation is mostly data. The shape mirrors the behavior-system pattern: define poses → define the animation → register → wire the behavior.

1. **Choose the archetype.** Pick from [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java). Add a new value only if the motion is genuinely new; "swing a hammer" is `SWING_HEAVY`, not `HAMMER_SWING`.
2. **Choose the item category.** Pick from [ItemCategory](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java). Add a new category only if the classifier needs to distinguish a new shape of item.
3. **Author the poses.** Create a `*Poses.java` factory class in `domain/animation/` colocated with related animations. Mirror [ButcheringPoses](../../src/main/java/dev/breezes/settlements/domain/animation/ButcheringPoses.java): `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, `public static final Pose` constants, private factory helpers for the shared shape. Reuse `AnimationTargets` constants — do not invent new targets unless authoring requires it.
4. **Author the animation.** Create a `*Animations.java` factory in the same package. Each animation is one `public static KeyframeAnimation foo()` method built with `KeyframeAnimation.fromPoses()`. Tick constants for impact frames live as `private static final int` fields on the animation class — server behaviors import them directly for damage timing.
5. **Register in the library.** Add an entry to the `InMemoryAnimationLibrary` map in [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:26): `AnimationKey.of(archetype, category)`.
6. **Wire the behavior.** Call `villager.setMotion(archetype)` at the relevant phase transitions in the behavior. Restore `IDLE` on completion and on interruption — see [ButcherLivestockBehavior:100-186](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/animals/butchering/ButcherLivestockBehavior.java:100) for the working pattern.
7. **Test.** Build green. The pose/track/animation primitives have unit-test coverage; new animations should at minimum compile against the registry. Visual verification is in-game.

---

## Adding a new socket

1. **Add the id.** New value in [SocketId](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java).
2. **Add the rig binding.** New `Socket.builder()` entry in [InMemorySocketRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java:24) with the bone, local translation, local rotation, and `inheritsBoneTransform`. Defaults to `true`; flip false only for free-floating world anchors.
3. **Bind a slot to it.** Update or add an entry in [InMemorySlotAnchorRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java:25) pointing the desired `AttachmentSlot` at the new `SocketId`.
4. **Calibrate.** The local-transform numbers are empirical. Tweak in-game until the rest pose matches the intended position.

No renderer changes are needed. The render path resolves through the socket registry by id.

---

## Adding a new animation target

ModelPart targets and slot targets follow different patterns.

**Model-part target** (e.g. for a future "wings" rig):

1. Add a `public static final AnimationTarget<V>` to [AnimationTargets](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java). Choose the value type, neutral value, interpolator, and intended policy.
2. Apply the target in [VanillaVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/VanillaVillagerModel.java:165) — read `frame.get(YOUR_TARGET)` and apply additively or absolutely to the relevant `ModelPart`. Match the policy enum you declared.
3. If the target is absolute and needs to distinguish "no track" from "neutral track", guard with `frame.has(YOUR_TARGET)`.

**Slot target** (e.g. a per-slot tint):

1. Add a factory method to [SlotTargets](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java) using the `computeIfAbsent` interning pattern. Pick neutral, interpolator, policy.
2. Read the target at the appropriate site in [AttachmentRenderLayer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java) (or wherever the new effect lives).

In both cases, the policy enum is documentation for the next reader — the runtime behavior is whatever your consumer code does.

---

## Deferred work

Designed-for but not yet implemented:

- **Animation events** (`onTick(int, AnimationEvent)`, `ClientAnimationEventListener` Dagger multibinding). Intended for client-only visual flair (camera shake, ambient particle bursts, transient prop attach/detach). Gameplay timing stays server-side and reads tick constants directly from the animation file — there is no plan for a server-side event bus.
- **Per-arm bones and sockets.** Bundled with the future split-arm (vindicator-style) model. Will add `LEFT_ARM`/`RIGHT_ARM` to `ModelPartRef`, `LEFT_HAND_HOLD`/`RIGHT_HAND_HOLD` to `SocketId`, and per-arm `AnimationTargets`. The slot/socket indirection is what makes this an additive change instead of a rewrite.
- **Additional equipment slots** (`BELT_LEFT`, `BELT_RIGHT`, `BACK`, etc.). Drop in as enum values plus socket bindings.
- **Datapack-loaded animations and sockets.** The `ResourceLocation` ids and registry indirection are ready for it; the loader is not built.
- **Quaternion target + slerp interpolator.** Add when a motion wraps past 180° or combines three axes near gimbal angles. Current v1 motions stay within Euler-safe ranges.
- **Cosmetic / transient attachment providers.** Multibinding is ready; only `EquipmentAttachmentProvider` is implemented.
- **`AnimationTargetPolicy` enforcement.** Currently intent metadata only — see [target catalog notes](#the-policy-convention--intent-metadata-not-enforcement). If a future target consumer needs to choose application style dynamically, route through the policy enum then.
