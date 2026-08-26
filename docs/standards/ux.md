# UX Standard

Conventions for anything a player perceives or acts through: HUD surfaces and GUI screens, block and entity
interactions, item feedback, and the sound and particles that report a result. Assumes `docs/standards/foundations.md`.

## The bar

The reference points are Apple and Nintendo, and they are asking for different things.

Apple's contribution is **honesty and restraint**: direct manipulation over proxy panels, feedback proportional to the
action, an appearance that describes the behavior, disclosure that arrives when it is relevant, and forgiveness for
every exploratory press. Nintendo's is **teaching without text**: the world explains its own rules, the same grammar
holds everywhere so outcomes are predictable, curiosity is always rewarded with *something*, and every state change is
physical enough to feel.

Between them sits a third idea worth naming on its own, because it is the one most often skipped: a player must be able
to undo what they did with what they have (**U10**).

**This list is a floor, not a ceiling.** The rules below are the failures cheap enough to catch mechanically — each one
can be pointed at a diff and answered yes or no, which is the only reason it is written as a rule. Passing all of them
does not make a surface good. A surface is good when a player who has never read anything can act correctly on their
first try and enjoyed doing it, and nothing here measures that. Where a rule and that goal disagree, the goal wins and
the rule is wrong; say so and change it.

---

## Disclosure

### U1 — Silence is never a response

Every player action that is refused, rejected, or fails says so — in the moment, at the point of action. Doing nothing
is the worst available response: it is indistinguishable from lag, a wrong hand, an unloaded chunk, or a broken install,
so the player cannot even form a theory about what went wrong.

A refusal that states its reason is better than one that only states failure, and both are enormously better than
nothing.

### U2 — No gesture is its own only means of discovery

If the only way to learn that a gesture exists is to have already performed it, it does not exist. A confirmation
message on success is not discovery — it rewards a player who already guessed.

Every verb needs a passive path: shown on approach, on look, or by the shape of the object itself. An empty socket
teaches better than a line of text, and costs less.

### U3 — Absence teaches nothing

What is currently unavailable is shown as unavailable, with its reason, rather than hidden. Hiding makes an option
mysteriously missing; showing it dimmed teaches the rule that governs it.

Hiding also moves everything around it. A surface whose contents change position between visits cannot be learned, so
stable positions are worth more than a tidy list.

### U4 — State is legible without acting on it

Anything the player can change, they can see without changing it. State that appears only in response to a gesture is
gated behind **U2**'s problem, and state that appears only briefly is not legible at all — a player who looks away
during the window has no second chance short of acting again.

Persistent state earns a persistent representation. Transient emphasis is for transitions.

## Honesty

### U5 — Representation is a promise

How a thing appears describes what it does. An icon, a model, a color, and a sound each make a claim, and a player who
acts on that claim and is wrong has been lied to by the interface rather than by their own inattention.

The cost is highest where an appearance is already load-bearing elsewhere: reusing a silhouette the player has learned
imports every expectation attached to it, including the ones that do not apply.

### U6 — An action means only what the player can see it means

A gesture must never resolve to something the surface explaining it is not currently showing. If that surface is hidden,
disabled, occluded, or suppressed, the gesture falls back to its safe default — no exceptions, because the player's
information is what makes the action consensual.

This is what makes a sticky mode survivable. A mode is a promise to remember something on the player's behalf; if it can
act while unseen, it is a trap instead.

## Cost and consent

### U7 — Attention is not consent

Looking at something is not choosing it. An ambient surface does not capture the cursor, pause, block movement, or
otherwise have to be escaped from; it is dismissed by looking away, and a mis-open costs nothing.

A surface that must be exited is a modal screen wearing an ambient costume. A screen is legitimate once the player has
committed to it — that is a cost they chose.

### U8 — Ignoring the system costs nothing

Vanilla behavior is a strict subset. A player who never notices a system does what they have always done, and loses
nothing for it. Engagement is opt-in and free; so is opting out.

This also buys compatibility: a negative gesture that falls through to vanilla is the general escape hatch for conflicts
with mods not yet encountered.

### U9 — Spend the player's input budget reluctantly

A modpack has a hundred mods competing for the same keys, and every key claimed is one the player resolves by hand.
Prefer reusing gestures they already own — look, right-click, sneak, scroll — over introducing new ones.

A new binding is not forbidden; it has to earn its place by making the interaction genuinely better, rather than by
being the easiest thing to implement. Where a binding is an alternative route to something already reachable, ship it
unbound by default: zero conflicts, and a path in for players who need it.

## Reversibility

### U10 — Anything the player can set, they can unset

With what they have on hand, at the moment they want to. A setting that can only be cleared by the same item, entity, or
context that established it strands any player who no longer has it — and the interface gives no sign that it has
happened until they try.

Reversal is part of an action's design, not a follow-up feature. An action that cannot be undone must say so before it
resolves, not after.

### U11 — Show the consequence; do not add a step

A confirmation dialog is what a design reaches for once it knows an action is dangerous and has not made the danger
visible. Render the outcome into the point of action instead — what will be spent, given, destroyed, or overwritten,
named at the moment the player is about to commit.

The step is a real cost paid on every safe repetition, and it stops being read almost immediately, so it buys less
protection than showing the consequence and charges more.

## Composition

### U12 — One owner per region of the screen

Two surfaces that can draw in the same place need an explicit rule for which one wins, and one place that rule lives.
Precedence that emerges from registration order, iteration order over an unordered collection, or draw sequence is not a
rule — it is a coincidence that holds until a second contributor exists.

Suppression conditions belong to the region's owner rather than to each surface, or they drift apart and **U6**'s
fallback stops being uniform.
