# Documentation Standard

The bar for documentation. Assumes `docs/standards/foundations.md`.

---

## What this governs

**Durable documentation** must stay true or be deleted: reference docs in `docs/references/`, class/member/package
Javadoc, and inline comments.

**Working design docs** in `docs/working/` are the deliberate exception — they record a moment, so dates, phases,
rounds, and superseded proposals belong in them and none of the rules below apply, except **D11**, which exists for
them. Never cite one from durable documentation; a doc or comment that leans on one inherits its staleness the day the
design moves on. Other history-bearing artifacts are exempt under **F4**.

Standards here are normative documents rather than subsystem guides, so **D7** does not bind them.

## The three layers

Each owns a different axis. A fact on the wrong axis cannot stay true (**F1**), because whoever invalidates it will not
have that file open.

| Layer | Owns | Admission test |
|---|---|---|
| Reference doc | The **horizontal** — how components relate, ordering that spans them, cross-cutting invariants, architectural rationale, where to start reading | Would this survive a full internal rewrite of every class it names? |
| Class and member Javadoc | The **vertical** — this declaration's contract, fields, defaults, invariants, lifecycle, threading | Is this a fact about *this* declaration, and would it stay true if the body were rewritten? |
| Inline comment | Reasoning the code cannot carry — why complex logic takes this shape, load-bearing ordering, a non-obvious loophole or trap, an intentional choice that reads like an oversight. It may govern the block that follows, not just one line. | Would a competent reader otherwise change, reorder, or "simplify" this? |

---

## The rules

### D1 — Explain why, not what

Complex methods and logic areas must carry their reasoning. Never narrate mechanics the code already states.

Never restate a project convention the reader can find in the conventions themselves. **State the constraint, not the
benefit it yields under our conventions** — "a domain type must not name a Minecraft position type" is the durable
reason; "so it is testable without mocks" is a restatement of how we test, and it ages with the convention rather than
with the code.

Local implementation rationale belongs here, beside the logic it protects — inside the body, not hoisted into the
Javadoc above it (**D12**) and not into a reference doc.

### D2 — Javadoc must not reach across

A comment or Javadoc describes the code it annotates, not the internals or usage of other methods or classes — not how a
return value is consumed downstream, not when or by whom a method is called. Such cross-references drift when the
distant code changes while the comment stays behind. Only reach across when omitting it would severely hurt
understandability, and then describe the contract, not the implementation.

A declaration may **name** its immediate collaborators; it may not describe what they do. *"Runs between the gate and
the factory"* is orientation and survives their rewrites. *"The gate decides whether the occurrence reaches the
observer"* imports the gate's contract, and goes stale when the gate changes.

### D3 — Reference docs must not reach in

The mirror of **D2**, and the same rot in the opposite direction: a doc paragraph describing the inside of one class is
as much a violation as a Javadoc describing a distant one. A reference doc never contains:

- **D3a — tunable values.** Per **F3**, state the invariant or ordering a magnitude enforces, not the number. Values
  that are contracts rather than tunables may appear.
- **D3b — hand-maintained inventories of instances.** Any list that must gain an entry when a feature is added is a
  future liar, and it reads as exhaustive long after it stops being so. Point at the registry, factory, or DI module
  that owns the set; that also teaches where to add one. Legitimate exceptions: a generated or mechanically validated
  inventory, a set closed by design (where adding a member is itself a design change), and checklists of *seams*, which
  are stable and are themselves the contract.
- **D3c — field tables.** One row per field of one class is field Javadoc in exile.
- **D3d — implementation narration.** A doc may name a class and give its one-line role; the moment it explains how that
  class does its job, the source has forked.

### D4 — Diagram in Mermaid, never ASCII art

Mermaid renders on GitHub and in the IntelliJ Markdown preview, costs roughly half the characters, and edits are one
line, where ASCII art requires re-padding every row to rename one node. That cost is why ASCII diagrams are the first
thing in a doc to rot: they are expensive enough to edit that people leave them alone.

Below about three nodes, an inline arrow in prose beats both. Javadoc and comments get prose or an inline arrow only,
since Mermaid does not render there. Every rule here binds diagrams — no tunables in node labels (**D3a**), no diagram
needing a new node per registered type (**D3b**).

### D5 — Stable precision, not deliberate vagueness

