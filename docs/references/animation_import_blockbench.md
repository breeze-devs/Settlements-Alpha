# Importing Blockbench Animations

How to port a Blockbench-authored animation into our custom animation system. This is the **mechanical conversion**
companion to [animation_system.md](animation_system.md) — read that first for the concepts (motion plane, targets, arm
configuration, resolution). This doc only covers turning an exported clip into a registered `KeyframeAnimation`.

The transcription itself is rote; the failure modes are not. Read the **Pitfalls** section before you start — every one
of them has already bitten a previous conversion.

---

## The source

The input is Blockbench's **"Export → Java"** output: a vanilla `net.minecraft.client.animation.AnimationDefinition`
builder. Example shape:

```
AnimationDefinition.Builder.withLength(1.5F)
    .addAnimation("arm_straight_right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
        new Keyframe(0.4F, KeyframeAnimations.degreeVec(-80, 10, 10), ...), ...))
    .addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.POSITION, ...))
    .build();
```

Two facts that make this tractable:

1. **The clips are authored against our own rig** (`assets/settlements_villager.bbmodel`), so the Blockbench bone names
   (`arm_straight_right`, `arms_crossed`, `torso`, `nose`, `leg_left`, …) **match our model parts exactly**. Values are
   already in our coordinate space.
2. **We do not run vanilla `AnimationDefinition` at runtime** — our system is a reimplementation sampled in **ticks**,
   not seconds. You transcode the *data* into our `KeyframeAnimation` / `AnimationTrack` / `AnimationTarget` types; you
   never reference the exported class.

---

## Build directly from tracks

Blockbench exports dense curves where each bone can use its own keyframe schedule. Each
`addAnimation(bone, channel)` maps directly to one `AnimationTrack`, added through
`KeyframeAnimation.fromTracks()`. There is no intermediate pose representation.

---

## Conversion rules

| Source (vanilla) | Ours | Notes |
|---|---|---|
| timestamp `0.4F` (seconds) | `Math.round(0.4 * 20)` = `8` (ticks) | `Keyframe.tick` is an `int` and **must be unique within a track** (see Pitfalls) |
| `withLength(1.5F)` | `durationTicks(30)` | `Math.round(seconds * 20)` |
| `degreeVec(p, y, r)` | `RotationUtil.degrees(p, y, r)` | **verbatim** — both are pure deg→rad, applied additively |
| `posVec(x, y, z)` | `new Vec3(x, **-y**, z)` | **NEGATE Y.** Vanilla `posVec` returns `(x, -y, z)`; our `applyTranslation` does not flip. Forgetting this inverts every translation. |
| all-zero translation keyframe | `Vec3.ZERO` | |
| `scaleVec(x, y, z)` | `new Vector3f(x, y, z)` — **verbatim** | No Y flip, and **do not subtract 1.** See below. |
| `Interpolations.LINEAR` | `Easing.LINEAR` | |
| `Interpolations.CATMULLROM` | Choose after comparing with the source | No available easing reproduces the spline — see Pitfalls. |

Y-negation worked example — vanilla `posVec(0, -4.5, 2.5)` becomes `new Vec3(0.0, 4.5, 2.5)`.

### Scale is verbatim, despite what `scaleVec` returns

Vanilla `scaleVec(x, y, z)` returns `(x-1, y-1, z-1)` — an *offset* from unit scale, which vanilla then adds onto a base
of `1`. Our `*_SCALE` targets are `MULTIPLICATIVE` from a `(1,1,1)` neutral and
`applyScale` multiplies onto `resetPose`'s unit scale. The two systems meet at the same final size only if you
transcribe **the literal from the export**: `scaleVec(1, 1.5, 1)` → `new Vector3f(1.0F, 1.5F, 1.0F)`. Subtracting 1
yourself would land the part at half size instead of one-and-a-half.

Consequently the no-op scale keyframe is all- **ones**, not all-zeros.

### Bone → `AnimationTarget`

Targets live
in [AnimationTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java). A
`ROTATION` channel → the `*_ROTATION` target (`AnimationTrack.<Vector3f>`, values via `RotationUtil.degrees`). A
`POSITION` channel → the `*_TRANSLATION` target (`AnimationTrack.<Vec3>`, values via `new Vec3`). A `SCALE` channel →
the `*_SCALE` target (`AnimationTrack.<Vector3f>`, values via `new Vector3f`).

