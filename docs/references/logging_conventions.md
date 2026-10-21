# Logging Conventions

How Settlements code logs: getting a logger, choosing its category and each line's severity, passing exceptions, and
keeping arguments cheap. Read it before adding or changing a log line. Two priorities shape every rule here: logging
must never be the reason the mod lags during play, and the default server console must stay useful to the operator who
reads it.

[ILogger][ilogger] is the interface Settlements code logs through, and [LoggerFactory][logger-factory] creates its
instances. The [debugging configuration](#configuration) decides what Settlements forwards to the host's Log4j.

---

## Getting a logger

Declare the logger with Lombok's `@CustomLog`, giving the class's category as its topic:

```java
@CustomLog(topic = "behavior")
public class ShearSheepBehavior extends VillagerStateMachineBehavior {
```

[lombok.config][lombok-config] turns the annotation into a static `log` field created by `LoggerFactory`. A class
without a topic logs in the `general` category. Topics are the lowercase snake-case names [LogCategory][log-category]
declares.

The topic is a string literal, so the compiler cannot check it. An unknown topic fails at class initialization instead,
with an `IllegalArgumentException` that names the topic, the class, and the known topics. The JVM reports it as an
`ExceptionInInitializerError` the first time anything touches the class, and as a `NoClassDefFoundError` on every later
use. A misspelled topic therefore never logs silently in the wrong category, but it surfaces only when the class first
loads.

For an occasional call that belongs to another category, use `log.as(category)`:

```java
log.as(LogCategory.WORLDGEN).debug("Resolved template {} for {}", templateId, buildingType);
```

`as` returns a cached view under the same logger name, so it allocates nothing on a logging path. When most of a class's
calls belong to another category, change its topic instead.

A behavior passes its own logger to [AbstractBehavior][abstract-behavior], so the runtime's lines about its runs carry
that logger's name and topic. A behavior therefore declares `topic = "behavior"` even when it makes no call of its own;
without it, those lines log as `general`.

Code that cannot use Lombok calls `LoggerFactory.getLogger` directly. Two overloads mirror the annotation with and
without a topic, and a third takes a name, for a logger not named after a class.

## Choosing a category

A category is the feature family someone filters a log for, independent of severity and of the package a class sits in.
Each class has one, through its topic. [LogCategory][log-category] declares the set, and each constant's Javadoc says
which code belongs to it. The set is closed by design: operators filter on each category, so adding one is a design
decision.

Three boundaries are deliberate:

- `planning` is separate from `behavior` so that diagnosing a villager's day reads the lane decisions without every
  behavior's narration.
- A precondition that scans the world is `behavior`, not `sensor`. It answers one behavior's question; sensors maintain
  state that many consumers read.
- TOML configuration is `general`, not `resource_loading`. NeoForge reads the TOML files, and an operator filtering
  `resource_loading` is looking for datapack problems.

A class that serves two families takes the topic of the one most of its calls belong to, and reaches the other through
`as`.

### Adding a category

1. Add a constant to [LogCategory][log-category] with a lowercase snake-case topic and an uppercase marker name.
2. Choose a marker name that NeoForge's Log4j configuration does not filter on (see [Traps](#traps)).
   [LogCategoryTest][log-category-test] fails on such a name, and on a marker name two categories share.
3. State in the constant's Javadoc which code belongs to the category. When its boundary with an existing category is a
   judgment call, add the reason to the deliberate boundaries above.

A category never adds methods to `ILogger`. Its classes reach it through their topic and call the severity methods.

## Choosing a severity

| Level | Use for |
|---|---|
| ERROR | A defect, or broken data or configuration, that leaves the mod doing less than it should |
| WARN | Something unexpected that the handling fully absorbs, and that is worth acting on: an operator or modpack author can fix its cause, or it is a code defect to report |
| INFO | Rare operator milestones only |
| DEBUG | Per-operation narration, per-villager outcomes included |
| TRACE | Per-tick, per-check, per-candidate, and per-iteration detail |

**ERROR or WARN.** The deciding question is whether the handling leaves the mod whole. A fallback that keeps play going
does not make a problem WARN when the mod still does less than it should. Applied:

- Saved data: losing a whole record is ERROR, repairing or defaulting part of one is WARN, and the pruning an expected
  migration performs is DEBUG.
- An optional service an operator enabled that keeps failing is ERROR, even though the mod falls back without it: the
  operator enabled something that does not work.
- A retry: each failed attempt the retry absorbs is WARN with its throwable, and giving up is one ERROR.
- A code defect caught at run time, such as a key missing from a registry the code itself fills, is ERROR.

**Expected gameplay outcomes are never WARN or ERROR.** No path, a target gone, an item missing, a precondition not met,
and an interruption are part of play; log them at DEBUG or TRACE. Never raise a line's severity to make it visible. To
see narration while investigating, lower `standard_log_level` instead.

**INFO is for rare operator milestones,** such as the configuration summary at startup or a settlement's founding. At
the default threshold every INFO line reaches every server console, so nothing per villager, per behavior run, or per
data load is INFO.

**Per-villager narration is DEBUG, outcomes included.** A trade, a tame, or a catch recurs for every villager every day,
and at INFO it would flood the console.

**Per-tick and per-check detail is TRACE:** each precondition miss, each candidate considered, and each iteration.

**A behavior run logs one start line and one stop line.** At DEBUG, a run's lifecycle reads as its start, its stage
transitions, and one stop line that carries the stop reason; the runtime's other lifecycle lines are TRACE. The
behavior runtime, [AbstractBehavior][abstract-behavior] and [StateMachineBehavior][state-machine-behavior], owns those
lines. A behavior adds no lifecycle lines of its own, though it still narrates what it decides and achieves: it
explains a stop through the reason it passes to `requestStop`, an expected failure through the code and details of a
[StepResult][step-result] fail, and a defect through an abort with its cause. A fail reaches the stop line at DEBUG, as
the expected outcome it is, and the runtime logs an abort at ERROR with the cause's stack trace.

**Log an event once.**

- Not before a rethrow. Put the context into the exception's message, and let the code that handles the exception log
  it.
- Not before `requestStop` or a returned fail. The runtime logs the reason.
- Not in both a callee and its caller. The class that owns the failure logs it; a caller that only sees the consequence
  logs at DEBUG or not at all.
- Not as an attempt line followed by an outcome line. Log the outcome, with the target in it.

**No log-once guards or latches.** A line that repeats because its handling is wrong is fixed in its handling, so the
state that keeps producing it ends: close the broken session, or record the cooldown on a skip. A guard would hide the
repetition while the faulty state stays in place.

## Exceptions

Pass the original throwable as the last argument, with no `{}` placeholder for it. This is Log4j's convention: an
unconsumed trailing throwable becomes the event's exception, and its stack trace is printed. A throwable consumed by a
placeholder is formatted as an ordinary argument, and its stack trace is lost. `ILogger` has no throwable-first
overloads.

```java
} catch (IOException e) {
    log.error("Failed to save settlement {}", settlementId, e);
}
```

- Never pass only `e.getMessage()` or `e.toString()`. The stack trace is lost, and `getMessage()` can be null.
  Formatting the message into a placeholder beside the throwable only repeats it.
- A deliberate cancellation is not a failure. Work cancelled at server stop or superseded by newer work completes with a
  `CancellationException`, often wrapped in a `CompletionException`. Unwrap the `CompletionException` to its cause, and
  skip a `CancellationException` rather than logging it as an error.

## Argument cost

Settlements decides whether to admit a call before it does anything else. A rejected call returns before the message is
formatted, before any argument's `toString` runs, and before Log4j is reached. What no gate can skip is the caller's own
work: Java evaluates every argument expression, boxes primitive arguments, and allocates the varargs array before the
call.

- Pass objects, not strings built for the message: `entity.getUUID()`, not `entity.getUUID().toString()`. The object's
  `toString` then runs only for an admitted line.
- Keep argument expressions cheap, because they run at every level. Prefer a value the object already holds to one it
  builds on demand: `entity.getUUID()` rather than `entity.getName()`, which builds a text component.
- An argument expression that can throw breaks its caller at every level, including when the line would be rejected.
- Never concatenate into the format, and never build it with `String.format` or `.formatted`. The string is then built
  at every level, and a value containing `{}` is read as a placeholder. The format is a constant.
- Guard work that grows with its input, such as a stream over a collection, with `isEnabled`:

  ```java
  if (log.isEnabled(LogLevel.TRACE)) {
      log.trace("Candidates: {}", candidates.stream().map(Candidate::id).toList());
  }
  ```

- A disabled call still pays its varargs array and the boxing of its primitive arguments. A TRACE line on a per-tick or
  per-candidate path therefore deserves a look: guard it, trim its arguments, or drop it.

There is no supplier or lazy-argument API. A lambda that captures locals still allocates on every call, and `isEnabled`
covers every case a supplier would, with no allocation.

These rules bind play paths. A line that runs once at startup may afford a costlier argument, as long as the argument
cannot throw.

## Configuration

Logging settings live in `config/settlements/debugging.toml`, section `[logging]`. [LoggingConfig][logging-config] owns
the keys and their descriptions:

- `enabled` is the master switch. When it is false, Settlements logs nothing, warnings and errors included; Minecraft,
  NeoForge, and other mods log as usual.
- `standard_log_level` is the least severe level Settlements forwards to normal logging, or `OFF`. It defaults to
  `INFO`; [LogThreshold][log-threshold] holds the accepted values.

Changes take effect at the next game start. Closing and reopening a single-player world is not a restart.

Settlements' threshold sits in front of Log4j, so a call below it never reaches Log4j. An admitted line then goes
wherever the host's appenders send it. A normal NeoForge install sends INFO and above to the console and `latest.log`,
and DEBUG and above to `debug.log`. With `standard_log_level = DEBUG`, a DEBUG line therefore reaches `debug.log` only,
and an admitted TRACE line reaches no file unless the host's Log4j configuration changes as well. Dev runs (`runClient`,
`runServer`) use ModDevGradle's generated Log4j configuration instead: its root level is the `logLevel` in
[build.gradle][build-gradle], and its console prints everything that level admits.

