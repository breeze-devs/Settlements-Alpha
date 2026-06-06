# Animation System v2

This document covers how villager animations are authored, resolved per-villager, and applied during rendering on the custom Blockbench rig.
For the Dagger fundamentals that underpin the wiring, see [Dagger Guide](dagger_guide.md).
For server-side behavior wiring (which calls into the motion plane), see [Behavior System](behavior_system.md).

> **What changed since v1.** The villager now renders on a custom Blockbench rig ([SettlementsVillagerModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java), replacing the deleted `VanillaVillagerModel`) with **split arms** (per-side crossed/straight geometry), **baked socket empties** addressed by an ancestor-chain bone walk, a new **`ArmConfiguration`** discrete render state, a **motion-generation** counter that lets one-shot actions replay, a much larger **animation-target catalog**, and an in-game **debug pose overlay**. The three-plane conceptual model from v1 survives intact; almost every concrete name moved.

---

## Overview

The system has three orthogonal planes. Each plane is owned by a different layer, lives on a different lifetime, and never encodes another:

| Plane | What it describes | Authority | Lifetime |
|---|---|---|---|
| **Motion** | What motion the villager is currently *expressing* (e.g. `SWING_HEAVY`), plus a generation counter so the same motion can replay | Server intent, client picks animation | Transient — changes per behavior phase |
| **Loadout** | What items / cosmetics / props the villager *carries* and where | Server | Persistent |
| **Presentation** | How a given (slot, item-category) pair *renders* visually, and where it attaches on the rig | Client config | Static — registry lookup |

The render layer is the single point where the three meet. **No plane encodes another:** an animation never names an item, a slot never names a behavior, a presentation profile never names a state. This is what keeps the surface area small as new tools, cosmetics, and motions land.

A second guiding rule: **the renderer iterates attachments — it does not special-case "the main hand."** Every visible thing on the villager (held tool, future belt axe, future hat) is a [RenderableAttachment](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java) with an anchor, content, and a category. The render path is uniform.

One cross-cutting concern rides on top of the motion plane: **arm configuration** (crossed vs. straight, per arm). It is a discrete render state carried by the chosen animation and consumed by both the model (geometry visibility) and the presentation plane (which hand socket the item rides). It gets its own section below because it spans the model/presentation boundary.

---

## The custom rig

**File:** [SettlementsVillagerModel.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java)

The villager is a custom Blockbench model (`HierarchicalModel` implementing `HeadedModel`, `VillagerHeadModel`), authored in `assets/settlements_villager.bbmodel` and exported to the `createBodyLayer()` mesh ([SettlementsVillagerModel.java:132](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:132)). It is an **illager-style rig** — single-segment limbs (no elbows or knees by design) and crossed arms modelled as separate geometry from straight arms.

The bone hierarchy (constructor at [SettlementsVillagerModel.java:88](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:88)):

```
root → villager → torso → head → { monobrow, eyes → (eye_left/right → eyelid, eyeball → pupil), nose, mouth, headwear, hat }
                         → torso → arms → arms_crossed  → { arms_crossed_socket▣,
                         │                                  arm_crossed_center_whole,
                         │                                  arm_crossed_left, arm_crossed_center_left,
                         │                                  arm_crossed_right, arm_crossed_center_right }
                         │              → arms_straight → { arm_straight_left  → arm_straight_left_socket▣,
                         │                                  arm_straight_right → arm_straight_right_socket▣ }
                         → torso → bodywear
              → villager → legs → { leg_left, leg_right, feet_center_socket▣ }
```

