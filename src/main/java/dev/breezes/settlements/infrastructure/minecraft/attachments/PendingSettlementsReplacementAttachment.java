package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;

/**
 * Low-level accessor for the {@link AttachmentRegistry#VILLAGER_PENDING_SETTLEMENTS_REPLACEMENT}
 * marker on a villager.
 * <p>
 * The marker records a "replace this villager with a Settlements villager" decision taken
 * during village generation. That decision can be made inside a {@code WorldGenRegion} — potentially
 * many ticks, a save/unload, or an entire server session before the villager next enters a live ServerLevel,
 * so the marker is serialized to survive the gap and still drive the deferred
 * conversion. {@link #clear} strips it just before the conversion copies the villager's NBT, keeping
 * the resulting Settlements villager free of the flag.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class PendingSettlementsReplacementAttachment {

    public static boolean isPending(LivingEntity entity) {
        return entity.getData(AttachmentRegistry.VILLAGER_PENDING_SETTLEMENTS_REPLACEMENT);
    }

    public static void mark(LivingEntity entity) {
        entity.setData(AttachmentRegistry.VILLAGER_PENDING_SETTLEMENTS_REPLACEMENT, true);
    }

    public static void clear(LivingEntity entity) {
        entity.setData(AttachmentRegistry.VILLAGER_PENDING_SETTLEMENTS_REPLACEMENT, false);
    }

}
