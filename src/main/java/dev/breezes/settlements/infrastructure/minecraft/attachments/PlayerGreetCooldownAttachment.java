package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;

/**
 * Transient per-player state that prevents an entire crowd from greeting the same player
 * simultaneously. The first villager to greet stamps the cooldown; all subsequent villagers
 * observe it and stay quiet until the window expires. Not serialized: resets on relog.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PlayerGreetCooldownAttachment {

    public static boolean canBeGreeted(Player player, long gameTime) {
        return gameTime >= player.getData(AttachmentRegistry.PLAYER_GREET_COOLDOWN);
    }

    public static void markGreeted(Player player, long nextGreetableGameTick) {
        player.setData(AttachmentRegistry.PLAYER_GREET_COOLDOWN, nextGreetableGameTick);
    }

}
