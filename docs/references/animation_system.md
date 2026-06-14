# Animation System v3

This document covers how villager animations are authored, composited per-villager, and applied during rendering on the custom Blockbench rig.
For the Dagger fundamentals that underpin the wiring, see [Dagger Guide](dagger_guide.md).
For server-side behavior wiring (which drives the motion plane), see [Behavior System](behavior_system.md).
For the Blockbench → track export workflow, see [Animation Import](animation_import_blockbench.md).

> **What changed since v2.** The single replaceable animation slot is gone. A villager's pose is now the composite of a small **layer stack** that a frame compositor folds bottom-to-top every frame ([LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java)). One root change spawned several others:
> - **The policy enum is now executable.** [AnimationTargetPolicy](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargetPolicy.java) (`ADDITIVE` / `MULTIPLICATIVE` / `ABSOLUTE`) is consumed at runtime by [AnimationTarget.compose](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java:43) — no longer inert metadata. The per-type add/scale arithmetic lives in a sibling [TargetArithmetic](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetic.java), keeping `Interpolator` a pure lerp.
> - **One-shots return to idle on their own.** A transient action layer auto-pops when its clip finishes; behaviors no longer restore `IDLE` per phase. This also fixes the "sticky arms" bug — the arm config falls through to the base when the action pops.
> - **Locomotion has a home.** Four authored gait clips (stroll/walk/jog/run) ride the base layer, selected by a synced gait intent and phased by `limbSwing`. The hardcoded cosine leg swing was removed.
> - **Idle ambience.** A client-only Idle-Life channel adds breathing, blinking, and sporadic fidgets.
> - **`LoopMode` collapsed** to `ONCE` / `LOOP` / `PING_PONG` (`HOLD_LAST` dropped — lifetime is a layer property now).
> - **Authoring is tracks, not poses.** `Pose`, `PoseAnimationBuilder`, and every `*Poses.java` were deleted; clips are built directly from tracks via [KeyframeAnimation.fromTracks()](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:26).
> - **Arm configuration is a keyframed timeline** now, so a single clip can snap from one arm config to another mid-animation.
> - **The in-game debug pose overlay was retired** (the slider screen, the `DebugPoseOverride`, and the `J` keybind are gone).
>
> The three-plane conceptual model (Motion / Loadout / Presentation) survives; the Motion plane is now realized by the layer compositor, and a fourth **Locomotion** plane was added beside it.

---

## Overview

The system still separates concerns into orthogonal planes, each owned by a different layer, on a different lifetime, never encoding another:

| Plane | What it describes | Authority | Lifetime |
|---|---|---|---|
| **Motion** | What discrete motion the villager is *expressing* (e.g. `SWING_HEAVY`), plus a generation counter so the same motion can replay | Server intent, client composites | Transient — changes per behavior phase |
| **Locomotion** | Which authored *gait* the villager moves with (`STROLL`/`WALK`/`RUN`/`SPRINT`), phased and weighted by real ground speed | Server intent (gait byte), client phases | Persistent — under the action |
| **Loadout** | What items / cosmetics / props the villager *carries* and where | Server | Persistent |
| **Presentation** | How a given (slot, item-category) pair *renders*, and where it attaches on the rig | Client config | Static — registry lookup |

The render layer is the single point where the planes meet. **No plane encodes another:** an animation never names an item, a slot never names a behavior, a gait never names a task. This keeps the surface area small as new tools, cosmetics, and motions land.

A second guiding rule: **the renderer iterates attachments — it does not special-case "the main hand."** Every visible thing on the villager is a [RenderableAttachment](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java) with an anchor, content, and category. The render path is uniform.

One cross-cutting concern rides on top: **arm configuration** (crossed vs. straight, per arm). It is a discrete render state, now keyframed along the animation timeline and resolved from the live layer stack. It gets its own section because it spans the model/presentation boundary.

---

## The layer compositor

**Files:** [LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java), [AnimationLayer](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java), [AnimationLayerRole](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayerRole.java)

A villager's pose each frame is the bottom-to-top composite of a small, mostly-fixed stack. The bottom always exists; the top is transient. Because the stack always has a base to fall through to, "return to idle", "un-stick the arms", and "host the gaits" all became the same emergent behaviour.

```
        ┌─ Action ───────── transient one-shot / sustained loop (chop · cast · eat …)   synced
 fold   ├─ Idle-Life ────── breathe · blink · fidget — fades out as the gait ramps in   client-only
   ↑    ├─ Locomotion ───── stroll ↔ walk ↔ jog ↔ run, weighted by limbSwingAmount       synced gait byte
        └─ Base ─────────── the resolved IDLE clip; empty at rest, the floor to fall to   client-only
```

**Only `BASE` and `ACTION` are concrete [AnimationLayer](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java) entries** in the stack's `EnumMap`. Idle-Life and Locomotion are injected animator *components* ([IdleLifeAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimator.java), [LocomotionAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimator.java)) that the stack folds inline at the right depth. The `AnimationLayerRole` enum still names all three (`BASE`, `IDLE_LIFE`, `ACTION`) so future persistent layers slot in by precedence, but `IDLE_LIFE` is not a map entry — it is the injected animator.

### `AnimationLayer` — a self-contained player

Each layer owns its own crossfade (a current and an outgoing animation plus their start times), lifted straight out of the old single-slot animator:

- **Lifetime is a layer property, not a clip property.** `AnimationLayer.persistent(...)` ([:21](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:21)) and `AnimationLayer.transientAction(...)` ([:25](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:25)) differ only in whether the layer auto-pops. `replace(animation, gameTime, transient)` ([:29](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:29)) swaps current → outgoing and adopts the *new push's* lifetime — a one-shot replacing a sustained loop must still auto-pop, and a sustained loop replacing a one-shot must persist.
- `sample(gameTime, partialTicks)` ([:39](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:39)) returns the current frame, blending the outgoing into it via `frame.blendTo(...)` while a crossfade is in progress, then dropping the outgoing reference once blend progress hits `1.0`.
- `weight(gameTime, partialTicks)` ([:54](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:54)) is always `1.0` for a persistent layer; for a transient layer it is `1.0` until the duration is reached, then fades linearly to `0` across `blendOutTicks`.
- `isExpired(...)` ([:72](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:72)) is true once a transient layer is past `duration + blendOutTicks`. Persistent layers never expire.
- `activeArmConfiguration(...)` ([:81](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java:81)) samples the clip's arm-config timeline at the current elapsed tick (see [Arm configuration](#arm-configuration)).

### `LayerStack` — fold, auto-pop, and resolve arm config

`LayerStack.sample(gameTime, partialTicks, locomotionContext)` ([:64](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:64)) is the heart of the system. Per frame it:

1. **Auto-pops finished actions** first via `expireFinishedActions(...)` ([:148](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:148)) — if the action layer `isExpired`, it is removed, revealing whatever is underneath. This is what restores idle without behavior code.
2. **Folds bottom → top** with `AnimationFrame.composeOver(...)`, in the order **Base → Locomotion → Idle-Life → Action**:
   - the `BASE` layer (the resolved `IDLE` clip — empty at rest),
   - then the locomotion frame at weight `1.0` (the gait animator pre-weights itself by `limbSwingAmount`, so it contributes nothing at a standstill),
   - then the idle-life frame at weight `1 − locomotionWeight` (ambience recedes as the gait ramps in, so the breather's body bob never stacks onto the gait's),
   - then the `ACTION` layer at its own fading `weight(...)`.

