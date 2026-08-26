# Documentation Review Pass

How to run a pass over an existing document, or over the comments in a source file, without making it worse. The bar
itself is `docs/standards/documentation.md`, which this enforces rather than restates.

Scope is bounded by **D8**: a review corrects documentation and may write a new home for a relocated fact. It does not
refactor code, rename anything, or change behavior.

---

## Two passes, not one

A documentation review is a **correctness review that happens to also cut length**. Length falls out; it is not the goal
and not the measure of success.

The rules tell you what does not belong. They say nothing about whether what remains is *true*. Applied alone they
produce a doc that is shorter, more confident, better organized, and still wrong — worse than the original, because the
surviving claims now read as vetted. So a review has two independent passes, and skipping the second is the common
failure:

1. **Placement** — is this fact in the layer that will keep it honest?
2. **Correctness** — is this fact currently true?

A correctly-placed lie is still a lie. In practice the second pass finds more.

## Procedure

**1. Read the whole doc first.** Editing while reading produces local fixes to a document whose overall shape is the
actual problem — and shape problems (a section mirroring one method, a framing sentence that miscasts the subsystem) are
invisible from inside a paragraph.

**2. Classify every claim.** Horizontal and rationale stay. Vertical moves to the owning declaration — and to the right
position within it, since a fact explaining the body's shape belongs inside the body rather than above the signature
(**D12**) — or is deleted if already there. Tunables, hand-maintained inventories, field tables, and narration (**D3**)
become pointers. Chronology (**F4**) is deleted. Do not act yet — classification tells you where things go, not whether
any of it is true.

**3. Verify what you intend to keep**, by blast radius:

- **Absolute claims first.** "Only", "never", "always", "the single", "no path" — the most valuable statements in a doc
  and the most likely to have quietly become false. An exhaustiveness claim is one search: to confirm "X is the only
  consumer of Y", grep every reference to Y and read the exclusions. Never carry one forward on the strength of it
  having been there already.
- **Ordering claims.** If the doc says A precedes B, open the method.
- **Named entities.** Every class, file, package, and config key named still exists and is spelled right. A renamed
  class turns a helpful pointer into a dead end that costs a reader more than silence would.
- **"Complete" sets.** Any table a reader will treat as the full set gets enumerated from the source and diffed.

**4. Verify what you intend to delete.** The step that gets skipped, and the only one that loses information
permanently. Before deleting a fact because "the code owns it", confirm the code **actually** states it and states it
**correctly** (**F2**). If no owner exists — rationale the doc explains and no comment captures — write the home first,
then delete. A one-line comment at the call site is cheap; the knowledge is not recoverable once the paragraph is gone.

**5. Rewrite.** Each section owns one distinct thing. Two sections explaining one mechanism from different angles
diverge exactly as duplicated facts across files do. Spend the reclaimed budget on rationale (**D6**).

**6. Self-audit mechanically**, not by eye. Grep for numerals, dates, and version words. Reread each table asking "does
this need a row when a feature is added?" Reread each pointer asking "have I named this class, or narrated it?" Assume
you violated a rule somewhere; that assumption is usually correct.

**7. Report honestly.** State what you verified and what you did not. Silence reads as full verification and is the
easiest way to launder an unchecked claim into a trusted one.

## Reviewing comments in source

The two passes and every trap below carry over. What differs:

**The unit is a declaration, not a section.** Still read the whole class first. A class Javadoc that miscasts its class
is the stale headline wearing a different hat, and it is what every member Javadoc under it was written against.

**Sort each claim three ways, not two.** Contract stays above the signature. Reasoning about why the body took this
shape moves inside the body, beside the line it defends (**D12**). Rationale spanning components leaves the file
entirely, for the reference doc (**D6**). A Javadoc describing a collaborator's behavior is not a fourth category — it
is deleted (**D2**).

**Name the owner before cutting, then open it.** A fact in source has three plausible owners — the reference doc, an
annotation's own description field, and a comment already inside the body — and one fact told at all three is routine
rather than rare. Grep for it, read what you find, and confirm it says the same thing (**F2**). Where no owner exists,
write one first; step 4 applies unchanged.

**Prove the diff is comment-only.** **D8** bounds the scope, and in source it is mechanically checkable: strip comment
and blank lines from both versions and diff what remains. A whole-file rewrite can silently drop a line of code, and a
review that changed behavior is worse than the comments it fixed.

## Traps

**Lossy consolidation.** The one irreversible mistake available here. See **F2**.

**The exhaustive-looking table.** A table missing one row is worse than no table: it stops the reader from searching,
and they trust it precisely because it is formatted as complete. Enumerate from the source and keep it complete, or per
**D3b** point at the registry.

**Transcription.** A section whose structure mirrors one method's body is drift-in-waiting — it gains a step whenever
that method does, which is never, because nobody editing the method opens the doc. The tell is a numbered list that
reads like a call sequence. Keep only the orderings that are load-bearing, and state *why* each matters; an ordering
constraint with a reason survives a refactor, a transcribed step list does not. The same list in a class Javadoc is the
same failure at closer range (**D12**).

**The Javadoc that defends the body.** A paragraph above a signature arguing why the implementation is shaped this way,
where that reasoning usually already sits as a comment at the code itself. The tell is a Javadoc that reads like a
design decision above a method too short to have one — a delegating overload is the classic. It is not a summary of the
inline comment but a second copy of it, and it is the copy that drifts (**D12**, **F2**).

**Comment density as a signal.** A file that is three fifths comment can be entirely correct, and a terse one can be
mostly lies. Density measures nothing this pass is about; placement and truth are the measures (**D10**).

**The stale headline.** The intro paragraph is the least-reviewed and most-quoted text in any doc, written once when the
subsystem was young and inherited unread through every later edit — and it is what a reader builds their mental model
from. Verify the framing sentence as rigorously as any technical claim.

**The unread diagram.** A diagram is a set of claims and rots like any other, but review skips it because it is not
prose. A missing stage misleads harder than a missing sentence, because a reader takes a picture as the whole story. An
ASCII diagram is almost certainly the stalest thing in the file (**D4**); convert it as part of the pass.

**Deleting homeless rationale.** Not every well-written explanation is misplaced. Some is the only record of why
something is the way it is. Find or build its home before cutting.

**In-flight work.** Uncommitted or half-landed features will not match a doc, and documenting their internals in depth
guarantees rework. Document the shape that exists in the tree.

**Reviewing rules by intent instead of text.** If a rule as written would damage a legitimate artifact, the rule needs a
carve-out, not a silent exception. Fix the rule.

## What good output looks like

Drift is minimized and what remains is detectable. Every surviving line either cannot be regenerated by searching the
code, or is a precise pointer to where the answer lives. Named classes and entry points are deliberate maintenance
couplings — useful, and stale the day someone renames one; that is an accepted cost, not a claim of immunity.

Final check: for three surviving claims, ask whether the claim is owned where it belongs, whether every pointer still
resolves, and whether a routine edit next week would invalidate it *silently*.