| Blockbench bone | Rotation target | Translation target | Scale target |
|---|---|---|---|
| `arms_crossed` | `ARMS_CROSSED_ROTATION` | `ARMS_CROSSED_TRANSLATION` | — |
| `arms_straight` | `ARMS_STRAIGHT_ROTATION` | `ARMS_STRAIGHT_TRANSLATION` | — |
| `arm_crossed_left` / `arm_crossed_right` | `ARM_CROSSED_LEFT/RIGHT_ROTATION` | `…_TRANSLATION` | — |
| `arm_straight_left` / `arm_straight_right` | `ARM_STRAIGHT_LEFT/RIGHT_ROTATION` | `…_TRANSLATION` | — |
| `torso` | `BODY_ROTATION` | `BODY_TRANSLATION` | — |
| `nose` | `NOSE_ROTATION` | `NOSE_TRANSLATION` | — |
| `leg_left` / `leg_right` | `LEG_LEFT/RIGHT_ROTATION_OVERRIDE` (**BLENDED_ABSOLUTE**) | `LEG_LEFT/RIGHT_TRANSLATION` (additive) | — |
| `head` | `HEAD_ROTATION_OVERRIDE` (**BLENDED_ABSOLUTE**) | `HEAD_TRANSLATION` (additive) | — |
| `monobrow` | `MONOBROW_ROTATION` | `MONOBROW_TRANSLATION` | — |
| `mouth` | `MOUTH_ROTATION` | `MOUTH_TRANSLATION` | `MOUTH_SCALE` |
| `eyelid_left` / `eyelid_right` | — | `EYELID_LEFT/RIGHT_TRANSLATION` | — |
| `eyeball_left` / `eyeball_right` | — | `EYEBALL_LEFT/RIGHT_TRANSLATION` | `EYEBALL_LEFT/RIGHT_SCALE` |
| `pupil_left` / `pupil_right` | — | `PUPIL_LEFT/RIGHT_TRANSLATION` | `PUPIL_LEFT/RIGHT_SCALE` |

Leg and head translations are `ADDITIVE`, unlike the `BLENDED_ABSOLUTE` rotation overrides beside them. The overrides
replace vanilla gait/look rotation at full weight but preserve it while the layer fades in or out. Translation layers
onto `resetPose` and needs no `frame.has` gate.

If a channel has no target, add one entry in `AnimationTargets` and apply it
in [SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java).
Do not invent a target for a channel that is constant-neutral; drop it (see Pitfalls).

---

## Decisions the source does not contain

The export has geometry and timing. Everything else is a deliberate choice:

- **`arms(ArmConfiguration)`** — must match the arm geometry the clip actually drives. A clip that animates
  `arms_crossed` needs a **crossed** config (`BOTH_CROSSED`, or a mixed config keeping that side crossed); a clip that
  animates `arm_straight_*` needs a **straight** config. Get this wrong and the gesture renders on hidden geometry —
  i.e. invisible. This is the single most common error.
