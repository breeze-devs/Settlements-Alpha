# Platform Standard

Traps in Minecraft, NeoForge, and the build that have cost real debugging time here. These are not conventions we chose;
they are properties of the platform that punish the obvious approach.

A trap belongs here if it would still exist after a full rewrite of this mod on the same platform. A trap in *our*
subsystems belongs in that subsystem's reference doc or Javadoc instead, where whoever changes it is already working
(F1).

---

## Runtime

### P1 — Entity constructors run on both logical sides

Never resolve server components or touch server-owned state in an entity constructor. It runs during client-side
construction too, and the server graph may not exist yet even on the server. The result is a crash on dedicated servers
and replay clients.

Defer that work to a lifecycle point where the server graph is available. A logical-side check does **not** substitute
for deferral — it does not solve early construction. Where the deferred hook can itself run client-side, guard it by
side as well.

### P2 — Datapack-backed registries do not exist on the client

Never resolve a datapack reload-listener registry from client code. It is server-only and reads empty in multiplayer, so
the bug appears only on a real server. Denormalize the resolved value into synced block-entity NBT instead, and let the
client read that.

### P3 — Never mutate an in-use `ItemStack`'s data

Mutating the data of a stack the player is actively using cancels the use channel, ending the interaction mid-action.
Store channel state on a non-serialized player attachment and clear it in `releaseUsing`.

### P4 — Reach non-public Minecraft methods with an `@Invoker` mixin

An `@Invoker` is the supported route to a private or protected vanilla method.

**The wrapper must not share the vanilla method's signature.** A same-signature wrapper re-dispatches virtually and
recurses until the stack overflows. Give it a distinct name.

### P8 — A supplementary entity render re-enters every render callback

Drawing an entity a second time in a frame vanilla has already drawn it in — to feed an effect buffer such as the
outline pass — re-enters the entity render events and every render layer on that entity. Nothing in the callback
distinguishes the second call from a new frame, so a listener that advances time-based state, or draws decoration the
effect has no use for, runs twice against one frame: animation ages at double rate, timers expire early, and per-call
work doubles for every entity in the pass.

The pass must therefore be identifiable from inside the callback. Mark the `MultiBufferSource` the supplementary pass
hands to the dispatcher, and have every entity-render listener that is not part of the effect return on seeing that
mark. **A listener that omits the check fails silently** — no exception, only something running at the wrong rate — so
adding one is a seam that has to be walked, not a change that announces itself.

### P9 — A miss is still a `BlockHitResult`

`Minecraft.hitResult` reports a miss as `BlockHitResult.miss(...)` — the same class, not null and not another type — and
that result still answers `getBlockPos()` with the position where the ray stopped. **An `instanceof BlockHitResult`
check therefore passes while the player aims at open air.** Test `getType()`, never the class.

The field is also only assigned during a tick, so it is null on the frames between a level loading and its first tick.
Code that dereferences it must treat null as the miss it is.

Anything reading a look target should take it from whatever resolves it once for the frame rather than re-deriving it,
so one wrong check cannot disagree with another.

### P10 — The off-hand pass re-enters `useItemOn` with an empty stack

One right-click is up to two attempts. `Minecraft.startUseItem` loops `InteractionHand.values()` — main hand, then off
hand — and only a **consuming** result or `FAIL` leaves the loop. Anything non-consuming runs the whole per-hand
dispatch again with the off hand's stack, which for most players is `ItemStack.EMPTY`.

Inside `useItemOn` that second stack is indistinguishable from a player who deliberately clicked empty-handed. **A verb
keyed on "the hand is empty" therefore fires from an off hand the player never filled** — so it must also require
`hand == MAIN_HAND`. This is the same restriction vanilla applies to `useWithoutItem`, and a block that handles
empty-handed use inside `useItemOn` has to reproduce it rather than inherit it.

`FAIL` is the only non-consuming result that stops the loop, and it still permits `stack.useOn`, so it suppresses the
off-hand retry without costing vanilla placement.

### P11 — A cache keyed on reported inputs never sees the world change

Anything derived from block states — a cell count, a reachability answer, a footprint check — depends on inputs nothing
reports to the reader: a block paved over, flooded, or simply not yet streamed in while chunks are still arriving after
a join or a teleport. None of that moves a key built from synced entity fields, so **key equality alone pins the first
answer ever computed**, and a value taken mid-chunk-load stays wrong for as long as it is read.

Key equality covers the inputs that can be observed changing; a **max age** covers the ones that cannot. Both terms are
needed, and the age term is not redundant with the others.

### P12 — The font renderer reads a near-zero alpha as no alpha at all

