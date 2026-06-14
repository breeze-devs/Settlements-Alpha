# Importing Blockbench Animations

How to port a Blockbench-authored animation into our custom animation system. This is the **mechanical conversion** companion to [animation_system.md](animation_system.md) — read that first for the concepts (motion plane, targets, arm configuration, resolution). This doc only covers turning an exported clip into a registered `KeyframeAnimation`.

The transcription itself is rote; the failure modes are not. Read the **Pitfalls** section before you start — every one of them has already bitten a previous conversion.

---

## The source

The input is Blockbench's **"Export → Java"** output: a vanilla `net.minecraft.client.animation.AnimationDefinition` builder. Example shape:

```java
AnimationDefinition.Builder.withLength(1.5F)
    .addAnimation("arm_straight_right", new AnimationChannel(AnimationChannel.Targets.ROTATION,
        new Keyframe(0.0F, KeyframeAnimations.degreeVec(0, 0, 0), AnimationChannel.Interpolations.LINEAR),
        new Keyframe(0.4F, KeyframeAnimations.degreeVec(-80, 10, 10), ...), ...))
    .addAnimation("torso", new AnimationChannel(AnimationChannel.Targets.POSITION, ...))
    .build();
```

Two facts that make this tractable:

1. **The clips are authored against our own rig** (`assets/settlements_villager.bbmodel`), so the Blockbench bone names (`arm_straight_right`, `arms_crossed`, `torso`, `nose`, `leg_left`, …) **match our model parts exactly**. Values are already in our coordinate space.
2. **We do not run vanilla `AnimationDefinition` at runtime** — our system is a reimplementation sampled in **ticks**, not seconds. You transcode the *data* into our `KeyframeAnimation` / `AnimationTrack` / `AnimationTarget` types; you never reference the exported class.

---

## Use tracks, not poses

[animation_system.md](animation_system.md) presents the pose-driven builder as the primary authoring API. **For ports, ignore it and use the track escape hatch.** These exports are dense, pre-baked curves where each bone keys on its own irregular schedule — there are no shared "key poses" to snapshot, so poses would either lose fidelity or degenerate into one-pose-per-keyframe ceremony. Each `addAnimation(bone, channel)` maps **1:1** to one `AnimationTrack`.

`KeyframeAnimation.fromPoses()` is still the entry point — it just gets `.track(...)` calls instead of `.at(...)` calls. `build()` tolerates zero poses.

---

## Conversion rules

| Source (vanilla) | Ours | Notes |
|---|---|---|
| timestamp `0.4F` (seconds) | `Math.round(0.4 * 20)` = `8` (ticks) | `Keyframe.tick` is an `int` and **must be unique within a track** (see Pitfalls) |
| `withLength(1.5F)` | `durationTicks(30)` | `Math.round(seconds * 20)` |
| `degreeVec(p, y, r)` | `RotationUtil.degrees(p, y, r)` | **verbatim** — both are pure deg→rad, applied additively |
| `posVec(x, y, z)` | `new Vec3(x, **-y**, z)` | **NEGATE Y.** Vanilla `posVec` returns `(x, -y, z)`; our `applyTranslation` does not flip. Forgetting this inverts every translation. |
| all-zero translation keyframe | `Vec3.ZERO` | |
| `Interpolations.LINEAR` | `Easing.LINEAR` | `CATMULLROM` → `Easing.CUBIC` (close, not identical — note it if used) |

Y-negation worked example — vanilla `posVec(0, -4.5, 2.5)` becomes `new Vec3(0.0, 4.5, 2.5)`.

### Bone → `AnimationTarget`

Targets live in [AnimationTargets.java](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationTargets.java). A `ROTATION` channel → the `*_ROTATION` target (`AnimationTrack.<Vector3f>`, values via `RotationUtil.degrees`). A `POSITION` channel → the `*_TRANSLATION` target (`AnimationTrack.<Vec3>`, values via `new Vec3`).