> Sampling **mutates**: it auto-pops finished actions and advances the idle-life timers. It is a deliberate render-thread side effect — call it once per frame from the renderer, not as a pure query.

`triggerAction(animation, gameTime)` ([:38](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:38)) pushes/replaces a **transient** action; `setSustainedAction(...)` ([:47](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:47)) a **persistent** one; `clearAction()` ([:56](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:56)) removes it outright (used for `IDLE`).

`armConfiguration(...)` ([:104](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:104)) walks the stack **top-down** and takes the first layer that owns an active arm config: Action → Idle-Life fidget → Base → Locomotion gait → fall back to `ArmConfiguration.BOTH_CROSSED`. Arm config is a non-blendable snap, so the first owner found is taken outright. When the action layer pops, there is nothing overriding the config anymore, so it falls through to the resting `BOTH_CROSSED` automatically — **this is the sticky-arms fix.**

### Compositing — the policy is the merge rule

**File:** [AnimationFrame.composeOver](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java:76)

`composeOver(over, weight)` folds one frame onto another. For each target present in `over`, the merge is delegated to [AnimationTarget.compose](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java:43), which dispatches on the target's `policy`:

| Policy | `compose(base, over, weight)` |
|---|---|
| `ADDITIVE` | `base ⊕ (over ⊖ neutral) · weight` — sum the delta-from-neutral, scaled by weight. A missing target reads neutral ⇒ no-op, so additive layers are always safe to apply. |
| `MULTIPLICATIVE` | `base · lerp(1, over, weight)` — e.g. slot scale. |
| `ABSOLUTE` | `lerp(base, over, weight)` for continuous types; step types snap to `over`. Topmost present wins. |

The per-type arithmetic (`add`, `subtract`, `scale`, `multiply`) lives in [TargetArithmetic](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetic.java) with standard instances in [TargetArithmetics](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetics.java) (`FLOAT`, `VEC3`, `VECTOR3F`); step types only ever travel the `ABSOLUTE` path and have no arithmetic. Each `AnimationTarget` resolves its arithmetic by value type when one is not supplied. This is the same math the model's `applyAnimationFrame` already hardcoded for a single frame — V3 lifts it into the domain and generalizes it across N layers, where it is unit-tested Minecraft-free.

---

## The custom rig

**File:** [SettlementsVillagerModel.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java)

The villager is a custom Blockbench model (`HierarchicalModel` implementing `HeadedModel`, `VillagerHeadModel`), authored in `assets/settlements_villager.bbmodel` and exported to the `createBodyLayer()` mesh ([:139](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:139)). It is an **illager-style rig** — single-segment limbs (no elbows or knees by design) and crossed arms modelled as separate geometry from straight arms.

The bone hierarchy (constructor at [:95](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:95)):

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

