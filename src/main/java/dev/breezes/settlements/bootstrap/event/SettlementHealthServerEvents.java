package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.bell.SettlementBellRules;
import dev.breezes.settlements.application.settlement.health.SettlementHealthService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.world.ChunkPosition;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Drives each server level's settlement health cadence, and revives a fading settlement as soon as a bell placed in its
 * claim makes it healthy.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementHealthServerEvents {

    private final SettlementHealthService healthService;

    @SubscribeEvent
    public void onLevelTickPost(LevelTickEvent.Post event) {
        // Only tick on server level
        if (event.getLevel() instanceof ServerLevel level) {
            this.healthService.tick(level);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        // Only tick on server level
        if (event.getLevel() instanceof ServerLevel level) {
            this.healthService.forgetLevel(level);
        }
    }

    // Lowest priority, other mods that may cancel the placement run first
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && SettlementBellRules.isBell(event.getPlacedBlock())) {
            BlockPos bell = event.getPos();
            this.healthService.reviveForBell(level, new ChunkPosition(SectionPos.blockToSectionCoord(bell.getX()),
                    SectionPos.blockToSectionCoord(bell.getZ())));
        }
    }

}
