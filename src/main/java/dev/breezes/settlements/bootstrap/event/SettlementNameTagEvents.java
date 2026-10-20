package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.settlement.bell.SettlementBellRules;
import dev.breezes.settlements.di.ServerComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.settlement.naming.SettlementRename;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementClientView;
import dev.breezes.settlements.infrastructure.minecraft.attachments.SettlementDirectory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles renaming of the settlement claiming a bell through main-hand use of a named tag.
 * <p>
 * A successful rename consumes one tag. Unclaimed bells and unchanged names leave the interaction unhandled.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementNameTagEvents {

    // Run after higher-priority protection handlers so their cancellations and item-use denials prevent a rename
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(@Nonnull PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        // A main-hand bell interaction can consume the click before an off-hand tag is tried
        // This event precedes the game-mode checks, so enforce build permission before changing settlement state.
        // A protection handler can deny the item's use without cancelling the event, which leaves the bell free to ring.
        if (event.getHand() != InteractionHand.MAIN_HAND || !player.mayBuild() || event.getUseItem().isFalse()) {
            return;
        }

        ItemStack tag = event.getItemStack();
        Optional<String> name = SettlementBellRules.nameTagName(tag);
        Level level = event.getLevel();
        BlockPos bell = event.getPos();
        if (name.isEmpty() || !SettlementBellRules.isBell(level.getBlockState(bell))) {
            return;
        }

        // Remote clients have no server graph; predict from synced state and keep mutations on the server.
        boolean renamed = player instanceof ServerPlayer serverPlayer
                ? renameOnServer(serverPlayer, bell, name.get(), tag)
                : predictIfServerWillRename(level, bell, name.get());
        if (renamed) {
            // A consuming client result prevents an off-hand retry (P10), while cancellation suppresses bell use.
            // The client still sends the main-hand interaction to the server.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        }
    }

    private static boolean renameOnServer(ServerPlayer player, BlockPos bell, String name, ItemStack tag) {
        ServerComponent server = SettlementsDagger.serverOrNull();
        if (server == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        ChunkPosition chunk = new ChunkPosition(SectionPos.blockToSectionCoord(bell.getX()),
                SectionPos.blockToSectionCoord(bell.getZ()));
        boolean renamed = server.settlementStore().findClaiming(level, chunk)
                .flatMap(claimant -> server.settlementNamer().rename(level, claimant.id(), name, player))
                .filter(SettlementRename.Renamed.class::isInstance)
                .isPresent();
        if (renamed) {
            tag.consume(1, player);
        }
        return renamed;
    }

    /**
     * Predicts whether the requested name would change the settlement claiming the bell, using the given client
     * level's synced state.
     * <p>
     * Returns false when the claim or its settlement summary is unavailable. The server may reach a different
     * result if its state has changed.
     */
    private static boolean predictIfServerWillRename(Level level, BlockPos bell, String name) {
        Optional<SettlementDirectory> directory = SettlementClientView.directory(level);
        Optional<UUID> claimant = SettlementClientView.claimAt(level, bell);
        if (directory.isEmpty() || claimant.isEmpty()) {
            return false;
        }
        return directory.get().find(claimant.get())
                .map(summary -> SettlementRename.of(summary.name(), name, SettlementClientView.names(directory.get())))
                .filter(SettlementRename.Renamed.class::isInstance)
                .isPresent();
    }

}
