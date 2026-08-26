# Testing Standard

The bar for tests, new and existing. Assumes `docs/standards/foundations.md`.

---

## T1 — Evidence that a test works

Three levels, and they are not interchangeable:

- **Every test must have a named counterexample** — the specific defect that should make it fail. If you cannot name one
  in a sentence, the test does not belong. This is a thought exercise while authoring, not a build command.
- **A regression test should be observed failing** against the regression it protects, before the fix lands.
- **A mutation sample demonstrates suite sensitivity to one defect** — a production line is deliberately broken and the
  suite run against it. Green tests over that path are a signal worth investigating, *not* proof they are worthless:
  they may correctly cover a different aspect of it. One mutation validates one defect. Procedure:
  `docs/workflows/test-review.md`.

Any manual mutation must be restored before the working tree is handed off.

## T2 — Assert behavior, never tunables

Per **F3**. Never assert a tunable's value, and never assert an arithmetic relation to a literal —
`assertEquals(3, SOUND.getRadius())` and `assertTrue(soundRadius > sightRadius * 2)` are the same mistake in different
clothes. Both restate the source, so they verify nothing and break on the next balance pass.

Instead, place the event at `soundRadius` chunks away and assert sound is heard there while sight is not. Retuning then
re-aims the test rather than breaking it. Derive inputs from the production constant; where a test needs a magnitude for
its own arithmetic, declare a test-owned constant.

Relationships between constants *are* worth asserting when they are genuine design invariants. Values that are contracts
rather than tunables (**F3**) may be asserted exactly.

## T3 — Assert meaning, not rendered text

Exact user-facing strings, log lines, `toString()` output, and formatted messages are tunables made of letters. Assert
semantic identity: a key, an id, an enum, a structured field.

Where canonical text or bytes *are* the contract — key ordering, escaping, signatures, byte-for-byte interoperability —
assert the exact representation, and assert the parsed meaning alongside it. See **T10**.

## T4 — Assert decisions, not plumbing

A test earns its place by exercising a decision: branching, boundaries, invariants, arithmetic, ordering, state
transitions, contracts other code relies on.

Purely mechanical assignment and generated plumbing have no decision in them — a test of a field-assigning constructor,
a Lombok builder, a record accessor, or a framework's own behavior can only fail when someone renames something, which
is not a defect. A constructor or default **is** a valid target when it validates, normalizes, enforces an invariant, or
establishes externally meaningful policy.

If you cannot state what a test protects, delete it rather than keep it for the coverage line.

## T5 — Assert outcomes, not interactions

Prefer state and return values. `verify(collaborator).doThing()` asserts *how* the code works, coupling the test to a
structure refactoring is allowed to change.

Interaction assertions are legitimate exactly when the interaction **is** the contract:

- **non-invocation** — "the emitter must not be called when the gate is off"; `never()` is the only way to state it;
- **lifecycle obligations** — a superseded handle must be cancelled, a resource released;
- **boundary traffic** — what crosses a port to an external service.

A `verify` with `any()` in most argument positions and a loose cardinality asserts almost nothing and cannot fail for a
real bug. If that is the assertion, write the outcome it was standing in for.

## T6 — Mock at boundaries, not inside the domain

Use real objects for pure domain types; they are cheap and exercise the actual contract. Reserve mocks for ports and
external boundaries — gateways, transports, anything with I/O behind it. A test with a mock for every collaborator is
testing the wiring diagram it just drew.

## T7 — Minecraft stays out of automated tests

Do not mock or depend on Minecraft components in unit tests. Extract and test domain logic where that separation is
already architecturally justified; do **not** refactor working code solely to make it unit-testable, and do not note in
a comment that a class is Minecraft-free (**D1**).

Minecraft-bound behavior may be left for manual in-game verification. When it is, the implementation handoff must
enumerate the change-specific scenarios to exercise, drawn from:

- normal behavior, and failure or gated behavior;
- fresh entity spawn versus saved-entity load;
- chunk unload and reload; world save and reload;
- single-player versus dedicated server; client/server synchronization;
- lifecycle boundaries — first tick, final tick, delayed callbacks.

**An unexecuted scenario is reported as requested or pending, never as passing.**

## T8 — Tests must be deterministic

Injected time only — `ClockTicks` and `GameTicks` exist so a test can control it. No wall-clock reads, no `sleep`, no
unseeded randomness.

Fixture identity is derived, not random. A random id is invisible until the day it reaches an assertion or a serialized
form, and then it fails with its cause nowhere in the diff.

No shared mutable state, no dependence on execution order.

## T9 — Share builders, never share assertion-relevant values

Shared fixtures cut duplication, keep tests readable, and raise the floor on what a well-formed input looks like. They
fail when they hide the thing under test.

**A test must be readable without opening the fixture.** Whatever makes this case *this case* appears in the test body;
everything incidental comes from the helper. A builder taking overrides — sane defaults in the helper, the salient slot
passed by the caller — satisfies both.

- **Build from real production contracts**, not hand-rolled stand-ins. A test that invents its own descriptor, schema,
  or config can pass while production is broken.
- **Filler must look like filler**, and no test may assert on a filler-derived value.

## T10 — A contract fixture set is discovered, never enumerated

When a fixture directory *is* the contract — a wire format shared with another repository, a codec corpus — discover the
files at test time and fail loudly on a file with no registered case. A hardcoded list silently drops whatever nobody
remembered to add (**F2**, and the same instinct as **D3b**).

Compare parsed structures by default, so formatting cannot cause a false failure; add exact-representation assertions
where the representation is contractual (**T3**).

## T11 — Arrange, Act, Assert; name for behavior

Structure every test as Arrange, Act, Assert. Name it for the behavior and the condition, not the method under test:
`promotes_whenScoreClearsTypeFloor`, not `testPromote`. A test name documents a contract, and per **F4** references no
tickets or phases.

## T12 — No coverage target, but an obligation

There is deliberately no coverage percentage. A number drives exactly the tests **T4** forbids. Coverage tooling is for
*finding* untested decisions, not a goal, and no change is required to move it.

In its place: **new or modified branching, boundaries, invariants, arithmetic, ordering, and state transitions require
behavioral tests** where they are testable without Minecraft. Where automation is infeasible, the handoff states the
verification gap and enumerates the manual scenarios (**T7**).

## T13 — A test that cannot fail is a liability

**Deleting a bad test is a win, not a regression.** A test that cannot fail contributes nothing but the false confidence
of a green suite and the cost of maintaining it; reluctance to delete is what lets a suite decay.

Triage and the procedure for working through an existing suite: `docs/workflows/test-review.md`.
