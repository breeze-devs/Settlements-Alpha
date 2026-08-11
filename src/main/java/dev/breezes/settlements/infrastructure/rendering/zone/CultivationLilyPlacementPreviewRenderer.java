package dev.breezes.settlements.infrastructure.rendering.zone;

import dev.breezes.settlements.di.ClientScope;
import dev.breezes.settlements.domain.farming.CultivationZone;
import dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation.CultivationLilyHeldPlacementFrame;
import dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation.CultivationLilyPlacementCandidate;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * The footprint a held Cultivation Lily, in either hand, would claim if placed right now, and the cell the
 * pad itself would land on.
 */
@ClientSide
@ClientScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class CultivationLilyPlacementPreviewRenderer {

    private static final float FULLY_OPAQUE = 1.0F;

    /**
     * How tall is the center placement preview halo.
     */
    private static final double CENTER_MARKER_REACH_BLOCKS = 0.1D;

    private final CultivationLilyHeldPlacementFrame heldPlacementFrame;

    public void render(@Nonnull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Optional<CultivationLilyPlacementCandidate> candidate = heldPlacementFrame.resolve(level, player, partialTick);
        if (candidate.isEmpty()) {
            return;
        }

        BlockPos placementPos = candidate.get().placementPos();
        Vector3f color = CultivationZoneStyle.prospectiveOutlineColor();
        long gameTime = level.getGameTime();

        ZoneWallRenderer.render(event, CultivationZoneGeometry.canopyPlane(CultivationZone.atDefault(placementPos)),
                color, FULLY_OPAQUE, gameTime);
        ZoneWallRenderer.render(event, CultivationZoneGeometry.centerCellPlane(placementPos),
                color, FULLY_OPAQUE, gameTime, CENTER_MARKER_REACH_BLOCKS);
    }

}