State a precise claim at its authority. Where volatile detail cannot stay coupled to the document, give a precise
pointer to the authority rather than duplicating the detail.

Vague prose is not a virtue — it can mislead while being hard to falsify. It is only the fallback: when precision cannot
be maintained and no pointer exists, vague beats false. A confidently wrong statement costs a debugging session and can
be argued from in a design decision.

### D6 — Cross-cutting rationale is doc-exclusive

Why a design is shaped this way across components, what alternatives were tried, what broke, what a contributor will be
tempted to "fix" but must not. Code cannot hold this and stays silent about it forever, so this is the part of a
reference doc that should grow.

Scope: architectural rationale spanning components. Local reasoning stays in comments (**D1**); chronology and
superseded drafts stay in working docs (**F4**).

### D7 — Aim a reference doc at a contributor's first hour

Entry points, the shape of the flow, the invariants, and the traps that cost a day; everything else is reachable from
those. Brevity is not the goal — every surviving line being load-bearing is.

### D8 — A documentation change never changes behavior

A documentation edit — including a review pass — may correct comments and Javadoc, and may write a new home for a fact
relocated out of a doc. It may **not** refactor code, rename anything, or change behavior.

Where a comment is wrong because the *production behavior* is wrong, report the defect and leave the documentation alone
until a separate code change establishes the correct owner. Editing the doc to match broken code deletes the accurate
account and leaves the survivor unchallenged — the failure **F2** warns about, wearing a different hat.

### D9 — Comments are code-first

Comments and Javadoc are read as source — in an editor, a diff, or a review — never as a generated HTML page. Markup
that only pays off once rendered is therefore pure cost: it inflates the line it decorates and forces a re-flow on every
edit. This rule governs Java comments only; reference docs are Markdown and are written as Markdown.

Write a list as plain lines — `- item` for an unordered list, `1.` `2.` where the order is the point. Never `<ul>`,
`<ol>`, `<li>`, `<b>`, or an HTML entity escape. `<p>` is the sole permitted tag: one token, and the only markup that
still buys something, since a bare blank line collapses and the paragraph break is lost with it.

`{@code}` is prohibited around a bare identifier, literal, member, or type name — write the name. It is warranted only
where the text contains `<`, `>`, or `&`, such as a generic signature, and the alternative would be an entity escape.

`{@link}` **may** stay, because unlike `{@code}` it is behavior rather than decoration: the IDE navigates it, and a
rename rewrites it, where a plain name in prose goes stale silently (**F1**). Link a reference a reader would want to
follow; do not link every name a sentence happens to mention.

### D10 — State a fact once

A fact usually earns one sentence. A qualifier repeated in a second clause, a trailing recap, or both the class Javadoc
and the field Javadoc it describes adds length without adding information — and costs the reader's attention on the
second telling, not just the writer's on the first.

This trims phrasing, not reasoning: **D1**'s "why" stays — in its shortest true form. Complexity earns length;
thoroughness does not. The test is whether a sentence could be deleted with no fact lost, not whether the paragraph
could be shorter.

### D11 — A design doc has a companion roadmap

Every design doc under `docs/working/` is paired with a roadmap doc beside it — `<design>.md` and
`<design>-roadmap.md`. The design doc is the authority on *why*; the roadmap owns the order of work, the done criteria,
and the status, and nothing else holds status for that work. A design that is not yet scheduled, or already shipped,
still has the pair: the roadmap says so and states what has to happen before it can be filled in, or where the record of
the finished work went.

A roadmap doc must tell whoever implements from it to check its items off **actively, at the moment each item is
verified** — not at the end of a session, and never on "should work". Status that lives in an implementer's head or a
chat transcript is status nobody else can read, and a roadmap whose boxes lag its code is the same lie **F1** guards
against, wearing a checklist. Where several roadmaps are sequenced by one orchestrating tracker, that tracker holds a
mirror of each roadmap's status and nothing about its phases; the roadmap's close-out step is what updates the mirror.

### D12 — Javadoc answers the caller, the body answers the maintainer

Javadoc states what a caller needs in order to call correctly: what it does, what to pass, what comes back, what it
throws, and what it costs them to get wrong. An architectural defense of the implementation above the signature makes
every caller read a design document to learn a signature, and it drifts besides — the next person to change the body is
inside it, not above it.

