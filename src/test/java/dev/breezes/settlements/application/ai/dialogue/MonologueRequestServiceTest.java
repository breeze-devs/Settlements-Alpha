package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.inference.InferenceStreamHandle;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueGateway;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.application.ai.inference.monologue.VillagerMonologueResult;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MonologueRequestService}'s pure conversion path
 * ({@link MonologueRequestService#toPack(VillagerMonologueResult)}) and the empty-input
 * short-circuit of {@link MonologueRequestService#requestPacksStreaming}.
 * <p>
 * Lines arrive from the backend as bare strings — a filtered line is omitted server-side, so the
 * mod sanitizes (strips formatting, clamps length) and prunes empty buckets/villagers but never
 * drops by status. No Minecraft objects; pure domain logic.
 */
@ExtendWith(MockitoExtension.class)
class MonologueRequestServiceTest {

    @Mock
    private MonologueRequestAssembler assembler;
    @Mock
    private MonologueGateway gateway;

    private MonologueRequestService service;

    @BeforeEach
    void setUp() {
        OccasionSetResolver occasionSetResolver = new OccasionSetResolver();
        DialogueConfig config = new DialogueConfig("REHEARSED", true, 120, 12, 30);
        this.service = new MonologueRequestService(assembler, gateway, occasionSetResolver, config);
    }

    @Test
    void toPack_validLine_mappedToCorrectOccasionBucket() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .bucket(Occasion.MORNING, List.of("Good morning!"))
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert
        assertTrue(pack.isPresent());
        assertEquals(uuid, pack.get().getVillagerId());
        assertEquals(List.of("Good morning!"), pack.get().getLinesByOccasion().get(Occasion.MORNING));
    }

    @Test
    void toPack_sanitizeApplied_stripsFormattingCodes() {
        // Arrange — line carries formatting codes that must be stripped before install
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .bucket(Occasion.EVENING, List.of("§aThis is coloured text"))
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert
        assertTrue(pack.isPresent());
        String installedLine = pack.get().getLinesByOccasion().get(Occasion.EVENING).get(0);
        assertFalse(installedLine.contains("§"), "Formatting codes must be stripped before install");
        assertTrue(installedLine.contains("This is coloured text"));
    }

    @Test
    void toPack_multipleOccasions_allMappedCorrectly() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .bucket(Occasion.IDLE, List.of("Just standing here"))
                .bucket(Occasion.WORK, List.of("Back to work"))
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert
        assertTrue(pack.isPresent());
        assertEquals(2, pack.get().getLinesByOccasion().size());
        assertEquals(List.of("Just standing here"), pack.get().getLinesByOccasion().get(Occasion.IDLE));
        assertEquals(List.of("Back to work"), pack.get().getLinesByOccasion().get(Occasion.WORK));
    }

    @Test
    void toPack_allLinesSanitizedToBlank_returnsEmpty() {
        // Arrange — pure formatting codes sanitize to blank, so no line survives
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .bucket(Occasion.IDLE, List.of("§r§l"))
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert — a villager with no surviving lines is dropped entirely
        assertTrue(pack.isEmpty());
    }

    @Test
    void toPack_emptyBucket_returnsEmpty() {
        // Arrange — the only bucket arrives empty (e.g. SIS omitted every line for it)
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .bucket(Occasion.MORNING, List.of())
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert
        assertTrue(pack.isEmpty());
    }

    @Test
    void toPack_noBuckets_returnsEmpty() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        VillagerMonologueResult result = VillagerMonologueResult.builder()
                .villagerId(uuid)
                .build();

        // Act
        Optional<VillagerPack> pack = service.toPack(result);

        // Assert
        assertTrue(pack.isEmpty());
    }

    @Test
    void requestPacksStreaming_emptyCollection_returnsNoOpHandle() {
        // Arrange
        Collection<BaseVillager> empty = List.of();

        // Act
        InferenceStreamHandle handle = service.requestPacksStreaming(empty, ignored -> {
        });

        // Assert — noOp: completion already done, cancel is safe to call
        assertTrue(handle.completion().isDone());
        handle.cancel(); // must not throw
    }

}
