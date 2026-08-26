# Test Review Pass

How to work through tests that already exist. The bar itself is `docs/standards/testing.md`, which this enforces rather
than restates.

---

## Triage

Every test lands in one of three buckets:

- **keep** — exercises a real decision and can fail for a real reason;
- **rewrite** — right target, wrong assertion. Constant-asserting (**T2**), text-asserting (**T3**), and
  interaction-asserting (**T5**) tests usually have a good outcome assertion hiding behind them;
- **delete** — vacuous, tautological, or testing the framework (**T4**).

Rewrites and additions apply directly. **Deletions are flagged rather than performed**, then applied in bulk once the
set is confirmed. **T13** is why the delete bucket is not a last resort.

## Sampling suite sensitivity

To check that the suite detects a defect rather than merely covering the line (**T1**):

1. Mutate one production line — invert a condition, drop a filter, return a constant.
2. Run the suite.
3. Restore the line.

One mutation validates one defect. Green tests over the mutated path are a signal worth investigating, *not* proof they
are worthless — they may correctly cover a different aspect of that line.

Per **T1**, no mutation may survive into a handed-off working tree.

## Reporting

State which tests you ran and which you only read. Where behavior was left to manual in-game verification, enumerate the
scenarios (**T7**) and report each as requested or pending — never as passing.
