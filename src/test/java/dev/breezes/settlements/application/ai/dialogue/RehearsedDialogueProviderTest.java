package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RehearsedDialogueProvider}'s pack store, install, sample, and refresh logic.
 * No Minecraft objects; seeds state through the real epoch-guarded install path via {@link #seed}.
 */
@ExtendWith(MockitoExtension.class)
class RehearsedDialogueProviderTest {

    @Mock
    private DialogueProvider fallback;
    @Mock
    private MonologueRequestService monologueRequestService;

    private RehearsedDialogueProvider provider;

    @BeforeEach
    void setUp() {
        this.provider = new RehearsedDialogueProvider(fallback, monologueRequestService);
    }

    /**
     * Seeds packs at a fresh epoch through the real epoch-guarded install path, mirroring one
     * evening sweep installing its results (the provider no longer exposes a batch seed method).
     */
    private void seed(VillagerPack... packs) {
        long epoch = provider.rotateSweepHandle(InferenceStreamHandle.noOp());
        for (VillagerPack pack : packs) {
            provider.installOnePack(epoch, pack);
        }
    }

    // -----------------------------------------------------------------------
    // install + sampleAmbientLine — happy path
    // -----------------------------------------------------------------------

    @Test
    void sampleAmbientLine_matchingOccasionInstalled_returnsInstalledLine() {
        // Arrange
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.MORNING, List.of("Rise and shine!"))
                .build();
        seed(pack);

        DialogueContext context = DialogueContext.builder()
                .occasion(Occasion.MORNING)
                .build();

        // Act
        Optional<DialogueLine> line = provider.sampleAmbientLine(villagerUuid, context);

        // Assert
        assertTrue(line.isPresent());
        assertFalse(line.get() instanceof DialogueLine.Translatable, "Installed lines must be Literal, not Translatable");
        DialogueLine.Literal literal = (DialogueLine.Literal) line.get();
        assertEquals("Rise and shine!", literal.text());
    }

    @Test
    void sampleAmbientLine_differentOccasionRequested_fallsBackToScripted() {
        // Arrange — pack installed for MORNING, but we ask for EVENING
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.MORNING, List.of("Good morning!"))
                .build();
        seed(pack);

        DialogueLine.Translatable fallbackLine = (DialogueLine.Translatable) DialogueLine.translatable("dialogue.test.evening");
        DialogueContext context = DialogueContext.builder()
                .occasion(Occasion.EVENING)
                .build();
        when(fallback.sampleAmbientLine(eq(villagerUuid), any())).thenReturn(Optional.of(fallbackLine));

        // Act
        Optional<DialogueLine> line = provider.sampleAmbientLine(villagerUuid, context);

        // Assert — no EVENING pack, must fall back
        assertTrue(line.isPresent());
        assertTrue(line.get() instanceof DialogueLine.Translatable);
    }

    @Test
    void sampleAmbientLine_noPackInstalled_fallsBackToScripted() {
        // Arrange — no pack installed for this UUID at all
        UUID villagerUuid = UUID.randomUUID();
        DialogueLine.Translatable fallbackLine = (DialogueLine.Translatable) DialogueLine.translatable("dialogue.test.idle");
        DialogueContext context = DialogueContext.builder().occasion(Occasion.IDLE).build();
        when(fallback.sampleAmbientLine(eq(villagerUuid), any())).thenReturn(Optional.of(fallbackLine));

        // Act
        Optional<DialogueLine> line = provider.sampleAmbientLine(villagerUuid, context);

        // Assert
        assertTrue(line.isPresent());
        assertTrue(line.get() instanceof DialogueLine.Translatable);
    }

    @Test
    void sampleAmbientLine_packDepletedAfterAllDraws_fallsBackToScripted() {
        // Arrange — pack has only one line; draw it, then request again
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.IDLE, List.of("Only line"))
                .build();
        seed(pack);

        DialogueContext context = DialogueContext.builder().occasion(Occasion.IDLE).build();
        DialogueLine.Translatable fallbackLine = (DialogueLine.Translatable) DialogueLine.translatable("dialogue.fallback");
        when(fallback.sampleAmbientLine(eq(villagerUuid), any())).thenReturn(Optional.of(fallbackLine));

        // Act — first draw succeeds, second falls back
        Optional<DialogueLine> first = provider.sampleAmbientLine(villagerUuid, context);
        Optional<DialogueLine> second = provider.sampleAmbientLine(villagerUuid, context);

        // Assert
        assertTrue(first.isPresent());
        assertTrue(first.get() instanceof DialogueLine.Literal);
        assertTrue(second.isPresent());
        assertTrue(second.get() instanceof DialogueLine.Translatable);
    }

    // -----------------------------------------------------------------------
    // needsRefresh
    // -----------------------------------------------------------------------

    @Test
    void needsRefresh_noPackInstalled_returnsTrue() {
        // Arrange
        UUID villagerUuid = UUID.randomUUID();

        // Act + Assert
        assertTrue(provider.needsRefresh(villagerUuid));
    }

    @Test
    void needsRefresh_allCoreOccasionsInstalled_returnsFalse() {
        // Arrange — install a full core set; every core occasion has at least one line
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.IDLE, List.of("Idling"))
                .linesByOccasion(Occasion.WORK, List.of("Working"))
                .linesByOccasion(Occasion.MORNING, List.of("Morning"))
                .linesByOccasion(Occasion.EVENING, List.of("Evening"))
                .linesByOccasion(Occasion.REST_DAY, List.of("Rest day"))
                .build();
        seed(pack);

        // Act + Assert
        assertFalse(provider.needsRefresh(villagerUuid));
    }

    @Test
    void needsRefresh_installedSubsetAllFull_returnsFalse() {
        // Arrange — this is exactly the nitwit path: the service never requests WORK for nitwits,
        // so a nitwit's installed map legitimately omits it. needsRefresh must treat that as full,
        // not stale, since the provider only manages whatever was installed.
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.IDLE, List.of("Idling"))
                .linesByOccasion(Occasion.MORNING, List.of("Morning"))
                .linesByOccasion(Occasion.EVENING, List.of("Evening"))
                .linesByOccasion(Occasion.REST_DAY, List.of("Rest day"))
                .build();
        seed(pack);

        // Act + Assert — WORK absent but all installed packs are full → no refresh
        assertFalse(provider.needsRefresh(villagerUuid));
    }

    @Test
    void needsRefresh_coreOccasionPackDepleted_returnsTrue() {
        // Arrange — install a full pack, then exhaust IDLE by drawing it
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.IDLE, List.of("Only idle line"))
                .linesByOccasion(Occasion.WORK, List.of("Working"))
                .linesByOccasion(Occasion.MORNING, List.of("Morning"))
                .linesByOccasion(Occasion.EVENING, List.of("Evening"))
                .linesByOccasion(Occasion.REST_DAY, List.of("Rest day"))
                .build();
        seed(pack);
        // Drain IDLE
        provider.sampleAmbientLine(villagerUuid, DialogueContext.builder().occasion(Occasion.IDLE).build());

        // Act + Assert — IDLE is now depleted → needs refresh
        assertTrue(provider.needsRefresh(villagerUuid));
    }

    // -----------------------------------------------------------------------
    // new sweep — atomic replace
    // -----------------------------------------------------------------------

    @Test
    void newSweep_replacesExistingPackForSameVillager() {
        // Arrange — install a pack, then install a new one for the same villager
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack firstPack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.MORNING, List.of("Old morning line"))
                .linesByOccasion(Occasion.IDLE, List.of("Old idle line"))
                .linesByOccasion(Occasion.WORK, List.of("Old work line"))
                .linesByOccasion(Occasion.EVENING, List.of("Old evening line"))
                .linesByOccasion(Occasion.REST_DAY, List.of("Old rest day line"))
                .build();
        seed(firstPack);

        VillagerPack newPack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.MORNING, List.of("New morning line"))
                .linesByOccasion(Occasion.IDLE, List.of("New idle line"))
                .linesByOccasion(Occasion.WORK, List.of("New work line"))
                .linesByOccasion(Occasion.EVENING, List.of("New evening line"))
                .linesByOccasion(Occasion.REST_DAY, List.of("New rest day line"))
                .build();

        // Act
        seed(newPack);

        // Assert — new lines are served
        DialogueContext context = DialogueContext.builder().occasion(Occasion.MORNING).build();
        Optional<DialogueLine> line = provider.sampleAmbientLine(villagerUuid, context);
        assertTrue(line.isPresent());
        DialogueLine.Literal literal = (DialogueLine.Literal) line.get();
        assertEquals("New morning line", literal.text());
    }

    // -----------------------------------------------------------------------
    // isEnabled
    // -----------------------------------------------------------------------

    @Test
    void isEnabled_withPacksInstalled_returnsTrue() {
        // Arrange
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.IDLE, List.of("Some line"))
                .build();
        seed(pack);

        // Act + Assert
        assertTrue(provider.isEnabled());
    }

    @Test
    void isEnabled_noPacksButFallbackEnabled_returnsTrue() {
        // Arrange
        when(fallback.isEnabled()).thenReturn(true);

        // Act + Assert
        assertTrue(provider.isEnabled());
    }

    // -----------------------------------------------------------------------
    // H1 — epoch guard: stale results are dropped
    // -----------------------------------------------------------------------

    @Test
    void installOnePack_currentEpoch_installsAndIsServed() {
        // Arrange
        InferenceStreamHandle handle = mock(InferenceStreamHandle.class);
        long epoch = provider.rotateSweepHandle(handle); // epoch advances to 1

        UUID uuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(uuid)
                .linesByOccasion(Occasion.IDLE, List.of("Fresh line"))
                .build();

        // Act
        provider.installOnePack(epoch, pack);

        // Assert — pack installed; the Literal type confirms it came from the rehearsed store
        DialogueContext ctx = DialogueContext.builder().occasion(Occasion.IDLE).build();
        Optional<DialogueLine> line = provider.sampleAmbientLine(uuid, ctx);
        assertTrue(line.isPresent());
        assertTrue(line.get() instanceof DialogueLine.Literal, "Installed rehearsed lines must be Literal");
    }

    @Test
    void installOnePack_staleEpoch_dropsResult() {
        // Arrange — advance epoch to 2 by rotating twice
        InferenceStreamHandle h1 = mock(InferenceStreamHandle.class);
        provider.rotateSweepHandle(h1); // epoch = 1

        InferenceStreamHandle h2 = mock(InferenceStreamHandle.class);
        provider.rotateSweepHandle(h2); // epoch = 2

        UUID uuid = UUID.randomUUID();
        VillagerPack stale = VillagerPack.builder()
                .villagerId(uuid)
                .linesByOccasion(Occasion.IDLE, List.of("Stale line"))
                .build();

        DialogueLine fallbackLine = DialogueLine.translatable("dialogue.test.fallback");
        DialogueContext ctx = DialogueContext.builder().occasion(Occasion.IDLE).build();
        when(fallback.sampleAmbientLine(eq(uuid), any())).thenReturn(Optional.of(fallbackLine));

        // Act — try to install with epoch 1 (now stale; current is 2)
        provider.installOnePack(1, stale);

        // Assert — stale pack not installed; fallback (Translatable) is served
        Optional<DialogueLine> line = provider.sampleAmbientLine(uuid, ctx);
        assertTrue(line.isPresent());
        assertTrue(line.get() instanceof DialogueLine.Translatable,
                "Stale result must be dropped; fallback Translatable must be served");
    }

    // -----------------------------------------------------------------------
    // H1 — cancellation: a new sweep cancels the previous in-flight handle
    // -----------------------------------------------------------------------

    @Test
    void rotateSweepHandle_cancelsExistingHandle() {
        // Arrange — install an old handle as the current in-flight sweep
        InferenceStreamHandle oldHandle = mock(InferenceStreamHandle.class);
        provider.rotateSweepHandle(oldHandle); // epoch=1, oldHandle becomes inflight

        InferenceStreamHandle newHandle = mock(InferenceStreamHandle.class);

        // Act — rotating to a second handle should cancel the first
        provider.rotateSweepHandle(newHandle); // epoch=2

        // Assert — the old handle was cancelled
        verify(oldHandle).cancel();
    }

    // -----------------------------------------------------------------------
    // H2 — per-villager progressive install: each pack installs as it arrives
    // -----------------------------------------------------------------------

    @Test
    void installOnePack_multiplePacksAtCurrentEpoch_eachInstalledIndependently() {
        // Arrange
        InferenceStreamHandle handle = mock(InferenceStreamHandle.class);
        long epoch = provider.rotateSweepHandle(handle);

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        VillagerPack pack1 = VillagerPack.builder()
                .villagerId(uuid1)
                .linesByOccasion(Occasion.WORK, List.of("Pack one work"))
                .build();
        VillagerPack pack2 = VillagerPack.builder()
                .villagerId(uuid2)
                .linesByOccasion(Occasion.IDLE, List.of("Pack two idle"))
                .build();

        // Act — install arrives progressively, one at a time
        provider.installOnePack(epoch, pack1);
        provider.installOnePack(epoch, pack2);

        // Assert — both packs independently accessible
        DialogueContext workCtx = DialogueContext.builder().occasion(Occasion.WORK).build();
        DialogueContext idleCtx = DialogueContext.builder().occasion(Occasion.IDLE).build();

        Optional<DialogueLine> line1 = provider.sampleAmbientLine(uuid1, workCtx);
        Optional<DialogueLine> line2 = provider.sampleAmbientLine(uuid2, idleCtx);

        assertTrue(line1.isPresent());
        assertEquals("Pack one work", ((DialogueLine.Literal) line1.get()).text());
        assertTrue(line2.isPresent());
        assertEquals("Pack two idle", ((DialogueLine.Literal) line2.get()).text());
    }

    // -----------------------------------------------------------------------
    // evict — villager removal drops its packs (no unbounded growth across churn)
    // -----------------------------------------------------------------------

    @Test
    void evict_installedVillager_subsequentSampleFallsBack() {
        // Arrange — install a pack, then evict the villager as BaseVillager.remove would
        UUID villagerUuid = UUID.randomUUID();
        VillagerPack pack = VillagerPack.builder()
                .villagerId(villagerUuid)
                .linesByOccasion(Occasion.MORNING, List.of("Rise and shine!"))
                .build();
        seed(pack);

        DialogueLine.Translatable fallbackLine = (DialogueLine.Translatable) DialogueLine.translatable("dialogue.test.morning");
        DialogueContext context = DialogueContext.builder().occasion(Occasion.MORNING).build();
        when(fallback.sampleAmbientLine(eq(villagerUuid), any())).thenReturn(Optional.of(fallbackLine));

        // Act
        provider.evict(villagerUuid);

        // Assert — the rehearsed pack is gone; sampling falls through to the scripted floor
        Optional<DialogueLine> line = provider.sampleAmbientLine(villagerUuid, context);
        assertTrue(line.isPresent());
        assertTrue(line.get() instanceof DialogueLine.Translatable, "Evicted villager must fall back to scripted");
    }

    @Test
    void evict_forwardsToFallback() {
        // Arrange
        UUID villagerUuid = UUID.randomUUID();

        // Act
        provider.evict(villagerUuid);

        // Assert — the decorator forwards eviction to its fallback provider
        verify(fallback).evict(villagerUuid);
    }

}