- **`loopMode`** — `ONCE` vs `LOOP` is decided by how the behavior *uses* it, **not** the Blockbench `.looping()` flag
  (that's a preview-only artifact and is routinely wrong). A one-shot action re-triggered per beat is `ONCE`; a
  sustained state is `LOOP`.
- **archetype × category** — see Wiring.
- **`blendInTicks` / `blendOutTicks`** — these also control how `BLENDED_ABSOLUTE` head/leg ownership fades to and from
  the existing model rotation. Use enough time to avoid a visible snap; tune against the clip.
- **Timing constants** — expose gameplay-relevant frames and durations as `public static final int` on the factory.
  Existing examples include `FishingAnimations.CAST_IMPACT_TICK` and `HarvestCropAnimations.HARVEST_AT_TICK` /
  `HARVEST_DURATION_TICKS`.

---

## Pitfalls

1. **Arm config vs driven geometry.** (Above.) Cross-check: list the arm bones the clip animates, confirm they're
   visible under the config you chose.
2. **Y-negation on translations.** Forgetting flips every `*_TRANSLATION` track upside down.
3. **Tick rounding & collisions.** Convert with `Math.round(seconds * 20)`. Duplicate ticks throw, but there is no
   universal sample to discard. Preserve a key authored exactly on an integer tick over a nearby sub-tick key. For dense
   or ambiguous curves, resample the source curve at integer-tick times. Otherwise retain the sample that best preserves
   the source value and direction at that tick. Never cascade collisions forward; that distorts timing and can push the
   closing key past `durationTicks`. Preserve the effective source values at tick 0 and `durationTicks`, intentional
   holds and gaps, unique ticks, and the clip duration.
4. **Don't drop channels that map.** Only drop a channel whose every keyframe is neutral — all-zero for
   rotation/translation, all- **one** for scale. Everything else has a target now; check the table before assuming
   otherwise.
5. **A single-keyframe channel is a pose, not a no-op.** Blockbench emits `n=1` channels to hold a constant non-neutral
   offset for the whole clip — crossed pupils, splayed legs at `(0, ±7.5, 0)`, a squashed eyeball. `AnimationTrack`
   accepts one keyframe and `sample()` returns it flat. Transcribe them; dropping them loses the pose.
6. **BLENDED_ABSOLUTE leg/head targets.** These targets replace rather than add at full weight. Their application weight
   follows layer transitions, so entry starts from the current vanilla/lower-layer rotation and exit returns to it over
   `blendOutTicks`. Only author `HEAD_ROTATION_OVERRIDE` when the clip should own look direction.
7. **`Easing.CUBIC` is not Catmull-Rom.** It is segment smoothstep with no spline tangents or overshoot. It is not
   categorically closer than `LINEAR`; compare both with the source. Sparse curves may need extra sampled keys.
8. **Blockbench shows both arm rigs; the game shows one.** Animators routinely key `arms_crossed` *and* `arm_straight_*`
   in the same clip because both are visible while authoring. Only the set matching the declared `arms(...)` renders.
   Before dropping the losing side, check whether it is actually a mid-clip hand-off that wants
   `armConfigurationAt(tick, …)` — `HarvestCropAnimations` and `TillAnimations` both use the crossed channel as a
   return-to-rest tail.
9. **Loop seams must be transform-continuous.** Compare the effective transform before the wrap with tick 0. Euler
   rotations differing by whole turns can render identically while preserving one-way winding; translation and scale
   generally need equal endpoints. Inspect both pose and direction at the seam.

Locomotion clips *are* portable now: `DefaultLocomotionAnimator` phases the clip off `limbSwing` against its
`durationTicks`, mapping one vanilla limb cycle to one authored cycle so the cadence survives agility-modified speeds.
Register the gait in `LocomotionAnimationLibrary` by `NavigationType` rather than as an archetype.

---

## File structure

One factory class per clip (or per family), in
`dev.breezes.settlements.domain.animation`. [HarvestCropAnimations.java](../../src/main/java/dev/breezes/settlements/domain/animation/HarvestCropAnimations.java)
is the canonical worked example (multiple arms, body translation, legs, nose). Skeleton:

```java

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FooAnimations {

    public static final int FOO_DURATION_TICKS = 30;
    public static final int FOO_PEAK_TICK = 14; // the action frame, for behavior coordination

    public static KeyframeAnimation foo() {
        return KeyframeAnimation.fromTracks()
                .id(ResourceLocationUtil.mod("animation/<family>/<clip>"))
                .durationTicks(FOO_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(4).blendOutTicks(4)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR),
                                new Keyframe<>(8, RotationUtil.degrees(-80f, 10f, 10f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f), Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0, Vec3.ZERO, Easing.LINEAR),
                                new Keyframe<>(8, new Vec3(0.0, 1.0, 1.0), Easing.LINEAR),
                                new Keyframe<>(30, Vec3.ZERO, Easing.LINEAR)))
                        .build())
                // ...one .track(...) per usable channel...
                .build();
    }
}
```

Imports needed beyond the same-package types: `RotationUtil`, `ResourceLocationUtil` (`shared.util`), `ArmConfiguration`
(`domain.presentation`), `Vec3` (`net.minecraft.world.phys`), `Vector3f` (`org.joml`), `List` (`java.util`), plus
Lombok.

---

## Wiring (makes the clip resolvable)

1. **Archetype** — pick one
   from [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java), or
   append a new value (**at the end** — ordinal is the network wire format). Reuse before inventing: a new tool's swing
   is still `SWING_HEAVY`.
2. **Category** — usually `ItemCategory.GENERIC`. Use a specific category (e.g. `AXE`) only to specialize a variant for
   that item; the resolver falls back `exact → (archetype, GENERIC) → idle`.
3. **Register** — add a `Map.entry(AnimationKey.of(archetype, category), FooAnimations.foo())`
   to [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java).
4. **New targets** — only if a channel had no home (see Bone → Target).

Registration makes the clip *resolvable*, not *played*. Drive `ONCE` actions with `villager.triggerMotion(archetype)`
and sustained states with `villager.setMotion(archetype)`. One-shots auto-pop after their blend-out; do not restore
`IDLE` per phase. Clear sustained motion when its owning state ends, with behavior teardown as a safety net.
See [animation_system.md](animation_system.md) §"Adding a new animation".

---

## Verify

- `./gradlew compileJava` (view only the tail — Gradle is noisy).
- Compare the curve and loop seam with the source in Blockbench.
- Verify in-game through the real archetype or locomotion path. The retired debug-pose overlay is unavailable, so
  runtime wiring is required for final visual verification.
