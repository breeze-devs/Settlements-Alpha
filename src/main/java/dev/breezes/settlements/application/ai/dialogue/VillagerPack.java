package dev.breezes.settlements.application.ai.dialogue;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft-free boundary type carrying a villager's pre-generated lines, grouped by occasion.
 * <p>
 * This type is produced by {@link MonologueRequestService} (which knows nothing about Minecraft)
 * and consumed by {@link RehearsedDialogueProvider#installOnePack}, keeping the install path
 * free of MC imports on the HTTP client's executor thread.
 */
@Builder
@Getter
public final class VillagerPack {

    private final UUID villagerId;

    /**
     * Lines are already sanitized and capped — no further processing is needed at draw time.
     */
    @Singular("linesByOccasion")
    private final Map<Occasion, List<String>> linesByOccasion;

}
