# Foundations

Principles shared by every standard here. A principle earns a place only if it generates rules in **two or more**
standards; one that feeds a single standard belongs in that standard.

---

## F1 — Co-locate a fact with the edit that invalidates it

For any statement — prose, comment, or assertion — ask who makes it false, and whether they will have this file open
when they do. If not, it is in the wrong place.

A doc restating a constant is invalidated by whoever retunes it, who is editing the config class. A test asserting that
constant's literal breaks for the same person, for the same non-reason. The failure is asymmetric: in tests it is loud
and merely wasteful, in prose it is silent, and the document goes on asserting something false indefinitely.

## F2 — One authority per fact

Every authored fact has one authoritative, manually maintained source. **Manually maintained copies are prohibited.**

Not every second occurrence is a copy. Three things are legitimate:

- an **independent contract assertion** — a test that states an observable contract in its own terms is an oracle, not a
  duplicate. It must not compute its expectation through the production path it is testing.
- a **generated representation** — produced from the authority by tooling.
- a **mechanically validated representation** — kept honest by a check that fails when it diverges.

What is prohibited is a hand-written restatement that nothing keeps equal.

**Consolidation safety.** When collapsing a duplicate, verify against the authority *before* choosing which copy dies.
Duplicated facts have usually already diverged — that is what duplication does — so one copy is probably wrong, and it
is not always the one you expect. Deleting the accurate copy to preserve a stale authority is the worst outcome this
rule can produce, because the survivor is then unchallenged.

## F3 — A tunable's value is not a contract; the behavior it produces is

Constants get retuned after playtesting; that is what they are for. Restating a tunable's value adds no information and
converts every retune into unrelated breakage elsewhere. State or assert the **behavior** it produces, and the
**relationships** between values that are genuine design invariants — sighting a zombie must out-weigh sighting another
villager. The ordering is the contract; the number is not.

Derive magnitudes from the constant rather than hardcoding a matching literal. Where a specific magnitude is needed for
its own sake, declare it locally rather than borrowing a production one.

**This rule governs tunables, not every number.** A value that is itself a contract may be stated exactly: protocol
identifiers, serialized-format requirements, units, hard compatibility limits, and user-visible defaults an operator
needs. The test is whether changing it is a retune or a breaking change.

## F4 — Durable artifacts record no chronology

No implementation phases, commit stages, review rounds, dates, or verification stamps — "the C1 addition", "added in
phase 4", "unlike the old system", "flagged in review", "(verified 2026-07-14)". Current-state documentation must make
sense to a reader who knows nothing about how the code got here. Git owns the sequence.

Where future re-work needs a marker, use a `TODO: <what needs to happen and why>`. `TODO`s are pruned periodically and
act as explicit markers; prose flags silently rot.

**Artifacts whose purpose is history are exempt** — changelogs, migration guides, compatibility notes, audit records,
architectural decision records, and working design docs under `docs/working/`.

Durable rationale **may** describe a rejected alternative when the reason still protects the current design. State the
enduring constraint, not the sequence of past work: *"merging was rejected because it asserts two things were one"*,
never *"we tried merging in phase 3"*.
