# Standards

How we work in this repository. Read the one that matches what you are about to touch — they are written to be ingested
independently.

| Standard | Read it before | Ids |
|---|---|---|
| `docs/standards/foundations.md` | anything; it is short, and every other standard cites it | `F1`… |
| `docs/standards/code.md` | writing or modifying Java | `C1`… |
| `docs/standards/testing.md` | writing or reviewing tests | `T1`… |
| `docs/standards/documentation.md` | writing or reviewing reference docs, Javadoc, or comments | `D1`… |
| `docs/standards/platform.md` | touching Minecraft/NeoForge integration or the build | `P1`… |

Comments and Javadoc are governed by the documentation standard, not the code standard — they are documentation that
happens to live in a `.java` file.

Running a review pass over existing work is a procedure rather than a rule, and lives in `docs/workflows/`.

These documents set the bar. Work below the bar gets reworked, regardless of whoever wrote it.

## How to read these

**Force of terms.** *Must*, *never*, and *required* are non-negotiable. *Should* and *prefer* are defaults you may
depart from for a concrete, stated reason. *May* marks an allowed option, not a recommendation.

A justified departure belongs in the review or the implementation handoff. It becomes a durable comment only when the
reason is an enduring fact about the code it annotates. If a mandatory rule would damage a legitimate artifact, revise
the rule rather than take an undocumented exception.

**Citing.** Cite the id rather than restating the rule: *"T5 — this is an interaction assertion; what outcome was it
standing in for?"*

**Stable ids.** Ids are never renumbered or reused. A recycled id fails *silently* — the reader finds a rule, it is the
wrong one, and nothing signals the substitution. A dangling id fails loudly and costs one question. Numbers are free;
keep counting up.

**Retiring a rule.** Grep `docs/standards/` and `docs/workflows/` for the id, fix the live cross-references, then delete
the rule outright. No stub heading: a tombstone sits in the reading path forever to serve citations that stopped being
read weeks ago. Record the burned number in one line at the foot of the document, so the next author does not reach for
it:

```
Retired: <id> → <successor id>.
```

Name a successor only where one exists. A rule whose subject is gone — a platform trap erased by a migration — takes the
number and no arrow; deleting it is the whole record, and Git holds the rest.