| Blockbench bone | Rotation target | Translation target |
|---|---|---|
| `arms_crossed` | `ARMS_CROSSED_ROTATION` | `ARMS_CROSSED_TRANSLATION` |
| `arms_straight` | `ARMS_STRAIGHT_ROTATION` | `ARMS_STRAIGHT_TRANSLATION` |
| `arm_crossed_left` / `arm_crossed_right` | `ARM_CROSSED_LEFT/RIGHT_ROTATION` | `…_TRANSLATION` |
| `arm_straight_left` / `arm_straight_right` | `ARM_STRAIGHT_LEFT/RIGHT_ROTATION` | `…_TRANSLATION` |
| `torso` | `BODY_ROTATION` | `BODY_TRANSLATION` |
| `nose` | `NOSE_ROTATION` | `NOSE_TRANSLATION` |
| `leg_left` / `leg_right` | `LEG_LEFT/RIGHT_ROTATION_OVERRIDE` (**ABSOLUTE**) | — none — |
| `head` | `HEAD_ROTATION_OVERRIDE` (**ABSOLUTE**) | — none — |

**No target exists for** `head`/`leg` POSITION or any SCALE channel. If a clip genuinely needs one, add it (one entry in `AnimationTargets` + one apply line in [SettlementsVillagerModel.applyAnimationFrame](../../src/main/java/dev/breezes/settlements/infrastructure/minecraft/entities/villager/model/SettlementsVillagerModel.java)) — `BODY_TRANSLATION` and the `NOSE_*` targets were added exactly this way. Do not invent a target for a channel that is constant-zero; drop it (see Pitfalls).

---

## Decisions the source does not contain

The export has geometry and timing. Everything else is a deliberate choice:

