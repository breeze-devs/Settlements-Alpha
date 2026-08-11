# Player Surfaces

This document covers the client-side regions that render information near the player's view without asking for input
first — content anchored below the crosshair, and outlines drawn over entities. Both are read-only composites: several
unrelated features may each want to draw into the same region on the same frame, and neither region has a natural owner
unless one is assigned. **U12** governs this; this doc is where its reasoning for these two regions lives, rather than
being restated at every call site.

A committed surface — a screen the player opened on purpose — is out of scope here. These regions are ambient: dismissed
by looking away, never capturing input, never blocking movement (**U7**).

**Where to start reading:** the crosshair HUD region and its two provider seams (one keyed on the held item, one keyed
on the aim target) live under `presentation/ui/hud`. Entity outlines and their provider seam live under
`infrastructure/rendering/highlight`. Each region's owner is the only place that resolves precedence or suppression for
that region; nothing else in the seam needs to.

---

## Why a region needs one owner

Two contributors that can each decide to draw into the same pixels need an explicit answer to "which one,"
or the answer becomes whichever one happens to run last in an unordered collection — a coincidence that holds only until
a second contributor exists. Both regions resolve precedence the same way for exactly this reason, rather than each
growing its own ad hoc fix.

The fix has three parts, and all three matter independently:

- **A declared priority per contributor**, not registration order or collection iteration order. Iteration order over a
  multibound, unordered collection is not a policy — it changes with unrelated code elsewhere and cannot be reasoned
  about from either contributor's own source.
- **A deterministic tiebreak** for contributors that declare equal priority, so two equally-ranked contributors still
  produce the same outcome on every run rather than one that merely happens to be stable today.
- **One place the ordering is decided** — computed once when the region's owner is constructed, not recomputed every
  frame. The set of contributors is fixed for the life of the process, so re-deriving the same order on every frame buys
  nothing and risks the tiebreak drifting from the constructor's version over time.

A contributor never sorts itself against its peers; it only declares where it stands, and the region's owner resolves
the rest.

## The crosshair HUD region

Content below the crosshair answers one of two different questions, and a single contributor is asked only one of them:

- **What is the player holding?** Independent of where the player is looking.
- **What is the player aiming at?** A block hit is a first-class case here, not an entity surface with a block bolted on
  afterward — the two seams that answer these questions are declared separately so a contributor's registration also
  declares which question it answers, and so the two groups can be given different cross-cutting treatment (see below)
  without either seam needing to know the other exists.

**Precedence between the two:** the aim-based answer wins whenever it has one to give. What the player is about to
right-click describes the click's consequence more directly than what happens to be in their hand, so when both would
draw something, the aim-based content is what's shown; held-item content is the fallback shown once every aim-based
contributor has declined the current frame. Within either group, ties resolve by the declared-priority-then-tiebreak
rule above.

**Suppression is the region's decision, not each contributor's.** A HUD hidden by the player, an open screen, spectator
mode, or a missing player/level all mean the region draws nothing at all, decided once before any contributor is asked.
Pushing that check into each contributor risks one of them disagreeing about when it's safe to draw, which is exactly
the inconsistency **U6** exists to prevent: a gesture must never resolve to something the player could not see
explained.

**The aim target is read the same way the game's own right-click dispatch resolves one — not a fresh raycast.**
A contributor that raycasts independently risks disagreeing with what a right-click is actually about to hit, which
would make the surface describe a click other than the one the player is about to make.

The exception proves the rule: an item whose *own* use resolves a target differently from the game's shared pick must
follow the item, not the pick. Placement on a fluid is the case that exists today — the shared pick passes straight
through water to the block behind it, so a surface previewing a water placement that used it would describe a spot the
item would never choose. Such a contributor owns a resolver of its own rather than raycasting inline, mirroring the
geometry the item's own placement uses — ray, world border, still-water-source, and replaceability. That resolver is
honestly scoped to geometry alone: it says nothing about build height, player or item permission, entity obstruction, or
protection, so a preview clearing it can still have the eventual placement refused for a reason the preview never
checked.

Where a gesture is previewed by more than one surface, every surface shares the *same instance* of the contributor's
resolver rather than each opening its own — duplicating a resolver is not what this exception licenses, and two
independently-resolved answers can silently disagree the moment either gains a stateful input. A resolver that must also
decide which hand a gesture comes from — because the region's default of the main-hand stack alone would miss a gesture
made from the off hand — is still within the exception: it is one more thing the contributor's own resolver, rather than
the region's generic hand-off, is responsible for getting right. The test throughout is whether the contributor's answer
matches *what this gesture will do*, which is what the shared pick is usually the cheapest route to and occasionally the
wrong one.

