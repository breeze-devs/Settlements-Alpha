# Code Standard

Conventions for Java here. Assumes `docs/standards/foundations.md`. Comments and Javadoc are governed by
`docs/standards/documentation.md`; Minecraft and build traps by `docs/standards/platform.md`.

---

## C1 — Dependency inversion, wired by Dagger

Depend on interfaces owned by the consuming layer; let Dagger supply the implementation. Domain code never reaches into
infrastructure, and a port's implementation lives on the far side of the boundary it serves. Follow Dagger conventions
for scopes, modules, and multibindings rather than inventing parallel wiring.

**Every project-defined `@Qualifier` placed on a field that feeds a Lombok-generated constructor must be listed in
`lombok.copyableAnnotations`.** Lombok drops an unlisted qualifier silently: it compiles, runs, and injects the wrong
binding. Qualifiers used only on explicit constructor parameters or `@Provides` methods do not need the entry.

Keep same-type bindings qualified so a mis-wire fails at compile time rather than at runtime.

`TODO: add an automated check that every applicable qualifier is listed in lombok.config — this failure is silent and too consequential to rely on review memory.`

## C2 — Builders for caller-constructed values

Prefer a Lombok builder for wide values a caller assembles: DTOs, records, configuration objects, test fixtures. At 4 or
more arguments, positional calls stop being readable and a transposed pair of same-typed arguments compiles cleanly.

Do **not** add a builder to a Dagger-owned service because it has many dependencies. Dagger owns that construction path;
see **C3**.

Gson bypasses Lombok builders and `@Singular` defaults entirely, so a wire DTO's nullable collections must be normalized
at the gateway rather than trusted to a builder default.

## C3 — Constructor injection

Generated constructor injection is the default where construction is straight assignment:

```java
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
```

Write an explicit `@Inject` constructor when construction must validate, normalize, defensively copy, index, or derive
state.

Injected dependencies are `private final`. No field injection, no setter injection.

## C4 — Absence contracts

**Non-null is the default, and only nullability is marked.** An unannotated return is non-null; do not write `@Nonnull`
on one. Restating the default on every declaration is what makes the marked case hard to spot, and the marked case is
the only one a reader has to act on. A return that can be null carries `@Nullable`, on the declaration and on every
override. Parameters are unaffected — they carry their explicit annotations as the surrounding code already does.

Prefer `Optional<T>` for interface and method return contracts where absence is meaningful — that is where an explicit
absence contract earns its keep.

A return **may** instead use `@Nullable T` where allocation sensitivity justifies it (sufficiently frequent tick
processing) or a framework convention requires it. Producing a present `Optional` may allocate; the JVM may elide it,
but code on a hot path must not depend on that. The declaration and all its overrides must carry `@Nullable`, and
callers must handle absence explicitly before dereferencing.

Never use `Optional` as a field, method parameter, or collection element. Local `Optional` variables **are** allowed
when consuming or composing an `Optional`-returning API — do not convert one to `null` merely to avoid a local.

Use one `@Nullable` annotation family repo-wide.

## C5 — Import, never inline

No fully-qualified class names in code. Use a non-static import; a static import only where the call site genuinely
reads better for it. An inline FQCN is acceptable only to resolve a genuine name collision.

## C6 — Always brace

No inline or brace-less `if`. Every conditional and loop body gets braces, single-statement or not.

## C7 — Prefer domain time over raw ticks

Express durations and cadences through the domain interfaces — `ClockTicks` for nearly everything, `GameTicks` where
world time specifically is meant. A bare `int` of ticks loses a distinction the domain draws, and the two are not
interchangeable.

## C8 — A private constructor declares which kind of class this is

A class callers never instantiate says so with a Lombok constructor annotation, and **which** annotation is
load-bearing — it is how a reader tells the two cases apart at a glance:

- **Stateless class**, only static members: `@NoArgsConstructor(access = AccessLevel.PRIVATE)`. Instance state appearing
  here later is a mistake, and this is the form that catches it — an uninitialized `final` field fails the build.
  Reaching for `force = true` to quiet that means the class stopped being stateless; give it a real constructor instead.
- **Value class reached through a static factory**, where the constructor is private because the factory is the only
  admissible way in: `@AllArgsConstructor(access = AccessLevel.PRIVATE)`. A new field must break every factory, and here
  it does, because the factories are real call sites.

`@AllArgsConstructor` on a stateless class is the one combination that fails silently: with no fields it generates the
same no-arg constructor, and when a field arrives it absorbs it into the signature that nothing calls.

Never hand-write an empty private constructor. It states nothing the annotation does not, in more lines.

## C9 — Qualify a type name that escapes its package

A type takes its owning component's name as a prefix when **both** hold: its bare name is a general domain noun, and it
is referenced from outside its own package. `OccurrenceAttentionPolicy`, not `AttentionPolicy` — a villager also pays
attention to behaviors, players, and conversations, and the reader at the call site has only the name.

Types used only inside their own package keep the short name. The package already qualifies them, so a prefix there is
stutter, and stutter trains readers to skim prefixes rather than read them.

Apply the rule uniformly once a type meets both conditions. A rule applied only where it feels contestable is one the
next contributor has to relitigate per type, which costs more than the longer name it saved. A uniform application that
produces an unbearable name is evidence against the rule, not grounds for an exception.