## Traps

- **Categories surface as Log4j markers.** A category other than `general` reaches normal logs as a marker named after
  it, which the host's layouts print beside the logger name, as in `[…HarvestPumpkinBehavior/BEHAVIOR]`. Search a log
  for the marker to filter a category. A `general` line carries no marker.
- **A marker NeoForge filters on would bypass levels.** NeoForge's Log4j configuration accepts or denies events by
  marker name before any appender's level applies, each marker by a switch with its own default. A category sharing one
  of those names would have its lines below WARN kept or dropped by that switch instead of by levels.
  [LogCategoryTest][log-category-test] checks marker names against its own copy of NeoForge's list, so recheck that copy
  against the host's `log4j2.xml` when NeoForge is upgraded.
- **`isEnabled` answers only Settlements' admission.** In a normal install, the host's root logger admits every level
  and leaves rejection to its appenders, so Log4j itself reports every level enabled, and Settlements' own threshold is
  all `isEnabled` can report. A true answer does not mean the line is visible anywhere: an admitted TRACE line still
  reaches no file there. Use it to skip argument work, not to infer what an operator sees.
- **The policy is installed once per game run.** The mod constructor in [SettlementsMod][mod-entry] installs it from the
  debugging configuration before anything else; a second install throws `IllegalStateException` and leaves the first in
  force. Loggers created earlier, in static initializers, follow the default policy, `DEFAULT` on
  [LoggingPolicy][logging-policy], until the install. So does every logger in a JVM where no mod constructor runs, such
  as a unit test run. A unit test therefore never calls `LoggerFactory.installPolicy`: the first install binds every
  later test in the same JVM, and every later install throws.