- **`arms(ArmConfiguration)`** — must match the arm geometry the clip actually drives. A clip that animates `arms_crossed` needs a **crossed** config (`BOTH_CROSSED`, or a mixed config keeping that side crossed); a clip that animates `arm_straight_*` needs a **straight** config. Get this wrong and the gesture renders on hidden geometry — i.e. invisible. This is the single most common error.
- **`loopMode`** — `ONCE` vs `LOOP` is decided by how the behavior *uses* it, **not** the Blockbench `.looping()` flag (that's a preview-only artifact and is routinely wrong). A one-shot action re-triggered per beat is `ONCE`; a sustained state is `LOOP`.
- **archetype × category** — see Wiring.
- **`blendInTicks` / `blendOutTicks`** — 1–3 ticks is a sane default; tune later.
- **Timing constants** — expose the key action frame(s) and duration as `public static final int` on the factory class so server behaviors can coordinate (damage tick, grab tick, etc.) without an event bus. See `SwingAnimations.SWING_IMPACT_TICKS` and `HarvestCropAnimations.HARVEST_PEAK_TICK` / `HARVEST_DURATION_TICKS`.

---

## Pitfalls

1. **Arm config vs driven geometry.** (Above.) Cross-check: list the arm bones the clip animates, confirm they're visible under the config you chose.
2. **Y-negation on translations.** Forgetting flips every `*_TRANSLATION` track upside down.
3. **Tick rounding & collisions.** Use `Math.round` (half-up: `12.5 → 13`). If two keyframes round to the same tick, bump the later one by +1 — duplicate ticks throw at construction.
4. **Don't drop channels that map.** Only drop (a) channels whose every keyframe is all-zero (no-op) and (b) channels with no target (head/leg POSITION, scale). Legs *do* have targets — keep them.
5. **ABSOLUTE leg/head targets.** `LEG_*` and `HEAD_*` overrides *replace* rather than add. Harmless for legs in a stationary clip (rest rotation is 0, so `=δ` equals `+=δ`), but `HEAD_ROTATION_OVERRIDE` suppresses vanilla look-tracking for the clip's duration — only use it if the clip should own the head.
6. **Locomotion does not fit yet.** Walk/jog/run/stroll are cyclic and speed-driven; the motion plane is gameTime-sampled with no `limbSwing` coupling, so a ported walk foot-slides and fights the vanilla leg swing. Do **not** port locomotion clips without the dedicated locomotion-driver design.

---

## File structure

One factory class per clip (or per family), in `dev.breezes.settlements.domain.animation`. [HarvestCropAnimations.java](../../src/main/java/dev/breezes/settlements/domain/animation/HarvestCropAnimations.java) is the canonical worked example (multiple arms, body translation, legs, nose). Skeleton:

```java
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class FooAnimations {

    public static final int FOO_DURATION_TICKS = 30;
    public static final int FOO_PEAK_TICK = 14; // the action frame, for behavior coordination

    public static KeyframeAnimation foo() {
        return KeyframeAnimation.fromPoses()
                .id(ResourceLocationUtil.mod("animation/<family>/<clip>"))
                .durationTicks(FOO_DURATION_TICKS)
                .loopMode(LoopMode.ONCE)
                .blendInTicks(2).blendOutTicks(3)
                .arms(ArmConfiguration.BOTH_STRAIGHT)
                .track(AnimationTrack.<Vector3f>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_ROTATION)
                        .keyframes(List.of(
                                new Keyframe<>(0,  RotationUtil.degrees(0f, 0f, 0f),    Easing.LINEAR),
                                new Keyframe<>(8,  RotationUtil.degrees(-80f, 10f, 10f), Easing.LINEAR),
                                new Keyframe<>(30, RotationUtil.degrees(0f, 0f, 0f),    Easing.LINEAR)))
                        .build())
                .track(AnimationTrack.<Vec3>builder()
                        .target(AnimationTargets.ARM_STRAIGHT_RIGHT_TRANSLATION)
                        .keyframes(List.of(
                                new Keyframe<>(0,  Vec3.ZERO,                Easing.LINEAR),
                                new Keyframe<>(8,  new Vec3(0.0, 1.0, 1.0),  Easing.LINEAR), // posVec(0,-1,1), Y negated
                                new Keyframe<>(30, Vec3.ZERO,                Easing.LINEAR)))
                        .build())
                // ...one .track(...) per usable channel...
                .build();
    }
}
```

Imports needed beyond the same-package types: `RotationUtil`, `ResourceLocationUtil` (`shared.util`), `ArmConfiguration` (`domain.presentation`), `Vec3` (`net.minecraft.world.phys`), `Vector3f` (`org.joml`), `List` (`java.util`), plus Lombok.

---

## Wiring (makes the clip resolvable)

1. **Archetype** — pick one from [AnimationArchetype](../../src/main/java/dev/breezes/settlements/domain/animation/AnimationArchetype.java), or append a new value (**at the end** — ordinal is the network wire format). Reuse before inventing: a new tool's swing is still `SWING_HEAVY`.
2. **Category** — usually `ItemCategory.GENERIC`. Use a specific category (e.g. `AXE`) only to specialize a variant for that item; the resolver falls back `exact → (archetype, GENERIC) → idle`.
3. **Register** — add a `Map.entry(AnimationKey.of(archetype, category), FooAnimations.foo())` to [ClientAnimationModule.animationLibrary](../../src/main/java/dev/breezes/settlements/di/modules/client/ClientAnimationModule.java).
4. **New targets** — only if a channel had no home (see Bone → Target).

Registration makes the clip *resolvable*, not *played*. To play it, a server behavior must drive the motion plane: `villager.triggerMotion(archetype)` for a `ONCE` action, `setMotion(archetype)` for a `LOOP` state, and `setMotion(IDLE)` on completion and interruption. See [animation_system.md](animation_system.md) §"Adding a new animation" for the behavior side.

---

## Verify

- `./gradlew compileJava` (view only the tail — Gradle is noisy).
- In-game: a clip only renders once something triggers its archetype. The **J** debug-pose overlay only covers crossed-arms / head / body, so a freshly ported gesture on other bones can't be eyeballed until its behavior trigger is wired — wiring is the gate to visual verification.