Text drawing treats a packed color whose alpha byte is below 4 as a color that never specified an alpha channel, and
substitutes fully opaque. **Text faded toward invisible therefore does not disappear — it flashes back to full
brightness** on whichever frame lands in that range. Whether any frame of a given fade lands there depends on where the
frame times happen to fall, so the flash is intermittent, and it presents as a blink at the end of an animation rather
than as anything to do with color.

Clamp the alpha *byte* up to the floor, never down through it, and let a separate visibility decision — not the color —
be what stops the draw. Four out of 255 is under two percent opacity, so the clamp costs nothing visible.

Clamping the *fraction* being scaled instead is the wrong repair for the same symptom: it brightens a translucent base
color across its entire fade rather than only at the tail, which is a defect that never announces itself.

### P13 — A private-use codepoint is stored correctly and shows up nowhere

Inline icons are drawn by mapping a texture to a codepoint in a private font, which puts a Private Use Area character
(`\uE000` and up) into source, resource JSON, and any tooling in between. Nothing renders a glyph for it outside the
game, so **the character survives the file while vanishing from every view of that file** — editors, diffs, terminal
output, and the reads a tool performs before editing. A read-then-write round trip silently drops it, and an edit
matched against text containing one silently fails to match.

That is worse than an encoding that fails outright, because every check short of running the game reports success.

Write these as `\uXXXX` escapes everywhere — Java, JSON, docs, commit messages. Both forms parse to the same codepoint,
and only the escape is legible to the tools that have to maintain it. Where one has to be produced programmatically,
build it from a char code rather than typing it, or the generating script is itself unreadable.

Related in kind: a raw control character makes git classify a file as binary, so its diff disappears from review
entirely. Any non-printing character in source is worth this suspicion.

### P14 — A lenient list field discards the whole list, not just the bad element

A list codec tracks per-element decode failures internally and can still surface the successfully-decoded elements as a
*partial* result, but wrapping that list in a lenient optional field never looks at the partial: lenient field decoding
treats "this field's decode carries any error" as "the field is absent" and substitutes the field's default whole. **One
hand-edited or corrupted element therefore silently erases every element the list held, not only the corrupted one** —
the opposite of the entry-scoped drop a lenient wrapper is reached for in the first place.

Recovering the list's own partial result — the elements that decoded cleanly — needs a combinator applied to the list
codec itself, before any optional-field wrapping, that promotes a partial result to a success while logging what was
dropped and why. A plain (non-lenient) optional field can then sit on top for the field's own presence, since the list
codec beneath it no longer surfaces per-element failures as field-level errors at all.

A codec-backed attachment compounds this: reading it discards the *whole attachment* on any decode error that isn't a
clean success, so the same swallow-everything failure mode reaches all the way up from one corrupted element to every
entry the attachment held.

### P15 — A Blockbench group's origin exports as its bone's offset

Moving a group's origin re-pivots the rig in Blockbench and moves the **bone** in the export, leaving that group's
geometry standing off the origin it is drawn at by exactly the distance the pivot moved. The model still renders where
it did, so the export looks correct from every angle that does not rotate it.

**A rotation applied at the draw origin therefore swings the model around a point that far away** rather than turning it
about the pivot that was chosen. Nothing reports the discrepancy, and the severity scales with the correction: nudging a
pivot slightly still looks roughly right, while moving it the length of the model makes the rotation orbit a point out
in space — which presents as a socket or placement fault, since the pivot is the one thing that was just made correct.

Rotate about an offset the model reports from its own baked root bone instead of about the draw origin. Derive that
offset from the bone rather than restating it as a literal: a hand-copied copy is invalidated by the next export, and
invalidated specifically by whoever is re-pivoting the rig, who has no reason to look for it (**F1**).

## Build

### P5 — Dagger needs `javax.inject` on the runtime classpath, permanently

`dagger.internal.Provider` extends `javax.inject.Provider`, so the package must resolve at runtime no matter how modern
the rest of the graph is. Migrating to `jakarta.inject` does not remove the requirement and cannot resolve the JPMS
module collision that follows from shipping both. Both packages must resolve, and the `javax.inject` coordinate must
deduplicate to a single canonical artifact.

### P6 — ModDevGradle's `jarJar` fails on manifest-less jars

Some artifacts ship without a `MANIFEST.MF`, which the `jarJar` task NPEs on. The repair is a `buildSrc` artifact
transform that supplies one. Expect to extend it when adding a dependency that gets jarJar'd.

### P7 — Pin the Gradle daemon's JDK

The Gradle version in use does not run on the newest JDKs. The toolchain is pinned deliberately; changing it is a build
migration, not a version bump.
