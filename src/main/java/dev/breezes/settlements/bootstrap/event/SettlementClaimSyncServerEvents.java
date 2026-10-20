package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.persistence.SettlementStoreListener;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps {@link AttachmentRegistry#SETTLEMENT_CLAIM} matching the settlement store on every chunk a player watches.
 */
@ServerSide
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementClaimSyncServerEvents implements SettlementStoreListener {

    private final SettlementStore settlementStore;

    // Reconciled on each watch, which precedes the chunk's send, rather than once at load. A chunk that drops below full
    // status stays in memory out of getChunkNow's reach and returns to full with no second load event, so a claim set
    // only at load would go out stale after a change made in between.
    @SubscribeEvent
    public void onChunkWatch(@Nonnull ChunkWatchEvent.Watch event) {
        LevelChunk chunk = event.getChunk();
        ChunkPos position = chunk.getPos();
        Optional<UUID> claimant = this.settlementStore.findClaiming(event.getLevel(),
                        new ChunkPosition(position.x, position.z))
                .map(Settlement::id);
        // Every write is sent to the players already watching the chunk, so a claim that already matches is left alone
        if (claimant.equals(chunk.getExistingData(AttachmentRegistry.SETTLEMENT_CLAIM))) {
            return;
        }

        // Direct holder writes preserve sync without marking the chunk unsaved for this transient claim
        claimant.ifPresentOrElse(
                id -> chunk.getAttachmentHolder().setData(AttachmentRegistry.SETTLEMENT_CLAIM.get(), id),
                () -> chunk.getAttachmentHolder().removeData(AttachmentRegistry.SETTLEMENT_CLAIM.get()));
    }

    @Override
    public void onSettlementChanged(@Nonnull ServerLevel level, @Nullable Settlement before, @Nullable Settlement after) {
        // Ignore updates because a stored settlement's claim cannot change.
        // A player holds only chunks at full status, which getChunkNow reaches; any other chunk is reconciled when next
        // watched, so no terrain is loaded for a claim change.
        if (before == null && after != null) {
            for (ChunkPosition claimed : after.bounds().chunks()) {
                LevelChunk chunk = fullChunk(level, claimed);
                if (chunk != null) {
                    chunk.getAttachmentHolder().setData(AttachmentRegistry.SETTLEMENT_CLAIM.get(), after.id());
                }
            }
        } else if (before != null && after == null) {
            for (ChunkPosition claimed : before.bounds().chunks()) {
                LevelChunk chunk = fullChunk(level, claimed);
                if (chunk != null) {
                    chunk.getAttachmentHolder().removeData(AttachmentRegistry.SETTLEMENT_CLAIM.get());
                }
            }
        }
    }

    /**
     * Returns the chunk if it is at full status, without loading it, or null.
     */
    @Nullable
    private static LevelChunk fullChunk(ServerLevel level, ChunkPosition chunk) {
        return level.getChunkSource().getChunkNow(chunk.x(), chunk.z());
    }

}