**No contributor reads platform input state directly.** Everything a contributor needs — the held stack, the resolved
aim target, the acting player — is resolved once per frame by the region's owner and handed down. This is what keeps the
two questions above answerable identically regardless of which contributor is asked, and it is a hard rule: a
contributor that reaches around the frame to read platform state directly can end up disagreeing with what the rest of
the region resolved for the same frame.

**Reveal is not instantaneous for an aim-based answer, in either direction.** A crosshair sweeping across a crowd should
not strobe a contributor's content on and off for every entity or block it crosses, so an aim-based answer only reaches
the region once the aim has held on the same target, continuously, for a short stretch — one shared definition of both
"short" and "the same target," not one per contributor. Holding on *a* target is not enough: switching from one target
straight to a different one restarts the hold exactly as losing the target outright would, so a sweep across several
distinct targets never reads as one continuous dwell no matter how little empty space separates them. Losing the target
does not cut the reveal instantly either — content already showing fades out over a short window rather than
disappearing on the target's last frame, so a brief, incidental flick off the target does not read as a hard cut.
Held-item content has no such delay in either direction: holding an item is already a deliberate action, not something
the crosshair passes over incidentally, so only the aim-based group needs one.

## Entity outlines

The same one-owner problem exists for outlining entities: more than one feature may want to outline the same entity in
different colors on the same frame (a villager that is both a resting highlight target and the thing under the
crosshair, say). It is resolved with the same tools — declared priority, a deterministic tiebreak, computed once —
applied per entity: the highest-priority contributor to claim a given entity wins that entity's color, regardless of
which contributor happened to run first.

This is a distinct region from the crosshair HUD (a different part of the frame, a different rendering stage, a
different extension seam) that converges on the identical precedence discipline. A future third region should reach for
the same shape rather than re-deriving it: declare priority, break ties deterministically, resolve once, keep
suppression and precedence at the region's owner rather than scattered across contributors.

## What a preview may claim

An ambient surface that describes an action the player has not taken yet is a **preview**, and two rules keep one
honest.

**Show the outcome before the commit rather than explaining the refusal after it.** **U1** says a refused action must
say why, and the reflex it invites is an error message on the failed click. A preview outranks that: it is continuous
rather than one-shot, it costs the player nothing to consult, and it removes the failed click instead of narrating it.
Where a preview covers a gesture, the post-hoc refusal is redundant, and shipping both trains the player to ignore the
one that fires less often. So a gesture with a live preview may be silent on refusal — but *only* while the preview is
actually reaching that player, which is what makes **U6**'s fallback load-bearing here rather than incidental.

**A preview never renders a verdict it did not compute.** The temptation is to reuse the placed thing's vocabulary — its
colors, its counts, its valid/invalid styling — because the preview is describing the same kind of object. That silently
upgrades a guess into a promise: a prospective answer is computed from client state, on a target that moves every frame,
for an object that does not exist yet, and the authoritative answer belongs to the server once it does. Where the
preview genuinely cannot know, it says less and stays neutral, rather than picking whichever verdict looks likelier.
Being wrong in the player's favor is worse than being quiet: they act on the promise and watch it invert the moment the
thing is real.

## Client-state discipline

Anything a region's owner or a contributor caches across frames — a dwell timer, a scan result, a resolved target — is
state that belongs to the current play session, not to the process. Two rules keep it honest:

- **Read elapsed real time from a monotonic source, never from world time.** World time is per-save and can run backward
  relative to a cached deadline when the player leaves one world for another; a monotonic clock cannot regress, so a
  cadence measured against it cannot silently stop firing for the rest of the process.
- **A cached level or entity reference is only as good as its next comparison against the level it came from.**
  Traveling between dimensions replaces the level without ending the play session, so nothing else signals that a cached
  reference has gone stale. State that must not survive a session boundary declares itself as such, so it is reset in
  one place rather than relying on every holder to remember independently.

## Extending a region

A new contributor to either the crosshair HUD or entity outlines declares which question it answers (which of the
region's seams it implements), declares its own priority, and is registered the same way its neighbors are — nothing
about the region's owner changes to admit it. An empty result from a contributor means it has nothing to say for the
current frame, not that the region should render nothing on its behalf; the region already renders nothing only when
every contributor across every relevant seam agrees.