[ilogger]: ../../src/main/java/dev/breezes/settlements/shared/logging/ILogger.java
[logger-factory]: ../../src/main/java/dev/breezes/settlements/shared/logging/LoggerFactory.java
[log-category]: ../../src/main/java/dev/breezes/settlements/shared/logging/LogCategory.java
[log-category-test]: ../../src/test/java/dev/breezes/settlements/shared/logging/LogCategoryTest.java
[log-threshold]: ../../src/main/java/dev/breezes/settlements/shared/logging/levels/LogThreshold.java
[logging-policy]: ../../src/main/java/dev/breezes/settlements/shared/logging/LoggingPolicy.java
[logging-config]: ../../src/main/java/dev/breezes/settlements/infrastructure/logging/LoggingConfig.java
[lombok-config]: ../../lombok.config
[build-gradle]: ../../build.gradle
[mod-entry]: ../../src/main/java/dev/breezes/settlements/SettlementsMod.java
[abstract-behavior]: ../../src/main/java/dev/breezes/settlements/application/ai/behavior/runtime/AbstractBehavior.java
[state-machine-behavior]: ../../src/main/java/dev/breezes/settlements/application/ai/behavior/runtime/StateMachineBehavior.java
[step-result]: ../../src/main/java/dev/breezes/settlements/application/ai/behavior/workflow/steps/StepResult.java
