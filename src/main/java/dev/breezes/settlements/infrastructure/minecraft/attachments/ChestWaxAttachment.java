package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Low-level accessor for the CHEST_WAXED attachment on a chest BlockEntity.
 * <p>
 * Higher-level callers should use {@link dev.breezes.settlements.infrastructure.minecraft.chest.ChestWaxService},
 * which handles double-chest partner propagation automatically.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChestWaxAttachment {

    public static boolean isWaxed(BlockEntity blockEntity) {
        return blockEntity.getData(AttachmentRegistry.CHEST_WAXED);
    }

    public static void setWaxed(BlockEntity blockEntity, boolean waxed) {
        blockEntity.setData(AttachmentRegistry.CHEST_WAXED, waxed);
        // Notify the chunk that this block entity's data changed so it is saved to disk.
        blockEntity.setChanged();
    }

}
