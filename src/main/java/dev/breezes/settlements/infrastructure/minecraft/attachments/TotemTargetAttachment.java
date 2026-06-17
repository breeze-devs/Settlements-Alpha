package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.player.Player;

import java.util.OptionalInt;

/**
 * Tracks the villager a player has locked onto while channeling the villager totem.
 * <p>
 * The target is a transient, per-channel state owned by the player.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TotemTargetAttachment {

    /**
     * Sentinel for "no target". Entity ids are non-negative, so a negative value can never collide with a real target.
     */
    public static final int NO_TARGET = -1;

    public static void setTarget(Player player, int villagerEntityId) {
        player.setData(AttachmentRegistry.PLAYER_TOTEM_TARGET, villagerEntityId);
    }

    public static OptionalInt getTarget(Player player) {
        int entityId = player.getData(AttachmentRegistry.PLAYER_TOTEM_TARGET);
        return entityId < 0 ? OptionalInt.empty() : OptionalInt.of(entityId);
    }

    public static void clearTarget(Player player) {
        player.setData(AttachmentRegistry.PLAYER_TOTEM_TARGET, NO_TARGET);
    }

}