`▣` marks the four **baked socket empties** — cube-less `PartDefinition`s positioned in Blockbench whose only job is to be an attach point. They are reached at render time by [ModelPartRef](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java) (see [Presentation plane](#presentation-plane)).

Two structural facts the rest of the system leans on:

- **Crossed arms are split into five pieces.** `arm_crossed_left` / `arm_crossed_right` are the two arms; `arm_crossed_center_whole` is the full bar where they meet; `arm_crossed_center_left` / `arm_crossed_center_right` are half-caps. Exactly which of these are visible is decided each frame from the [arm configuration](#arm-configuration). This is what lets one arm uncross while the other stays folded without a seam.
- **Legs branch off `villager` directly, not through `torso`.** Every socket bone-walk that reaches the feet skips `torso` for this reason ([SettlementsVillagerModel.java:268](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:268)).

The model is registered as `settlements:base_villager#main` in [ClientModEvents](../../src/main/java/dev/breezes/settlements/bootstrap/event/ClientModEvents.java) — see [Model & renderer registration](#model--renderer-registration).

---

## End-to-end flow

A single frame of villager rendering walks all three planes:

1. **Behavior sets motion intent** on the server. [HarvestMelonBehavior:164](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/farming/HarvestMelonBehavior.java:164) calls `villager.triggerMotion(AnimationArchetype.SWING_HEAVY)` to start a one-shot swing; continuous states call `villager.setMotion(...)` instead (see [Motion plane](#motion-plane)).
2. **Two motion bytes sync.** [BaseVillager.java:115](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:115) declares `DATA_MOTION_ARCHETYPE` and `DATA_MOTION_GENERATION`, both `SynchedEntityData<Byte>` that vanilla broadcasts on change.
3. **The renderer polls.** [SettlementsVillagerRenderer.getOrUpdateAnimator](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:96) reads `villager.getMotion()` and `villager.getMotionGeneration()` and compares both to the animator's last-seen values.
4. **The animator transitions** if archetype *or* generation changed. [VillagerAnimator.onMotionChanged](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:34) calls the resolver, moves the previous animation to "outgoing" for crossfade-out, and starts the new animation.
5. **The resolver picks an animation.** [DefaultAnimationResolver.resolve](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java:15) looks up `(archetype, mainHandCategory)` in the [AnimationLibrary](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLibrary.java) with a fallback chain (exact → archetype × `GENERIC` → idle).
6. **The animator samples a frame.** [VillagerAnimator.sample](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:94) returns an [AnimationFrame](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java), blending the outgoing animation into the current one if a crossfade is in progress.
7. **The debug overlay splices in** (if active), then **the frame and the arm config are stashed.** The renderer stores the frame on itself and the model, and snaps the current animation's `ArmConfiguration` onto the model ([SettlementsVillagerRenderer.java:74](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:74)).
8. **The model reads ModelPart-flavored targets** in `setupAnim` ([SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:296)); **the attachment layer reads slot-flavored targets and resolves the socket** ([AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79)).
9. **The behavior restores `IDLE`** on completion or interruption ([HarvestMelonBehavior:220](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/farming/HarvestMelonBehavior.java:220)).

The animator and the behavior tick at the same rate against the same `triggerMotion(...)` start point, so timing constants live in the animation file (e.g. `SwingAnimations.SWING_IMPACT_TICKS`) and are imported directly by the server behavior when it needs to coordinate damage windows or sounds. This is a coordination contract, not duplication — there is no event bus between the two.

---

## Motion plane

Motion is named in **archetypal** terms, not items or tasks. A butcher swinging an axe and a farmer breaking a melon both use `SWING_HEAVY`. The item taxonomy (handled by the loadout plane) is what disambiguates which animation gets played for that archetype.

### `AnimationArchetype`

**File:** [AnimationArchetype.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java)

A small enum of motion intents, in ordinal order: `IDLE`, `SWING_HEAVY`, `CAST`, `REEL_IN`, `REEL_OUT`, `POINT`, `INTERACT`, `SURVEY_WITH_SPYGLASS`, `WRITE_TO_MAP`, `EAT`. Provides `toNetworkByte()` / `fromNetworkByte()` with safe fallback to `IDLE` on unknown values.

Encoded as `byte` (the ordinal) because the enum will not exceed 256 values for the foreseeable future, and one byte through `SynchedEntityData` is the cheapest possible sync surface. **Order is the wire format** — append new values, do not reorder.

### Two setters: continuous vs. one-shot

**File:** [BaseVillager.java:204-231](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:204)

The motion state is two synced bytes — the archetype and a **generation counter** — exposed through two distinct write methods:

- `setMotion(AnimationArchetype)` — **idempotent / continuous.** Writes only the archetype byte. Setting the same archetype repeatedly (e.g. every tick) does **not** restart the animation. Use it for looping or sustained states (`IDLE`, the `REEL_OUT` jig-fight loop, the `EAT` loop).
- `triggerMotion(AnimationArchetype)` — **one-shot.** Bumps `DATA_MOTION_GENERATION` *and* writes the archetype, forcing the client to replay from frame 0 even when the archetype is unchanged. Use it for discrete actions (`SWING_HEAVY`, `INTERACT`, `CAST`, `REEL_IN`, `SURVEY_WITH_SPYGLASS`, `WRITE_TO_MAP`). The counter is a `byte` and intentionally wraps `127 → -128`; only inequality matters.
- `getMotion()` / `getMotionGeneration()` — read back (client during render, server during behavior tick).

The convention in practice: **`ONCE` animations are driven by `triggerMotion`, `LOOP`/sustained states by `setMotion`, and every behavior restores `setMotion(IDLE)` on completion and interruption** — otherwise the last motion sticks until the next transition. See [HarvestMelonBehavior](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/farming/HarvestMelonBehavior.java:164) (one-shot swing) and [EatFoodBehavior:85](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/hunger/EatFoodBehavior.java:85) (sustained eat loop) for the two patterns.

Why a generation counter at all? A villager breaking three melons in a row issues `triggerMotion(SWING_HEAVY)` three times against an unchanged archetype byte. Without the counter the renderer would see no change and the swing would never replay. The counter is the minimal "this is a fresh action" signal.

### Change detection is a poll, not an event

The renderer reads the two synced bytes each frame and compares to the animator's `lastSeenArchetype` / `lastSeenGeneration`. This avoids event-subscription lifecycle issues during world load and the cost is two cached byte reads per villager. State changes that arrive before the equipment sync (see [tickContext lag fix](#crossfade-and-the-equipment-sync-lag)) are resolved on the next frame without re-entering the motion-change path.

---

## Arm configuration

The split-arm rig needs to know, per arm, whether to show **crossed** or **straight** geometry. That choice cannot crossfade — geometry visibility is binary — so it is a **discrete render state snapped at the animation transition**, not a blended value.

### `ArmPose` and `ArmConfiguration`

**Files:** [ArmPose.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmPose.java), [ArmConfiguration.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmConfiguration.java)

`ArmPose` is a two-value enum: `CROSSED`, `STRAIGHT`. `ArmConfiguration` is a `record(ArmPose left, ArmPose right)` with four named constants:

| Constant | left | right |
|---|---|---|
| `BOTH_CROSSED` (default) | CROSSED | CROSSED |
| `BOTH_STRAIGHT` | STRAIGHT | STRAIGHT |
| `LEFT_CROSSED_RIGHT_STRAIGHT` | CROSSED | STRAIGHT |
| `RIGHT_CROSSED_LEFT_STRAIGHT` | STRAIGHT | CROSSED |

### It rides the animation and snaps at transition

An `ArmConfiguration` is a property of **each [KeyframeAnimation](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:23)** (default `BOTH_CROSSED` when unset). The animator exposes the **current** animation's config — never the outgoing one — via [VillagerAnimator.currentArmConfiguration](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:90), and the renderer snaps it onto the model after sampling. Because it reads *current*, the geometry flips instantly at the transition rather than trying to interpolate through an impossible half-crossed state.

### One config, three consumers

The same snapped `ArmConfiguration` is the single source of truth for three things, which is what keeps them from desyncing:

1. **Per-arm geometry visibility** — [SettlementsVillagerModel.applyAnimationFrame:301](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:301) toggles `arm_crossed_*` vs. `arm_straight_*`, and picks the center bar piece: whole bar when both arms are crossed, the matching half-cap when exactly one is.
2. **Which hand socket the item rides** — [AttachmentRenderLayer:91](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:91) reads `config.left()` for `OFF_HAND`, `config.right()` for `MAIN_HAND`, and asks the slot anchor for the matching socket (crossed → `CROSSED_ARMS_CENTER`, straight → the per-hand socket).
3. **Which arm targets do anything** — the model applies all twelve arm targets unconditionally, but a target aimed at hidden geometry is a no-op, so authors only need to drive the targets matching the config they declared (see [Animation primitives](#animation-primitives)).

> **Mapping convention:** `OFF_HAND → left arm`, `MAIN_HAND → right arm`. Both the model's visibility logic and the render layer's socket selection follow it.

---

## Loadout plane

The loadout is the source of truth for what the villager is carrying. Today only `MAIN_HAND` and `OFF_HAND` are wired; the design carries enough naming to add belt / back / cosmetic slots without renderer or animation refactors.

### `AttachmentSlot` and `EquipmentSlot`

**Files:** [AttachmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentSlot.java), [EquipmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/inventory/EquipmentSlot.java)

`AttachmentSlot` is the marker supertype the renderer indexes by. `EquipmentSlot` is the v1 implementation with `MAIN_HAND` and `OFF_HAND`. Future cosmetic slot enums can be added as parallel implementations of `AttachmentSlot` without touching the renderer.

### `RenderableAttachment` and `AttachmentContent`

**Files:** [RenderableAttachment.java](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java), [AttachmentContent.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentContent.java)

A `RenderableAttachment` is a `(slot, content, category)` triple. `AttachmentContent` is a sealed hierarchy:
- `ItemContent(ItemStack)` — rendered via `ItemInHandRenderer`. The only path implemented today.
- `ModelContent` — placeholder for future custom models (hats, balloons). TODO stub in the render layer.
- `BillboardContent` — placeholder for future textured-quad props. TODO stub in the render layer.

### `AttachmentProvider` — Dagger multibinding

**File:** [AttachmentProvider.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentProvider.java)

Providers contribute `RenderableAttachment` lists into the render path. Bound `@IntoSet` in [ClientAttachmentModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java). The layer sorts them by `renderOrder()` then class name before iteration, so layering between providers is deterministic.

There is exactly one today: [EquipmentAttachmentProvider](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/attachment/EquipmentAttachmentProvider.java), which reads `IVillagerEquipment` and emits one `ItemContent` per occupied slot (its `renderOrder()` is the interface default, `0`).

### `ItemCategory` — taxonomy for held items

**File:** [ItemCategory.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java)

An open enum (`AXE`, `SWORD`, `MACE`, `PICKAXE`, `SHOVEL`, `HOE`, `FISHING_ROD`, `MAP`, `TORCH`, `LANTERN`, `SPYGLASS`, `GENERIC`, …) with a single `of(ItemStack)` classifier centralizing all item-type-to-category dispatch. This is the only place in the codebase that branches on concrete item classes; render and animation code never do.

Category-based dispatch is what makes "swinging an iron axe ≈ swinging a diamond axe" automatic: both resolve to `(SWING_HEAVY, AXE)`. Material is irrelevant.

---

## Presentation plane

The presentation plane converts a `(slot, category)` pair into a sequence of transforms ready to feed to the `PoseStack`. Three pieces of data feed it: the **socket** (where the slot lives on the rig), the **display profile** (how this category-of-item sits in this slot), and the **frame** (per-frame animation deltas).

### `ModelPartRef` — addressing a bone by enum

**File:** [ModelPartRef.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java)

An enum naming the bones the presentation plane can attach to: `ROOT`, `BODY`, `HEAD`, `ARMS`, `ARMS_CROSSED_SOCKET`, `ARM_STRAIGHT_RIGHT_SOCKET`, `ARM_STRAIGHT_LEFT_SOCKET`, `FEET_CENTER_SOCKET`. The domain references bones by this enum, never by `ModelPart` instances — the mapping to live parts lives in the infrastructure model (next paragraph), keeping the domain free of Minecraft rig types.

A `ModelPartRef` resolves to a **full ancestor bone chain**, not a single bone, in [SettlementsVillagerModel.applyBoneTransform](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:256). The render layer's pass enters model space *after* the model's own `root()` push/pop has already closed, so nothing from the body render persists — reaching a socket means re-walking the hierarchy from the top, calling `translateAndRotate` on each ancestor in order. For example `ARM_STRAIGHT_RIGHT_SOCKET` replays `root → villager → torso → arms → arms_straight → arm_straight_right → arm_straight_right_socket`. This walk reads the poses `setupAnim` already wrote, so the socket inherits everything an animation did to the arm.

### `Socket` — bone + local transform

**File:** [Socket.java](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java)

A socket is an attach point on the rig. It carries `id`, `bone` (a `ModelPartRef`), `localTranslation`, `localRotation`, `localScale` (default `1.0`), and `inheritsBoneTransform` (default `true`).

This is the same pattern Unity ships as **sockets**, Unreal as **skeletal sockets**, and Source as **`$attachment`**. When `inheritsBoneTransform` is true, the socket's world transform follows whatever the bone is doing in the current frame (rotated arms, walking bob, etc.) via the ancestor-chain walk above. When false, the socket is a free-floating world anchor (the `Socket.identity(id)` fallback uses this for unknown ids).

Because the attach point's *position* now comes from a Blockbench empty, socket `localTranslation` is typically `Vec3.ZERO` — placement is delegated to the rig, and only the item's resting `localRotation` is tuned in code.

### `SocketId` — named attach points

**File:** [SocketId.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java)

Enum, not `ResourceLocation`, for compile-time exhaustiveness — same rationale as `AttachmentSlot`. Four values:

| `SocketId` | Bone (`ModelPartRef`) | Purpose |
|---|---|---|
| `CROSSED_ARMS_CENTER` | `ARMS_CROSSED_SOCKET` | Hand-meeting point on the crossed-arms silhouette; the rest-pose hold |
| `HAND_RIGHT` | `ARM_STRAIGHT_RIGHT_SOCKET` | Right hand when that arm is straight |
| `HAND_LEFT` | `ARM_STRAIGHT_LEFT_SOCKET` | Left hand when that arm is straight |
| `FEET_CENTER` | `FEET_CENTER_SOCKET` | Ground-contact anchor for future providers; no slot bound yet |

### `SocketRegistry` and `SlotAnchor`

**Files:** [SocketRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketRegistry.java), [InMemorySocketRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java), [SlotAnchor.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java), [InMemorySlotAnchorRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java)

`SocketRegistry.get(SocketId)` returns the registered `Socket`, or `Socket.identity(id)` (root-anchored, no inheritance) as a safe fallback for unknown ids. [InMemorySocketRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java:24) registers the four sockets above.

`SlotAnchor` is the per-slot binding plus slot-only render concerns: `socket: SocketId` (the default / crossed socket), `defaultDisplayContext: ItemDisplayContext`, and `@Nullable straightSocket: SocketId`. The method `socketFor(ArmPose)` ([SlotAnchor.java:23](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java:23)) returns `straightSocket` when the arm is `STRAIGHT` (and one is set), else the default — falling back to the default keeps anchors without a straight socket from ever producing a null lookup.

The defaults ([InMemorySlotAnchorRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java:25)) bind both hands to `CROSSED_ARMS_CENTER` as their crossed socket, and add per-hand straight sockets: `MAIN_HAND → HAND_RIGHT`, `OFF_HAND → HAND_LEFT`. So the held item rides the crossed-arms center while the arm is folded and snaps to the per-hand socket the moment the animation declares that arm straight.

### `AttachmentDisplayProfile` — per `(slot, category)` transform

**Files:** [AttachmentDisplayProfile.java](../../src/main/java/dev/breezes/settlements/domain/presentation/AttachmentDisplayProfile.java), [InMemoryAttachmentDisplayProfileRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemoryAttachmentDisplayProfileRegistry.java)

Different items sit differently in the same slot. The display profile (`translation`, `rotation`, `scale`, optional `displayContextOverride`) lets a designer tune just the deltas that matter, with lookup order: exact `(slot, category)` → `(slot, GENERIC)` → identity profile. A single `(slot, GENERIC)` entry covers every uncategorized item, and specific entries (axe, pickaxe, fishing rod, spyglass, …) override it as you author them.

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

### Target catalog

**[AnimationTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java)** owns every model-part target — 25 of them, grouped by rig region. All rotations are `Vector3f` (radians, neutral zero) via `Interpolators.VECTOR3F`; all translations are `Vec3` (neutral `Vec3.ZERO`) via `Interpolators.VEC3`.

**Arms — the static routing matrix (12 targets, all `ADDITIVE`).** Four "block" targets drive a whole arm-set; eight "individual" targets each bind to exactly one fixed bone. An author drives the subset matching their declared arm config; the rest are no-ops on hidden geometry.

| Target (×2: `_ROTATION`, `_TRANSLATION`) | Bone driven |
|---|---|
| `ARMS_CROSSED_*` | `arms_crossed` (both crossed arms together) |
| `ARMS_STRAIGHT_*` | `arms_straight` (both straight arms together) |
| `ARM_CROSSED_LEFT_*` / `ARM_CROSSED_RIGHT_*` | one crossed arm |
| `ARM_STRAIGHT_LEFT_*` / `ARM_STRAIGHT_RIGHT_*` | one straight arm |

**Head & body (2).** `HEAD_ROTATION_OVERRIDE` (`ABSOLUTE` — replaces vanilla head tracking; used by the cartographer survey/mark clips), `BODY_ROTATION` (`ADDITIVE` torso lean).

**Legs (2, `ABSOLUTE`).** `LEG_LEFT_ROTATION_OVERRIDE`, `LEG_RIGHT_ROTATION_OVERRIDE` — absolute so an animation can *suppress* the vanilla walk-swing entirely (e.g. a moonwalk).

**Root motion (2, `ADDITIVE`).** `ROOT_TRANSLATION`, `ROOT_ROTATION` — displace/rotate the whole entity in model space for ground-pound recoil, kill-aura spin, hover bob, without touching individual bones.

**Face — provisional (7, `ADDITIVE`).** `MONOBROW_TRANSLATION`, `MONOBROW_ROTATION`, `MOUTH_TRANSLATION`, `EYELID_LEFT/RIGHT_TRANSLATION`, `PUPIL_LEFT/RIGHT_TRANSLATION`.

> **Scaffolding, not dead code.** The straight-arm / individual-arm targets, both leg overrides, root motion, and the entire face block currently have **no authored consumers** — they are intentional infrastructure for future boss/emote/asymmetric animations. The face ids and axes in particular are flagged **PROVISIONAL** in the source and may be renamed once the first authored face clip validates the convention. Do not remove them as "unused," and prefer consuming them over inventing new targets.

**[SlotTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java)** — per-slot targets, interned via `ConcurrentHashMap` so repeated `SlotTargets.translation(MAIN_HAND)` calls return the same instance and frame-map lookups stay flat:

| Factory | Value type | Neutral | Interpolator | Policy |
|---|---|---|---|---|
| `translation(slot)` | `Vec3` | zero | `VEC3` | `ADDITIVE` |
| `rotation(slot)` | `Vector3f` | zero | `VECTOR3F` | `ADDITIVE` |
| `scale(slot)` | `Float` | `1.0` | `FLOAT` | `MULTIPLICATIVE` |
| `visibility(slot)` | `Boolean` | `true` | `BOOLEAN_STEP` | `ABSOLUTE` |
| `displayContext(slot)` | `ItemDisplayContext` | `NONE` | `ITEM_DISPLAY_CONTEXT_STEP` | `ABSOLUTE` |

### The policy convention — intent metadata, not enforcement

**Important:** [AnimationTargetPolicy](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargetPolicy.java) (`ADDITIVE`, `MULTIPLICATIVE`, `ABSOLUTE`) is declared on every target but **not consulted at runtime**. The application style is hardcoded at the consumption sites:

- ModelPart targets in [SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:296): `applyRotation` / `applyTranslation` do `+=`; `applyAbsoluteRotation` does `=` for `HEAD_ROTATION_OVERRIDE` and the two leg overrides, each inside a `frame.has(...)` guard.
- Slot targets in [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79): translation/rotation/scale layer onto the `PoseStack`, visibility early-returns, displayContext falls back through `displayContextFor(...)`.

When adding a new target, set the policy enum to match the consumer behavior, but understand that the renderer will do whatever it's coded to do — the enum is a contract for the next reader, not a runtime dispatcher.

### `Keyframe`, `AnimationTrack`, `KeyframeAnimation`

**Files:** [Keyframe.java](../../src/main/java/dev/breezes/settlements/domain/animation/Keyframe.java), [AnimationTrack.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTrack.java), [KeyframeAnimation.java](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java)

A `Keyframe<V>` is a tick offset, a value, and the easing curve to the *next* keyframe. An `AnimationTrack<V>` is one target plus a sorted list of keyframes plus the interpolator. A `KeyframeAnimation` is a `ResourceLocation` id, a duration, a `LoopMode`, blend-in/out durations, an **`ArmConfiguration`** (default `BOTH_CROSSED`), and a list of tracks.

`KeyframeAnimation.sample(elapsedTicks)` resolves the elapsed-ticks against the loop mode ([LoopMode.java](../../src/main/java/dev/breezes/settlements/domain/animation/LoopMode.java) — `ONCE`, `LOOP`, `PING_PONG`, `HOLD_LAST`), samples each track, and returns an `AnimationFrame`.

The animation id is a `ResourceLocation` rather than a string — keeps the door open for datapack-loaded animations and animation-bound sounds/particles.

### `AnimationFrame`

**File:** [AnimationFrame.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java)

Immutable map of `AnimationTarget<?> → Object`. `frame.get(target)` returns the target's neutral value if absent, or a typed value otherwise. `frame.has(target)` lets consumers distinguish "explicitly absent" from "explicitly neutral" — used by `HEAD_ROTATION_OVERRIDE` and the leg overrides so a track present but holding the neutral still overrides, and a missing track does not.

`frame.blendTo(other, t)` produces a blended frame for crossfade. Each target's `from`/`to` defaults to its `neutralValue` when missing, so partial animations cannot leave stale poses behind. `frame.overlay(map)` returns a copy with the given targets replaced — used by the [debug pose overlay](#debug-pose-overlay) to splice slider values over a sampled frame.

### `Easing` and `Interpolator`

**Files:** [Easing.java](../../src/main/java/dev/breezes/settlements/domain/animation/Easing.java), [Interpolator.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolator.java), [Interpolators.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolators.java)

`Easing` is a `float → float` shape applied to normalized progress between two keyframes (`LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`, `STEP`, `CUBIC`). `Interpolator<V>` is the type-specific lerp. Standard interpolators: `VEC3`, `VECTOR3F`, `FLOAT`, `BOOLEAN_STEP`, `ITEM_DISPLAY_CONTEXT_STEP`. Boolean and display-context targets use step interpolators (no notion of "halfway between two contexts") and pair with `ABSOLUTE` policy.

Quaternion-based rotation interpolation is not implemented. The current animation set stays under 180° single-axis pitch where Euler `lerp` is correct. Add a parallel `Quaternion` target + slerp interpolator when a motion wraps past 180° or combines three axes near gimbal angles.

---

## Authoring animations with poses

Direct track authoring is supported but discouraged. The pose-driven builder is the primary API — same `KeyframeAnimation` output, much less typing, and the pose pattern composes (`pose.with(other)`).

### `Pose`

**File:** [Pose.java](../../src/main/java/dev/breezes/settlements/domain/animation/Pose.java)

A `Pose` is a **snapshot in space** — an immutable `AnimationTarget<?> → value` map describing one configuration of bones and slot transforms. It is not a path in time. Construction: `Pose.of(target, value)` for a single-target pose, `pose.with(target, value)` to add or override one target, `pose.with(otherPose)` to merge two poses (later overlay wins on collision). Construction validates value types against `target.getValueType()`, so a type mismatch fails at pose creation, not at sample time.

Pose factories are per-behavior `*Poses.java` classes, not a global enum. [SwingPoses.java](../../src/main/java/dev/breezes/settlements/domain/animation/SwingPoses.java) is the canonical example: three named `public static final Pose` constants (`ARMS_REST`, `ARMS_RAISED`, `ARMS_IMPACT`) built from a private `arms(pitchDegrees)` factory that wraps `RotationUtil.degrees(...)` (degree authoring sugar → radians `Vector3f`). This keeps related poses colocated and named.

### `PoseAnimationBuilder`

**Files:** [PoseAnimationBuilder.java](../../src/main/java/dev/breezes/settlements/domain/animation/PoseAnimationBuilder.java), [KeyframeAnimation.fromPoses()](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java)

Entry point: `KeyframeAnimation.fromPoses()` returns a `PoseAnimationBuilder`. Chain `.id(...)`, `.durationTicks(...)`, `.loopMode(...)`, `.blendInTicks(...)`, `.blendOutTicks(...)`, and `.arms(ArmConfiguration)` (defaults `BOTH_CROSSED`), then add timeline entries with `.at(tick, pose, easingToNext)`.

Under the hood, the builder expands poses into independent per-target tracks: each target gets a sorted track containing one keyframe per pose that defines it. **Targets in a pose are keyed independently** — if an intermediate pose omits a target, that target interpolates directly between the surrounding poses that *do* include it. This is what makes sparse poses work: you do not have to repeat every target on every keyframe.

Varying segment speeds emerge from **tick density × easing curve**, not from a pose property. A long tick gap with `EASE_IN` is slow; a short gap with `EASE_OUT` is fast. Fishing's "slow wind-up, fast release" is two `.at(...)` calls timed appropriately against the same pose set.

### Track override escape hatch

`PoseAnimationBuilder.track(AnimationTrack<?>)` accepts hand-authored tracks alongside pose-driven `.at(...)` calls. If a manual track targets the same property as a pose, the manual track wins (the pose contribution for that target is dropped). Use this when one target needs a curve no clean pose set captures.

### The canonical example and the factory inventory

[SwingAnimations.swingHeavy()](../../src/main/java/dev/breezes/settlements/domain/animation/SwingAnimations.java:23) is the working example: a 12-tick `ONCE` animation, one-tick blend-in, three-tick blend-out, four pose keyframes through rest → raised → impact → rest, driving the single target `ARMS_CROSSED_ROTATION`. It defaults to `BOTH_CROSSED`, so the held item follows the arm because `MAIN_HAND` rides `CROSSED_ARMS_CENTER` on the `arms_crossed` bone — no hand-coordinated compensation needed. Tick constants for the impact frame (`SWING_DURATION_TICKS`, `SWING_RAISE_TICKS`, `SWING_IMPACT_TICKS`) are `public static final int` fields on the class so server behaviors import them directly for damage timing.

The current factory set, all registered in [ClientAnimationModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:30):

| Factory | Archetype × Category | Loop / duration |
|---|---|---|
| `SwingAnimations.swingHeavy()` | `SWING_HEAVY` × `GENERIC` | `ONCE` / 12t |
| `FishingAnimations.cast()` | `CAST` × `FISHING_ROD` | `ONCE` / 20t |
| `FishingAnimations.jigFight()` | `REEL_OUT` × `FISHING_ROD` | `LOOP` / 40t |
| `FishingAnimations.reelYank()` | `REEL_IN` × `FISHING_ROD` | `ONCE` / 14t |
| `InteractAnimations.interact()` | `INTERACT` × `GENERIC` | `ONCE` / 12t |
| `CartographerAnimations.surveyWithSpyglass()` | `SURVEY_WITH_SPYGLASS` × `SPYGLASS` | `ONCE` / 160t |
| `CartographerAnimations.markMap()` | `WRITE_TO_MAP` × `MAP` | `ONCE` / 40t |
| `EatingAnimations.eat()` | `EAT` × `GENERIC` | `LOOP` / 16t |

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

The chain means: material never matters (`(SWING_HEAVY, AXE)` covers iron and diamond alike); a missing item-specific variant degrades to the archetype-generic one; and new item categories never break anything.

### `AnimationResolver` and `DefaultAnimationResolver`

**Files:** [AnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationResolver.java), [DefaultAnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java)

The resolver is a thin adapter that pulls `mainHandCategory` out of the context and delegates to the library. It exists so future selection logic (read multiple slots, mood-driven variant selection, A/B by villager UUID) can be slotted in without changing the runtime call sites.

---

## Runtime: `VillagerAnimator`

**File:** [VillagerAnimator.java](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java)

One animator per villager. Holds the current animation, the outgoing animation (during crossfade), game-time markers for both, and the last-seen archetype / generation / context. Constructed with an `AnimationResolver`, initialized in the `IDLE` archetype against the resolver's default.

### Motion change → crossfade

`onMotionChanged(archetype, generation, context, gameTime)` early-returns if **both** the archetype and the generation are unchanged. Otherwise it moves the current animation to outgoing (preserving its start time so it keeps advancing during blend-out), resolves the new animation, and sets it as current starting at `gameTime`. Including the generation in the guard is what lets `triggerMotion` replay the same archetype.

`sample(gameTime, partialTicks)` samples the current animation, and — if outgoing is non-null — samples outgoing and blends them via `frame.blendTo(...)`. Blend progress is `elapsedTicksSinceTransition / currentAnimation.blendInTicks`, clamped to `[0, 1]` (and `1.0` immediately when `blendInTicks <= 0`). When the blend completes the outgoing reference is dropped *after* sampling, which prevents a one-frame pop at the transition boundary. Missing tracks blend against the target's neutral value, so a crossfade between two animations with disjoint track sets cannot leave stale poses on either side.

### Arm config snaps, it does not blend

`currentArmConfiguration()` returns the **current** animation's `ArmConfiguration` — never the outgoing one. Geometry visibility cannot crossfade, so the arm set flips discretely at the transition. The renderer reads this after sampling and snaps it onto the model (see [Arm configuration](#arm-configuration)).

### Crossfade and the equipment sync lag

`tickContext(context, gameTime)` is the subtlety. `SynchedEntityData` (the motion bytes) arrives on the client one frame before `ClientboundSetEquipmentPacket` (the item update), so the first frame after a swing may resolve against `mainHandCategory = GENERIC` and pick the wrong variant.

The renderer calls `tickContext(...)` every frame the motion has *not* changed. If the context now differs from `lastResolvedContext` and the re-resolved animation has a different id, the animator transitions current → outgoing and starts the corrected animation under a fresh blend. This is treated as a soft transition, not a motion change, so `lastSeenArchetype` / `lastSeenGeneration` do not move. The net effect: a one-frame visual glitch under the wrong variant, then a clean crossfade to the right one.

---

## Render layer composition

The render path applies a fixed composition order onto the `PoseStack`. Each layer adds a delta on top of the previous one; nothing replaces. The exact site is [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79).

For each visible attachment (visibility checked first via the slot's `visibility` target, which early-returns):

1. **Socket selection** — the layer reads the model's snapped `ArmConfiguration`, picks the arm for this slot (`OFF_HAND → left`, else `right`), and resolves `anchor.socketFor(armPose)` to a `Socket`.
2. **Bone world transform** — if `socket.inheritsBoneTransform`, `applyBoneTransform(poseStack, socket.getBone())` replays the **full ancestor bone chain** to the socket bone. This is what makes the attachment ride the arm through everything `setupAnim` did to it.
3. **Socket local transform** — the socket's `localTranslation`, `localRotation`, `localScale` (the offset from the empty to the item).
4. **Display profile transform** — per-`(slot, category)` translation/rotation/scale that tunes how *this* item sits in *this* slot.
5. **Animation slot deltas** — the current frame's `SlotTargets.translation/rotation/scale` for this slot. These are *deltas* on top of the resting pose; neutral in an empty animation.
6. **`renderContent(...)`** — resolves the display context (`SlotTargets.displayContext` → profile override → anchor default), then dispatches on `AttachmentContent` subtype. `ItemContent` renders via `ItemInHandRenderer`; the other two paths are TODO stubs.

### IDLE = the authored rest pose

A villager in `IDLE` with no tracks evaluated returns an empty `AnimationFrame`. Every model-part target lookup returns its neutral, every slot delta is zero, and the render reduces to the rig's authored rest pose (crossed arms, baked into the `arms_crossed` PartPose) with the held item on the calibrated `CROSSED_ARMS_CENTER` socket. Unlike v1, this is **not** vanilla `CrossedArmsItemLayer` parity — it is a deliberate authored baseline on the custom rig, tuned in Blockbench and via the socket's `localRotation`.

### Model parts: `setupAnim` and additive application

**File:** [SettlementsVillagerModel.setupAnim](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:222)

`setupAnim` first calls `resetPose` on every part, then applies vanilla-style logic (head yaw/pitch tracking, unhappy-villager head wobble, cosine leg swing on the new rig's leg parts), then calls `applyAnimationFrame`. The reset prevents frame-to-frame drift and cross-villager model-state leaks, since the same `SettlementsVillagerModel` instance renders every villager.

`applyAnimationFrame` ([:296](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:296)) first resolves per-arm visibility from the snapped `ArmConfiguration`, then applies targets:

- The **twelve arm targets** are applied additively and **unconditionally** — a target aimed at hidden geometry is a harmless no-op, so authors only drive the ones matching their declared config.
- `BODY_ROTATION` and root motion (`ROOT_TRANSLATION`/`ROOT_ROTATION`) apply additively on top of `resetPose`'s baseline.
- `HEAD_ROTATION_OVERRIDE` and the two `LEG_*_ROTATION_OVERRIDE` targets apply **absolutely**, each guarded by `frame.has(...)` so that an animation *without* the track leaves vanilla head tracking / walk-swing untouched, while a track holding the neutral still overrides.
- Face targets apply additively (provisional).

The additive default preserves the "living, looking-at-you" quality: head tracking still works during a swing, the unhappy wobble still fires during a chop, idle leg motion still happens during a cast — unless an animation explicitly takes absolute control.

---

## Renderer orchestration

**File:** [SettlementsVillagerRenderer.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java)

Replaces the vanilla `VillagerRenderer` for `BaseVillager` (a `MobRenderer`). The vanilla `CrossedArmsItemLayer` is intentionally *not* registered; its job is now done by `AttachmentRenderLayer`. The two vanilla layers that still apply (`CustomHeadLayer`, `VillagerProfessionLayer`) are registered alongside.

Per-frame, the renderer:

1. Applies baby scale if needed.
2. Looks up the per-villager animator and advances it via `getOrUpdateAnimator(...)` — reads `getMotion()` + `getMotionGeneration()`, calls `onMotionChanged(...)` when either changed, otherwise `tickContext(...)` for the equipment-sync lag.
3. Samples a frame, **passes it through `debugPoseOverride.applyTo(...)`**, stashes it on `this.currentFrame` and on `model.setAnimationFrame(...)`.
4. Snaps `model.setArmConfiguration(animator.currentArmConfiguration())` — read after sampling so frame and geometry come from the same resolved animation.
5. Calls `super.render(...)`, which triggers `setupAnim` (model reads the frame) and then iterates layers (`AttachmentRenderLayer` reads `renderer.currentFrame()`).
6. In a `finally` block, clears the frame to `AnimationFrame.EMPTY`, nulls the model frame, and resets the model's arm config to `BOTH_CROSSED` — so a thrown render cannot leak state to the next entity.

The renderer construction reaches into the Dagger graph via `SettlementsDagger.client()` because Minecraft owns renderer construction — constructor injection is not possible, the same workaround pattern as `BaseVillager`. It pulls the `AttachmentRenderLayer` dependencies and the `DebugPoseOverride` singleton from there.

---

## Per-entity animator lifecycle

**File:** [ClientAnimatorRegistry.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java)

Per-villager `VillagerAnimator` instances live in a `ConcurrentHashMap<Integer, VillagerAnimator>` keyed by client entity id. `getOrCreate(villager)` lazily creates on first render.

Pruning is **periodic, not event-driven**: every `PRUNE_INTERVAL_LOOKUPS` (600) lookups the registry walks its keys and removes entries whose entity no longer exists in the client level (via an injected `IntPredicate`). This single check covers despawn, death, chunk unload, and dimension changes without coupling `BaseVillager` (or any entity class) to renderer state.

Lives in the `@ClientSessionScope` graph — the whole registry resets on client logout, which is what we want.

---

## Debug pose overlay

**Files:** [DebugPoseOverride.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/debug/DebugPoseOverride.java), [DebugPoseOverlayScreen.java](../../src/main/java/dev/breezes/settlements/presentation/ui/debug/DebugPoseOverlayScreen.java), [SettlementsKeyMappings.java](../../src/main/java/dev/breezes/settlements/presentation/ui/keybindings/SettlementsKeyMappings.java)

An in-game authoring aid for dialing in pose values without a rebuild. Press **`J`** (the `OPEN_DEBUG_POSE_OVERLAY` keybind) to open a slider screen; the [DebugPoseOverlayClientGameEvents](../../src/main/java/dev/breezes/settlements/bootstrap/event/DebugPoseOverlayClientGameEvents.java) handler opens it on the consumed key press.

`DebugPoseOverride` is a `@ClientScope` singleton holding `volatile` slider values. While enabled, [applyTo(frame)](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/debug/DebugPoseOverride.java:64) overlays four targets onto whatever the animator sampled — `ARMS_CROSSED_ROTATION`, `ARMS_CROSSED_TRANSLATION`, `HEAD_ROTATION_OVERRIDE`, `BODY_ROTATION` — via `frame.overlay(...)`. The renderer calls it post-sample / pre-`setupAnim` ([SettlementsVillagerRenderer.java:74](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:74)), so it is zero-cost when disabled (early return) and overlays every visible villager when on.

The screen enables the override on `init()` and disables it on `onClose()`, and its sliders report values in the **same units the `*Poses.java` factories use** (degrees for rotation, world units for translation). The authoring loop is **drag → read → paste**: tune a slider, read the number off its label, paste it into a pose factory. It does not touch per-arm, straight-arm, leg, root, or face targets, and it does not change `ArmConfiguration` — it is a crossed-arms + head + body bring-up tool.

---

## DI wiring

Two modules carry the framework's bindings; both are client-side.

### `ClientAnimationModule`

**File:** [ClientAnimationModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java)

- `@Binds AnimationResolver` ← `DefaultAnimationResolver`.
- `@Provides @ClientSessionScope AnimationLibrary` — the catalog, currently the eight entries listed under [the factory inventory](#the-canonical-example-and-the-factory-inventory).

`AnimationLibrary` is `@ClientSessionScope` so that future hot-reloads or session-scoped overrides have somewhere to hang. The provider returns a fresh `InMemoryAnimationLibrary` per session; the underlying `KeyframeAnimation` instances are immutable and safely shared.

### `ClientAttachmentModule`

**File:** [ClientAttachmentModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java)

- `@Binds @IntoSet AttachmentProvider` ← `EquipmentAttachmentProvider` (multibinding so additional providers can be added without touching the renderer).
- `@Provides @ClientScope SlotAnchorRegistry` ← `InMemorySlotAnchorRegistry.defaults()`.
- `@Provides @ClientScope SocketRegistry` ← `InMemorySocketRegistry.defaults()`.
- `@Provides @ClientScope AttachmentDisplayProfileRegistry` ← `InMemoryAttachmentDisplayProfileRegistry.defaults()`.

These are `@ClientScope` (app-lifetime), not `@ClientSessionScope`, because the rig and profile data does not change per-session. The `DebugPoseOverride` singleton is also `@ClientScope`, exposed on `ClientComponent`.

### Model & renderer registration

**File:** [ClientModEvents.java](../../src/main/java/dev/breezes/settlements/bootstrap/event/ClientModEvents.java)

- The entity renderer is registered in `registerEntityRenderers()`: `EntityRenderers.register(EntityRegistry.BASE_VILLAGER.get(), SettlementsVillagerRenderer::new)`.
- The model's `LayerDefinition` is registered on `RegisterLayerDefinitions`: `event.registerLayerDefinition(SettlementsVillagerModel.LAYER, SettlementsVillagerModel::createBodyLayer)`, where `LAYER` is `settlements:base_villager#main`. The renderer bakes it with `context.bakeLayer(SettlementsVillagerModel.LAYER)`.

---

## Adding a new animation

A new animation is mostly data. The shape mirrors the behavior-system pattern: define poses → define the animation → register → wire the behavior.

1. **Choose the archetype.** Pick from [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java). Add a new value (at the end — ordinal is the wire format) only if the motion is genuinely new; "swing a hammer" is `SWING_HEAVY`, not `HAMMER_SWING`.
2. **Choose the item category.** Pick from [ItemCategory](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java). Add a new category only if the classifier needs to distinguish a new shape of item.
3. **Author the poses.** Create a `*Poses.java` factory colocated with related animations. Mirror [SwingPoses](../../src/main/java/dev/breezes/settlements/domain/animation/SwingPoses.java): `@AllArgsConstructor(access = AccessLevel.PRIVATE)`, `public static final Pose` constants, private factory helpers. Reuse `AnimationTargets` constants — prefer the existing scaffolding (straight-arm, leg, root, face targets) over inventing new ones.
4. **Author the animation.** Create a `*Animations.java` factory in the same package. Each animation is one `public static KeyframeAnimation foo()` built with `KeyframeAnimation.fromPoses()`. Set `.arms(...)` if the motion needs a non-default arm configuration. Tick constants for impact frames live as `private static final int` fields and are imported by server behaviors for damage timing.
5. **Register in the library.** Add an entry to [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:30): `AnimationKey.of(archetype, category)`.
6. **Wire the behavior.** Use `villager.triggerMotion(archetype)` for a one-shot (`ONCE`) action or `villager.setMotion(archetype)` for a sustained (`LOOP`) state, at the relevant phase transitions. Restore `setMotion(IDLE)` on completion and interruption — see [HarvestMelonBehavior](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/farming/HarvestMelonBehavior.java:164) for the one-shot pattern.
7. **Test.** Build green; the pose/track/animation primitives have unit-test coverage. Use the [debug pose overlay](#debug-pose-overlay) (key `J`) for fast in-game calibration; final verification is visual.

---

## Adding a one-handed or asymmetric gesture

The split-arm rig makes mixed poses (one arm folded, one extended) a data change. To author, say, a wave or a totem-ponder:

1. Build the animation with the matching arm configuration, e.g. `.arms(ArmConfiguration.LEFT_CROSSED_RIGHT_STRAIGHT)`.
2. Drive the **straight** target for the acting arm (`ARM_STRAIGHT_RIGHT_*`) and leave the resting arm alone — it stays crossed automatically because the config keeps `arm_crossed_left` visible.
3. The held item follows: with the right arm straight, `MAIN_HAND` resolves through `socketFor(STRAIGHT)` to `HAND_RIGHT` and rides the straight-arm socket, no extra wiring.

The center-bar half-caps handle the seam where the still-crossed arm terminates, so you do not need to author geometry — only the arm config and the acting-arm targets.

---

## Adding a new socket

1. **Add the empty in Blockbench.** Create a cube-less group at the attach point and re-export the rig so the bone exists in `createBodyLayer()`.
2. **Add the id.** New value in [SocketId](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java), and a corresponding [ModelPartRef](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java) value for the bone.
3. **Add the bone-chain case.** Add the `ModelPartRef` to the switch in [SettlementsVillagerModel.applyBoneTransform](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:256), listing the full ancestor chain from `root` to the new empty.
4. **Register the socket.** New `Socket.builder()` entry in [InMemorySocketRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java:24). Leave `localTranslation` at `Vec3.ZERO` (the empty positions it) and tune `localRotation` for item orientation. `inheritsBoneTransform` defaults `true`; flip false only for free-floating world anchors.
5. **Bind a slot (if it carries an item).** Point an `AttachmentSlot` at the new `SocketId` in [InMemorySlotAnchorRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java:25), via `socket` and/or `straightSocket`.
6. **Calibrate.** Tune the empty in Blockbench and the `localRotation` in-game until the rest pose matches the intent.

No renderer changes are needed beyond the bone-chain case — the render path resolves through the socket registry by id.

---

## Adding a new animation target

ModelPart targets and slot targets follow different patterns.

**Model-part target:**

1. Add a `public static final AnimationTarget<V>` to [AnimationTargets](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java). Choose the value type, neutral, interpolator, and intended policy.
2. Apply the target in [SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:296) — read `frame.get(YOUR_TARGET)` and apply additively (`applyRotation`/`applyTranslation`) or absolutely (`applyAbsoluteRotation`) to the relevant `ModelPart`. Match the policy enum you declared.
3. If the target is absolute and needs to distinguish "no track" from "neutral track", guard with `frame.has(YOUR_TARGET)`.

**Slot target:**

1. Add a factory method to [SlotTargets](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java) using the `computeIfAbsent` interning pattern. Pick neutral, interpolator, policy.
2. Read the target at the appropriate site in [AttachmentRenderLayer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java).

In both cases, the policy enum is documentation for the next reader — the runtime behavior is whatever your consumer code does.

---

## Deferred work

Designed-for but not yet implemented:

- **Animation events** (`onTick(int, AnimationEvent)`, `ClientAnimationEventListener` Dagger multibinding). Intended for client-only visual flair (camera shake, ambient particle bursts, transient prop attach/detach). Gameplay timing stays server-side and reads tick constants directly from the animation file — there is no plan for a server-side event bus.
- **Animation consumers for the scaffolding targets.** The straight-arm/individual-arm targets, leg overrides, root motion, and the provisional face block exist but have no authored clips yet. The face ids/axes may change once the first real face animation validates them.
- **A two-handed (both-hands-on-one-item) socket.** Blocked pending a dedicated socket empty in the Blockbench rig + re-export; once added it is ~one `SocketId` + `ModelPartRef` + bone-chain case + registry entry, like the feet socket. No marker exists in code yet.
- **Additional equipment slots** (`BELT_LEFT`, `BELT_RIGHT`, `BACK`, etc.). Drop in as `EquipmentSlot`-style values plus socket bindings.
- **Datapack-loaded animations and sockets.** The `ResourceLocation` ids and registry indirection are ready for it; the loader is not built.
- **Quaternion target + slerp interpolator.** Add when a motion wraps past 180° or combines three axes near gimbal angles. Current motions stay within Euler-safe ranges.
- **Cosmetic / transient attachment providers.** Multibinding is ready; only `EquipmentAttachmentProvider` is implemented, and `ModelContent` / `BillboardContent` are still TODO stubs in the render layer.
- **`AnimationTargetPolicy` enforcement.** Currently intent metadata only — see [the policy convention](#the-policy-convention--intent-metadata-not-enforcement). If a future target consumer needs to choose application style dynamically, route through the policy enum then.

### Now implemented (was deferred in v1)

- **Per-arm bones and sockets** (the vindicator-style split-arm rig) — shipped as the `arm_*` bones, `HAND_LEFT`/`HAND_RIGHT` sockets, `ARM_STRAIGHT_*` / `ARM_CROSSED_*` targets, and the `ArmConfiguration` discrete render state. The slot/socket indirection is what made this additive rather than a rewrite.
