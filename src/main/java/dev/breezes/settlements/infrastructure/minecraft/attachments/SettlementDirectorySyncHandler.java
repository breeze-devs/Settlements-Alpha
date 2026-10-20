package dev.breezes.settlements.infrastructure.minecraft.attachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Encodes and decodes full snapshots and idempotent incremental updates for a level's {@link SettlementDirectory}.
 */
public final class SettlementDirectorySyncHandler implements AttachmentSyncHandler<SettlementDirectory> {

    private static final StreamCodec<ByteBuf, Optional<SettlementSummary>> ENTRY_CODEC =
            ByteBufCodecs.optional(SettlementSummary.STREAM_CODEC);

    /**
     * Writes a full snapshot when initialSync is true; otherwise writes and clears the directory's pending changes.
     */
    @Override
    public void write(RegistryFriendlyByteBuf buf, SettlementDirectory directory, boolean initialSync) {
        // A same-dimension respawn retains the client's directory, so a snapshot must explicitly request replacement.
        // Even an update with no pending changes needs a complete payload: NeoForge does not suppress empty update writes.
        buf.writeBoolean(initialSync);
        buf.writeBoolean(directory.foundingAllowed());
        // Initial sync serves one client; clearing pending changes here would lose updates owed to other clients.
        Collection<UUID> settlementIds = initialSync ? directory.summaries().keySet() : directory.takeUnsent();
        buf.writeVarInt(settlementIds.size());
        // A client's snapshot may already contain changes that remained pending while no players were present.
        // Current values and explicit removals make those changes safe to apply again.
        for (UUID settlementId : settlementIds) {
            UUIDUtil.STREAM_CODEC.encode(buf, settlementId);
            ENTRY_CODEC.encode(buf, directory.find(settlementId));
        }
    }

    /**
     * Decodes a snapshot or applies an incremental update, returning a new directory without modifying previousValue.
     * If previousValue is null, an incremental update starts from an empty directory.
     */
    @Override
    public SettlementDirectory read(IAttachmentHolder holder,
                                    RegistryFriendlyByteBuf buf,
                                    @Nullable SettlementDirectory previousValue) {
        boolean whole = buf.readBoolean();
        boolean foundingAllowed = buf.readBoolean();
        Map<UUID, SettlementSummary> summaries = whole || previousValue == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(previousValue.summaries());
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            UUID settlementId = UUIDUtil.STREAM_CODEC.decode(buf);
            Optional<SettlementSummary> summary = ENTRY_CODEC.decode(buf);
            if (summary.isPresent()) {
                summaries.put(settlementId, summary.get());
            } else {
                summaries.remove(settlementId);
            }
        }
        return new SettlementDirectory(foundingAllowed, summaries);
    }

}
