# Packet Handler Annotation Auto-Registration (Low-Level Design)

## Context

Current network architecture has:

- `PacketRegistry` as NeoForge payload wiring (`playToClient` / `playToServer`)
- `ClientSidePacketReceiver` and `ServerSidePacketReceiver` doing orchestration + handler map lookup
- hardcoded handler maps inside receivers

Target direction:

- keep composition root explicit
- reduce boilerplate for handler registration
- improve safety via startup validation

---

## Goals

1. Keep `PacketRegistry` as the explicit wiring composition root.
2. Move packet->handler mapping out of receivers.
3. Auto-register handlers from annotation metadata (`@HandleClientPacket` / `@HandleServerPacket`).
4. Fail fast at startup on duplicate/invalid bindings.

## Non-goals (Phase 1)

1. Do **not** auto-register NeoForge payloads yet.
2. Do **not** remove explicit packet wiring from `PacketRegistry` yet.

---

## Proposed New Types

### 1) Explicit side-specific annotations

**File**: `src/main/java/dev/breezes/settlements/infrastructure/network/packet/annotations/HandleClientPacket.java`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HandleClientPacket {
    Class<? extends ClientBoundPacket> value();
}
```

**File**: `src/main/java/dev/breezes/settlements/infrastructure/network/packet/annotations/HandleServerPacket.java`

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HandleServerPacket {
    Class<? extends ServerBoundPacket> value();
}
```

Rationale:

- no `side` argument to accidentally mismatch
- code search clarity (`HandleServerPacket` immediately reveals server handlers)
- simpler validation and cleaner mental model

### 2) Descriptor model (no side enum needed)

**File**: `src/main/java/dev/breezes/settlements/infrastructure/network/packet/registry/ClientPacketHandlerBinding.java`

```java
public record ClientPacketHandlerBinding(
        Class<? extends ClientBoundPacket> packetClass,
        ClientSidePacketHandler<? extends ClientBoundPacket> handler
) {}
```

**File**: `src/main/java/dev/breezes/settlements/infrastructure/network/packet/registry/ServerPacketHandlerBinding.java`

```java
public record ServerPacketHandlerBinding(
        Class<? extends ServerBoundPacket> packetClass,
        ServerSidePacketHandler<? extends ServerBoundPacket> handler
) {}
```

Or skip binding records entirely and build the two maps directly in the processor.

> ✅ `PacketHandlingSide` enum is intentionally removed. Side is encoded by annotation type and target registry.

### 3) Processor/bootstrap

**File**: `src/main/java/dev/breezes/settlements/infrastructure/network/packet/registry/PacketHandlerAnnotationProcessor.java`

Responsibilities:

1. Scan mod classes via `ModFileScanData` for both:
   - `@HandleClientPacket`
   - `@HandleServerPacket`
2. Load handler class by reflection.
3. Instantiate handler (require public no-arg constructor).
4. Validate by annotation kind:
   - `@HandleClientPacket` => handler must implement `ClientSidePacketHandler<?>`
   - `@HandleServerPacket` => handler must implement `ServerSidePacketHandler<?>`
5. Build two maps (client and server), reject duplicates.
6. Publish immutable registries.

### 4) Registries

**File**: `.../packet/registry/ClientPacketHandlerRegistry.java`

```java
public final class ClientPacketHandlerRegistry {
    private static Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> handlers = Map.of();
    public static void initialize(Map<Class<? extends ClientBoundPacket>, ClientSidePacketHandler<? extends ClientBoundPacket>> map) { ... }
    public static <T extends ClientBoundPacket> ClientSidePacketHandler<T> get(Class<T> packetClass) { ... }
}
```

**File**: `.../packet/registry/ServerPacketHandlerRegistry.java`

```java
public final class ServerPacketHandlerRegistry {
    private static Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> handlers = Map.of();
    public static void initialize(Map<Class<? extends ServerBoundPacket>, ServerSidePacketHandler<? extends ServerBoundPacket>> map) { ... }
    public static <T extends ServerBoundPacket> ServerSidePacketHandler<T> get(Class<T> packetClass) { ... }
}
```

---

## Existing Class Changes

### 1) PacketRegistry (keep explicit wiring)

**File**: `infrastructure/network/packet/PacketRegistry.java`

Changes:

1. At start of `setupPackets(...)`, call processor initialization once:
   - `PacketHandlerAnnotationProcessor.initialize();`
2. Keep existing `registrar.playToClient/playToServer` explicit entries unchanged in phase 1.
3. Optional: add validation helper to assert each wired packet has a handler in corresponding registry.

### 2) Client receiver

**File**: `ClientSidePacketReceiver.java`

Changes:

1. Remove local static `HANDLERS` map.
2. Resolve with `ClientPacketHandlerRegistry.get(data.getClass())`.
3. Keep crash behavior (`MissingPacketHandlerCrashReport`) if missing.

### 3) Server receiver

**File**: `ServerSidePacketReceiver.java`

Changes:

1. Remove local static `HANDLERS` map.
2. Resolve with `ServerPacketHandlerRegistry.get(data.getClass())`.
3. Keep crash behavior if missing.

### 4) Handler classes

Add annotation to every handler class, e.g.:

```java
@HandleClientPacket(ClientBoundOpenBehaviorControllerPacket.class)
public class ClientBoundOpenBehaviorControllerPacketHandler implements ClientSidePacketHandler<ClientBoundOpenBehaviorControllerPacket> { ... }
```

```java
@HandleServerPacket(ServerBoundOpenBehaviorControllerPacket.class)
public class ServerBoundOpenBehaviorControllerPacketHandler implements ServerSidePacketHandler<ServerBoundOpenBehaviorControllerPacket> { ... }
```

---

## Validation & Crash Strategy

On startup (during annotation processing), crash with clear message if:

1. Duplicate handler binding for same packet class within client or server registry.
2. Handler annotation/interface mismatch.
3. Packet type mismatch for side.
4. Handler cannot be instantiated.
5. Missing handler for explicitly wired packet (optional phase-1 validation).

Can reuse `MissingPacketHandlerCrashReport` for missing mappings, and optionally add a dedicated:

- `InvalidPacketRegistrationCrashReport`

for annotation/contract violations.

---

## Sequence (Phase 1 Runtime)

1. NeoForge fires register payload handlers event.
2. `PacketRegistry.setupPackets(...)` runs.
3. `PacketHandlerAnnotationProcessor.initialize()` scans + validates + populates registries.
4. `PacketRegistry` explicitly wires packet payload IDs/codecs to receivers.
5. Receiver gets packet and resolves handler from side registry.
6. Missing/invalid -> crash report; found -> invoke handler.

---

## Migration Plan

### Phase 1 (recommended now)

1. Add annotation/processor/registries.
2. Annotate all existing handlers.
3. Move receiver lookup to registries.
4. Keep explicit payload wiring.

### Phase 2 (optional later)

1. Introduce packet annotation (`@RegisterPacket` on packet classes).
2. Auto-generate payload registration descriptors.
3. Replace manual wiring entries in `PacketRegistry`.

---

## Why this is clean architecture

- Composition root remains explicit (`PacketRegistry`).
- Reflection/annotation is constrained to infrastructure startup.
- Receivers become pure orchestration components.
- Registration concerns are centralized and testable.
- Developer ergonomics improve (add handler + annotation, no map edits).

---

## Queued Refactor Checklist (Do After UI Is Complete)

> Status: **Deferred** until Behavior UI work is finished.
> 
> Delivery strategy: small, reviewable, **separate commits**.

### Pre-refactor gate

- [ ] Behavior UI milestone complete/merged.
- [ ] Current networking branch rebased on latest main.
- [ ] `gradlew.bat compileJava --no-daemon` baseline passes before refactor.

### Commit 1 — Introduce annotations and registries only (no behavior change)

- [ ] Add `@HandleClientPacket` annotation.
- [ ] Add `@HandleServerPacket` annotation.
- [ ] Add `ClientPacketHandlerRegistry`.
- [ ] Add `ServerPacketHandlerRegistry`.
- [ ] Add scaffolding processor class (`PacketHandlerAnnotationProcessor`) with TODOs or minimal no-op init.
- [ ] Compile check.

### Commit 2 — Implement annotation scanning + validation

- [ ] Implement `ModFileScanData` scan for both annotations.
- [ ] Instantiate handlers and validate annotation/interface contract.
- [ ] Detect duplicate packet bindings and fail fast.
- [ ] Initialize client/server registries with immutable maps.
- [ ] Add startup log summary of resolved handler bindings.
- [ ] Compile check.

### Commit 3 — Migrate existing handlers to annotations

- [ ] Annotate all current client packet handlers with `@HandleClientPacket(...)`.
- [ ] Annotate all current server packet handlers with `@HandleServerPacket(...)`.
- [ ] Remove manual packet->handler maps from receivers only after annotations are complete.
- [ ] Compile check.

### Commit 4 — Receiver refactor to registry lookup

- [ ] Update `ClientSidePacketReceiver` to resolve via `ClientPacketHandlerRegistry`.
- [ ] Update `ServerSidePacketReceiver` to resolve via `ServerPacketHandlerRegistry`.
- [ ] Keep missing-handler crash behavior (`MissingPacketHandlerCrashReport`).
- [ ] Compile check.

### Commit 5 — Composition root hookup + safety checks

- [ ] In `PacketRegistry.setupPackets(...)`, call `PacketHandlerAnnotationProcessor.initialize()` once.
- [ ] Keep explicit `playToClient/playToServer` wiring unchanged (phase 1 scope).
- [ ] Optional guard: assert every wired packet has a corresponding handler binding.
- [ ] Compile check.

### Commit 6 — Cleanup + verification

- [ ] Remove dead code/imports from old map-based registration.
- [ ] Smoke test packet flows (open/close/heartbeat + bubble packets).
- [ ] Final compile check.
- [ ] Squash-fixups only, preserve commit boundaries above.