The split is mechanical. A fact that stays true after the body is rewritten is contract, and belongs in the Javadoc. A
fact that explains the body's shape is an inline comment, on the line it defends. Rationale spanning components leaves
the file entirely (**D6**).

A hard invariant earns as many sentences as it takes. What is never earned is the same reasoning told twice, or told to
a reader who did not ask. See *Worked examples*.

---

## Worked examples

### D12 — where a fact goes

**The base case.** The reasoning is not deleted; it moves to where its reader is.

```java
/**
 * Admits a row onto the given calendar day.
 * <p>
 * Pruning first, and against the row's own day rather than against a clock, is load-bearing twice
 * over. It is this lane's only live enforcement of the window -- every other prune caller runs at
 * load time -- so without it a subject loaded once and never again would accumulate days for as
 * long as it kept opening rows. And because the day pruned against is the one being written to,
 * replaying persisted rows in the order they were opened reproduces exactly the prune history that
 * already happened live, rather than a different one derived from whenever the reload occurs.
 */
```

```java
/**
 * Admits an entry onto the given calendar day, pruning entries outside the retention window first.
 *
 * @throws IllegalArgumentException if an entry with the same id already exists
 */
public Admission openEntry(long calendarDay, Entry entry) {
    // ...
    // Prune on write using logical day (not wall-clock) to bound memory and preserve replay determinism
    prune(calendarDay);
```

**A method's Javadoc defends its own body, not a callee's.** The paragraph below sits above a single delegating line —
the method holds no cursor, no offset, and no draw:

```java
/**
 * Runs the greedy interleaving walk over the window, delegating each step's choice to the strategy.
 * <p>
 * A random start-phase offset is added to the initial cursor so peers generated in the same tick do
 * not produce identical layouts. The offset draws from the seeded RNG and is frozen into the
 * persisted result; the anti-lockstep property still holds because the seed is per-subject and
 * per-day, not because the draw is live. The offset and the gap advance belong to the walk itself
 * rather than to the selection, so they apply identically whichever strategy is supplied.
 */
public static List<Slot> pack(List<Candidate> pool, int start, int end, Strategy strategy, Random rng) {
    return pack(pool, start, end, strategy, rng, new HashMap<>());
}
```

The paragraph describes the *overload's* body, where that reasoning usually already sits as a comment beside the draw.
It is therefore not a summary but a second copy (**F2**), and the two diverge. Keep the first sentence; delete the rest.

**A class's Javadoc holds identity and invariants, never a walk of one method.**

```java
/**
 * Pipeline that activates the per-subject observation scaffolding.
 * <p>
 * On each call to tick it:
 * <p>
 * 1. Drains the delta from the bus using the subject's cursor.
 * 2. Runs each new event through the gate, discarding what this subject could not perceive, then
 *    past the router, which decides between the two stores.
 * 3. Reduces each surviving event's bindings to the subset this observer could see.
 */
```

The list gains a step whenever the method does — which is never, because nobody editing the method scrolls up to it.
Sequence spanning collaborators is the reference doc's axis, and a doc keeps only the orderings that are load-bearing,
each with its reason; steps 2 and 3 also import the gate's and the router's contracts (**D2**). What belongs here is the
class's identity and the invariant it enforces — *an entry is first-hand or it does not exist* — which survives any
rewrite of the body.

**Length is not the measure.** Nothing below is bloat, at three fifths comment lines:

```java
/**
 * Outcome of adding an entry to a day.
 * <p>
 * Unlike a capacity-bounded store, a day never refuses an entry for being one too many -- the
 * diagnostic ceiling only ever warns. The one real refusal is a sealed day.
 */
public enum Admission {

    /**
     * Added, and the day's diagnostic ceiling was not exceeded.
     */
    ADMITTED,

    /**
     * Added, but the day's count is now over its diagnostic ceiling -- worth logging, never worth
     * refusing the entry over.
     */
    ADMITTED_OVER_DIAGNOSTIC_CEILING,

    /**
     * The day is sealed and admits no new entries.
     */
    REFUSED_DAY_SEALED

}
```

Every line is a fact a caller needs, stated once, at the declaration that owns it.

---

## Holding work to this bar

Reviewing an existing document, or the comments in a source file, is a procedure rather than a rule:
`docs/workflows/documentation-review.md`.
