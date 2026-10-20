package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What a client was told about the settlements of its level, read from {@link AttachmentRegistry#SETTLEMENT_CLAIM} and
 * {@link AttachmentRegistry#SETTLEMENT_DIRECTORY} alone.
 * <p>
 * Every reader takes a client level. Server code reads the settlement store instead, which the attachments are derived
 * from.
 */
@ClientSide
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementClientView {

    /**
     * Returns the directory the server last sent for the level, or empty until the server's first sync.
     * Each sync replaces the directory, so a caller looks it up again rather than keeping it.
     */
    public static Optional<SettlementDirectory> directory(@Nonnull Level level) {
        return level.getExistingData(AttachmentRegistry.SETTLEMENT_DIRECTORY);
    }

    /**
     * Returns the id of the settlement claiming the chunk, or empty when no settlement claims it.
     * A chunk this client does not hold reads as unclaimed.
     */
    public static Optional<UUID> claimAt(@Nonnull Level level, @Nonnull BlockPos position) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ()));
        return chunk == null ? Optional.empty() : claimOf(chunk);
    }

    /**
     * Returns the id of the settlement claiming a chunk this client holds, or empty when no settlement claims it.
     */
    public static Optional<UUID> claimOf(@Nonnull LevelChunk chunk) {
        return chunk.getExistingData(AttachmentRegistry.SETTLEMENT_CLAIM);
    }

    /**
     * Returns the name of every settlement in the directory, which are the names of every settlement the level stores.
     */
    public static List<String> names(@Nonnull SettlementDirectory directory) {
        return directory.summaries().values().stream()
                .map(SettlementSummary::name)
                .toList();
    }

}