- **Crossed arms are split into five pieces.** `arm_crossed_left` / `arm_crossed_right` are the two arms; `arm_crossed_center_whole` is the full bar where they meet; `arm_crossed_center_left` / `arm_crossed_center_right` are half-caps. Which are visible is decided each frame from the [arm configuration](#arm-configuration) — this is what lets one arm uncross while the other stays folded without a seam.
- **Legs branch off `villager` directly, not through `torso`.** Every socket bone-walk that reaches the feet skips `torso` ([applyBoneTransform:278](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:278)).

The model is registered as `settlements:base_villager#main` ([SettlementsVillagerModel.LAYER:42](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:42)) in [ClientModEvents](../../src/main/java/dev/breezes/settlements/bootstrap/event/ClientModEvents.java) — see [Model & renderer registration](#model--renderer-registration).

---

## End-to-end flow

A single frame of villager rendering walks all the planes:

1. **Behavior sets motion intent** on the server. [HarvestMelonBehavior:164](../../src/main/java/dev/breezes/settlements/application/ai/behavior/usecases/villager/farming/HarvestMelonBehavior.java:164) calls `villager.triggerMotion(AnimationArchetype.SWING_HEAVY)` for a one-shot swing; continuous states call `villager.setMotion(...)`. Navigation behaviors set the gait intent through the navigation manager.
2. **The synced bytes update.** [BaseVillager.java:115](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:115) declares `DATA_MOTION_ARCHETYPE` + `DATA_MOTION_GENERATION`, and [:125](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:125) declares `DATA_LOCOMOTION_NAVIGATION_TYPE` — all bytes vanilla broadcasts on change.
3. **The renderer polls.** [SettlementsVillagerRenderer.getOrUpdateAnimator](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:86) reads `getMotion()` / `getMotionGeneration()` and compares both to the animator's last-seen values; it hands the gait byte to the model via `prepareAnimation(...)`.
4. **The animator updates the stack.** [VillagerAnimator.onMotionChanged](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:39) resolves the new animation and routes it to the action layer — `clearAction()` for `IDLE`, `triggerAction(...)` when the generation bumped, else `setSustainedAction(...)`.
5. **The resolver picks an animation.** [DefaultAnimationResolver](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java) looks up `(archetype, mainHandCategory)` in the [AnimationLibrary](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLibrary.java) with a fallback chain (exact → archetype × `GENERIC` → idle).
6. **The model samples and applies the composited frame.** In `setupAnim` ([:230](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:230)) it builds a `LocomotionAnimationContext` from `limbSwing` / `limbSwingAmount`, calls `animator.sample(...)` for one folded `AnimationFrame`, reads the animator's `currentArmConfiguration(...)`, then `applyAnimationFrame()` writes targets onto the bones.
7. **The attachment layer reads slot-flavored targets and resolves the socket** ([AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79)).
8. **The behavior restores `IDLE` only at teardown** — one-shots return to idle on their own via auto-pop; behaviors set `setMotion(IDLE)` in `onBehaviorStop` for an honest level-state, not per phase.

The animator and the behavior tick at the same rate against the same `triggerMotion(...)` start point, so timing constants live in the animation file and are imported directly by the server behavior when it needs to coordinate damage windows or sounds. This is a coordination contract, not duplication — there is no event bus between the two.

---

## Motion plane

Motion is named in **archetypal** terms, not items or tasks. A butcher swinging an axe and a farmer breaking a melon both use `SWING_HEAVY`; the item taxonomy (loadout plane) disambiguates which clip plays.

### `AnimationArchetype`

**File:** [AnimationArchetype.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java)

A small enum of motion intents — `IDLE`, `SWING_HEAVY`, `CAST`, `REEL_IN`, `REEL_OUT`, `POINT`, `INTERACT`, `SURVEY_WITH_SPYGLASS`, `WRITE_TO_MAP`, `EAT`, `WAVE`, `PICK_UP`, `HARVEST`. Provides `toNetworkByte()` / `fromNetworkByte()` with safe fallback to `IDLE` on unknown values.

Encoded as `byte` (the ordinal) because one byte through `SynchedEntityData` is the cheapest possible sync surface. **Order is the wire format** — append new values, do not reorder.

### Two setters: continuous vs. one-shot

**File:** [BaseVillager.java:204-240](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:204)

The motion state is two synced bytes — the archetype and a **generation counter** — written through two methods, **unchanged in name and contract from v2**:

- `setMotion(AnimationArchetype)` ([:216](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:216)) — **idempotent / continuous.** Writes only the archetype byte; setting the same archetype repeatedly does not restart. Use it for sustained states (`IDLE`, the `REEL_OUT` jig-fight loop, the `EAT` loop) and for teardown (`setMotion(IDLE)`).
- `triggerMotion(AnimationArchetype)` ([:231](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:231)) — **one-shot.** Bumps `DATA_MOTION_GENERATION` *and* writes the archetype, forcing replay from frame 0 even when the archetype is unchanged. Use it for discrete actions (`SWING_HEAVY`, `INTERACT`, `CAST`, `REEL_IN`, `HARVEST`, `PICK_UP`, …). The counter is a `byte` and intentionally wraps `127 → -128`; only inequality matters.
- `getMotion()` / `getMotionGeneration()` — read back during render.

**How the two setters become layer lifetimes** ([VillagerAnimator.onMotionChanged:50](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:50)): the synced delta is mapped to the action layer — `IDLE` ⇒ `clearAction()`; a **generation bump** ⇒ `triggerAction(...)` (transient, auto-pops); same generation, new archetype ⇒ `setSustainedAction(...)` (persistent until cleared). So `triggerMotion` clips return to idle on their own, while `setMotion` loops persist until the behavior ends them — which is correct, since leaving a loop *is* a state change.

### Change detection is a poll, not an event

The renderer reads the synced bytes each frame and compares to the animator's `lastSeenArchetype` / `lastSeenGeneration`. This avoids event-subscription lifecycle issues during world load, at a cost of two cached byte reads per villager. Finishing a clip early on the client never desyncs behavior — nothing server-side branches on `getMotion()`; only the renderers read it.

---

## Locomotion plane

Locomotion is **four authored cyclic clips** — not one clip morphed along a blend axis. They are the moving state of the base layer; idle (speed 0) is its rest. Three independent signals drive it, and keeping them independent is the whole trick.

### `NavigationType` — the gait, by intent

**File:** [NavigationType.java](../../src/main/java/dev/breezes/settlements/domain/ai/navigation/NavigationType.java)

The gait enum is `NavigationType` (it doubles as the AI's movement-speed intent): `STROLL`, `WALK`, `RUN`, `SPRINT`, each carrying a `baseModifier` speed scalar. It provides `toNetworkByte()` / `fromNetworkByte()` (fallback `WALK`).

**Selection is gait intent, never speed.** Agility genetics scales movement speed, so a high-agility *stroll* can outpace a low-agility *run* — a speed threshold would mis-pick the gait. The AI sets the intent: [VanillaMemoryNavigationManager.navigateTo](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/navigation/VanillaMemoryNavigationManager.java:46) calls `villager.setLocomotionNavigationType(type)`, which writes the synced `DATA_LOCOMOTION_NAVIGATION_TYPE` byte ([BaseVillager.java:245](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java:245)). The renderer reads it back via `getLocomotionNavigationType()` and threads it through the model.

### `LocomotionAnimator` — select, phase, weight

**Files:** [LocomotionAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimator.java), [DefaultLocomotionAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultLocomotionAnimator.java), [LocomotionAnimationContext](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimationContext.java)

The context is a `record(NavigationType navigationType, float limbSwing, float limbSwingAmount)`. The animator's three jobs map one-to-one onto the three driving signals:

- **Select = gait.** `library.resolve(context.navigationType())` picks the clip.
- **Phase = `limbSwing`.** `animationTick(...)` ([:39](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultLocomotionAnimator.java:39)) maps one vanilla limb cycle to one authored clip cycle: `limbSwing · durationTicks / VANILLA_LIMB_SWING_CYCLE`. Sampling against accumulated distance, not game time, keeps footfalls planted across agility-modified speeds and removes foot-slide.
- **Weight = `limbSwingAmount`.** `weight(...)` ([:48](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultLocomotionAnimator.java:48)) returns `0` below a standstill epsilon, else `limbSwingAmount / FULL_WEIGHT_LIMB_SWING_AMOUNT` clamped to `[0,1]`. Because a working villager is ~stationary, its locomotion weight ≈ 0 while it acts, so gait arm-swing cannot collide with an action's arm targets — **which is exactly why per-bone masks stay deferred.**

`sample(...)` returns the chosen clip pre-weighted by `weight()`; the `LayerStack` then folds that frame at weight `1.0`. `activeArmConfiguration(...)` returns the gait's declared config only while weight > 0.

### The gait clips

**File:** [LocomotionAnimations.java](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimations.java)

Four `LOOP` clips of decreasing cycle length as cadence rises, built from tracks by a shared `gait(...)` helper (3-tick blend in/out, `BOTH_CROSSED` arms):

| `NavigationType` | Clip factory | Duration |
|---|---|---|
| `STROLL` | `stroll()` | 24 ticks |
| `WALK` | `walk()` | 20 ticks |
| `RUN` | `jog()` | 16 ticks |
| `SPRINT` | `run()` | 12 ticks |

> The clip factory names (`jog`/`run`) read one notch faster than the enum value they back (`RUN`/`SPRINT`) — the mapping lives in [ClientAnimationModule.locomotionAnimationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:80). The four clips drive legs via `LEG_*_ROTATION_OVERRIDE` (`ABSOLUTE`), torso via `BODY_ROTATION`/`BODY_TRANSLATION`, arm swing via `ARMS_CROSSED_ROTATION`, and a nose bob via `NOSE_*`, with rotation amplitudes and body bob scaling up from stroll to run.

The **hardcoded cosine leg swing was removed**: `setupAnim` now zeroes leg rotation at standstill ([:247](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:247)) so a second procedural gait can't fight the distance-phased locomotion layer. Because the leg targets are `ABSOLUTE` and the locomotion weight fades to zero at a standstill, the legs rest at neutral when the villager is still.

---

## Idle-Life ambience

**Files:** [IdleLifeAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimator.java), [DefaultIdleLifeAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultIdleLifeAnimator.java), [IdleLifeAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimations.java), [IdleLifeAnimationContext](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimationContext.java)

A **client-only, additive** channel that gives idle villagers the "always alive" quality. It adds nothing to the sync surface; the `IdleLifeAnimator.NONE` no-op is used wherever ambience isn't wanted (and by `LayerStack`'s convenience constructor). The context is a `record(int entityId, long gameTime, float partialTicks, boolean actionActive)`.

`DefaultIdleLifeAnimator.sample(...)` ([:35](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultIdleLifeAnimator.java:35)) composes three sub-channels over each other:

- **Base idle** — a seamless `LOOP` breather (gentle `BODY_TRANSLATION` bob + a little `NOSE_ROTATION`).
- **Blink** — a 6-tick `ONCE` clip on the eyelid targets, fired on a randomized 50–140 tick timer.
- **Fidget** — a short additive gesture chosen by a **fidget selector** ([advanceFidget:93](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultIdleLifeAnimator.java:93)): a 120–300 tick timer with weighted-random, **no-repeat** selection ([nextFidgetIndex:113](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultIdleLifeAnimator.java:113)), seeded by entity id (`entityId · 31 + 17`) so villagers desync. It only fires when `actionActive()` is false — i.e. when the action layer is empty.

Two fidgets ship today: `glanceAround()` (pupil + body yaw) and `adjustSleeves()`, which uses the **arm-config timeline** to switch to `LEFT_CROSSED_RIGHT_STRAIGHT` at tick 0 and snap back to `BOTH_CROSSED` at tick 42.

> **First cut, with stubs.** The idle-life clips are explicitly placeholder stubs ([IdleLifeAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimations.java) header comment). Two channels from the original design are **not yet wired**: the **expression channel** (face presets on `MONOBROW`/`MOUTH`) and the **`Mood`** value that was meant to bias which expression/gesture is chosen. The face targets exist (see [target catalog](#target-catalog)) but no clip drives `MONOBROW`/`MOUTH`. Fidget gating is on "action empty" only — there is no speed gate (idle-life is faded by locomotion weight in the stack instead).

The stack composites idle-life at weight `1 − locomotionWeight`, so the ambience cleanly recedes the moment a gait takes over.

---

## Arm configuration

The split-arm rig needs to know, per arm, whether to show **crossed** or **straight** geometry. That choice cannot crossfade — geometry visibility is binary — so it is a **discrete render state snapped from the live stack**, not a blended value.

### `ArmPose` and `ArmConfiguration`

**Files:** [ArmPose.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmPose.java), [ArmConfiguration.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmConfiguration.java)

`ArmPose` is `{ CROSSED, STRAIGHT }`. `ArmConfiguration` is a `record(ArmPose left, ArmPose right)` with four constants: `BOTH_CROSSED` (default), `BOTH_STRAIGHT`, `LEFT_CROSSED_RIGHT_STRAIGHT`, `RIGHT_CROSSED_LEFT_STRAIGHT`.

### It is keyframed along the clip, then resolved from the stack

**New in v3:** arm configuration is a **timeline**, not a single per-clip value. [ArmConfigurationTimeline](../../src/main/java/dev/breezes/settlements/domain/animation/ArmConfigurationTimeline.java) is a sorted list of `(tick, ArmConfiguration)` keyframes that `sample(tick)` resolves with a step function (no blending — it snaps to the last keyframe at or before the tick). A clip can therefore change arm config mid-animation; `adjustSleeves` uses this to extend one arm and fold it back.

The builder seeds a keyframe at tick 0 from `.arms(...)` and adds more with `.armConfigurationAt(tick, config)` ([TrackAnimationBuilder:51](../../src/main/java/dev/breezes/settlements/domain/animation/TrackAnimationBuilder.java:51)). `KeyframeAnimation.armConfigurationAt(elapsed)` ([:63](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:63)) samples it; `armConfigurationOverride()` ([:59](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:59)) is the tick-0 value.

At render time, [LayerStack.armConfiguration](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java:104) resolves the *effective* config top-down across the stack (Action → Idle-Life → Base → Locomotion → `BOTH_CROSSED`). Because it reads the active layer, the geometry flips instantly at a transition and falls back automatically when the action pops.

### One config, three consumers

The snapped `ArmConfiguration` is the single source of truth for three things, which keeps them from desyncing:

1. **Per-arm geometry visibility** — [applyAnimationFrame:342](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:342) toggles `arm_crossed_*` vs. `arm_straight_*` and picks the center-bar piece (whole bar when both crossed, the matching half-cap when exactly one is).
2. **Which hand socket the item rides** — [AttachmentRenderLayer:91](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:91) reads `config.left()` for `OFF_HAND`, `config.right()` for `MAIN_HAND`, and asks the slot anchor for the matching socket.
3. **Which arm targets do anything** — the model applies all twelve arm targets unconditionally, but a target aimed at hidden geometry is a no-op, so authors drive only the targets matching the config they declared.

> **Mapping convention:** `OFF_HAND → left arm`, `MAIN_HAND → right arm`. Both the model's visibility logic and the render layer's socket selection follow it.

---

## Loadout plane

The loadout is the source of truth for what the villager is carrying. Today only `MAIN_HAND` and `OFF_HAND` are wired; the naming carries enough to add belt / back / cosmetic slots without renderer or animation refactors. *(Unchanged from v2.)*

### `AttachmentSlot` and `EquipmentSlot`

**Files:** [AttachmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentSlot.java), [EquipmentSlot.java](../../src/main/java/dev/breezes/settlements/domain/inventory/EquipmentSlot.java)

`AttachmentSlot` is the marker supertype the renderer indexes by. `EquipmentSlot` implements it with `MAIN_HAND` and `OFF_HAND`. Future cosmetic slot enums can be added as parallel implementations without touching the renderer.

### `RenderableAttachment` and `AttachmentContent`

**Files:** [RenderableAttachment.java](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java), [AttachmentContent.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentContent.java)

A `RenderableAttachment` is a `(slot, content, category)` triple. `AttachmentContent` is a sealed hierarchy: `ItemContent(ItemStack)` (the only path implemented today, rendered via `ItemInHandRenderer`), plus `ModelContent` and `BillboardContent` placeholders (TODO stubs in the render layer).

### `AttachmentProvider` — Dagger multibinding

**File:** [AttachmentProvider.java](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentProvider.java)

Providers contribute `RenderableAttachment` lists into the render path, bound `@IntoSet` in [ClientAttachmentModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java). The layer sorts them by `renderOrder()` then class name before iteration, so layering is deterministic. There is one today: [EquipmentAttachmentProvider](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/attachment/EquipmentAttachmentProvider.java), which reads `IVillagerEquipment` and emits one `ItemContent` per occupied slot.

### `ItemCategory` — taxonomy for held items

**File:** [ItemCategory.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java)

An open enum (`AXE`, `SWORD`, `MACE`, `PICKAXE`, `SHOVEL`, `HOE`, `FISHING_ROD`, `MAP`, `TORCH`, `LANTERN`, `SPYGLASS`, `GENERIC`, …) with a single `of(ItemStack)` classifier centralizing all item-type-to-category dispatch — the only place that branches on concrete item classes. This makes "swinging an iron axe ≈ swinging a diamond axe" automatic: both resolve to `(SWING_HEAVY, AXE)`. Material is irrelevant.

---

## Presentation plane

The presentation plane converts a `(slot, category)` pair into a sequence of transforms for the `PoseStack`. Three pieces feed it: the **socket** (where the slot lives on the rig), the **display profile** (how this category-of-item sits in this slot), and the **frame** (per-frame animation deltas). *(Unchanged from v2.)*

### `ModelPartRef` — addressing a bone by enum

**File:** [ModelPartRef.java](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java)

An enum naming the attachable bones: `ROOT`, `BODY`, `HEAD`, `ARMS`, `ARMS_CROSSED_SOCKET`, `ARM_STRAIGHT_RIGHT_SOCKET`, `ARM_STRAIGHT_LEFT_SOCKET`, `FEET_CENTER_SOCKET`. The domain references bones by this enum, never by `ModelPart` instances.

A `ModelPartRef` resolves to a **full ancestor bone chain**, not a single bone, in [SettlementsVillagerModel.applyBoneTransform](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:265). The render layer's pass enters model space *after* the model's own `root()` push/pop has closed, so reaching a socket means re-walking the hierarchy from the top, calling `translateAndRotate` on each ancestor in order. This walk reads the poses `setupAnim` already wrote, so the socket inherits everything an animation did to the arm.

### `Socket`, `SocketId`, registries, and `SlotAnchor`

**Files:** [Socket.java](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java), [SocketId.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java), [SocketRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketRegistry.java), [SlotAnchor.java](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java)

A `Socket` carries `id`, `bone` (a `ModelPartRef`), `localTranslation`/`localRotation`/`localScale`, and `inheritsBoneTransform` (default `true`). It is the same pattern Unity ships as **sockets**, Unreal as **skeletal sockets**, Source as **`$attachment`**. Because the position comes from a Blockbench empty, `localTranslation` is typically `Vec3.ZERO` and only the item's resting `localRotation` is tuned in code.

`SocketId` (enum, for compile-time exhaustiveness) has four values: `CROSSED_ARMS_CENTER` (the rest-pose hold), `HAND_RIGHT`, `HAND_LEFT`, and `FEET_CENTER`. `SocketRegistry.get(SocketId)` returns the registered socket or `Socket.identity(id)` as a safe fallback.

`SlotAnchor` is the per-slot binding: `socket` (default / crossed), `defaultDisplayContext`, and an optional `straightSocket`. `socketFor(ArmPose)` returns `straightSocket` when the arm is `STRAIGHT` (and one is set), else the default. The defaults bind both hands to `CROSSED_ARMS_CENTER` and add `MAIN_HAND → HAND_RIGHT`, `OFF_HAND → HAND_LEFT` — so the held item rides the crossed-arms center while folded and snaps to the per-hand socket the instant the animation declares that arm straight.

### `AttachmentDisplayProfile` — per `(slot, category)` transform

**Files:** [AttachmentDisplayProfile.java](../../src/main/java/dev/breezes/settlements/domain/presentation/AttachmentDisplayProfile.java), [InMemoryAttachmentDisplayProfileRegistry.java](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemoryAttachmentDisplayProfileRegistry.java)

The display profile (`translation`, `rotation`, `scale`, optional `displayContextOverride`) tunes just the deltas that matter, with lookup order exact `(slot, category)` → `(slot, GENERIC)` → identity. A single `(slot, GENERIC)` entry covers every uncategorized item; specific entries override it as you author them.

---

## Animation primitives

The pure-domain animation core lives under [domain/animation](../../src/main/java/dev/breezes/settlements/domain/animation/) and has no Minecraft renderer coupling — every type here is unit-testable. The tests in [src/test/.../domain/animation](../../src/test/java/dev/breezes/settlements/domain/animation/) cover sampling, easing, blending, loop modes, library fallback, and the per-policy compositor.

### `AnimationTarget<V>`

**File:** [AnimationTarget.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java)

A typed handle for one addressable animated property. Carries `id` (globally unique; equality/hashing use only this), `valueType`, `neutralValue` (the safe harbor during blend/compose when a side doesn't define the target), `interpolator` (the type-specific lerp), `arithmetic` (the [TargetArithmetic](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetic.java) used by additive/multiplicative compose — resolved by value type when unset), and `policy`.

`blend(from, to, t)` is the lerp. `compose(base, over, weight)` ([:43](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java:43)) is the **new** compositor entry point that dispatches on `policy` (see [Compositing](#compositing--the-policy-is-the-merge-rule)).

### Target catalog

**[AnimationTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java)** owns every model-part target — **28** of them. All rotations are `Vector3f` (radians, neutral zero); all translations are `Vec3` (neutral `Vec3.ZERO`).

| Group | Targets | Policy | Consumed by |
|---|---|---|---|
| **Arms (12)** | `ARMS_CROSSED_*`, `ARMS_STRAIGHT_*`, `ARM_CROSSED_LEFT/RIGHT_*`, `ARM_STRAIGHT_LEFT/RIGHT_*` (each `_ROTATION` + `_TRANSLATION`) | `ADDITIVE` | action clips, gait arm-swing, `adjustSleeves` |
| **Head (1)** | `HEAD_ROTATION_OVERRIDE` | `ABSOLUTE` | cartographer survey/mark |
| **Body (2)** | `BODY_ROTATION`, `BODY_TRANSLATION` | `ADDITIVE` | gait lean/bob, idle breather, harvest bend |
| **Nose (2)** | `NOSE_ROTATION`, `NOSE_TRANSLATION` | `ADDITIVE` | gait + idle secondary motion |
| **Legs (2)** | `LEG_LEFT/RIGHT_ROTATION_OVERRIDE` | `ABSOLUTE` | gait clips |
| **Root (2)** | `ROOT_TRANSLATION`, `ROOT_ROTATION` | `ADDITIVE` | (scaffolding — boss/emote) |
| **Face (7)** | `MONOBROW_TRANSLATION/ROTATION`, `MOUTH_TRANSLATION`, `EYELID_LEFT/RIGHT_TRANSLATION`, `PUPIL_LEFT/RIGHT_TRANSLATION` | `ADDITIVE` | eyelids/pupils by blink + glance; brow/mouth unused |

> **Scaffolding status changed.** In v2 the leg, root, and face blocks had no consumers. V3 now drives legs and body/nose from the gait clips, and eyelids/pupils from idle-life. Still unconsumed: the straight-arm/individual-arm targets, root motion, and the `MONOBROW`/`MOUTH` face targets — which remain flagged **PROVISIONAL** in source ([AnimationTargets.java:184](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java:184)) pending the first authored expression clip. Do not remove them as "unused"; prefer consuming them over inventing new targets.

**[SlotTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java)** — per-slot targets, interned via `ConcurrentHashMap` so repeated lookups return the same instance:

| Factory | Value type | Neutral | Policy |
|---|---|---|---|
| `translation(slot)` | `Vec3` | zero | `ADDITIVE` |
| `rotation(slot)` | `Vector3f` | zero | `ADDITIVE` |
| `scale(slot)` | `Float` | `1.0` | `MULTIPLICATIVE` |
| `visibility(slot)` | `Boolean` | `true` | `ABSOLUTE` |
| `displayContext(slot)` | `ItemDisplayContext` | `NONE` | `ABSOLUTE` |

### The policy convention — now executable

[AnimationTargetPolicy](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargetPolicy.java) (`ADDITIVE`, `MULTIPLICATIVE`, `ABSOLUTE`) is **consulted at runtime** by `AnimationTarget.compose` during stack folding — it is no longer inert intent metadata. Two things still apply the policy by hand and must stay consistent with the enum:

- **ModelPart targets** in [SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:337): arm/body/nose/root do `+=`; `HEAD_ROTATION_OVERRIDE` and the two leg overrides do `=`, each inside a `frame.has(...)` guard. *(This is the final apply onto `resetPose`; the cross-layer composition already happened in the domain.)*
- **Slot targets** in [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79): translation/rotation/scale layer onto the `PoseStack`; visibility early-returns; displayContext falls through.

### `Keyframe`, `AnimationTrack`, `KeyframeAnimation`

**Files:** [Keyframe.java](../../src/main/java/dev/breezes/settlements/domain/animation/Keyframe.java), [AnimationTrack.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTrack.java), [KeyframeAnimation.java](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java)

A `Keyframe<V>` is a tick offset, a value, and the easing curve to the *next* keyframe. An `AnimationTrack<V>` is one target plus a sorted keyframe list plus the interpolator. A `KeyframeAnimation` is a `ResourceLocation` id, a duration, a `LoopMode`, blend-in/out durations, an **`ArmConfigurationTimeline`**, and a list of tracks.

`sample(elapsedTicks)` resolves elapsed ticks against the loop mode, samples each track, and returns an `AnimationFrame`. [LoopMode](../../src/main/java/dev/breezes/settlements/domain/animation/LoopMode.java) is now `ONCE` (clamp/hold last frame), `LOOP` (wrap), `PING_PONG` (bounce) — **`HOLD_LAST` was dropped**; "ease into a pose and hold forever" is just a sustained layer whose `ONCE` clip clamps.

The id is a `ResourceLocation` to keep the door open for datapack-loaded clips and clip-bound sounds/particles.

### `AnimationFrame`

**File:** [AnimationFrame.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java)

Immutable map of `AnimationTarget<?> → Object`. `get(target)` returns the target's neutral if absent; `has(target)` distinguishes "explicitly absent" from "explicitly neutral" (used by the absolute head/leg overrides). Three combinators:

- `blendTo(other, t)` — crossfade. Each target's `from`/`to` defaults to neutral when missing, so partial animations cannot leave stale poses behind.
- `composeOver(over, weight)` — **the stack fold** (see [Compositing](#compositing--the-policy-is-the-merge-rule)). Per-target it delegates to `target.compose(...)`.
- `overlay(map)` — copy with given targets replaced wholesale (no blend).

Internally built frames use the package-private `ofOwned(...)` ownership-transfer factory to skip the defensive copy, since compositing builds and discards several frames per villager per frame.

### `Easing`, `Interpolator`, `TargetArithmetic`

**Files:** [Easing.java](../../src/main/java/dev/breezes/settlements/domain/animation/Easing.java), [Interpolator.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolator.java), [Interpolators.java](../../src/main/java/dev/breezes/settlements/domain/animation/Interpolators.java), [TargetArithmetic.java](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetic.java)

`Easing` is a `float → float` shape on normalized progress (`LINEAR`, `EASE_IN`, `EASE_OUT`, `EASE_IN_OUT`, `STEP`, `CUBIC`). `Interpolator<V>` is the **pure** type-specific lerp (`VEC3`, `VECTOR3F`, `FLOAT`, `BOOLEAN_STEP`, `ITEM_DISPLAY_CONTEXT_STEP`). `TargetArithmetic<V>` is the sibling for compositing: `add`, `subtract`, `scale`, `multiply` — implemented for `FLOAT`/`VEC3`/`VECTOR3F` in [TargetArithmetics](../../src/main/java/dev/breezes/settlements/domain/animation/TargetArithmetics.java); step types never need it (they only travel the `ABSOLUTE` path). Keeping arithmetic out of `Interpolator` was a deliberate split so the lerp interface stays minimal.

Quaternion rotation interpolation is still not implemented — the current set stays within Euler-safe ranges. Add a parallel `Quaternion` target + slerp when a motion wraps past 180° or combines three axes near gimbal angles.

---

## Authoring animations with tracks

Animations are authored as **tracks** — the pose-driven builder and the `*Poses.java` classes were removed in v3. Entry point: [KeyframeAnimation.fromTracks()](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java:26) returns a [TrackAnimationBuilder](../../src/main/java/dev/breezes/settlements/domain/animation/TrackAnimationBuilder.java).

Chain the metadata — `.id(...)`, `.durationTicks(...)`, `.loopMode(...)`, `.blendInTicks(...)`, `.blendOutTicks(...)`, `.arms(ArmConfiguration)` (the tick-0 arm config; defaults `BOTH_CROSSED`), and optionally `.armConfigurationAt(tick, config)` for a mid-clip arm-config snap — then add one `.track(AnimationTrack<?>)` per animated target. Each track is `AnimationTrack.<V>builder().target(...).keyframes(List.of(new Keyframe<>(tick, value, easing), ...)).build()`.

Varying segment speeds emerge from **tick density × easing curve**, not from any pose property: a long gap with `EASE_IN` is slow; a short gap with `EASE_OUT` is fast.

### The factory pattern and inventory

Animations are grouped into per-feature `*Animations.java` factory classes (`@AllArgsConstructor(access = PRIVATE)`, `public static KeyframeAnimation foo()` methods, degree-authoring helpers wrapping `RotationUtil.degrees(...)`). [LocomotionAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimations.java) is a clear worked example; tick constants for impact frames live as `private static final int` fields and are imported by server behaviors for damage timing.

The action library is wired in [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:49):

| `(archetype, category)` | Factory |
|---|---|
| `(SWING_HEAVY, GENERIC)` | `ChopAnimations.chopCrossedArms()` |
| `(CAST, FISHING_ROD)` | `FishingAnimations.cast()` |
| `(REEL_OUT, FISHING_ROD)` | `FishingAnimations.jigFight()` |
| `(REEL_IN, FISHING_ROD)` | `FishingAnimations.reelYank()` |
| `(INTERACT, GENERIC)` | `InteractAnimations.interact()` |
| `(SURVEY_WITH_SPYGLASS, SPYGLASS)` | `CartographerAnimations.surveyWithSpyglass()` |
| `(WRITE_TO_MAP, MAP)` | `CartographerAnimations.markMap()` |
| `(EAT, GENERIC)` | `EatingAnimations.eat()` |
| `(WAVE, GENERIC)` | `WaveAnimations.wave()` |
| `(PICK_UP, GENERIC)` | `PickUpAnimations.pickUp()` |
| `(HARVEST, GENERIC)` | `HarvestCropAnimations.harvestCrop()` |

The four legacy pose-authored clips (Cartographer / Eating / Fishing / Interact) were migrated to tracks during the cutover. `POINT` exists as an archetype but has no library entry yet (it resolves through the fallback chain to the empty idle).

---

## Resolution

A villager's current animation is chosen by `(archetype, item-category)`, with fallback to archetype-default to stay graceful for unknown items.

### `AnimationSelectionContext`, `AnimationKey`, `AnimationLibrary`

**Files:** [AnimationSelectionContext.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationSelectionContext.java), [AnimationKey.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationKey.java), [AnimationLibrary.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLibrary.java), [InMemoryAnimationLibrary.java](../../src/main/java/dev/breezes/settlements/domain/animation/InMemoryAnimationLibrary.java)

`AnimationSelectionContext` is a record carrying `mainHandCategory` (with `generic()` / `of(...)` factories); the record shape leaves room for future fields (off-hand, time of day, mood). `AnimationKey` is `(archetype, category)`. `AnimationLibrary.resolve(archetype, category)` uses a three-tier fallback in `InMemoryAnimationLibrary`:

1. Exact `(archetype, category)` — most specific.
2. `(archetype, GENERIC)` — archetype-default for unknown items.
3. `(IDLE, GENERIC)` — last resort; if absent, a built-in empty `idle` animation (zero duration, no tracks).

So material never matters, a missing item-specific variant degrades to the archetype-generic one, and new item categories never break anything.

### `AnimationResolver` and `DefaultAnimationResolver`

**Files:** [AnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationResolver.java), [DefaultAnimationResolver.java](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultAnimationResolver.java)

A thin adapter: `resolve(archetype, context)` pulls `mainHandCategory` out of the context and delegates to the library. It exists so future selection logic (read multiple slots, mood-driven variants, A/B by UUID) slots in without changing call sites.

---

## Runtime: `VillagerAnimator`

**File:** [VillagerAnimator.java](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java)

One animator per villager. It is now a thin owner of a [LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java) plus the resolver and the last-seen archetype / generation / context, constructed with an `AnimationResolver`, an `IdleLifeAnimator`, a `LocomotionAnimator`, and the entity id. It initializes the stack's base layer with the resolved `IDLE` clip. The crossfade machinery moved *into* the layer; the animator no longer holds outgoing animations itself.

### Motion change → routed to the action layer

`onMotionChanged(archetype, generation, context, gameTime)` ([:39](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:39)) early-returns if both archetype and generation are unchanged. Otherwise it resolves the animation and routes it: `IDLE` → `clearAction()`, a generation bump → `triggerAction(...)`, same generation → `setSustainedAction(...)`. Including the generation in the guard is what lets `triggerMotion` replay the same archetype.

### Equipment-sync lag correction

`tickContext(context, gameTime)` ([:75](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:75)) is the subtlety carried over from v2. `SynchedEntityData` (the motion bytes) arrives one frame before `ClientboundSetEquipmentPacket`, so the first frame after a swing may resolve against `mainHandCategory = GENERIC`. The renderer calls `tickContext` every frame the motion did *not* change; if the context differs and there is an active action, the animator re-resolves and `setSustainedAction(...)`s the corrected clip — without touching `lastSeenArchetype` / `lastSeenGeneration`, so it is not treated as a motion change.

### Sampling and arm config

`sample(gameTime, partialTicks, locomotionContext)` ([:115](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:115)) and `currentArmConfiguration(..., locomotionContext)` ([:105](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java:105)) both delegate to the `LayerStack`. The renderer reads both from the same up-to-date animator so the frame and the snapped geometry always come from one resolved state.

---

## Render layer composition

The render path applies a fixed composition order onto the `PoseStack`; each step adds a delta, nothing replaces. The exact site is [AttachmentRenderLayer.renderAttachment](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java:79). *(Unchanged from v2.)*

For each visible attachment (visibility checked first via the slot's `visibility` target):

1. **Socket selection** — reads the model's snapped `ArmConfiguration`, picks the arm for this slot, resolves `anchor.socketFor(armPose)` to a `Socket`.
2. **Bone world transform** — if `socket.inheritsBoneTransform`, replays the full ancestor bone chain so the attachment rides the arm through everything `setupAnim` did.
3. **Socket local transform** — the offset from the empty to the item.
4. **Display profile transform** — per-`(slot, category)` tuning.
5. **Animation slot deltas** — the frame's `SlotTargets.translation/rotation/scale` for this slot.
6. **`renderContent(...)`** — resolves the display context, then dispatches on `AttachmentContent` subtype.

### IDLE = the authored rest pose

A villager in `IDLE` with no active action composites to an empty `AnimationFrame` (base idle clip is empty, locomotion ≈ 0 at standstill, idle-life is additive ambience). Every model-part lookup returns neutral, every slot delta is zero, and the render reduces to the rig's authored crossed-arms rest with the held item on the calibrated `CROSSED_ARMS_CENTER` socket.

### Model parts: `setupAnim` and additive application

**File:** [SettlementsVillagerModel.setupAnim](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:230)

`setupAnim` first calls `resetPose` on every part, applies vanilla-style head yaw/pitch tracking and unhappy-villager head wobble, **zeroes the legs** (the gait layer owns leg motion now), then calls `resolveAnimationFrame(limbSwing, limbSwingAmount)` ([:312](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:312)) — which builds the `LocomotionAnimationContext`, samples the animator for one composited frame, and snaps the arm config — and finally `applyAnimationFrame()` ([:337](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:337)). The reset prevents frame-to-frame drift and cross-villager leaks, since the same model instance renders every villager.

`applyAnimationFrame` resolves per-arm visibility from the snapped config, then applies targets: the twelve arm targets additively and unconditionally; body/nose/root additively; `HEAD_ROTATION_OVERRIDE` and the two leg overrides absolutely, each guarded by `frame.has(...)` so a clip without the track leaves vanilla tracking untouched; face targets additively. The additive default preserves the "living, looking-at-you" quality — head tracking, the unhappy wobble, and idle ambience all still apply unless a clip explicitly takes absolute control.

The model holds the prepared-animation state set by the renderer (`animator`, `locomotionNavigationType`, `animationGameTime`, `animationPartialTicks`) plus the resolved frame and arm config; `clearPreparedAnimation()` ([:298](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:298)) resets all of it after each entity so a thrown render cannot leak state.

---

## Renderer orchestration

**File:** [SettlementsVillagerRenderer.java](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java)

Replaces the vanilla `VillagerRenderer` for `BaseVillager` (a `MobRenderer`). The vanilla `CrossedArmsItemLayer` is intentionally *not* registered; its job is done by `AttachmentRenderLayer`. Registered layers: `AttachmentRenderLayer`, `CustomHeadLayer`, `VillagerProfessionLayer`, and `SootOverlayRenderLayer`.

Per-frame, `render(...)` ([:58](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:58)):

1. Applies baby scale if needed.
2. `getOrUpdateAnimator(...)` ([:86](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:86)) — fetches the per-villager animator, polls `getMotion()` + `getMotionGeneration()`, calls `onMotionChanged(...)` when either changed, else `tickContext(...)` for the equipment-sync lag.
3. `model.prepareAnimation(animator, villager.getLocomotionNavigationType(), gameTime, partialTicks)` ([:70](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java:70)) — stashes the animator and gait byte onto the model.
4. `super.render(...)`, which triggers `setupAnim` (model samples the frame and snaps the arm config) and then iterates the layers.
5. In a `finally` block, `model.clearPreparedAnimation()` so nothing leaks to the next entity.

The frame the model resolved is exposed for tests via `currentFrame()`. The renderer reaches the Dagger graph through `SettlementsDagger.client()` / `clientSessionOrThrow()` because Minecraft owns renderer construction — the same workaround as `BaseVillager`.

> The v2 **debug pose overlay was removed** in the cutover. There is no longer a `DebugPoseOverride`, slider screen, or `J` keybind; in-game calibration is done by editing tracks and rebuilding (or in Blockbench), and the renderer no longer runs a post-sample override pass.

---

## Per-entity animator lifecycle

**File:** [ClientAnimatorRegistry.java](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java)

Per-villager `VillagerAnimator` instances live in a `ConcurrentHashMap<Integer, VillagerAnimator>` keyed by client entity id. `getOrCreate(villager)` ([:38](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java:38)) lazily creates on first render, wiring in a per-entity idle-life animator (`idleLifeAnimatorFactory.create(entityId)`) plus the shared locomotion animator and resolver. The registry itself is constructor-injected with those three dependencies.

Pruning is **periodic, not event-driven**: every `PRUNE_INTERVAL_LOOKUPS` (600) lookups the registry removes entries whose entity no longer exists in the client level. This single check covers despawn, death, chunk unload, and dimension changes without coupling any entity class to renderer state. It lives in the `@ClientSessionScope` graph, so the whole registry resets on client logout.

---

## DI wiring

Two client-side modules carry the framework's bindings.

### `ClientAnimationModule`

**File:** [ClientAnimationModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java)

- `@Binds AnimationResolver` ← `DefaultAnimationResolver`.
- `@Binds IdleLifeAnimatorFactory` ← `DefaultIdleLifeAnimatorFactory` (per-entity idle animators).
- `@Binds LocomotionAnimator` ← `DefaultLocomotionAnimator` (shared).
- `@Provides @ClientSessionScope AnimationLibrary` — the action catalog (the eleven entries above).
- `@Provides @ClientSessionScope IdleLifeAnimationLibrary` — `baseIdle` + `blink` + the fidget list.
- `@Provides @ClientSessionScope LocomotionAnimationLibrary` — the four `NavigationType → clip` entries.

All three libraries are `@ClientSessionScope` so future hot-reloads or session-scoped overrides have somewhere to hang; the underlying `KeyframeAnimation` instances are immutable and safely shared.

### `ClientAttachmentModule`

**File:** [ClientAttachmentModule.java](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java)

- `@Binds @IntoSet AttachmentProvider` ← `EquipmentAttachmentProvider`.
- `@Provides @ClientScope` for `SlotAnchorRegistry`, `SocketRegistry`, `AttachmentDisplayProfileRegistry` (the `InMemory*.defaults()`).

These are `@ClientScope` (app-lifetime), not session-scoped, because the rig and profile data does not change per-session.

### Model & renderer registration

**File:** [ClientModEvents.java](../../src/main/java/dev/breezes/settlements/bootstrap/event/ClientModEvents.java)

The entity renderer is registered in `registerEntityRenderers()`; the model's `LayerDefinition` is registered on `RegisterLayerDefinitions` for `settlements:base_villager#main`, and the renderer bakes it with `context.bakeLayer(SettlementsVillagerModel.LAYER)`.

---

## Sync surface

Append-only, one byte each, on the cheap `SynchedEntityData` surface:

| Field | Purpose |
|---|---|
| `DATA_MOTION_ARCHETYPE` | the current motion archetype |
| `DATA_MOTION_GENERATION` | bump to replay a one-shot |
| `DATA_LOCOMOTION_NAVIGATION_TYPE` | the gait intent (`NavigationType`) |

Base and Idle-Life add nothing to the wire — they are client-only.

---

## Adding a new animation

A new animation is mostly data. The shape mirrors the behavior-system pattern: define tracks → register → wire the behavior.

1. **Choose the archetype.** Pick from [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java); add a value (at the end — ordinal is the wire format) only if the motion is genuinely new.
2. **Choose the item category.** Pick from [ItemCategory](../../src/main/java/dev/breezes/settlements/domain/presentation/ItemCategory.java); add one only if the classifier needs a new shape of item.
3. **Author the clip.** Create or extend a `*Animations.java` factory. Build with `KeyframeAnimation.fromTracks()`, one `.track(...)` per animated target. Set `.arms(...)` (and `.armConfigurationAt(...)` for a mid-clip snap) if the motion needs a non-default arm config. Reuse `AnimationTargets` constants — prefer the existing scaffolding (straight-arm, root, face) over inventing new targets. Tick constants for impact frames live as `private static final int` fields imported by server behaviors.
4. **Register in the library.** Add an `AnimationKey.of(archetype, category)` entry to [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java:49).
5. **Wire the behavior.** Use `villager.triggerMotion(archetype)` for a one-shot (`ONCE`) action or `villager.setMotion(archetype)` for a sustained (`LOOP`) state. **Do not restore `IDLE` per phase** — a one-shot auto-pops back to idle on its own. Set `setMotion(IDLE)` only at behavior teardown (`onBehaviorStop`) and when leaving a sustained loop.
6. **Test.** Build green; the primitives have unit-test coverage. Final verification is visual.

---

## Adding a one-handed or asymmetric gesture

The split-arm rig makes mixed poses (one arm folded, one extended) a data change. To author a wave or totem-ponder:

1. Build the clip with the matching arm config, e.g. `.arms(ArmConfiguration.LEFT_CROSSED_RIGHT_STRAIGHT)` — or snap into and out of it mid-clip with `.armConfigurationAt(tick, config)` (as `adjustSleeves` does).
2. Drive the **straight** target for the acting arm (`ARM_STRAIGHT_RIGHT_*`) and leave the resting arm alone — it stays crossed because the config keeps `arm_crossed_left` visible.
3. The held item follows: with the right arm straight, `MAIN_HAND` resolves through `socketFor(STRAIGHT)` to `HAND_RIGHT`, no extra wiring.

The center-bar half-caps handle the seam where the still-crossed arm terminates, so you only author the arm config and the acting-arm targets.

---

## Adding a new socket

1. **Add the empty in Blockbench** at the attach point and re-export so the bone exists in `createBodyLayer()`.
2. **Add the id** — a new [SocketId](../../src/main/java/dev/breezes/settlements/domain/presentation/SocketId.java) value and a matching [ModelPartRef](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java).
3. **Add the bone-chain case** to the switch in [applyBoneTransform](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:265), listing the full ancestor chain from `root`.
4. **Register the socket** in [InMemorySocketRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySocketRegistry.java); leave `localTranslation` at `Vec3.ZERO` and tune `localRotation`.
5. **Bind a slot** (if it carries an item) in [InMemorySlotAnchorRegistry.defaults](../../src/main/java/dev/breezes/settlements/domain/presentation/InMemorySlotAnchorRegistry.java).
6. **Calibrate** in Blockbench and via `localRotation` until the rest pose matches intent.

No renderer changes are needed beyond the bone-chain case.

---

## Adding a new animation target

**Model-part target:**

1. Add a `public static final AnimationTarget<V>` to [AnimationTargets](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java). Choose value type, neutral, interpolator, and policy. Supply a `TargetArithmetic` only if the value type isn't covered by `TargetArithmetics.forValueType(...)`.
2. Apply it in [applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java:337) — `+=` for additive, `=` (guarded by `frame.has(...)`) for absolute. Match the policy you declared, because the cross-layer compositor will also honor it.

**Slot target:**

1. Add a factory to [SlotTargets](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java) using the `computeIfAbsent` interning pattern.
2. Read it at the appropriate site in [AttachmentRenderLayer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java).

The policy enum is now consulted during compositing, so set it to truly match the target's intended math.

---

## Deferred work

Designed-for but not yet implemented:

- **The Idle-Life expression channel + `Mood`.** Face targets (`MONOBROW`/`MOUTH`) exist but no clip drives them, and there is no mood value biasing fidget/expression selection. The current idle-life clips are stubs.
- **Per-bone masks.** Deferred while locomotion weight ≈ 0 during work (a villager is ~stationary when it acts, so gait sway can't collide with an action). Revisit when a genuine act-while-moving clip is authored.
- **Animation events** (`onTick`, a `ClientAnimationEventListener` multibinding) for client-only flair (camera shake, particle bursts). Gameplay timing stays server-side, reading tick constants directly.
- **The remaining scaffolding targets** — straight-arm/individual-arm targets and root motion have no authored consumers yet; the `MONOBROW`/`MOUTH` ids stay PROVISIONAL.
- **A two-handed (both-hands-on-one-item) socket** — pending a dedicated Blockbench empty; then ~one `SocketId` + `ModelPartRef` + bone-chain case + registry entry.
- **Additional equipment slots** (`BELT_*`, `BACK`) and **cosmetic / transient attachment providers** (`ModelContent` / `BillboardContent` are TODO stubs).
- **Datapack-loaded animations and sockets** — the `ResourceLocation` ids and registry indirection are ready; the loader is not built.
- **Quaternion target + slerp interpolator** — add when a motion exceeds Euler-safe range.
