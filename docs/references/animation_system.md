# Animation System

This document is the entrypoint for contributors changing villager animation, locomotion, arm presentation, or rendered
attachments. It explains ownership boundaries and invariants that are easy to violate across files. It intentionally
does not duplicate current enum members, animation inventories, keyframes, model hierarchy, or dependency-injection
bindings; use the linked source files and the searches
in [Finding current implementations](#finding-current-implementations) for those details.

For the Blockbench-to-track workflow, see [Animation Import](animation_import_blockbench.md).

For settling a placement or magnitude in a running game, see
[Animation and Model Tuning](animation_model_tuning.md).

---

## Start here

| If you are changing... | Start with |
|---|---|
| Layering, fades, or action lifetime | [LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java), [AnimationLayer](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java), [AnimationFrame](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java) |
| Motion selection or replay | [VillagerAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java), [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java), [BaseVillager](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java) |
| Authored locomotion | [DefaultLocomotionAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultLocomotionAnimator.java), [LocomotionAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/LocomotionAnimations.java) |
| Breathing, blinking, or fidgets | [DefaultIdleLifeAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultIdleLifeAnimator.java), [IdleLifeAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimations.java) |
| Clip authoring or target semantics | [KeyframeAnimation](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java), [AnimationTrack](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTrack.java), [AnimationTargets](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java) |
| Arm geometry | [ArmConfiguration](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmConfiguration.java), [ArmConfigurationTimeline](../../src/main/java/dev/breezes/settlements/domain/animation/ArmConfigurationTimeline.java), [SettlementsVillagerModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java) |
| Items, props, or sockets | [RenderableAttachment](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java), [Socket](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java), [AttachmentRenderLayer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java) |
| Umbrella carry, deploy, or the rain gate | [UmbrellaAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/UmbrellaAnimator.java), [UmbrellaCarryAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/UmbrellaCarryAnimations.java), [UmbrellaAttachmentProvider](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/attachment/UmbrellaAttachmentProvider.java) |
| Client registration | [ClientAnimationModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java), [ClientAttachmentModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java) |

---

## Mental model

Animation-related presentation is split into four independent planes:

| Plane | Question it answers | Authority |
|---|---|---|
| Motion | Which discrete action is the villager expressing? | Server intent, synchronized to clients |
| Locomotion | Which gait should play, at what phase and strength? | Server gait intent; client distance and render state |
| Loadout | Which items, props, and cosmetics exist in each attachment slot? | Server |
| Presentation | Which socket and local transform should render a slot/category pair? | Client registries |

Do not encode one plane inside another. An animation does not select an item, a socket does not select a behavior, and a
gait is selected by navigation intent rather than inferred from item or animation data.

A rendered pose is folded bottom-to-top:

```text
Umbrella     carry gesture and canopy, while the rain gate holds
Action       transient one-shot or sustained work pose
Idle-Life    breathing, blinking, fidgets
Locomotion   authored gait, weighted by movement
Base         resolved idle floor
```

Only Base and Action are
concrete [AnimationLayer](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java) entries.
Locomotion, Idle-Life, and Umbrella are animator components folded at their required depth
by [LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java). The umbrella composes
last because it must also survive the sleep pose, which replaces everything beneath it.

---

## Runtime flow

1. Server behavior writes motion intent
   through [BaseVillager](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/BaseVillager.java).
   Navigation writes gait intent separately.
2. Minecraft synchronizes the motion archetype, motion generation, and navigation type bytes.
3. [SettlementsVillagerRenderer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/SettlementsVillagerRenderer.java)
   obtains the
   per-entity [VillagerAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java),
   updates its synchronized state, and prepares the model.
4. [SettlementsVillagerModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java)
   resets the rig, applies vanilla look state, samples one animation frame, resolves arm configuration, and applies the
   frame.
5. [AttachmentRenderLayer](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentRenderLayer.java)
   renders every attachment from the resulting bone poses and slot targets.

One animator exists per client-side
villager. [ClientAnimatorRegistry](../../src/main/java/dev/breezes/settlements/infrastructure/rendering/animation/ClientAnimatorRegistry.java)
owns that lifecycle.

### Motion replay

Motion synchronization uses two values:

- the archetype identifies the action;
- the generation changes when the same one-shot must replay.

Use the continuous setter for level states that should not restart when written repeatedly. Use the trigger setter for
discrete actions that must restart even when the archetype is unchanged. One-shot layers remove themselves after their
duration and blend-out; behavior code should not repeatedly restore idle during the clip.

### Equipment synchronization lag

Motion data can arrive before equipment
data. [VillagerAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java) therefore
re-resolves the current action when
its [AnimationSelectionContext](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationSelectionContext.java)
changes. Preserve this correction path when changing selection inputs.

---

## Composition invariants

[AnimationTargetPolicy](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargetPolicy.java) is
executable behavior, not descriptive
metadata. [AnimationFrame](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationFrame.java) must
compose a target with the same semantics that the model or attachment renderer uses when applying it.

| Policy | Meaning |
|---|---|
| `ADDITIVE` | Add the authored delta from the target neutral value. Missing is a no-op. |
| `MULTIPLICATIVE` | Multiply by a factor blended from the unit neutral. |
| `ABSOLUTE` | Replace the lower value; step-valued targets snap. |
| `BLENDED_ABSOLUTE` | Replace model ownership while carrying a separate coverage weight so entry and exit can yield to the underlying pose. |

For coverage-tracked targets:

- no value means no coverage;
- a present value with no stored weight means full coverage;
- zero coverage is represented by omitting the target;
- the final consumer must apply the frame value by `applicationWeight(target)`.

Head and leg rotation overrides use coverage because they replace look tracking or gait baselines. Applying their value
directly at full strength defeats the transition logic even though the code still compiles.

Target identity is its globally unique string id. If two separately created targets share an id, they are treated as the
same address; type, neutral, interpolator, arithmetic, and policy must therefore agree.

---

## Layer lifecycle

[AnimationLayer](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationLayer.java) owns clip playback,
replacement crossfades, contribution weight, and lifetime.

- A persistent layer remains until cleared.
- A transient layer expires after duration plus blend-out.
- Replacing a layer adopts the new push's lifetime.
- Replacement starts from the contribution already held, avoiding a snap to full strength.
- Clearing captures the current contribution and fades down from it. A clear must never make a layer stronger.
- Repeating a clear does not restart its fade.
- Arm configuration is discrete and reads the current clip, not the outgoing clip.

[LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java) removes expired actions
before folding. Its sampling operation still mutates layer lifecycle state by retiring actions and completed crossfades;
do not treat it as an arbitrary pure query.

---

## Locomotion, Idle-Life, umbrella, and sleep

### Locomotion

Gait selection is based
on [NavigationType](../../src/main/java/dev/breezes/settlements/domain/ai/navigation/NavigationType.java), not raw
speed. Genetics and effects can change speed without changing intent.

[DefaultLocomotionAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/DefaultLocomotionAnimator.java):

- selects a clip from navigation intent;
- phases it from `limbSwing`, which is distance-based and keeps footfalls stable across speed changes;
- weights it from `limbSwingAmount`;
- returns no contribution at a standstill.

The model's procedural leg swing is disabled because authored locomotion owns leg motion.

### Idle-Life

[IdleLifeAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/IdleLifeAnimator.java) separates
lifecycle advancement from frame construction:

- `advance(context)` updates blink and fidget state and must tolerate repeated observation of one render context;
- `sample(context)` reads the already-advanced state without changing timers;
- `activeArmConfiguration(context)` is also read-only.

[LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java) always advances Idle-Life,
but only constructs its frame when `1 - locomotionWeight` is positive. At full gait, hidden ambience therefore keeps
valid schedules without allocating an unused frame. A fully hidden idle fidget cannot override gait arm geometry.

Idle-Life is seeded per entity so villagers do not breathe in lockstep. Fidgets stop starting while an action is active,
and an active fidget fades away when work takes over.

Authoring rule: an ambient fidget should generally not own `HEAD_ROTATION_OVERRIDE`. Coverage makes ownership smooth,
but it still suppresses vanilla gaze while fully covered. Prefer eyes, brow, torso, and additive head-adjacent motion
unless taking the gaze is intentional and readable.

### Umbrella

The umbrella is a rain-gated attachment rather than an archetype: no behavior selects it and no archetype resolves to
it. [UmbrellaAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/UmbrellaAnimator.java) owns its
deploy/stow state machine and separates advancement from frame construction, as Idle-Life does, so a supplementary
render pass cannot age it twice (**P8**).

Its gate is steadied twice, on opposite sides of the network boundary, and neither pass substitutes for the other:

- the server debounces its rain sample before publishing, so a villager crossing under an eave never puts a flicker on
  the wire;
- each client animator holds its own delay before committing a change that does arrive, so a crowd whose gate flips on
  one tick does not raise in unison.

Two consequences are easy to break from the outside:

- **Visibility outlives the gate.** Whether to render the attachment at all is the animator's answer, not the gate's, so
  the umbrella stays on screen for the whole lower-and-close instead of disappearing the instant the rain stops.
- **Sleep shuts the gate rather than dropping the umbrella.** The sleep pose replaces the body outright, but the
  umbrella is still advanced and composed over it, so a villager going to bed stows one instead of losing it mid-motion.

The carry clips and the canopy clips overlap by design, in both directions. The carry pair additionally carries an
invariant that a retune can break with nothing failing until someone watches an interrupt; it is stated
at [UmbrellaCarryAnimations](../../src/main/java/dev/breezes/settlements/domain/animation/UmbrellaCarryAnimations.java).

### Sleep

Sleep is vanilla-synchronized state, not motion-plane data. The renderer polls `isSleeping()`; the `SLEEP` archetype
exists as a library lookup key only.

Sleep is a discrete pose switch. While
sleeping, [VillagerAnimator](../../src/main/java/dev/breezes/settlements/domain/animation/VillagerAnimator.java) samples
the sleep clip; while awake, it samples the normal stack. Entering sleep restarts the authored sleep phase.

The awake stack is not sampled during sleep, but its schedules use absolute game time and are not rebased. An action can
be expired and an ambient event can be due when the villager wakes. That variation is intentional.

---

## Arm configuration

[ArmConfiguration](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmConfiguration.java) is a discrete
per-arm choice between crossed and straight geometry. It cannot be blended because it changes which model pieces and
sockets exist visually.

A clip can keyframe arm configuration
through [ArmConfigurationTimeline](../../src/main/java/dev/breezes/settlements/domain/animation/ArmConfigurationTimeline.java). [LayerStack](../../src/main/java/dev/breezes/settlements/domain/animation/LayerStack.java)
resolves the effective configuration top-down:

```text
Action -> visible Idle-Life fidget -> Base -> Locomotion -> both crossed
```

The resolved value controls all of the following:

- crossed versus straight arm geometry;
- which hand socket an attachment uses;
- which authored arm targets are visible.

Mapping convention: off-hand uses the left arm and main-hand uses the right arm. Keep model visibility and socket
selection aligned with that convention.

---

## Attachments and presentation

A [RenderableAttachment](../../src/main/java/dev/breezes/settlements/domain/attachment/RenderableAttachment.java)
describes a slot, content, and
category. [AttachmentProvider](../../src/main/java/dev/breezes/settlements/domain/attachment/AttachmentProvider.java)
implementations discover attachments; client bindings are assembled
in [ClientAttachmentModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAttachmentModule.java).

Presentation is resolved independently:

- [SlotAnchor](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java) maps a slot and arm pose
  to a socket;
- [Socket](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java) identifies a model bone and
  local transform;
- [AttachmentDisplayProfile](../../src/main/java/dev/breezes/settlements/domain/presentation/AttachmentDisplayProfile.java)
  supplies slot/category tuning;
- [SlotTargets](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java) carries animation deltas
  for a slot;
- [AttachmentModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentModel.java)
  is the seam for content that draws as a baked rig rather than through the item pipeline, and
  [AttachmentModelRegistry](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/rendering/AttachmentModelRegistry.java)
  resolves a content's model id to one. That registry is built by the renderer rather than supplied by Dagger, because
  baking a layer is only reachable from the renderer's own context.

The render order is:

1. choose the socket from slot and resolved arm pose;
2. replay the full root-to-bone transform chain;
3. apply the socket-local transform;
4. apply the display profile;
5. apply animation slot deltas, turning about the content's own root pivot rather than about the origin it is drawn at
   (**P15**);
6. render the attachment content.

A socket transform must start at the model root. Attachment rendering occurs after the model render pass has closed its
pose-stack scope, so no ancestor transform can be assumed to remain active.

For the authoritative model hierarchy,
inspect [SettlementsVillagerModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java)
or the Blockbench source at [settlements_villager.bbmodel](../../assets/models/settlements_villager.bbmodel). Do not
copy the hierarchy into this document.

---

## Authoring animations

Animations are track-based.
A [KeyframeAnimation](../../src/main/java/dev/breezes/settlements/domain/animation/KeyframeAnimation.java) contains
metadata, an arm-configuration timeline, and
typed [AnimationTrack](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTrack.java) instances.

Each track owns:

- one [AnimationTarget](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTarget.java);
- ordered, unique keyframe ticks;
- typed values;
- the easing curve from each keyframe to the next.

Use [TrackAnimationBuilder](../../src/main/java/dev/breezes/settlements/domain/animation/TrackAnimationBuilder.java) for
authored clips. Animation factories group clips by
feature; [ClientAnimationModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java)
is the authoritative registration map.

### Add an action animation

1. Add or choose
   an [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java).
2. Author a factory clip using tracks and explicit blend/lifetime metadata.
3. Register the archetype/category key
   in [ClientAnimationModule](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java).
4. Trigger or set the motion from the owning behavior according to replay semantics.
5. Keep gameplay timing constants with the authored clip when behavior code must coordinate an impact, sound, or release
   tick.
6. Test sampling, lifetime, missing-target behavior, and return to the underlying layer.

Do not add a documentation inventory of registered clips. Search the registration map; it is the source of truth.

### Add an asymmetric gesture

1. Declare the correct
   initial [ArmConfiguration](../../src/main/java/dev/breezes/settlements/domain/presentation/ArmConfiguration.java).
2. Add timeline changes only where geometry must switch.
3. Author only the visible arm targets required by each phase.
4. Verify held attachments move to the matching socket when geometry changes.
5. Test both arms independently.

### Add an animation target

1. Define the target id, value type, neutral value, interpolator, arithmetic, and policy
   in [AnimationTargets](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java)
   or [SlotTargets](../../src/main/java/dev/breezes/settlements/domain/animation/SlotTargets.java).
2. Apply it in the final model or attachment consumer with exactly the same policy semantics.
3. Add compositor tests, including missing targets and partial weights.
4. If it is coverage-tracked, test both its frame coverage and its final application.
5. Add it to authored clips only after the consumer exists.

Do not duplicate the target catalog here; search target declarations and usages.

### Add a socket

1. Add the model reference only if an
   existing [ModelPartRef](../../src/main/java/dev/breezes/settlements/domain/presentation/ModelPartRef.java) cannot
   address the bone.
2. Add the complete root-to-bone chain
   in [SettlementsVillagerModel](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java).
3. Register the [Socket](../../src/main/java/dev/breezes/settlements/domain/presentation/Socket.java).
4. Map the appropriate slot and arm pose
   through [SlotAnchor](../../src/main/java/dev/breezes/settlements/domain/presentation/SlotAnchor.java).
5. Add or update the slot/category display profile.
6. Verify transforms in motion, not only at the rest pose.

---

## Finding current implementations

Prefer a code search over adding inventories to this document. Useful starting searches from the repository root:

```powershell
rg "AnimationKey\.of" src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java
rg "target\(AnimationTargets\." src/main/java/dev/breezes/settlements/domain/animation
rg "SlotTargets\." src/main/java
rg "AnimationArchetype\." src/main/java
rg "ModelPartRef\." src/main/java
rg "SocketId\." src/main/java
rg "AttachmentSlot\." src/main/java
rg "InMemoryAttachmentModelRegistry" src/main/java
```

Use the code for:

- current enum members;
- registered clips and category fallbacks;
- exact keyframes and authored values;
- the live model hierarchy;
- socket coordinates and display profiles;
- dependency-injection bindings;
- current animation or attachment inventories.

Those facts change frequently and are safer to discover than to synchronize manually in prose.

---

## Testing and validation

Animation core tests live under [domain/animation tests](../../src/test/java/dev/breezes/settlements/domain/animation).
When changing the system, cover the smallest relevant boundary:

- track interpolation and loop endpoints;
- frame composition for every affected policy;
- partial coverage and missing targets;
- blend-in, replacement, clear, and expiration boundaries;
- one-shot replay and sustained action lifetime;
- Idle-Life update versus sampling behavior;
- locomotion zero/full-weight behavior;
- arm-configuration precedence;
- sleep pose selection;
- umbrella gate delay, reversal handoff, and visibility across a whole stow;
- target application in the model or attachment layer.

Run the animation suite while iterating:

```powershell
.\gradlew.bat test --tests "dev.breezes.settlements.domain.animation.*"
```

Run the full suite before handoff:

```powershell
.\gradlew.bat test
```

Visual verification is still required for authored motion, socket placement, clipping, Euler rotation behavior, and
transitions involving geometry visibility.

---

## Deliberate constraints

These are design boundaries, not current-work inventories:

- Rotations use Euler vectors. Introduce quaternion targets and spherical interpolation before authoring rotations that
  wrap past 180 degrees or expose gimbal behavior.
- Per-bone masks are not part of the current compositor. Add them only when target sparsity and layer ordering cannot
  express a real overlap.
- Arm geometry switches discretely; it is not crossfaded.
- Sleep switches discretely with vanilla bed state.
- The renderer iterates attachments rather than special-casing the main hand.
- Target policy must agree at composition and application.
